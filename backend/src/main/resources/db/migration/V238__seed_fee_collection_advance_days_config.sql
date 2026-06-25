INSERT INTO system_configurations (config_key, config_value, description, data_type, category, is_editable, created_at, updated_at)
VALUES (
    'fee.collection_advance_days',
    '30',
    'Number of days before a term starts that its fee due date is allowed to be set',
    'INTEGER',
    'FEE',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (config_key) DO NOTHING;
