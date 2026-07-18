package com.priceforge.catalog.application.service;

import com.priceforge.catalog.application.dto.CreateProductCommand;
import com.priceforge.catalog.application.dto.CreateProductResult;
import com.priceforge.catalog.application.port.in.CreateProductUseCase;
import com.priceforge.catalog.application.port.out.ProductRepositoryPort;
import com.priceforge.catalog.domain.exception.DuplicateSkuException;
import com.priceforge.catalog.domain.model.Product;
import com.priceforge.catalog.domain.model.ProductCreated;
import com.priceforge.sharedkernel.EventPublisher;
import com.priceforge.sharedkernel.Money;
import com.priceforge.sharedkernel.ProductId;
import java.time.Instant;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateProductService implements CreateProductUseCase {

    private final ProductRepositoryPort productRepository;
    private final EventPublisher eventPublisher;

    public CreateProductService(
            ProductRepositoryPort productRepository,
            @Qualifier("catalogEventPublisher") EventPublisher eventPublisher) {
        this.productRepository = productRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public CreateProductResult execute(CreateProductCommand command) {
        if (productRepository.existsBySku(command.sku())) {
            throw new DuplicateSkuException(command.sku());
        }

        Product product = Product.create(
            ProductId.newId(),
            command.sku(),
            command.name(),
            command.description(),
            Money.of(command.basePriceAmount(), command.currencyCode()),
            command.category(),
            command.customAttributes() != null ? command.customAttributes() : Map.of(),
            Instant.now());

        productRepository.save(product);
        eventPublisher.publish(ProductCreated.of(product));

        return new CreateProductResult(product.id());
    }
}
