CREATE TABLE stock_events (
    id BIGINT NOT NULL AUTO_INCREMENT,
    event_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(60) NOT NULL,
    account_id VARCHAR(100) NOT NULL,
    sku VARCHAR(120) NOT NULL,
    marketplace VARCHAR(80) NULL,
    external_order_id VARCHAR(120) NULL,
    occurred_at TIMESTAMP(6) NOT NULL,
    received_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    processing_status VARCHAR(40) NOT NULL,
    payload JSON NOT NULL,
    payload_hash CHAR(64) NOT NULL,
    failure_reason VARCHAR(500) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_stock_events_event_id UNIQUE (event_id),
    INDEX idx_stock_events_aggregate_order (account_id, sku, occurred_at, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE stock_projections (
    id BIGINT NOT NULL AUTO_INCREMENT,
    account_id VARCHAR(100) NOT NULL,
    sku VARCHAR(120) NOT NULL,
    available INT NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_stock_projections_account_sku UNIQUE (account_id, sku),
    CONSTRAINT ck_stock_projections_available_non_negative CHECK (available >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE stock_movements (
    id BIGINT NOT NULL AUTO_INCREMENT,
    event_id VARCHAR(100) NOT NULL,
    account_id VARCHAR(100) NOT NULL,
    sku VARCHAR(120) NOT NULL,
    movement_type VARCHAR(60) NOT NULL,
    quantity_delta INT NOT NULL,
    previous_available INT NOT NULL,
    current_available INT NOT NULL,
    reason VARCHAR(255) NULL,
    occurred_at TIMESTAMP(6) NOT NULL,
    processed_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_stock_movements_event_id FOREIGN KEY (event_id) REFERENCES stock_events (event_id),
    INDEX idx_stock_movements_aggregate_order (account_id, sku, occurred_at, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE order_reservations (
    id BIGINT NOT NULL AUTO_INCREMENT,
    account_id VARCHAR(100) NOT NULL,
    marketplace VARCHAR(80) NOT NULL,
    external_order_id VARCHAR(120) NOT NULL,
    sku VARCHAR(120) NOT NULL,
    quantity INT NOT NULL,
    status VARCHAR(40) NOT NULL,
    created_event_id VARCHAR(100) NOT NULL,
    cancelled_event_id VARCHAR(100) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_order_reservations_logical_order UNIQUE (account_id, marketplace, external_order_id, sku),
    CONSTRAINT fk_order_reservations_created_event_id FOREIGN KEY (created_event_id) REFERENCES stock_events (event_id),
    CONSTRAINT fk_order_reservations_cancelled_event_id FOREIGN KEY (cancelled_event_id) REFERENCES stock_events (event_id),
    CONSTRAINT ck_order_reservations_quantity_positive CHECK (quantity > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
