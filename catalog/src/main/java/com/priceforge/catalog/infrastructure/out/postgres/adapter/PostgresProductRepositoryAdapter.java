package com.priceforge.catalog.infrastructure.out.postgres.adapter;

import com.priceforge.catalog.application.port.out.ProductRepositoryPort;
import com.priceforge.catalog.domain.model.Product;
import com.priceforge.catalog.domain.model.ProductStatus;
import com.priceforge.catalog.infrastructure.out.postgres.mapper.ProductPersistenceMapper;
import com.priceforge.catalog.infrastructure.out.postgres.repository.SpringDataProductPostgresRepository;
import com.priceforge.sharedkernel.ProductId;
import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class PostgresProductRepositoryAdapter implements ProductRepositoryPort {

    private final SpringDataProductPostgresRepository springDataRepository;
    private final ProductPersistenceMapper mapper;

    public PostgresProductRepositoryAdapter(
            SpringDataProductPostgresRepository springDataRepository, ProductPersistenceMapper mapper) {
        this.springDataRepository = springDataRepository;
        this.mapper = mapper;
    }

    @Override
    public Product save(Product product) {
        return mapper.toDomain(springDataRepository.save(mapper.toEntity(product)));
    }

    @Override
    public boolean existsBySku(String sku) {
        return springDataRepository.existsBySku(sku);
    }

    @Override
    public Optional<Product> findById(ProductId id) {
        return springDataRepository.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public Optional<BigDecimal> findCheapestActiveBasePriceAmount(Optional<String> category) {
        return springDataRepository.findCheapestActiveBasePriceAmount(ProductStatus.ACTIVE, category.orElse(null));
    }
}
