package com.priceforge.catalog.application.service;

import com.priceforge.catalog.application.dto.DeactivateProductCommand;
import com.priceforge.catalog.application.dto.DeactivateProductResult;
import com.priceforge.catalog.application.port.in.DeactivateProductUseCase;
import com.priceforge.catalog.application.port.out.ProductRepositoryPort;
import com.priceforge.catalog.domain.exception.ProductNotFoundException;
import com.priceforge.catalog.domain.model.Product;
import com.priceforge.catalog.domain.model.ProductDeactivated;
import com.priceforge.sharedkernel.EventPublisher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeactivateProductService implements DeactivateProductUseCase {

    private final ProductRepositoryPort productRepository;
    private final EventPublisher eventPublisher;

    public DeactivateProductService(
            ProductRepositoryPort productRepository,
            @Qualifier("catalogEventPublisher") EventPublisher eventPublisher) {
        this.productRepository = productRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public DeactivateProductResult execute(DeactivateProductCommand command) {
        Product product = productRepository.findById(command.productId())
            .orElseThrow(() -> new ProductNotFoundException(command.productId()));

        product.deactivate();
        productRepository.save(product);

        eventPublisher.publish(ProductDeactivated.of(product));

        return new DeactivateProductResult(product.id(), product.status());
    }
}
