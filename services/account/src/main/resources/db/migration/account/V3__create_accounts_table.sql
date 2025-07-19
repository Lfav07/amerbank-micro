CREATE EXTENSION IF NOT EXISTS "pgcrypto";

ALTER TABLE accounts
ADD UNIQUE (customer_id);
