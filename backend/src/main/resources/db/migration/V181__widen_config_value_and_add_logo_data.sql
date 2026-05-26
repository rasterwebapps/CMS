ALTER TABLE system_configurations ALTER COLUMN config_value TYPE TEXT;

INSERT INTO system_configurations (config_key, config_value, description, data_type, category, is_editable, created_at, updated_at) VALUES
('college.logo_data', '', 'Base64 data URL of the college logo uploaded via the Branding settings page', 'STRING', 'BRANDING', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
