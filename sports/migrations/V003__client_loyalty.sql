-- Additive, sports-only. No automatic runtime migration.
BEGIN;
ALTER TABLE sports.reservations ADD COLUMN reward_percent integer NOT NULL DEFAULT 0 CHECK (reward_percent IN (0,50,100));
CREATE TABLE sports.loyalty_policies (
 tenant_id uuid NOT NULL, client_id uuid NOT NULL, state jsonb NOT NULL,
 PRIMARY KEY(tenant_id,client_id), FOREIGN KEY(tenant_id,client_id) REFERENCES sports.clients(tenant_id,id)
);
CREATE TABLE sports.loyalty_ledger (
 tenant_id uuid NOT NULL, id uuid NOT NULL, client_id uuid NOT NULL, reservation_id uuid,
 event_type varchar(30) NOT NULL, actor_id uuid NOT NULL, definition jsonb NOT NULL,
 created_at timestamptz NOT NULL DEFAULT now(), PRIMARY KEY(tenant_id,id),
 FOREIGN KEY(tenant_id,client_id) REFERENCES sports.clients(tenant_id,id),
 FOREIGN KEY(tenant_id,reservation_id) REFERENCES sports.reservations(tenant_id,id),
 FOREIGN KEY(tenant_id,actor_id) REFERENCES sports.users(tenant_id,id)
);
CREATE UNIQUE INDEX loyalty_one_finish ON sports.loyalty_ledger(tenant_id,reservation_id) WHERE event_type IN ('PLAYED','REDEEMED');
CREATE INDEX loyalty_client_history ON sports.loyalty_ledger(tenant_id,client_id,created_at DESC);
CREATE FUNCTION sports.reject_loyalty_mutation() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'Loyalty history is immutable'; END $$;
CREATE TRIGGER loyalty_immutable BEFORE UPDATE OR DELETE ON sports.loyalty_ledger FOR EACH ROW EXECUTE PROCEDURE sports.reject_loyalty_mutation();
INSERT INTO sports.schema_history(version,description) VALUES(3,'Client loyalty policies, immutable ledger and reservation reward');
COMMIT;
