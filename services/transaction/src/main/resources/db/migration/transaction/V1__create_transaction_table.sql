CREATE TABLE transactions (
   id SERIAL PRIMARY KEY,
   amount NUMERIC(19,2) NOT NULL,
   description VARCHAR(255),
   from_account VARCHAR(20) NOT NULL,
   to_account VARCHAR(20) NOT NULL,
   type VARCHAR(20) NOT NULL,
   status VARCHAR(20) NOT NULL,
   failure_reason VARCHAR(255),
   created_at TIMESTAMP DEFAULT now(),
   updated_at TIMESTAMP DEFAULT now(),
);
