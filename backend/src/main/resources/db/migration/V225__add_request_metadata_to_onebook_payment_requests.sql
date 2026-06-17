-- V225: Add request_metadata column to onebook_payment_requests.
-- Stores initiating-side context (e.g. scholarship academicYearId / termNumber)
-- so the webhook handler can reconstruct records when OneBook calls back.

ALTER TABLE onebook_payment_requests ADD COLUMN request_metadata JSONB;
