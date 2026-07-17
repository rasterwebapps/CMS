-- ExamResult gains a persisted PASS/FAIL outcome, derived from marksObtained vs. 50% of
-- Examination.maxMarks whenever a result is PUBLISHED. This is the signal the new Student
-- Promotion feature uses to detect subject-wise arrears (INC / Dr. MGR Medical University
-- promotion model: a student can carry a failed subject into the next year, but must clear it
-- before Final Year). Stored rather than computed on read so "latest published result per
-- student+subject" queries stay a simple filter, and so a later supplementary result naturally
-- supersedes an old FAIL. External/university marks only for v1 — internal/CIA marks don't exist
-- in this system yet.

ALTER TABLE exam_results ADD COLUMN outcome VARCHAR(10);

-- Backfill already-published historical results so existing exam data participates in arrear
-- detection immediately, not only for results saved from now on.
UPDATE exam_results er
SET outcome = CASE
    WHEN er.marks_obtained IS NULL OR ex.max_marks IS NULL THEN NULL
    WHEN er.marks_obtained * 100 >= ex.max_marks * 50 THEN 'PASS'
    ELSE 'FAIL'
END
FROM examinations ex
WHERE er.examination_id = ex.id AND er.status = 'PUBLISHED';
