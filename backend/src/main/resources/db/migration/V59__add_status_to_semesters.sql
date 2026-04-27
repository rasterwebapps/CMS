ALTER TABLE semesters
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'UPCOMING';

-- Back-fill existing rows with the correct derived status
UPDATE semesters
SET status = CASE
    WHEN CURRENT_DATE < start_date THEN 'UPCOMING'
    WHEN CURRENT_DATE > end_date   THEN 'COMPLETED'
    ELSE                                'ONGOING'
END;
