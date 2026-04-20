-- Migrate any existing CONVERTED enquiry records to the new ADMITTED status.
-- The CONVERTED value is kept in the application enum for backward compatibility
-- but ADMITTED is the canonical terminal success state going forward.
UPDATE enquiries SET status = 'ADMITTED' WHERE status = 'CONVERTED';
