-- V227: Add bank account details to the students table.
-- Used when pushing refund / scholarship disbursement payments to OneBook.
-- All columns are nullable — bank details are only collected when a payout is needed.

ALTER TABLE students ADD COLUMN IF NOT EXISTS bank_account_holder  VARCHAR(200);
ALTER TABLE students ADD COLUMN IF NOT EXISTS bank_account_number  VARCHAR(30);
ALTER TABLE students ADD COLUMN IF NOT EXISTS bank_ifsc_code       VARCHAR(15);
ALTER TABLE students ADD COLUMN IF NOT EXISTS bank_branch          VARCHAR(100);
ALTER TABLE students ADD COLUMN IF NOT EXISTS bank_name            VARCHAR(100);
ALTER TABLE students ADD COLUMN IF NOT EXISTS bank_account_type    VARCHAR(20);
