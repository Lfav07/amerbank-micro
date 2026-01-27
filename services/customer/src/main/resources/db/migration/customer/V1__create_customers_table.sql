CREATE SEQUENCE customers_id_seq
    START WITH 1
    INCREMENT BY 50;

CREATE TABLE customers (
    id BIGINT NOT NULL PRIMARY KEY,
    user_id BIGINT NOT NULL  UNIQUE,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    date_of_birth DATE NOT NULL,
    kyc_verified BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL
);
