package com.priceforge.catalog.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.priceforge.catalog.application.dto.CreateProductCommand;
import com.priceforge.catalog.application.dto.DeactivateProductCommand;
import com.priceforge.catalog.application.dto.DeactivateProductResult;
import com.priceforge.catalog.application.service.support.InMemoryProductRepositoryPort;
import com.priceforge.catalog.application.service.support.RecordingEventPublisher;
import com.priceforge.catalog.domain.exception.ProductNotFoundException;
import com.priceforge.catalog.domain.model.ProductDeactivated;
import com.priceforge.catalog.domain.model.ProductStatus;
import com.priceforge.sharedkernel.ProductId;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class DeactivateProductServiceTest {

    private final InMemoryProductRepositoryPort productRepository = new InMemoryProductRepositoryPort();
    private final RecordingEventPublisher eventPublisher = new RecordingEventPublisher();
    private final CreateProductService createProductService =
        new CreateProductService(productRepository, eventPublisher);
    private final DeactivateProductService service =
        new DeactivateProductService(productRepository, eventPublisher);

    @Test
    void deactivatesTheProductAndPublishesProductDeactivated() {
        ProductId productId = createProductService.execute(
            new CreateProductCommand("SKU-1", "Widget", "A widget", BigDecimal.TEN, "EUR", "URBAN", null))
            .productId();
        eventPublisher.publishedEvents().clear();

        DeactivateProductResult result = service.execute(new DeactivateProductCommand(productId));

        assertThat(result.status()).isEqualTo(ProductStatus.INACTIVE);
        assertThat(eventPublisher.publishedEvents()).hasSize(1);
        assertThat(eventPublisher.publishedEvents().get(0)).isInstanceOf(ProductDeactivated.class);
    }

    @Test
    void rejectsDeactivatingAProductThatDoesNotExist() {
        assertThatThrownBy(() -> service.execute(new DeactivateProductCommand(ProductId.newId())))
            .isInstanceOf(ProductNotFoundException.class);
    }
}
