-- college.logo_url is kept for optional URL-based logos; actual uploaded logos use college.logo_data (V181)
INSERT INTO system_configurations (config_key, config_value, description, data_type, category, is_editable, created_at, updated_at) VALUES
('college.logo_url', '', 'Optional URL of the college logo (leave blank when using the uploaded logo)', 'STRING', 'BRANDING', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
