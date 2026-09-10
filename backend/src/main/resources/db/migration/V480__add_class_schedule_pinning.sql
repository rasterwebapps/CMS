-- Pinning for DRAFT skeleton cells.
--
-- Until now every Global Auto-Schedule run began by clearing the whole DRAFT grid
-- (TimetableGlobalAutoScheduleService#purgeDraftCellsForRebuild), which made a re-run idempotent
-- but also destroyed any manual drag-move/swap an admin had made -- the tradeoff was documented
-- and deliberate. That is incompatible with the draft-review model, where the produced grid is
-- explicitly a starting point the admin then reshapes by hand before approving.
--
-- A pinned cell is one a human positioned on purpose. The rebuild leaves it standing and places
-- everything else around it, so manual work survives automation instead of being overwritten.
-- Defaults to false so every existing row keeps exactly today's behaviour.

ALTER TABLE class_schedules
    ADD COLUMN IF NOT EXISTS is_pinned BOOLEAN NOT NULL DEFAULT FALSE;

-- Only DRAFT rows are ever rebuilt, so only they can meaningfully be pinned; the partial index
-- matches the exact predicate purgeDraftCellsForRebuild filters on.
CREATE INDEX IF NOT EXISTS idx_class_schedules_pinned
    ON class_schedules (term_instance_id)
    WHERE is_pinned;

-- Pin/unpin is its own operation, not a free rider on MOVE: unpinning re-exposes a cell to being
-- overwritten by the next automation run, which is a materially different (and less reversible)
-- consequence than repositioning it. Per the operation-wise permission mapping rule it therefore
-- gets its own dedicated permission, defaulted to whichever role already holds the closest
-- existing tier -- TIMETABLE_SKELETON_MOVE, the other manual grid-editing capability.
INSERT INTO permissions (code, display_name, category, screen_label, created_at) VALUES
    ('TIMETABLE_SKELETON_PIN', 'Pin/Unpin Timetable Skeleton Cells', 'CURRICULUM', 'Timetable', CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT rp.role_id, new_p.id
FROM role_permissions rp
JOIN permissions old_p ON rp.permission_id = old_p.id AND old_p.code = 'TIMETABLE_SKELETON_MOVE'
CROSS JOIN (SELECT id FROM permissions WHERE code = 'TIMETABLE_SKELETON_PIN') new_p
WHERE NOT EXISTS (
    SELECT 1 FROM role_permissions x WHERE x.role_id = rp.role_id AND x.permission_id = new_p.id
);

-- DEV_ADMIN / SUPPORT_ADMIN catch-all sync
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r
CROSS JOIN permissions p
WHERE r.name IN ('DEV_ADMIN', 'SUPPORT_ADMIN')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
