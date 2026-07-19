CREATE TABLE pricing.pricing_rule (
    id                 UUID PRIMARY KEY,
    type               VARCHAR(20) NOT NULL,
    time_window_start  TIME,
    time_window_end    TIME,
    segment            VARCHAR(20),
    minimum_quantity   INTEGER,
    zone               VARCHAR(100),
    discount_type      VARCHAR(20) NOT NULL,
    discount_value     NUMERIC(19, 4) NOT NULL,
    priority           INTEGER NOT NULL,
    combination_mode   VARCHAR(20) NOT NULL,
    scope_product_id   UUID,
    scope_category     VARCHAR(100),
    status             VARCHAR(20) NOT NULL,
    created_at         TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_pricing_rule_status ON pricing.pricing_rule (status);
