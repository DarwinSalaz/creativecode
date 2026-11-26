ALTER TABLE customers
    ADD COLUMN IF NOT EXISTS latitude NUMERIC(10, 7),
    ADD COLUMN IF NOT EXISTS longitude NUMERIC(10, 7);

ALTER TABLE customers
    DROP CONSTRAINT IF EXISTS customers_latitude_check;

ALTER TABLE customers
    ADD CONSTRAINT customers_latitude_check
    CHECK (latitude IS NULL OR latitude BETWEEN -90 AND 90);

ALTER TABLE customers
    DROP CONSTRAINT IF EXISTS customers_longitude_check;

ALTER TABLE customers
    ADD CONSTRAINT customers_longitude_check
    CHECK (longitude IS NULL OR longitude BETWEEN -180 AND 180);
