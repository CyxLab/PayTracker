CREATE TABLE number_series (
    id BIGSERIAL PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    financial_year VARCHAR(10) NOT NULL,
    current_value INTEGER NOT NULL DEFAULT 0,

    CONSTRAINT uq_series_year
        UNIQUE (tenant_id, financial_year)
);

INSERT INTO number_series ( tenant_id, financial_year, current_value) VALUES 
('11111111-1111-1111-1111-111111111111', '2026', 2),
( '22222222-2222-2222-2222-222222222222', '2026', 1);