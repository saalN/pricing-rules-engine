package com.priceforge.catalog.application.port.out;

import com.priceforge.catalog.domain.model.Product;

public interface ProductRepositoryPort {

    Product save(Product product);

    boolean existsBySku(String sku);
}
