-- V217: Backfill guideline_commission_amount for enquiries created before V213 was applied.
-- For each enquiry that has both an agent and a program, look up the first matching
-- agent_commission_guidelines row (ordered by id to match Java findFirst() behaviour).
-- Enquiries with no matching guideline remain NULL.
UPDATE enquiries e
SET    guideline_commission_amount = (
    SELECT acg.suggested_commission
    FROM   agent_commission_guidelines acg
    WHERE  acg.agent_id   = e.agent_id
    AND    acg.program_id = e.program_id
    ORDER  BY acg.id
    LIMIT  1
)
WHERE  e.agent_id   IS NOT NULL
AND    e.program_id IS NOT NULL
AND    e.guideline_commission_amount IS NULL;
