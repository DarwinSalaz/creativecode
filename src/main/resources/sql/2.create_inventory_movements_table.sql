-- Crear tabla para movimientos de inventario
CREATE TABLE IF NOT EXISTS inventory_movements (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    movement_type VARCHAR(10) NOT NULL CHECK (movement_type IN ('ENTRADA', 'SALIDA')),
    quantity INT NOT NULL,
    previous_quantity INT NOT NULL,
    new_quantity INT NOT NULL,
    user_id BIGINT NOT NULL,
    username VARCHAR(255) NOT NULL,
    movement_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    description TEXT,
    wallet_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- Índices para mejorar el rendimiento
    INDEX idx_product_id (product_id),
    INDEX idx_wallet_id (wallet_id),
    INDEX idx_user_id (user_id),
    INDEX idx_movement_date (movement_date),
    INDEX idx_movement_type (movement_type),
    
    -- Claves foráneas (opcional, dependiendo de la configuración de la BD)
    -- FOREIGN KEY (product_id) REFERENCES products(product_id),
    -- FOREIGN KEY (wallet_id) REFERENCES wallets(wallet_id),
    -- FOREIGN KEY (user_id) REFERENCES application_users(user_id)
);

-- Insertar algunos datos de ejemplo (opcional)
-- INSERT INTO inventory_movements (product_id, movement_type, quantity, previous_quantity, new_quantity, user_id, username, wallet_id, description) 
-- VALUES 
-- (1, 'ENTRADA', 100, 0, 100, 1, 'admin', 1, 'Stock inicial'),
-- (1, 'SALIDA', 10, 100, 90, 1, 'admin', 1, 'Venta a cliente'); 