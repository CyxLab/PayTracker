-- V1: initial schema for PayTrack (multi-tenant)
-- Every business table carries tenant_id

CREATE TABLE tenant (
    id          UUID PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE app_user (
    id             BIGSERIAL PRIMARY KEY,
    tenant_id      UUID NOT NULL REFERENCES tenant(id),
    username       VARCHAR(100) NOT NULL,
    password_hash  VARCHAR(255) NOT NULL,
    role           VARCHAR(30)  NOT NULL,
    enabled        BOOLEAN NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_app_user_username UNIQUE (username)
);

CREATE INDEX idx_app_user_tenant ON app_user(tenant_id);

CREATE TABLE invoice (
    id          BIGSERIAL PRIMARY KEY,
    tenant_id   UUID NOT NULL REFERENCES tenant(id),
    invoice_number INTEGER,
    customer    VARCHAR(255) NOT NULL,
    amount      NUMERIC(15,2) NOT NULL,
    status      VARCHAR(20) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_invoice_tenant ON invoice(tenant_id);