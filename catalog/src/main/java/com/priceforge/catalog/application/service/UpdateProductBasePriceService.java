package com.priceforge.catalog.application.service;

import com.priceforge.catalog.application.dto.UpdateProductBasePriceCommand;
import com.priceforge.catalog.application.dto.UpdateProductBasePriceResult;
import com.priceforge.catalog.application.port.in.UpdateProductBasePriceUseCase;
import com.priceforge.catalog.application.port.out.ProductRepositoryPort;
import com.priceforge.catalog.domain.exception.ProductNotFoundException;
import com.priceforge.catalog.domain.model.Product;
import com.priceforge.catalog.domain.model.ProductPriceBaseUpdated;
import com.priceforge.sharedkernel.EventPublisher;
import com.priceforge.sharedkernel.Money;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateProductBasePriceService implements UpdateProductBasePriceUseCase {

    private final ProductRepositoryPort productRepository;
    private final EventPublisher eventPublisher;

    public UpdateProductBasePriceService(
            ProductRepositoryPort productRepository, EventPublisher eventPublisher) {
        this.productRepository = productRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public UpdateProductBasePriceResult execute(UpdateProductBasePriceCommand command) {
        Product product = productRepository.findById(command.productId())
            .orElseThrow(() -> new ProductNotFoundException(command.productId()));

        Money previousBasePrice = product.basePrice();
        Money newBasePrice =
            Money.of(command.newBasePriceAmount(), previousBasePrice.currency().getCurrencyCode());

        product.changeBasePrice(newBasePrice);
        productRepository.save(product);

        eventPublisher.publish(ProductPriceBaseUpdated.of(product, previousBasePrice, command.changedBy()));

        return new UpdateProductBasePriceResult(
            product.id(),
            previousBasePrice.amount(),
            newBasePrice.amount(),
            newBasePrice.currency().getCurrencyCode());
    }
}
