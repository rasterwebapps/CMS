CREATE TABLE system_configurations (
    id BIGSERIAL PRIMARY KEY,
    config_key VARCHAR(255) NOT NULL UNIQUE,
    config_value VARCHAR(1000) NOT NULL,
    description VARCHAR(500),
    data_type VARCHAR(50) NOT NULL,
    category VARCHAR(100) NOT NULL,
    is_editable BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_system_configurations_category ON system_configurations(category);
