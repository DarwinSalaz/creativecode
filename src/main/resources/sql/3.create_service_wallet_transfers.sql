CREATE TABLE IF NOT EXISTS service_wallet_transfers (
    service_wallet_transfer_id BIGSERIAL PRIMARY KEY,
    service_id BIGINT NOT NULL REFERENCES services(service_id),
    source_wallet_id INTEGER NOT NULL REFERENCES wallets(wallet_id),
    target_wallet_id INTEGER NOT NULL REFERENCES wallets(wallet_id),
    source_customer_id BIGINT NOT NULL REFERENCES customers(customer_id),
    target_customer_id BIGINT NOT NULL REFERENCES customers(customer_id),
    customer_strategy VARCHAR(40) NOT NULL,
    cash_control_id BIGINT REFERENCES cash_control(cash_control_id),
    transferred_by VARCHAR(255) NOT NULL,
    justification VARCHAR(1000) NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_service_wallet_transfers_service
    ON service_wallet_transfers(service_id);
