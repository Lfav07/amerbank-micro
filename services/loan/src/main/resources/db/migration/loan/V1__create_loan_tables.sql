CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE loans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    loan_number VARCHAR(20) NOT NULL UNIQUE,
    customer_id BIGINT NOT NULL,
    account_number VARCHAR(20) NOT NULL,
    principal_amount NUMERIC(19,2) NOT NULL CHECK (principal_amount > 0),
    interest_rate NUMERIC(5,2) NOT NULL CHECK (interest_rate >= 0),
    term_months INT NOT NULL CHECK (term_months > 0),
    monthly_payment NUMERIC(19,2) NOT NULL CHECK (monthly_payment > 0),
    total_amount NUMERIC(19,2) NOT NULL CHECK (total_amount > 0),
    remaining_balance NUMERIC(19,2) NOT NULL CHECK (remaining_balance >= 0),
    type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    rejection_reason TEXT,
    disbursed_at TIMESTAMPTZ,
    maturity_date DATE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ
);

CREATE INDEX idx_loans_customer_id ON loans(customer_id);
CREATE INDEX idx_loans_status ON loans(status);
CREATE INDEX idx_loans_loan_number ON loans(loan_number);

CREATE TABLE loan_payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    loan_id UUID NOT NULL REFERENCES loans(id) ON DELETE CASCADE,
    payment_number INT NOT NULL,
    amount_due NUMERIC(19,2) NOT NULL CHECK (amount_due > 0),
    principal_portion NUMERIC(19,2) NOT NULL CHECK (principal_portion >= 0),
    interest_portion NUMERIC(19,2) NOT NULL CHECK (interest_portion >= 0),
    due_date DATE NOT NULL,
    paid_date TIMESTAMPTZ,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ,
    UNIQUE(loan_id, payment_number)
);

CREATE INDEX idx_loan_payments_loan_id ON loan_payments(loan_id);
CREATE INDEX idx_loan_payments_status ON loan_payments(status);
CREATE INDEX idx_loan_payments_due_date ON loan_payments(due_date);