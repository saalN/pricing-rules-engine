package com.priceforge.catalog.infrastructure.out.postgres.entity;

import com.priceforge.catalog.domain.model.ProductStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "product", schema = "catalog")
public class ProductDocument {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String sku;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(name = "base_price_amount", nullable = false)
    private BigDecimal basePriceAmount;

    @Column(nullable = false)
    private String currency;

    private String category;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "custom_attributes", nullable = false)
    private Map<String, Object> customAttributes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ProductDocument() {
        // required by JPA
    }

    public ProductDocument(
            UUID id,
            String sku,
            String name,
            String description,
            BigDecimal basePriceAmount,
            String currency,
            String category,
            Map<String, Object> customAttributes,
            ProductStatus status,
            Instant createdAt) {
        this.id = id;
        this.sku = sku;
        this.name = name;
        this.description = description;
        this.basePriceAmount = basePriceAmount;
        this.currency = currency;
        this.category = category;
        this.customAttributes = customAttributes;
        this.status = status;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public String getSku() {
        return sku;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getBasePriceAmount() {
        return basePriceAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getCategory() {
        return category;
    }

    public Map<String, Object> getCustomAttributes() {
        return customAttributes;
    }

    public ProductStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
