package com.priceforge.catalog.infrastructure.out.postgres.repository;

import com.priceforge.catalog.domain.model.ProductStatus;
import com.priceforge.catalog.infrastructure.out.postgres.entity.ProductDocument;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataProductPostgresRepository extends JpaRepository<ProductDocument, UUID> {

    boolean existsBySku(String sku);

    @Query("SELECT MIN(p.basePriceAmount) FROM ProductDocument p "
        + "WHERE p.status = :status AND (:category IS NULL OR p.category = :category)")
    Optional<BigDecimal> findCheapestActiveBasePriceAmount(
        @Param("status") ProductStatus status, @Param("category") String category);
}
