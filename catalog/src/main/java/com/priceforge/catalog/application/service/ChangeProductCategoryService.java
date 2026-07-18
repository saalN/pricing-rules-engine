package com.priceforge.catalog.application.service;

import com.priceforge.catalog.application.dto.ChangeProductCategoryCommand;
import com.priceforge.catalog.application.dto.ChangeProductCategoryResult;
import com.priceforge.catalog.application.port.in.ChangeProductCategoryUseCase;
import com.priceforge.catalog.application.port.out.ProductRepositoryPort;
import com.priceforge.catalog.domain.exception.ProductNotFoundException;
import com.priceforge.catalog.domain.model.Product;
import com.priceforge.catalog.domain.model.ProductCategoryChanged;
import com.priceforge.sharedkernel.EventPublisher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChangeProductCategoryService implements ChangeProductCategoryUseCase {

    private final ProductRepositoryPort productRepository;
    private final EventPublisher eventPublisher;

    public ChangeProductCategoryService(
            ProductRepositoryPort productRepository,
            @Qualifier("catalogEventPublisher") EventPublisher eventPublisher) {
        this.productRepository = productRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public ChangeProductCategoryResult execute(ChangeProductCategoryCommand command) {
        Product product = productRepository.findById(command.productId())
            .orElseThrow(() -> new ProductNotFoundException(command.productId()));

        String previousCategory = product.category();
        product.changeCategory(command.newCategory());
        productRepository.save(product);

        eventPublisher.publish(ProductCategoryChanged.of(product, previousCategory));

        return new ChangeProductCategoryResult(product.id(), previousCategory, product.category());
    }
}
