package com.priceforge.catalog.application.service;

import com.priceforge.catalog.application.dto.FindCheapestActiveBasePriceQuery;
import com.priceforge.catalog.application.dto.FindCheapestActiveBasePriceResult;
import com.priceforge.catalog.application.port.in.FindCheapestActiveBasePriceUseCase;
import com.priceforge.catalog.application.port.out.ProductRepositoryPort;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class FindCheapestActiveBasePriceService implements FindCheapestActiveBasePriceUseCase {

    private final ProductRepositoryPort productRepository;

    public FindCheapestActiveBasePriceService(ProductRepositoryPort productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public FindCheapestActiveBasePriceResult execute(FindCheapestActiveBasePriceQuery query) {
        return new FindCheapestActiveBasePriceResult(
            productRepository.findCheapestActiveBasePriceAmount(Optional.ofNullable(query.category()))
                .orElse(null));
    }
}
