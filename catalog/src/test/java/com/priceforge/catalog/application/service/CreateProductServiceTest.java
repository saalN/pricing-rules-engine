package com.priceforge.catalog.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.priceforge.catalog.application.dto.CreateProductCommand;
import com.priceforge.catalog.application.dto.CreateProductResult;
import com.priceforge.catalog.application.port.out.ProductRepositoryPort;
import com.priceforge.catalog.domain.exception.DuplicateSkuException;
import com.priceforge.catalog.domain.exception.InvalidBasePriceException;
import com.priceforge.catalog.domain.model.Product;
import com.priceforge.catalog.domain.model.ProductCreated;
import com.priceforge.sharedkernel.DomainEvent;
import com.priceforge.sharedkernel.EventPublisher;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CreateProductServiceTest {

    private final InMemoryProductRepository productRepository = new InMemoryProductRepository();
    private final RecordingEventPublisher eventPublisher = new RecordingEventPublisher();
    private final CreateProductService service =
        new CreateProductService(productRepository, eventPublisher);

    private CreateProductCommand validCommand;

    @BeforeEach
    void setUp() {
        validCommand = new CreateProductCommand(
            "SKU-1", "Widget", "A widget", BigDecimal.TEN, "EUR", "URBAN", null);
    }

    @Test
    void createsAndPersistsTheProductAndPublishesProductCreated() {
        CreateProductResult result = service.execute(validCommand);

        assertThat(result.productId()).isNotNull();
        assertThat(productRepository.existsBySku("SKU-1")).isTrue();
        assertThat(eventPublisher.publishedEvents()).hasSize(1);
        assertThat(eventPublisher.publishedEvents().get(0)).isInstanceOf(ProductCreated.class);
    }

    @Test
    void rejectsADuplicateSku() {
        productRepository.existingSkus.add("SKU-1");

        assertThatThrownBy(() -> service.execute(validCommand))
            .isInstanceOf(DuplicateSkuException.class);
        assertThat(eventPublisher.publishedEvents()).isEmpty();
    }

    @Test
    void rejectsANonPositiveBasePrice() {
        CreateProductCommand invalidCommand = new CreateProductCommand(
            "SKU-1", "Widget", "A widget", BigDecimal.ZERO, "EUR", "URBAN", null);

        assertThatThrownBy(() -> service.execute(invalidCommand))
            .isInstanceOf(InvalidBasePriceException.class);
        assertThat(eventPublisher.publishedEvents()).isEmpty();
    }

    private static final class InMemoryProductRepository implements ProductRepositoryPort {

        private final Set<String> existingSkus = new HashSet<>();

        @Override
        public Product save(Product product) {
            existingSkus.add(product.sku());
            return product;
        }

        @Override
        public boolean existsBySku(String sku) {
            return existingSkus.contains(sku);
        }
    }

    private static final class RecordingEventPublisher implements EventPublisher {

        private final List<DomainEvent> publishedEvents = new ArrayList<>();

        @Override
        public void publish(DomainEvent event) {
            publishedEvents.add(event);
        }

        private List<DomainEvent> publishedEvents() {
            return publishedEvents;
        }
    }
}
