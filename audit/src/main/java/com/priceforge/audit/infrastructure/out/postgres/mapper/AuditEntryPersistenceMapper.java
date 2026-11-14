package com.priceforge.audit.infrastructure.out.postgres.mapper;

import com.priceforge.audit.domain.model.AuditEntry;
import com.priceforge.audit.domain.model.PriceCalculationSnapshot;
import com.priceforge.audit.infrastructure.out.postgres.entity.AuditEntryDocument;
import org.springframework.stereotype.Component;

@Component
public class AuditEntryPersistenceMapper {

    public AuditEntryDocument toEntity(AuditEntry entry) {
        PriceCalculationSnapshot snapshot = entry.priceSnapshot();
        return new AuditEntryDocument(
            entry.eventId(), entry.eventType(), entry.entityType(), entry.entityId(), entry.productId(),
            entry.userId(), entry.changedBy(), entry.previousState(), entry.newState(), entry.reason(),
            entry.payload(), entry.occurredAt(),
            snapshot == null ? null : snapshot.basePriceAmount(),
            snapshot == null ? null : snapshot.finalPriceAmount(),
            snapshot == null ? null : snapshot.currency(),
            snapshot == null ? null : snapshot.userSegment(),
            snapshot == null ? null : snapshot.quantity(),
            snapshot == null ? null : snapshot.requestedAt(),
            snapshot == null ? null : snapshot.location(),
            snapshot == null ? null : snapshot.zoneId());
    }

    public AuditEntry toDomain(AuditEntryDocument document) {
        PriceCalculationSnapshot snapshot = document.getFinalPriceAmount() == null
            ? null
            : new PriceCalculationSnapshot(
                document.getProductId(), document.getUserId(), document.getUserSegment(),
                document.getQuantity(), document.getRequestedAt(), document.getLocation(),
                document.getCurrency(), document.getZoneId(), document.getBasePriceAmount(),
                document.getFinalPriceAmount());

        return new AuditEntry(
            document.getEventId(), document.getEventType(), document.getEntityType(), document.getEntityId(),
            document.getProductId(), document.getUserId(), document.getChangedBy(), document.getPreviousState(),
            document.getNewState(), document.getReason(), document.getPayload(), document.getOccurredAt(),
            snapshot);
    }
}
