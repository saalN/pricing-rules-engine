package com.priceforge.catalog.domain.model;

import com.priceforge.catalog.domain.exception.InvalidBasePriceException;
import com.priceforge.catalog.domain.exception.ProductNotActiveException;
import com.priceforge.sharedkernel.Money;
import com.priceforge.sharedkernel.ProductId;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * The product aggregate. The base price is the starting point handed to the pricing engine — it
 * is never itself the final price a customer pays.
 */
public final class Product {

    private final ProductId id;
    private final String sku;
    private String name;
    private String description;
    private Money basePrice;
    private String category;
    private final Map<String, Object> customAttributes;
    private ProductStatus status;
    private final Instant createdAt;

    private Product(
            ProductId id,
            String sku,
            String name,
            String description,
            Money basePrice,
            String category,
            Map<String, Object> customAttributes,
            ProductStatus status,
            Instant createdAt) {
        this.id = id;
        this.sku = sku;
        this.name = name;
        this.description = description;
        this.basePrice = basePrice;
        this.category = category;
        this.customAttributes = customAttributes;
        this.status = status;
        this.createdAt = createdAt;
    }

    public static Product create(
            ProductId id,
            String sku,
            String name,
            String description,
            Money basePrice,
            String category,
            Map<String, Object> customAttributes,
            Instant createdAt) {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(sku, "sku must not be null");
        Objects.requireNonNull(basePrice, "basePrice must not be null");
        requirePositive(basePrice);

        return new Product(
            id,
            sku,
            name,
            description,
            basePrice,
            category,
            Map.copyOf(customAttributes),
            ProductStatus.ACTIVE,
            createdAt);
    }

    /**
     * Rehydrates a Product from persisted state. Unlike {@link #create}, this does not re-run
     * creation invariants (e.g. positive base price) — a persisted product was already validated
     * when it was created, and status transitions enforce their own invariants going forward.
     */
    public static Product reconstitute(
            ProductId id,
            String sku,
            String name,
            String description,
            Money basePrice,
            String category,
            Map<String, Object> customAttributes,
            ProductStatus status,
            Instant createdAt) {
        return new Product(
            id, sku, name, description, basePrice, category, Map.copyOf(customAttributes), status, createdAt);
    }

    private static void requirePositive(Money basePrice) {
        if (basePrice.isNegative() || basePrice.isZero()) {
            throw new InvalidBasePriceException(
                "Base price must be positive, but was " + basePrice.amount());
        }
    }

    /**
     * UC-02: changing the base price does not touch existing pricing rules — it only takes
     * effect on the next price calculation.
     */
    public void changeBasePrice(Money newBasePrice) {
        requireActive();
        requirePositive(newBasePrice);
        this.basePrice = newBasePrice;
    }

    /** UC-03: pricing rules tied to this product are not deleted, they simply stop applying. */
    public void deactivate() {
        requireActive();
        this.status = ProductStatus.INACTIVE;
    }

    /** UC-04: a product always belongs to exactly one category. */
    public void changeCategory(String newCategory) {
        Objects.requireNonNull(newCategory, "newCategory must not be null");
        this.category = newCategory;
    }

    private void requireActive() {
        if (status != ProductStatus.ACTIVE) {
            throw new ProductNotActiveException(id);
        }
    }

    public ProductId id() {
        return id;
    }

    public String sku() {
        return sku;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public Money basePrice() {
        return basePrice;
    }

    public String category() {
        return category;
    }

    public Map<String, Object> customAttributes() {
        return customAttributes;
    }

    public ProductStatus status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
