-- REVIEW ONLY: never executed by Spring or Gradle. Requires explicit authorization.
-- Run with a dedicated migration owner using a direct PostgreSQL connection.
BEGIN;
CREATE SCHEMA sports;
CREATE TABLE sports.schema_history (version integer PRIMARY KEY, description text NOT NULL, installed_at timestamptz NOT NULL DEFAULT now());
CREATE TABLE sports.tenants (
 id uuid PRIMARY KEY,
 settings jsonb NOT NULL,
 created_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE sports.venues (
 tenant_id uuid NOT NULL REFERENCES sports.tenants(id), id uuid NOT NULL,
 name varchar(120) NOT NULL, address varchar(250), PRIMARY KEY (tenant_id,id)
);
CREATE TABLE sports.users (
 tenant_id uuid NOT NULL REFERENCES sports.tenants(id), id uuid NOT NULL,
 username varchar(100) NOT NULL, name varchar(150) NOT NULL, password_hash varchar(100) NOT NULL,
 role varchar(20) NOT NULL CHECK (role IN ('ADMIN','EMPLOYEE')), active boolean NOT NULL DEFAULT true,
 created_at timestamptz NOT NULL DEFAULT now(), PRIMARY KEY (tenant_id,id), UNIQUE (tenant_id,username)
);
CREATE TABLE sports.sessions (
 token_hash char(64) PRIMARY KEY, tenant_id uuid NOT NULL, user_id uuid NOT NULL,
 expires_at timestamptz NOT NULL, FOREIGN KEY (tenant_id,user_id) REFERENCES sports.users(tenant_id,id)
);
CREATE INDEX sessions_expiry ON sports.sessions(expires_at);
CREATE TABLE sports.courts (
 tenant_id uuid NOT NULL REFERENCES sports.tenants(id), id uuid NOT NULL, venue_id uuid,
 name varchar(120) NOT NULL, sport varchar(80) NOT NULL, capacity integer NOT NULL CHECK (capacity BETWEEN 1 AND 100),
 duration_minutes integer NOT NULL CHECK (duration_minutes BETWEEN 5 AND 720), color varchar(7) NOT NULL,
 active boolean NOT NULL DEFAULT true, PRIMARY KEY (tenant_id,id),
 FOREIGN KEY (tenant_id,venue_id) REFERENCES sports.venues(tenant_id,id)
);
CREATE TABLE sports.clients (
 tenant_id uuid NOT NULL REFERENCES sports.tenants(id), id uuid NOT NULL, name varchar(150) NOT NULL,
 phone varchar(30), notes varchar(2000), PRIMARY KEY (tenant_id,id)
);
CREATE TABLE sports.teachers (
 tenant_id uuid NOT NULL REFERENCES sports.tenants(id), id uuid NOT NULL, name varchar(150) NOT NULL,
 phone varchar(30), notes varchar(2000), PRIMARY KEY (tenant_id,id)
);
CREATE TABLE sports.pricing_rules (
 tenant_id uuid NOT NULL, id uuid NOT NULL, court_id uuid NOT NULL,
 weekday integer NOT NULL CHECK (weekday BETWEEN 1 AND 7), from_minute integer NOT NULL CHECK (from_minute BETWEEN 0 AND 1439),
 to_minute integer NOT NULL CHECK (to_minute > from_minute AND to_minute <= 1440), amount numeric(14,2) NOT NULL CHECK (amount >= 0),
 PRIMARY KEY (tenant_id,id), FOREIGN KEY (tenant_id,court_id) REFERENCES sports.courts(tenant_id,id)
);
CREATE TABLE sports.recurrence_rules (
 tenant_id uuid NOT NULL, id uuid NOT NULL, court_id uuid NOT NULL, definition jsonb NOT NULL,
 PRIMARY KEY (tenant_id,id), FOREIGN KEY (tenant_id,court_id) REFERENCES sports.courts(tenant_id,id)
);
CREATE TABLE sports.reservations (
 tenant_id uuid NOT NULL, id uuid NOT NULL, court_id uuid NOT NULL,
 guest_name varchar(150) NOT NULL, phone varchar(30), client_id uuid, teacher_id uuid, recurrence_id uuid,
 kind varchar(20) NOT NULL CHECK (kind IN ('NORMAL','FIXED','CLASS','BLOCK')),
 status varchar(30) NOT NULL CHECK (status IN ('PENDING','CONFIRMED','IN_PROGRESS','FINISHED','CANCELLED_CLIENT','CANCELLED_RAIN','NO_SHOW')),
 scheduled_start_at timestamptz NOT NULL, scheduled_end_at timestamptz NOT NULL,
 actual_started_at timestamptz, actual_ended_at timestamptz, water_delivered_at timestamptz, sound_device varchar(100),
 notes varchar(2000), price_snapshot numeric(14,2), version integer NOT NULL DEFAULT 0,
 created_by uuid NOT NULL, updated_by uuid NOT NULL,
 created_at timestamptz NOT NULL DEFAULT now(), updated_at timestamptz NOT NULL DEFAULT now(),
 PRIMARY KEY (tenant_id,id), CHECK (scheduled_end_at > scheduled_start_at),
 CHECK (actual_ended_at IS NULL OR (actual_started_at IS NOT NULL AND actual_ended_at >= actual_started_at)),
 CHECK (status <> 'IN_PROGRESS' OR (actual_started_at IS NOT NULL AND actual_ended_at IS NULL)),
 CHECK (status <> 'FINISHED' OR actual_ended_at IS NOT NULL),
 CHECK (kind <> 'CLASS' OR teacher_id IS NOT NULL),
 FOREIGN KEY (tenant_id,court_id) REFERENCES sports.courts(tenant_id,id),
 FOREIGN KEY (tenant_id,client_id) REFERENCES sports.clients(tenant_id,id),
 FOREIGN KEY (tenant_id,teacher_id) REFERENCES sports.teachers(tenant_id,id),
 FOREIGN KEY (tenant_id,recurrence_id) REFERENCES sports.recurrence_rules(tenant_id,id),
 FOREIGN KEY (tenant_id,created_by) REFERENCES sports.users(tenant_id,id),
 FOREIGN KEY (tenant_id,updated_by) REFERENCES sports.users(tenant_id,id),
 UNIQUE (tenant_id,recurrence_id,scheduled_start_at)
);
CREATE INDEX reservations_calendar ON sports.reservations(tenant_id,court_id,scheduled_start_at);
CREATE INDEX reservations_pending ON sports.reservations(tenant_id,status,scheduled_start_at);
CREATE INDEX reservations_client ON sports.reservations(tenant_id,client_id,scheduled_start_at);
CREATE UNIQUE INDEX one_running_match_per_court ON sports.reservations(tenant_id,court_id) WHERE status='IN_PROGRESS';
CREATE TABLE sports.audit_events (
 tenant_id uuid NOT NULL, id uuid NOT NULL, actor_id uuid NOT NULL, entity_id uuid NOT NULL,
 action varchar(60) NOT NULL, reason varchar(500), created_at timestamptz NOT NULL DEFAULT now(),
 PRIMARY KEY (tenant_id,id), FOREIGN KEY (tenant_id,actor_id) REFERENCES sports.users(tenant_id,id)
);
CREATE INDEX audit_entity ON sports.audit_events(tenant_id,entity_id,created_at);
INSERT INTO sports.schema_history(version,description) VALUES (1,'Sports isolated MVP');
COMMIT;
-- No tenants, accounts or business records are seeded. Provisioning is a separate authorized step.
-- Runtime role: USAGE on sports; SELECT/INSERT/UPDATE on mutable sports tables;
-- audit_events: SELECT/INSERT only. Never grant access to public or schema ownership.
