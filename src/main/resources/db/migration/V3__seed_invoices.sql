-- V3: seed demo invoices for both organisations

INSERT INTO invoice (tenant_id, customer, amount, status) VALUES
  ('11111111-1111-1111-1111-111111111111', 'A-Customer-1', 1000.00, 'ISSUED'),
  ('11111111-1111-1111-1111-111111111111', 'A-Customer-2', 2500.00, 'ISSUED'),
  ('22222222-2222-2222-2222-222222222222', 'B-Customer-1', 9999.00, 'ISSUED');