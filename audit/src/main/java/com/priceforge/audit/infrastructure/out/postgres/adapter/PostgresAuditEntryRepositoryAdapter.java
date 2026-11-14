package com.priceforge.audit.infrastructure.out.postgres.adapter;

import com.priceforge.audit.application.dto.AuditEntryFilter;
import com.priceforge.audit.application.dto.AuditEntrySearchResult;
import com.priceforge.audit.application.port.out.AuditEntryRepositoryPort;
import com.priceforge.audit.domain.model.AuditEntry;
import com.priceforge.audit.infrastructure.out.postgres.entity.AuditEntryDocument;
import com.priceforge.audit.infrastructure.out.postgres.mapper.AuditEntryPersistenceMapper;
import com.priceforge.audit.infrastructure.out.postgres.repository.SpringDataAuditEntryPostgresRepository;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

@Repository
public class PostgresAuditEntryRepositoryAdapter implements AuditEntryRepositoryPort {

    private final SpringDataAuditEntryPostgresRepository springDataRepository;
    private final AuditEntryPersistenceMapper mapper;

    public PostgresAuditEntryRepositoryAdapter(
            SpringDataAuditEntryPostgresRepository springDataRepository, AuditEntryPersistenceMapper mapper) {
        this.springDataRepository = springDataRepository;
        this.mapper = mapper;
    }

    @Override
    public void saveIfAbsent(AuditEntry entry) {
        if (springDataRepository.existsById(entry.eventId())) {
            return;
        }
        springDataRepository.save(mapper.toEntity(entry));
    }

    @Override
    public Optional<AuditEntry> findById(UUID eventId) {
        return springDataRepository.findById(eventId).map(mapper::toDomain);
    }

    @Override
    public AuditEntrySearchResult search(AuditEntryFilter filter, int page, int size, boolean newestFirst) {
        Sort.Direction direction = newestFirst ? Sort.Direction.DESC : Sort.Direction.ASC;
        // eventId breaks ties so pages are stable when several events share an instant.
        Sort sort = Sort.by(direction, "occurredAt").and(Sort.by(direction, "eventId"));

        Page<AuditEntryDocument> result =
            springDataRepository.findAll(toSpecification(filter), PageRequest.of(page, size, sort));

        return new AuditEntrySearchResult(
            result.getContent().stream().map(mapper::toDomain).toList(), result.getTotalElements());
    }

    private static Specification<AuditEntryDocument> toSpecification(AuditEntryFilter filter) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (filter.entityType() != null) {
                predicates.add(builder.equal(root.get("entityType"), filter.entityType()));
            }
            if (filter.entityId() != null) {
                predicates.add(builder.equal(root.get("entityId"), filter.entityId()));
            }
            if (filter.productId() != null) {
                predicates.add(builder.equal(root.get("productId"), filter.productId()));
            }
            if (filter.userId() != null) {
                predicates.add(builder.equal(root.get("userId"), filter.userId()));
            }
            if (filter.from() != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("occurredAt"), filter.from()));
            }
            if (filter.to() != null) {
                predicates.add(builder.lessThanOrEqualTo(root.get("occurredAt"), filter.to()));
            }
            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
