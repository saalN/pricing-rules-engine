package com.priceforge.catalog.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.priceforge.catalog.application.dto.CreateProductCommand;
import com.priceforge.catalog.application.dto.FindCheapestActiveBasePriceQuery;
import com.priceforge.catalog.application.service.support.InMemoryProductRepositoryPort;
import com.priceforge.catalog.application.service.support.RecordingEventPublisher;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class FindCheapestActiveBasePriceServiceTest {

    private final InMemoryProductRepositoryPort productRepository = new InMemoryProductRepositoryPort();
    private final CreateProductService createProductService =
        new CreateProductService(productRepository, new RecordingEventPublisher());
    private final FindCheapestActiveBasePriceService service =
        new FindCheapestActiveBasePriceService(productRepository);

    @Test
    void returnsTheCheapestActivePriceInACategory() {
        createProductService.execute(
            new CreateProductCommand("SKU-1", "Widget", null, BigDecimal.valueOf(20), "EUR", "URBAN", null));
        createProductService.execute(
            new CreateProductCommand("SKU-2", "Gadget", null, BigDecimal.valueOf(5), "EUR", "URBAN", null));
        createProductService.execute(
            new CreateProductCommand("SKU-3", "Gizmo", null, BigDecimal.ONE, "EUR", "RURAL", null));

        var result = service.execute(new FindCheapestActiveBasePriceQuery("URBAN"));

        assertThat(result.cheapestBasePriceAmount()).isEqualByComparingTo(BigDecimal.valueOf(5));
    }

    @Test
    void returnsTheGlobalCheapestPriceWhenCategoryIsNull() {
        createProductService.execute(
            new CreateProductCommand("SKU-1", "Widget", null, BigDecimal.valueOf(20), "EUR", "URBAN", null));
        createProductService.execute(
            new CreateProductCommand("SKU-2", "Gizmo", null, BigDecimal.ONE, "EUR", "RURAL", null));

        var result = service.execute(new FindCheapestActiveBasePriceQuery(null));

        assertThat(result.cheapestBasePriceAmount()).isEqualByComparingTo(BigDecimal.ONE);
    }

    @Test
    void returnsNullWhenNoActiveProductMatchesTheScope() {
        var result = service.execute(new FindCheapestActiveBasePriceQuery("ZONE_X"));

        assertThat(result.cheapestBasePriceAmount()).isNull();
    }
}
