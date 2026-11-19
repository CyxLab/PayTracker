-- V2: seed two organisations (A=Meridian, B=Acme) and their users
-- Password for both = "Passw0rd!" (demo only)

INSERT INTO tenant (id, name) VALUES
  ('11111111-1111-1111-1111-111111111111', 'Meridian Supplies');

INSERT INTO tenant (id, name) VALUES
  ('22222222-2222-2222-2222-222222222222', 'Acme Distributors');

INSERT INTO app_user (tenant_id, username, password_hash, role) VALUES
  ('11111111-1111-1111-1111-111111111111', 'alice',
   '{bcrypt}$2a$10$t5ZwzoxsFvmr7krvMLzh/OFXaJSsdwh/n4TaGVkUG3RdB4lu86jyC',
   'FINANCE_MANAGER');

INSERT INTO app_user (tenant_id, username, password_hash, role) VALUES
  ('22222222-2222-2222-2222-222222222222', 'bob',
   '{bcrypt}$2a$10$t5ZwzoxsFvmr7krvMLzh/OFXaJSsdwh/n4TaGVkUG3RdB4lu86jyC',
   'FINANCE_MANAGER');