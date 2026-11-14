package com.priceforge.audit.infrastructure.out.postgres.entity;

import com.priceforge.audit.domain.model.AuditEntityType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Append-only (the table's trigger rejects UPDATE/DELETE): no setters, only the full constructor. */
@Entity
@Table(name = "audit_entry", schema = "audit")
public class AuditEntryDocument {

    @Id
    @Column(name = "event_id")
    private UUID eventId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false)
    private AuditEntityType entityType;

    @Column(name = "entity_id", nullable = false)
    private String entityId;

    @Column(name = "product_id")
    private String productId;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "changed_by")
    private String changedBy;

    @Column(name = "previous_state")
    private String previousState;

    @Column(name = "new_state")
    private String newState;

    private String reason;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private String payload;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "base_price_amount")
    private BigDecimal basePriceAmount;

    @Column(name = "final_price_amount")
    private BigDecimal finalPriceAmount;

    private String currency;

    @Column(name = "user_segment")
    private String userSegment;

    private Integer quantity;

    @Column(name = "requested_at")
    private Instant requestedAt;

    private String location;

    @Column(name = "zone_id")
    private String zoneId;

    protected AuditEntryDocument() {
        // required by JPA
    }

    public AuditEntryDocument(
            UUID eventId, String eventType, AuditEntityType entityType, String entityId, String productId,
            String userId, String changedBy, String previousState, String newState, String reason,
            String payload, Instant occurredAt, BigDecimal basePriceAmount, BigDecimal finalPriceAmount,
            String currency, String userSegment, Integer quantity, Instant requestedAt, String location,
            String zoneId) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.entityType = entityType;
        this.entityId = entityId;
        this.productId = productId;
        this.userId = userId;
        this.changedBy = changedBy;
        this.previousState = previousState;
        this.newState = newState;
        this.reason = reason;
        this.payload = payload;
        this.occurredAt = occurredAt;
        this.basePriceAmount = basePriceAmount;
        this.finalPriceAmount = finalPriceAmount;
        this.currency = currency;
        this.userSegment = userSegment;
        this.quantity = quantity;
        this.requestedAt = requestedAt;
        this.location = location;
        this.zoneId = zoneId;
    }

    public UUID getEventId() {
        return eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public AuditEntityType getEntityType() {
        return entityType;
    }

    public String getEntityId() {
        return entityId;
    }

    public String getProductId() {
        return productId;
    }

    public String getUserId() {
        return userId;
    }

    public String getChangedBy() {
        return changedBy;
    }

    public String getPreviousState() {
        return previousState;
    }

    public String getNewState() {
        return newState;
    }

    public String getReason() {
        return reason;
    }

    public String getPayload() {
        return payload;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public BigDecimal getBasePriceAmount() {
        return basePriceAmount;
    }

    public BigDecimal getFinalPriceAmount() {
        return finalPriceAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getUserSegment() {
        return userSegment;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public String getLocation() {
        return location;
    }

    public String getZoneId() {
        return zoneId;
    }
}
