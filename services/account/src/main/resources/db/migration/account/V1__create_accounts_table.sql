CREATE EXTENSION IF NOT EXISTS "pgcrypto";



CREATE TABLE accounts (
                          id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                          account_number VARCHAR(20) NOT NULL UNIQUE,
                          customer_id BIGINT NOT NULL,
                          balance NUMERIC(19,2) NOT NULL CHECK (balance >= 0),
                          type VARCHAR(20) NOT NULL,
                         status VARCHAR(20) NOT NULL,
                          created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          updated_at TIMESTAMPTZ
);
CREATE INDEX idx_accounts_customer_id ON accounts(customer_id);
