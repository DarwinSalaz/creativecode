BEGIN;

INSERT INTO user_profiles (user_profile_id, name, description)
VALUES (4, 'supervisor', 'Supervisor con acceso de consulta a carteras asociadas y registro de abonos')
ON CONFLICT (user_profile_id) DO UPDATE
SET name = EXCLUDED.name,
    description = EXCLUDED.description;

COMMIT;
