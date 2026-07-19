package com.priceforge.catalog.application.port.in;

import com.priceforge.catalog.application.dto.GetProductQuery;
import com.priceforge.catalog.application.dto.GetProductResult;
import java.util.Optional;

/**
 * Cross-context query: UC-06 (Pricing's price calculation) needs the product's base price,
 * currency, category and active status. Returns {@link Optional#empty()} rather than throwing a
 * Catalog-specific exception, so cross-context callers never need to depend on Catalog's domain
 * exception types.
 */
public interface GetProductUseCase {

    Optional<GetProductResult> execute(GetProductQuery query);
}
