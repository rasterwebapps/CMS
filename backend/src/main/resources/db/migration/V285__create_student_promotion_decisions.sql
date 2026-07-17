-- Audit trail for Student Promotion/Progression decisions (INC / Dr. MGR Medical University
-- model): one row per student each time an admin promotes a cohort from one term to the next.
-- outcome is one of PROMOTED, PROMOTED_WITH_ARREARS (carrying a failed subject forward),
-- DETAINED_REPEAT (stays in the same term for a repeat cycle), or GRADUATED (final term, no
-- arrears) — EXCLUDED students are skipped by the service and never get a row here.
-- to_term_instance_id is nullable because DETAINED_REPEAT students receive no new enrollment.

CREATE TABLE student_promotion_decisions (
    id                      BIGSERIAL PRIMARY KEY,
    student_id              BIGINT NOT NULL REFERENCES students(id),
    cohort_id               BIGINT NOT NULL REFERENCES cohorts(id),
    from_term_instance_id   BIGINT NOT NULL REFERENCES term_instances(id),
    to_term_instance_id     BIGINT REFERENCES term_instances(id),
    outcome                 VARCHAR(20) NOT NULL,
    decided_by              VARCHAR(100) NOT NULL,
    decided_at              TIMESTAMP WITH TIME ZONE NOT NULL,
    remarks                 VARCHAR(500),
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at              TIMESTAMP WITH TIME ZONE NOT NULL
);

-- Snapshot of the arrear subjects a student was carrying at the moment of this decision, so
-- promotion history remains accurate even if later exam results change the live arrear list.
CREATE TABLE student_promotion_decision_arrears (
    decision_id   BIGINT NOT NULL REFERENCES student_promotion_decisions(id),
    subject_id    BIGINT NOT NULL REFERENCES subjects(id)
);
