BEGIN;

ALTER TABLE products
    ADD COLUMN IF NOT EXISTS value_4_installments NUMERIC,
    ADD COLUMN IF NOT EXISTS value_6_installments NUMERIC,
    ADD COLUMN IF NOT EXISTS value_8_installments NUMERIC,
    ADD COLUMN IF NOT EXISTS value_10_installments NUMERIC;

ALTER TABLE products
    DROP CONSTRAINT IF EXISTS products_value_4_installments_non_negative,
    DROP CONSTRAINT IF EXISTS products_value_6_installments_non_negative,
    DROP CONSTRAINT IF EXISTS products_value_8_installments_non_negative,
    DROP CONSTRAINT IF EXISTS products_value_10_installments_non_negative;

ALTER TABLE products
    ADD CONSTRAINT products_value_4_installments_non_negative CHECK (value_4_installments IS NULL OR value_4_installments > 0),
    ADD CONSTRAINT products_value_6_installments_non_negative CHECK (value_6_installments IS NULL OR value_6_installments > 0),
    ADD CONSTRAINT products_value_8_installments_non_negative CHECK (value_8_installments IS NULL OR value_8_installments > 0),
    ADD CONSTRAINT products_value_10_installments_non_negative CHECK (value_10_installments IS NULL OR value_10_installments > 0);

COMMIT;
