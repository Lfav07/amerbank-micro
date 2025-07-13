
ALTER TABLE users
    DROP CONSTRAINT IF EXISTS fk_users_customer_id;


ALTER TABLE users
    DROP COLUMN IF EXISTS customer_id;