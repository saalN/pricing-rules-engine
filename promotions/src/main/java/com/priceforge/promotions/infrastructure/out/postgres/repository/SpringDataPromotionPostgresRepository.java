package com.priceforge.promotions.infrastructure.out.postgres.repository;

import com.priceforge.promotions.domain.model.PromotionStatus;
import com.priceforge.promotions.infrastructure.out.postgres.entity.PromotionDocument;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataPromotionPostgresRepository extends JpaRepository<PromotionDocument, UUID> {

    List<PromotionDocument> findAllByStatus(PromotionStatus status);
}
