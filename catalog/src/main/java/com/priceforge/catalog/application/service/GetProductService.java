package com.priceforge.catalog.application.service;

import com.priceforge.catalog.application.dto.GetProductQuery;
import com.priceforge.catalog.application.dto.GetProductResult;
import com.priceforge.catalog.application.port.in.GetProductUseCase;
import com.priceforge.catalog.application.port.out.ProductRepositoryPort;
import com.priceforge.catalog.domain.model.Product;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class GetProductService implements GetProductUseCase {

    private final ProductRepositoryPort productRepository;

    public GetProductService(ProductRepositoryPort productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public Optional<GetProductResult> execute(GetProductQuery query) {
        return productRepository.findById(query.productId()).map(this::toResult);
    }

    private GetProductResult toResult(Product product) {
        return new GetProductResult(
            product.id(),
            product.sku(),
            product.basePrice().amount(),
            product.basePrice().currency().getCurrencyCode(),
            product.category(),
            product.status().name());
    }
}
