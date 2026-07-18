package com.priceforge.catalog.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.priceforge.catalog.domain.exception.InvalidBasePriceException;
import com.priceforge.sharedkernel.Money;
import com.priceforge.sharedkernel.ProductId;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ProductTest {

    @Test
    void createsAProductWithActiveStatusAndAPositiveBasePrice() {
        Product product = Product.create(
            ProductId.newId(),
            "SKU-1",
            "Widget",
            "A widget",
            Money.of(BigDecimal.TEN, "EUR"),
            "URBAN",
            Map.of("weight_kg", 1.5),
            Instant.now());

        assertThat(product.status()).isEqualTo(ProductStatus.ACTIVE);
        assertThat(product.sku()).isEqualTo("SKU-1");
        assertThat(product.basePrice()).isEqualTo(Money.of(BigDecimal.TEN, "EUR"));
        assertThat(product.customAttributes()).containsEntry("weight_kg", 1.5);
    }

    @Test
    void rejectsAZeroBasePrice() {
        assertThatThrownBy(() -> Product.create(
                ProductId.newId(), "SKU-1", "Widget", "A widget",
                Money.of(BigDecimal.ZERO, "EUR"), "URBAN", Map.of(), Instant.now()))
            .isInstanceOf(InvalidBasePriceException.class);
    }

    @Test
    void rejectsANegativeBasePrice() {
        assertThatThrownBy(() -> Product.create(
                ProductId.newId(), "SKU-1", "Widget", "A widget",
                Money.of(BigDecimal.valueOf(-1), "EUR"), "URBAN", Map.of(), Instant.now()))
            .isInstanceOf(InvalidBasePriceException.class);
    }
}
