package com.priceforge.catalog.application.port.out;

import com.priceforge.catalog.domain.model.Product;
import com.priceforge.sharedkernel.ProductId;
import java.math.BigDecimal;
import java.util.Optional;

public interface ProductRepositoryPort {

    Product save(Product product);

    boolean existsBySku(String sku);

    Optional<Product> findById(ProductId id);

    /**
     * Cheapest base price among ACTIVE products, scoped to {@code category} if present or global
     * otherwise. Compares raw amounts regardless of currency — a conservative heuristic, not an
     * exact cross-currency comparison (the platform never converts currencies).
     */
    Optional<BigDecimal> findCheapestActiveBasePriceAmount(Optional<String> category);
}
