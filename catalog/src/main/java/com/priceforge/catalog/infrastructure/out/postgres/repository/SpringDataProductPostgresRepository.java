package com.priceforge.catalog.infrastructure.out.postgres.repository;

import com.priceforge.catalog.infrastructure.out.postgres.entity.ProductDocument;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataProductPostgresRepository extends JpaRepository<ProductDocument, UUID> {

    boolean existsBySku(String sku);
}
