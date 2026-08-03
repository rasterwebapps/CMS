-- Whether faculty can see the Planned target date/variance for their own subject's syllabus units
-- (admin/HOD always see it regardless of this toggle) -- see PortionBlueprintService's projection
-- endpoint and the Progress Report UI's faculty-facing gate. Defaults to hidden: faculty still see
-- the full ordered unit list and log actual completion exactly as today; only the target-date
-- comparison used for evaluation is gated by this flag.
INSERT INTO system_configurations (config_key, config_value, description, data_type, category, is_editable, created_at, updated_at)
VALUES (
    'PORTION_PLAN_VISIBLE_TO_FACULTY', 'false',
    'Whether faculty can see planned completion dates/variance for their own subjects (admin/HOD always see it)',
    'BOOLEAN', 'ACADEMICS', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
)
ON CONFLICT (config_key) DO NOTHING;
