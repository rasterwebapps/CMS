-- V223: OneBook integration configuration keys.
--
-- These keys control how OneCMS routes outbound payments (commissions,
-- refunds, scholarships) to the OneBook accounting application.
-- All keys are seeded with safe defaults (integration disabled).

INSERT INTO system_configurations (config_key, config_value, description, data_type, category, is_editable, created_at, updated_at) VALUES
    ('onebook.enabled',
     'false',
     'Master switch for OneBook integration. When true, online payments are routed to OneBook.',
     'BOOLEAN', 'INTEGRATION', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    ('onebook.api_url',
     '',
     'Base URL of the OneBook REST API (e.g. https://api.onebook.in/v1).',
     'STRING', 'INTEGRATION', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    ('onebook.api_key',
     '',
     'API key / bearer token used to authenticate outbound requests to OneBook.',
     'STRING', 'INTEGRATION', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    ('onebook.tenant_id',
     '',
     'Institution / tenant identifier assigned by OneBook for this college.',
     'STRING', 'INTEGRATION', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    ('onebook.webhook_secret',
     '',
     'Shared secret sent by OneBook in X-OneBook-Secret header on status callbacks. Verify this on every inbound webhook call.',
     'STRING', 'INTEGRATION', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    ('onebook.allow_cash_in_cms',
     'true',
     'When OneBook is enabled, cash payments can still be recorded directly in OneCMS. Online payments always go through OneBook.',
     'BOOLEAN', 'INTEGRATION', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)

ON CONFLICT (config_key) DO NOTHING;
