-- Sports-only finance ledger. Apply explicitly; never at application startup.
BEGIN;
CREATE TABLE sports.finance_settings (
 tenant_id uuid PRIMARY KEY REFERENCES sports.tenants(id), cash_base numeric(14,2) NOT NULL DEFAULT 150000 CHECK(cash_base>=0)
);
CREATE TABLE sports.finance_accounts (
 tenant_id uuid NOT NULL REFERENCES sports.tenants(id), id uuid NOT NULL, name varchar(150) NOT NULL,
 kind varchar(10) NOT NULL CHECK(kind IN ('CASH','BANK')), owner_id uuid,
 PRIMARY KEY(tenant_id,id), FOREIGN KEY(tenant_id,owner_id) REFERENCES sports.users(tenant_id,id),
 CHECK(kind='BANK' OR owner_id IS NOT NULL)
);
CREATE UNIQUE INDEX finance_cash_owner ON sports.finance_accounts(tenant_id,owner_id) WHERE kind='CASH';
CREATE TABLE sports.finance_receipts (
 tenant_id uuid NOT NULL, id uuid NOT NULL, actor_id uuid NOT NULL, name varchar(150) NOT NULL,
 content_type varchar(50) NOT NULL, content bytea NOT NULL CHECK(octet_length(content)<=2097152),
 PRIMARY KEY(tenant_id,id), FOREIGN KEY(tenant_id,actor_id) REFERENCES sports.users(tenant_id,id)
);
CREATE TABLE sports.finance_days (
 tenant_id uuid NOT NULL, id uuid NOT NULL, day date NOT NULL, actor_id uuid NOT NULL,
 cash numeric(14,2) NOT NULL CHECK(cash>=0), cash_base numeric(14,2) NOT NULL CHECK(cash_base>=0),
 transfers jsonb NOT NULL, note varchar(1000), created_at timestamptz NOT NULL DEFAULT now(),
 PRIMARY KEY(tenant_id,id), UNIQUE(tenant_id,day), FOREIGN KEY(tenant_id,actor_id) REFERENCES sports.users(tenant_id,id)
);
CREATE TABLE sports.finance_handovers (
 tenant_id uuid NOT NULL, id uuid NOT NULL, sender_id uuid NOT NULL, receiver_id uuid NOT NULL,
 expected numeric(14,2) NOT NULL CHECK(expected>=0), received numeric(14,2),
 status varchar(12) NOT NULL CHECK(status IN ('PENDING','ACCEPTED')), requested_day date NOT NULL,
 accepted_day date, note varchar(1000), created_at timestamptz NOT NULL DEFAULT now(), accepted_at timestamptz,
 PRIMARY KEY(tenant_id,id), FOREIGN KEY(tenant_id,sender_id) REFERENCES sports.users(tenant_id,id),
 FOREIGN KEY(tenant_id,receiver_id) REFERENCES sports.users(tenant_id,id),
 CHECK(sender_id<>receiver_id), CHECK(received IS NULL OR (received>=0 AND received<=expected))
);
CREATE UNIQUE INDEX finance_one_pending_handover ON sports.finance_handovers(tenant_id,sender_id) WHERE status='PENDING';
CREATE TABLE sports.finance_handover_days (
 tenant_id uuid NOT NULL, handover_id uuid NOT NULL, day_id uuid NOT NULL,
 PRIMARY KEY(tenant_id,day_id), FOREIGN KEY(tenant_id,handover_id) REFERENCES sports.finance_handovers(tenant_id,id),
 FOREIGN KEY(tenant_id,day_id) REFERENCES sports.finance_days(tenant_id,id)
);
CREATE TABLE sports.finance_entries (
 tenant_id uuid NOT NULL, id uuid NOT NULL, document_id uuid NOT NULL, account_id uuid NOT NULL,
 day date NOT NULL, kind varchar(20) NOT NULL CHECK(kind IN ('INCOME','EXPENSE','HANDOVER_OUT','HANDOVER_IN','SHORTAGE','ADJUSTMENT')),
 amount numeric(14,2) NOT NULL, actor_id uuid NOT NULL, note varchar(1000), receipt_id uuid, relates_to_month varchar(7),
 created_at timestamptz NOT NULL DEFAULT now(), PRIMARY KEY(tenant_id,id),
 FOREIGN KEY(tenant_id,account_id) REFERENCES sports.finance_accounts(tenant_id,id),
 FOREIGN KEY(tenant_id,actor_id) REFERENCES sports.users(tenant_id,id),
 FOREIGN KEY(tenant_id,receipt_id) REFERENCES sports.finance_receipts(tenant_id,id)
);
CREATE INDEX finance_entries_account ON sports.finance_entries(tenant_id,account_id,day);
CREATE INDEX finance_entries_month ON sports.finance_entries(tenant_id,day);
CREATE TABLE sports.finance_months (
 tenant_id uuid NOT NULL, month varchar(7) NOT NULL, actor_id uuid NOT NULL, note varchar(1000) NOT NULL,
 report jsonb NOT NULL, closed_at timestamptz NOT NULL DEFAULT now(),
 PRIMARY KEY(tenant_id,month), FOREIGN KEY(tenant_id,actor_id) REFERENCES sports.users(tenant_id,id)
);
CREATE TABLE sports.finance_commands (
 tenant_id uuid NOT NULL, id uuid NOT NULL, actor_id uuid NOT NULL, payload text NOT NULL, result jsonb NOT NULL,
 PRIMARY KEY(tenant_id,id), FOREIGN KEY(tenant_id,actor_id) REFERENCES sports.users(tenant_id,id)
);
CREATE FUNCTION sports.reject_finance_mutation() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN RAISE EXCEPTION 'Finance history is immutable; use a justified adjustment'; END $$;
CREATE TRIGGER finance_entries_immutable BEFORE UPDATE OR DELETE ON sports.finance_entries FOR EACH ROW EXECUTE PROCEDURE sports.reject_finance_mutation();
CREATE TRIGGER finance_days_immutable BEFORE UPDATE OR DELETE ON sports.finance_days FOR EACH ROW EXECUTE PROCEDURE sports.reject_finance_mutation();
CREATE TRIGGER finance_months_immutable BEFORE UPDATE OR DELETE ON sports.finance_months FOR EACH ROW EXECUTE PROCEDURE sports.reject_finance_mutation();
CREATE TRIGGER finance_commands_immutable BEFORE UPDATE OR DELETE ON sports.finance_commands FOR EACH ROW EXECUTE PROCEDURE sports.reject_finance_mutation();
INSERT INTO sports.schema_history(version,description) VALUES(5,'Isolated daily cash closing, custody, expenses and monthly ledger');
COMMIT;
