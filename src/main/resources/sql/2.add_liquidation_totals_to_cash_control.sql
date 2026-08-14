ALTER TABLE cash_control ADD COLUMN IF NOT EXISTS partial_liquidations_total DOUBLE PRECISION DEFAULT 0;
ALTER TABLE cash_control ADD COLUMN IF NOT EXISTS final_liquidations_total DOUBLE PRECISION DEFAULT 0;

UPDATE cash_control
SET partial_liquidations_total = COALESCE(partial_liquidations_total, 0),
    final_liquidations_total = COALESCE(final_liquidations_total, 0)
WHERE partial_liquidations_total IS NULL
   OR final_liquidations_total IS NULL;
