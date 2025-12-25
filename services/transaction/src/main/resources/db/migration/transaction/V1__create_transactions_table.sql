CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
   amount NUMERIC(19,2) NOT NULL,
   description VARCHAR(255),
   from_account VARCHAR(20) NOT NULL,
   to_account VARCHAR(20) NOT NULL,
   type VARCHAR(20) NOT NULL,
   status VARCHAR(20) NOT NULL,
   failure_reason VARCHAR(255),
   idempotency_key VARCHAR(255) UNIQUE  NOT NULL,
   created_at TIMESTAMP DEFAULT now(),
   updated_at TIMESTAMP DEFAULT now()
);
