-- V285 defined outcome VARCHAR(20), but PROMOTED_WITH_ARREARS (one of PromotionOutcome's own
-- five values) is 21 characters -- every promotion decision with that outcome has always failed
-- to insert with a Postgres "value too long for type character varying(20)" error, which
-- GlobalExceptionHandler's generic DataIntegrityViolationException fallback then reports to the
-- caller as a misleading "duplicate record" 409 instead of the real cause. Widened with headroom
-- for any future outcome value, not sized to exactly the longest current one.
ALTER TABLE student_promotion_decisions ALTER COLUMN outcome TYPE VARCHAR(30);
