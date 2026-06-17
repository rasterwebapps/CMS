-- V226: Realign OneBook config keys to match the actual OneBook API contract.
--
-- The initial seeding (V223) assumed a Bearer-token / tenant-ID model.
-- The real OneBook API uses username/password auth plus org_id, branch_id,
-- app_name, and paper_name — identical to how OnePharmacy is integrated.
-- Remove the wrong keys and seed the correct ones.

DELETE FROM system_configurations WHERE config_key IN ('onebook.api_key', 'onebook.tenant_id');

INSERT INTO system_configurations (config_key, config_value, description, data_type, category, is_editable, created_at, updated_at) VALUES
    ('onebook.username',
     '',
     'Username for authenticating outbound requests to OneBook.',
     'STRING', 'INTEGRATION', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    ('onebook.password',
     '',
     'Password for authenticating outbound requests to OneBook. Keep confidential.',
     'STRING', 'INTEGRATION', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    ('onebook.org_id',
     '',
     'Organisation ID assigned by OneBook for this institution.',
     'STRING', 'INTEGRATION', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    ('onebook.branch_id',
     '',
     'Branch ID within OneBook that represents this college.',
     'STRING', 'INTEGRATION', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    ('onebook.app_name',
     'ONECMS',
     'Application name registered in OneBook for this integration.',
     'STRING', 'INTEGRATION', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    ('onebook.paper_name',
     'SKS College Of Nursing',
     'Paper / entity name used in OneBook to identify the source of payments.',
     'STRING', 'INTEGRATION', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    ('onebook.integration_date',
     '',
     'Date OneCMS was integrated with OneBook (ISO-8601, e.g. 2024-11-10). Informational / audit field.',
     'STRING', 'INTEGRATION', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)

ON CONFLICT (config_key) DO NOTHING;
