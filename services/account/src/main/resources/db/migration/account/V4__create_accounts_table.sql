CREATE EXTENSION IF NOT EXISTS "pgcrypto";

ALTER TABLE accounts
DROP CONSTRAINT accounts_customer_id_key