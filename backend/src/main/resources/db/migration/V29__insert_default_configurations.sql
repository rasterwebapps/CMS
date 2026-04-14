INSERT INTO system_configurations (config_key, config_value, description, data_type, category, is_editable, created_at, updated_at) VALUES
('penalty.daily_rate', '100', 'Daily late fee penalty rate in rupees', 'DECIMAL', 'PENALTY', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('college.name', 'SKS College of Nursing', 'College name for branding', 'STRING', 'BRANDING', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('college.trust_name', 'VS Educational Trust', 'Trust name for branding', 'STRING', 'BRANDING', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('college.registration_number', '579/1997', 'Trust registration number', 'STRING', 'BRANDING', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('college.address', 'No.31, Nelkarapatti, Salem - 636 010', 'College address', 'STRING', 'BRANDING', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('receipt.prefix', 'RCP', 'Prefix for receipt numbers', 'STRING', 'RECEIPT', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('receipt.next_number', '1', 'Next sequential receipt number', 'INTEGER', 'RECEIPT', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
