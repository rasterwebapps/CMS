-- Add file storage columns to enquiry_documents so that scanned/uploaded
-- documents can be stored in the database alongside their metadata, and
-- later retrieved (viewed/downloaded) from the enquiry view screen.
ALTER TABLE enquiry_documents ADD COLUMN file_name    VARCHAR(255);
ALTER TABLE enquiry_documents ADD COLUMN content_type VARCHAR(255);
ALTER TABLE enquiry_documents ADD COLUMN file_size    BIGINT;
ALTER TABLE enquiry_documents ADD COLUMN file_data    BYTEA;
ALTER TABLE enquiry_documents ADD COLUMN uploaded_at  TIMESTAMP WITH TIME ZONE;
