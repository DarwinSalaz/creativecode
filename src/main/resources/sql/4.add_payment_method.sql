ALTER TABLE payments
    ADD COLUMN IF NOT EXISTS payment_method VARCHAR(20) NOT NULL DEFAULT 'CASH';

ALTER TABLE cash_movements
    ADD COLUMN IF NOT EXISTS payment_method VARCHAR(20);

UPDATE cash_movements
SET payment_method = 'CASH'
WHERE cash_movement_type IN ('fee_payment', 'cancel_payment')
  AND payment_method IS NULL;

ALTER TABLE payments
    DROP CONSTRAINT IF EXISTS payments_payment_method_check;

ALTER TABLE payments
    ADD CONSTRAINT payments_payment_method_check
    CHECK (payment_method IN ('CASH', 'TRANSFER'));

ALTER TABLE cash_movements
    DROP CONSTRAINT IF EXISTS cash_movements_payment_method_check;

ALTER TABLE cash_movements
    ADD CONSTRAINT cash_movements_payment_method_check
    CHECK (payment_method IS NULL OR payment_method IN ('CASH', 'TRANSFER'));
