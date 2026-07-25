CREATE TABLE promotions.promotion (
    id                       UUID PRIMARY KEY,
    name                     VARCHAR(255) NOT NULL,
    description              TEXT,
    valid_from               TIMESTAMPTZ NOT NULL,
    valid_until              TIMESTAMPTZ NOT NULL,
    global_usage_limit       INTEGER NOT NULL,
    per_user_usage_limit     INTEGER NOT NULL,
    global_usage_count       INTEGER NOT NULL DEFAULT 0,
    segment                  VARCHAR(20),
    category                 VARCHAR(100),
    minimum_purchase_amount  NUMERIC(19, 4),
    discount_type            VARCHAR(20) NOT NULL,
    discount_value           NUMERIC(19, 4) NOT NULL,
    priority                 INTEGER NOT NULL,
    combination_mode         VARCHAR(20) NOT NULL,
    status                   VARCHAR(20) NOT NULL,
    created_at               TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_promotion_status ON promotions.promotion (status);

-- Tracks per-user usage independently of promotion.global_usage_count (UC-11: "el límite por
-- usuario es independiente del límite global; ambos deben cumplirse").
CREATE TABLE promotions.promotion_usage (
    promotion_id  UUID NOT NULL REFERENCES promotions.promotion (id),
    user_id       UUID NOT NULL,
    usage_count   INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (promotion_id, user_id)
);
