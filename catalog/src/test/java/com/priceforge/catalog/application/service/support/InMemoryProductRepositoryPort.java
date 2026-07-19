package com.priceforge.catalog.application.service.support;

import com.priceforge.catalog.application.port.out.ProductRepositoryPort;
import com.priceforge.catalog.domain.model.Product;
import com.priceforge.catalog.domain.model.ProductStatus;
import com.priceforge.sharedkernel.ProductId;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class InMemoryProductRepositoryPort implements ProductRepositoryPort {

    private final Map<ProductId, Product> productsById = new HashMap<>();

    @Override
    public Product save(Product product) {
        productsById.put(product.id(), product);
        return product;
    }

    @Override
    public boolean existsBySku(String sku) {
        return productsById.values().stream().anyMatch(product -> product.sku().equals(sku));
    }

    @Override
    public Optional<Product> findById(ProductId id) {
        return Optional.ofNullable(productsById.get(id));
    }

    @Override
    public Optional<BigDecimal> findCheapestActiveBasePriceAmount(Optional<String> category) {
        return productsById.values().stream()
            .filter(product -> product.status() == ProductStatus.ACTIVE)
            .filter(product -> category.isEmpty() || Objects.equals(product.category(), category.get()))
            .map(product -> product.basePrice().amount())
            .min(BigDecimal::compareTo);
    }
}
