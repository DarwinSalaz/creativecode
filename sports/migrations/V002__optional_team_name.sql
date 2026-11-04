-- Additive migration. Applied only to the authorized local environment; production requires approval.
BEGIN;
ALTER TABLE sports.reservations ADD COLUMN team_name varchar(150);
COMMENT ON COLUMN sports.reservations.guest_name IS 'Nombre de la persona que realiza la reserva; también para clientes ocasionales.';
COMMENT ON COLUMN sports.reservations.team_name IS 'Equipo opcional. Nunca sustituye al nombre del cliente.';
INSERT INTO sports.schema_history(version,description) VALUES (2,'Optional team name; customer remains the reservation identity');
COMMIT;
