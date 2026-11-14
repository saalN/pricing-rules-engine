package com.priceforge.audit.application.service.support;

import com.priceforge.audit.application.dto.AuditEntryFilter;
import com.priceforge.audit.application.dto.AuditEntrySearchResult;
import com.priceforge.audit.application.port.out.AuditEntryRepositoryPort;
import com.priceforge.audit.domain.model.AuditEntry;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class InMemoryAuditEntryRepositoryPort implements AuditEntryRepositoryPort {

    private final Map<UUID, AuditEntry> entriesById = new LinkedHashMap<>();

    @Override
    public void saveIfAbsent(AuditEntry entry) {
        entriesById.putIfAbsent(entry.eventId(), entry);
    }

    @Override
    public Optional<AuditEntry> findById(UUID eventId) {
        return Optional.ofNullable(entriesById.get(eventId));
    }

    @Override
    public AuditEntrySearchResult search(AuditEntryFilter filter, int page, int size, boolean newestFirst) {
        Comparator<AuditEntry> byTime = Comparator.comparing(AuditEntry::occurredAt);
        List<AuditEntry> matching = entriesById.values().stream()
            .filter(entry -> filter.entityType() == null || entry.entityType() == filter.entityType())
            .filter(entry -> filter.entityId() == null || Objects.equals(entry.entityId(), filter.entityId()))
            .filter(entry -> filter.productId() == null || Objects.equals(entry.productId(), filter.productId()))
            .filter(entry -> filter.userId() == null || Objects.equals(entry.userId(), filter.userId()))
            .filter(entry -> filter.from() == null || !entry.occurredAt().isBefore(filter.from()))
            .filter(entry -> filter.to() == null || !entry.occurredAt().isAfter(filter.to()))
            .sorted(newestFirst ? byTime.reversed() : byTime)
            .toList();

        List<AuditEntry> pageEntries = matching.stream().skip((long) page * size).limit(size).toList();
        return new AuditEntrySearchResult(pageEntries, matching.size());
    }

    public int size() {
        return entriesById.size();
    }
}
