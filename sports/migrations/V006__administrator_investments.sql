-- Explicit migration only. New sports relations; does not change cash, finance or sales data.
BEGIN;
CREATE TABLE sports.investments (
 tenant_id uuid NOT NULL REFERENCES sports.tenants(id),
 id uuid NOT NULL,
 day date NOT NULL,
 category varchar(20) NOT NULL CHECK(category IN ('EQUIPMENT','IMPROVEMENT','REPAIR','OTHER')),
 description varchar(2000) NOT NULL CHECK(length(btrim(description))>0),
 amount numeric(14,2) NOT NULL CHECK(amount>0),
 actor_id uuid NOT NULL,
 created_at timestamptz NOT NULL DEFAULT now(),
 PRIMARY KEY(tenant_id,id),
 FOREIGN KEY(tenant_id,actor_id) REFERENCES sports.users(tenant_id,id)
);
CREATE INDEX investments_tenant_day ON sports.investments(tenant_id,day DESC);
CREATE TABLE sports.investment_contributions (
 tenant_id uuid NOT NULL,
 investment_id uuid NOT NULL,
 admin_id uuid NOT NULL,
 amount numeric(14,2) NOT NULL CHECK(amount>0),
 PRIMARY KEY(tenant_id,investment_id,admin_id),
 FOREIGN KEY(tenant_id,investment_id) REFERENCES sports.investments(tenant_id,id),
 FOREIGN KEY(tenant_id,admin_id) REFERENCES sports.users(tenant_id,id)
);
CREATE INDEX investment_contributions_admin ON sports.investment_contributions(tenant_id,admin_id);
CREATE TRIGGER investments_immutable BEFORE UPDATE OR DELETE ON sports.investments FOR EACH ROW EXECUTE PROCEDURE sports.reject_finance_mutation();
CREATE TRIGGER investment_contributions_immutable BEFORE UPDATE OR DELETE ON sports.investment_contributions FOR EACH ROW EXECUTE PROCEDURE sports.reject_finance_mutation();
INSERT INTO sports.schema_history(version,description) VALUES(6,'Administrator investments and external contributions, separate from cash');
COMMIT;
