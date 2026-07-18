package com.priceforge.catalog.application.port.out;

import com.priceforge.catalog.domain.model.Product;
import com.priceforge.sharedkernel.ProductId;
import java.util.Optional;

public interface ProductRepositoryPort {

    Product save(Product product);

    boolean existsBySku(String sku);

    Optional<Product> findById(ProductId id);
}
