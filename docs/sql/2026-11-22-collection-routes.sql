BEGIN;

CREATE TABLE IF NOT EXISTS collection_routes (
    route_id BIGSERIAL PRIMARY KEY,
    application_user_id BIGINT NOT NULL REFERENCES application_users(application_user_id),
    wallet_id INTEGER NOT NULL REFERENCES wallets(wallet_id),
    route_date DATE NOT NULL,
    name VARCHAR(160) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'draft' CHECK (status IN ('draft', 'active', 'completed')),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS collection_route_stops (
    route_stop_id BIGSERIAL PRIMARY KEY,
    route_id BIGINT NOT NULL REFERENCES collection_routes(route_id) ON DELETE CASCADE,
    service_id BIGINT NOT NULL REFERENCES services(service_id),
    customer_id BIGINT NOT NULL REFERENCES customers(customer_id),
    position INTEGER NOT NULL CHECK (position > 0),
    status VARCHAR(20) NOT NULL DEFAULT 'pending' CHECK (status IN ('pending', 'visited', 'paid', 'not_found', 'rescheduled')),
    observation VARCHAR(500),
    visited_at TIMESTAMP,
    UNIQUE (route_id, service_id),
    UNIQUE (route_id, position)
);

CREATE INDEX IF NOT EXISTS idx_collection_routes_user_date ON collection_routes(application_user_id, route_date DESC);
CREATE INDEX IF NOT EXISTS idx_collection_route_stops_route ON collection_route_stops(route_id, position);

COMMIT;
