-- Public login aliases keep internal tenant UUIDs and all foreign keys unchanged.
BEGIN;
ALTER TABLE sports.tenants ADD COLUMN slug varchar(63);
UPDATE sports.tenants SET slug = id::text WHERE slug IS NULL;
ALTER TABLE sports.tenants ALTER COLUMN slug SET NOT NULL;
ALTER TABLE sports.tenants ADD CONSTRAINT tenants_slug_format
    CHECK (slug ~ '^[a-z0-9][a-z0-9-]{1,62}$');
ALTER TABLE sports.tenants ADD CONSTRAINT tenants_slug_unique UNIQUE (slug);
INSERT INTO sports.schema_history(version,description) VALUES(4,'Public tenant login aliases');
COMMIT;
