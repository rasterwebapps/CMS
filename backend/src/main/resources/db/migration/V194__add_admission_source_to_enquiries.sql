-- Distinguish normal pipeline enquiries from synthetic ones created by Legacy Direct Admit.
-- ENQUIRY_FLOW = standard path (enquiry → fee finalization → documents → admission)
-- DIRECT_ADMIT  = created automatically when a legacy student is admitted via the direct-admit form
ALTER TABLE enquiries ADD COLUMN admission_source VARCHAR(20) NOT NULL DEFAULT 'ENQUIRY_FLOW';
UPDATE enquiries SET admission_source = 'ENQUIRY_FLOW' WHERE admission_source IS NULL;
