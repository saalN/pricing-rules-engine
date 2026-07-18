package com.priceforge.catalog.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.priceforge.catalog.application.dto.CreateProductCommand;
import com.priceforge.catalog.application.dto.CreateProductResult;
import com.priceforge.catalog.application.service.support.InMemoryProductRepositoryPort;
import com.priceforge.catalog.application.service.support.RecordingEventPublisher;
import com.priceforge.catalog.domain.exception.DuplicateSkuException;
import com.priceforge.catalog.domain.exception.InvalidBasePriceException;
import com.priceforge.catalog.domain.model.ProductCreated;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CreateProductServiceTest {

    private final InMemoryProductRepositoryPort productRepository = new InMemoryProductRepositoryPort();
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
        service.execute(validCommand);
        eventPublisher.publishedEvents().clear();

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
}
