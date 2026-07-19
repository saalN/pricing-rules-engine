package com.priceforge.pricing.infrastructure.out.postgres.adapter;

import com.priceforge.pricing.application.port.out.PricingRuleRepositoryPort;
import com.priceforge.pricing.domain.model.PricingRule;
import com.priceforge.pricing.domain.model.PricingRuleStatus;
import com.priceforge.pricing.infrastructure.out.postgres.mapper.PricingRulePersistenceMapper;
import com.priceforge.pricing.infrastructure.out.postgres.repository.SpringDataPricingRulePostgresRepository;
import com.priceforge.sharedkernel.PricingRuleId;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class PostgresPricingRuleRepositoryAdapter implements PricingRuleRepositoryPort {

    private final SpringDataPricingRulePostgresRepository springDataRepository;
    private final PricingRulePersistenceMapper mapper;

    public PostgresPricingRuleRepositoryAdapter(
            SpringDataPricingRulePostgresRepository springDataRepository, PricingRulePersistenceMapper mapper) {
        this.springDataRepository = springDataRepository;
        this.mapper = mapper;
    }

    @Override
    public PricingRule save(PricingRule rule) {
        return mapper.toDomain(springDataRepository.save(mapper.toEntity(rule)));
    }

    @Override
    public Optional<PricingRule> findById(PricingRuleId id) {
        return springDataRepository.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public List<PricingRule> findAllActive() {
        return springDataRepository.findAllByStatus(PricingRuleStatus.ACTIVE).stream()
            .map(mapper::toDomain)
            .toList();
    }
}
