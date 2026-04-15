-- Add referral type and fee guideline fields to enquiries table
ALTER TABLE enquiries ADD COLUMN referral_type_id BIGINT REFERENCES referral_types(id);
ALTER TABLE enquiries ADD COLUMN fee_guideline_total NUMERIC(12,2);
ALTER TABLE enquiries ADD COLUMN referral_additional_amount NUMERIC(12,2);
ALTER TABLE enquiries ADD COLUMN final_calculated_fee NUMERIC(12,2);
ALTER TABLE enquiries ADD COLUMN year_wise_fees TEXT;

-- Add admin fee finalization fields
ALTER TABLE enquiries ADD COLUMN finalized_total_fee NUMERIC(12,2);
ALTER TABLE enquiries ADD COLUMN finalized_discount_amount NUMERIC(12,2);
ALTER TABLE enquiries ADD COLUMN finalized_discount_reason VARCHAR(500);
ALTER TABLE enquiries ADD COLUMN finalized_net_fee NUMERIC(12,2);
ALTER TABLE enquiries ADD COLUMN finalized_by VARCHAR(255);
ALTER TABLE enquiries ADD COLUMN finalized_at TIMESTAMP WITH TIME ZONE;

CREATE INDEX idx_enquiries_referral_type ON enquiries(referral_type_id);
