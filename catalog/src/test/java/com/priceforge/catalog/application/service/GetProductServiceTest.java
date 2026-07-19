package com.priceforge.catalog.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.priceforge.catalog.application.dto.CreateProductCommand;
import com.priceforge.catalog.application.dto.CreateProductResult;
import com.priceforge.catalog.application.dto.GetProductQuery;
import com.priceforge.catalog.application.dto.GetProductResult;
import com.priceforge.catalog.application.service.support.InMemoryProductRepositoryPort;
import com.priceforge.catalog.application.service.support.RecordingEventPublisher;
import com.priceforge.sharedkernel.ProductId;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GetProductServiceTest {

    private final InMemoryProductRepositoryPort productRepository = new InMemoryProductRepositoryPort();
    private final CreateProductService createProductService =
        new CreateProductService(productRepository, new RecordingEventPublisher());
    private final GetProductService service = new GetProductService(productRepository);

    @Test
    void returnsTheProductSnapshotWhenItExists() {
        CreateProductResult created = createProductService.execute(
            new CreateProductCommand("SKU-1", "Widget", "d", BigDecimal.TEN, "EUR", "URBAN", null));

        Optional<GetProductResult> result = service.execute(new GetProductQuery(created.productId()));

        assertThat(result).isPresent();
        assertThat(result.get().basePriceAmount()).isEqualByComparingTo(BigDecimal.TEN);
        assertThat(result.get().currency()).isEqualTo("EUR");
        assertThat(result.get().category()).isEqualTo("URBAN");
        assertThat(result.get().status()).isEqualTo("ACTIVE");
    }

    @Test
    void returnsEmptyWhenTheProductDoesNotExist() {
        assertThat(service.execute(new GetProductQuery(ProductId.newId()))).isEmpty();
    }
}
