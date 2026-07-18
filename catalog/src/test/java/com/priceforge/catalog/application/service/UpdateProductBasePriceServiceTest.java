package com.priceforge.catalog.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.priceforge.catalog.application.dto.CreateProductCommand;
import com.priceforge.catalog.application.dto.DeactivateProductCommand;
import com.priceforge.catalog.application.dto.UpdateProductBasePriceCommand;
import com.priceforge.catalog.application.dto.UpdateProductBasePriceResult;
import com.priceforge.catalog.application.service.support.InMemoryProductRepositoryPort;
import com.priceforge.catalog.application.service.support.RecordingEventPublisher;
import com.priceforge.catalog.domain.exception.ProductNotActiveException;
import com.priceforge.catalog.domain.exception.ProductNotFoundException;
import com.priceforge.catalog.domain.model.ProductPriceBaseUpdated;
import com.priceforge.sharedkernel.ProductId;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class UpdateProductBasePriceServiceTest {

    private final InMemoryProductRepositoryPort productRepository = new InMemoryProductRepositoryPort();
    private final RecordingEventPublisher eventPublisher = new RecordingEventPublisher();
    private final CreateProductService createProductService =
        new CreateProductService(productRepository, eventPublisher);
    private final UpdateProductBasePriceService service =
        new UpdateProductBasePriceService(productRepository, eventPublisher);

    @Test
    void updatesTheBasePriceAndPublishesProductPriceBaseUpdated() {
        ProductId productId = createProductService.execute(
            new CreateProductCommand("SKU-1", "Widget", "A widget", BigDecimal.TEN, "EUR", "URBAN", null))
            .productId();
        eventPublisher.publishedEvents().clear();

        UpdateProductBasePriceResult result = service.execute(
            new UpdateProductBasePriceCommand(productId, BigDecimal.valueOf(20), "admin@priceforge.com"));

        assertThat(result.previousBasePriceAmount()).isEqualByComparingTo(BigDecimal.TEN);
        assertThat(result.newBasePriceAmount()).isEqualByComparingTo(BigDecimal.valueOf(20));
        assertThat(eventPublisher.publishedEvents()).hasSize(1);
        assertThat(eventPublisher.publishedEvents().get(0)).isInstanceOf(ProductPriceBaseUpdated.class);
    }

    @Test
    void rejectsUpdatingAProductThatDoesNotExist() {
        assertThatThrownBy(() -> service.execute(
                new UpdateProductBasePriceCommand(ProductId.newId(), BigDecimal.TEN, "admin@priceforge.com")))
            .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void rejectsUpdatingAnInactiveProduct() {
        ProductId productId = createProductService.execute(
            new CreateProductCommand("SKU-1", "Widget", "A widget", BigDecimal.TEN, "EUR", "URBAN", null))
            .productId();
        new DeactivateProductService(productRepository, eventPublisher)
            .execute(new DeactivateProductCommand(productId));

        assertThatThrownBy(() -> service.execute(
                new UpdateProductBasePriceCommand(productId, BigDecimal.valueOf(20), "admin@priceforge.com")))
            .isInstanceOf(ProductNotActiveException.class);
    }
}
