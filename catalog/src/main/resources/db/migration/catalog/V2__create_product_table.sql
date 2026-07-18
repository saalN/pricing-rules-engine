CREATE TABLE catalog.product (
    id                 UUID PRIMARY KEY,
    sku                VARCHAR(64) NOT NULL UNIQUE,
    name               VARCHAR(255) NOT NULL,
    description        TEXT,
    base_price_amount  NUMERIC(19, 4) NOT NULL,
    currency           VARCHAR(3) NOT NULL,
    category           VARCHAR(100),
    custom_attributes  JSONB NOT NULL DEFAULT '{}'::jsonb,
    status             VARCHAR(20) NOT NULL,
    created_at         TIMESTAMPTZ NOT NULL
);
