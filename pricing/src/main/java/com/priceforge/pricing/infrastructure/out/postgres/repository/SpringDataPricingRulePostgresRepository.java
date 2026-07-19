package com.priceforge.pricing.infrastructure.out.postgres.repository;

import com.priceforge.pricing.domain.model.PricingRuleStatus;
import com.priceforge.pricing.infrastructure.out.postgres.entity.PricingRuleDocument;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataPricingRulePostgresRepository extends JpaRepository<PricingRuleDocument, UUID> {

    List<PricingRuleDocument> findAllByStatus(PricingRuleStatus status);
}
