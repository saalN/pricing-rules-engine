CREATE TABLE audit.audit_entry (
    event_id            UUID PRIMARY KEY,
    event_type          VARCHAR(100)   NOT NULL,
    entity_type         VARCHAR(50)    NOT NULL,
    entity_id           VARCHAR(255)   NOT NULL,
    product_id          VARCHAR(255),
    user_id             VARCHAR(255),
    changed_by          VARCHAR(255),
    previous_state      TEXT,
    new_state           TEXT,
    reason              TEXT,
    payload             JSONB          NOT NULL,
    occurred_at         TIMESTAMPTZ    NOT NULL,
    recorded_at         TIMESTAMPTZ    NOT NULL DEFAULT now(),

    -- Only populated for PriceCalculated: the contextSnapshot (UC-14) in queryable form, so a
    -- stored calculation can be replayed without re-parsing the raw payload.
    base_price_amount   NUMERIC(19, 4),
    final_price_amount  NUMERIC(19, 4),
    currency            VARCHAR(3),
    user_segment        VARCHAR(50),
    quantity            INTEGER,
    requested_at        TIMESTAMPTZ,
    location            VARCHAR(255),
    zone_id             VARCHAR(100)
);

CREATE INDEX idx_audit_entry_entity ON audit.audit_entry (entity_type, entity_id, occurred_at);
CREATE INDEX idx_audit_entry_product ON audit.audit_entry (product_id, occurred_at);
CREATE INDEX idx_audit_entry_user ON audit.audit_entry (user_id, occurred_at);

-- Audit events are immutable (UC-14): enforce it in the database, not just by convention.
CREATE FUNCTION audit.reject_audit_entry_mutation() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'audit.audit_entry is append-only: % is not allowed', TG_OP;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER audit_entry_immutable
    BEFORE UPDATE OR DELETE ON audit.audit_entry
    FOR EACH ROW EXECUTE FUNCTION audit.reject_audit_entry_mutation();
