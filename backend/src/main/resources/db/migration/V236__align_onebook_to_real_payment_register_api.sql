-- V236: Align OneBook integration with the real OneBook payment-register API
-- contract (confirmed from OneBook's published API spec — previously placeholder).
--
-- Real contract requires a generated invoice/document number sent at creation
-- time, and OneBook reports the assigned register ID plus final payment
-- details via two separate inbound callbacks rather than one generic webhook.

INSERT INTO system_configurations (config_key, config_value, description, data_type, category, is_editable, created_at, updated_at) VALUES
    ('onebook.zone_name',
     'Asia/Calcutta',
     'Timezone identifier sent on every OneBook authentication request.',
     'STRING', 'INTEGRATION', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (config_key) DO NOTHING;

ALTER TABLE onebook_payment_requests
    ADD COLUMN invoice_number        VARCHAR(60),
    ADD COLUMN onebook_payment_number VARCHAR(100),
    ADD COLUMN onebook_bank_name      VARCHAR(150),
    ADD COLUMN onebook_payment_by     VARCHAR(255),
    ADD COLUMN onebook_batch_number   VARCHAR(100);

CREATE INDEX idx_obpr_invoice_number ON onebook_payment_requests(invoice_number);

ALTER TABLE enquiries
    ADD COLUMN commission_number VARCHAR(30);

ALTER TABLE scholarship_disbursements
    ADD COLUMN disbursement_number VARCHAR(30);
