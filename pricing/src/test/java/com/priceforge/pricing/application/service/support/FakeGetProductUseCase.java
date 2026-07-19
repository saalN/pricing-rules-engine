package com.priceforge.pricing.application.service.support;

import com.priceforge.catalog.application.dto.GetProductQuery;
import com.priceforge.catalog.application.dto.GetProductResult;
import com.priceforge.catalog.application.port.in.GetProductUseCase;
import com.priceforge.sharedkernel.ProductId;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** In-memory stand-in for Catalog's cross-context query, for Pricing's application-layer tests. */
public final class FakeGetProductUseCase implements GetProductUseCase {

    private final Map<ProductId, GetProductResult> productsById = new HashMap<>();

    public void register(GetProductResult product) {
        productsById.put(product.productId(), product);
    }

    @Override
    public Optional<GetProductResult> execute(GetProductQuery query) {
        return Optional.ofNullable(productsById.get(query.productId()));
    }
}
