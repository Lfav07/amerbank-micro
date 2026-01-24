CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TYPE account_type AS ENUM ('CHECKING', 'SAVINGS', 'BUSINESS');
CREATE TYPE account_status AS ENUM ('ACTIVE', 'SUSPENDED', 'CLOSED');

CREATE TABLE accounts (
                          id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                          account_number VARCHAR(20) NOT NULL UNIQUE,
                          customer_id BIGINT NOT NULL,
                          balance NUMERIC(19,2) NOT NULL CHECK (balance >= 0),
                          type account_type NOT NULL,
                          status account_status NOT NULL,
                          created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          updated_at TIMESTAMPTZ,
                          CONSTRAINT unique_customer_account_type UNIQUE (customer_id, type)
);

CREATE INDEX idx_accounts_customer_id ON accounts(customer_id);
