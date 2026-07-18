package com.priceforge.catalog.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.priceforge.catalog.application.dto.ChangeProductCategoryCommand;
import com.priceforge.catalog.application.dto.ChangeProductCategoryResult;
import com.priceforge.catalog.application.dto.CreateProductCommand;
import com.priceforge.catalog.application.service.support.InMemoryProductRepositoryPort;
import com.priceforge.catalog.application.service.support.RecordingEventPublisher;
import com.priceforge.catalog.domain.exception.ProductNotFoundException;
import com.priceforge.catalog.domain.model.ProductCategoryChanged;
import com.priceforge.sharedkernel.ProductId;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ChangeProductCategoryServiceTest {

    private final InMemoryProductRepositoryPort productRepository = new InMemoryProductRepositoryPort();
    private final RecordingEventPublisher eventPublisher = new RecordingEventPublisher();
    private final CreateProductService createProductService =
        new CreateProductService(productRepository, eventPublisher);
    private final ChangeProductCategoryService service =
        new ChangeProductCategoryService(productRepository, eventPublisher);

    @Test
    void changesTheCategoryAndPublishesProductCategoryChanged() {
        ProductId productId = createProductService.execute(
            new CreateProductCommand("SKU-1", "Widget", "A widget", BigDecimal.TEN, "EUR", "URBAN", null))
            .productId();
        eventPublisher.publishedEvents().clear();

        ChangeProductCategoryResult result =
            service.execute(new ChangeProductCategoryCommand(productId, "RURAL"));

        assertThat(result.previousCategory()).isEqualTo("URBAN");
        assertThat(result.newCategory()).isEqualTo("RURAL");
        assertThat(eventPublisher.publishedEvents()).hasSize(1);
        assertThat(eventPublisher.publishedEvents().get(0)).isInstanceOf(ProductCategoryChanged.class);
    }

    @Test
    void rejectsCategorizingAProductThatDoesNotExist() {
        assertThatThrownBy(() -> service.execute(
                new ChangeProductCategoryCommand(ProductId.newId(), "RURAL")))
            .isInstanceOf(ProductNotFoundException.class);
    }
}
