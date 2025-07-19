CREATE EXTENSION IF NOT EXISTS "pgcrypto";


ALTER TABLE accounts
ADD CONSTRAINT unique_customer_account_type UNIQUE (customer_id, type);