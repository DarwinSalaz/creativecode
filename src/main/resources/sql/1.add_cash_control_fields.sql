ALTER TABLE cash_control ADD COLUMN IF NOT EXISTS commission DOUBLE PRECISION;
ALTER TABLE cash_control ADD COLUMN IF NOT EXISTS closure_user CHARACTER VARYING(50);
ALTER TABLE cash_control ADD COLUMN IF NOT EXISTS closure_value_received DOUBLE PRECISION;
ALTER TABLE cash_control ADD COLUMN IF NOT EXISTS closure_date TIMESTAMP WITHOUT TIME ZONE;
ALTER TABLE cash_control ADD COLUMN IF NOT EXISTS closure_notes TEXT;
ALTER TABLE cash_control ADD COLUMN IF NOT EXISTS down_payments DOUBLE PRECISION;


ALTER TABLE expenses ADD COLUMN IF NOT EXISTS wallet_id INTEGER;
ALTER TABLE expenses ADD CONSTRAINT fk_wallet FOREIGN KEY (wallet_id) REFERENCES wallets (wallet_id);

ALTER TABLE products ADD COLUMN wallet_id INTEGER;
ALTER TABLE products ADD CONSTRAINT fk_product_wallet FOREIGN KEY (wallet_id) REFERENCES wallets (wallet_id);
ALTER TABLE products ADD COLUMN left_quantity INTEGER;

ALTER TABLE cash_movements ADD COLUMN IF NOT EXISTS cash_control_id BIGINT;
ALTER TABLE cash_movements ADD COLUMN IF NOT EXISTS commission DOUBLE PRECISION;
ALTER TABLE cash_movements ADD COLUMN IF NOT EXISTS created_at TIMESTAMP WITHOUT TIME ZONE;
ALTER TABLE cash_movements ADD COLUMN IF NOT EXISTS movement_type CHARACTER VARYING(10);

ALTER TABLE payment_schedule ADD COLUMN IF NOT EXISTS associated_payment_id BIGINT;
ALTER TABLE payment_schedule ADD CONSTRAINT associated_payment_id_fkey FOREIGN KEY (associated_payment_id) REFERENCES payments (payment_id);
ALTER TABLE payment_schedule ADD COLUMN IF NOT EXISTS customer_id BIGINT;