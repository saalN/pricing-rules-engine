package com.priceforge.catalog.application.service.support;

import com.priceforge.catalog.application.port.out.ProductRepositoryPort;
import com.priceforge.catalog.domain.model.Product;
import com.priceforge.sharedkernel.ProductId;
import java.util.HashMap;
import java.util.Map;
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
}
