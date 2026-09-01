-- "Rebalance now" (TimetableCapacityPlanningService#applyRebalance) moves already-committed
-- batches off an over-capacity Lab/Clinical venue onto a better-fitting eligible one, clearing
-- their placed sessions for re-placement -- a real mutation, not a view, so it gets its own
-- dedicated permission per this project's operation-wise permission mapping rule. Defaults to the
-- same tier as TIMETABLE_CAPACITY_PLANNER_BATCH_CREATE (the existing Capacity Planner mutation-tier
-- action on this same screen), not the VIEW permission (previewing the rebalance itself stays
-- gated by the existing TIMETABLE_CAPACITY_PLANNER_VIEW).

INSERT INTO permissions (code, display_name, category, screen_label, created_at) VALUES
    ('TIMETABLE_CAPACITY_PLANNER_REBALANCE', 'Rebalance Over-Capacity Venue', 'CURRICULUM', 'Timetable', CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT rp.role_id, new_p.id
FROM role_permissions rp
JOIN permissions old_p ON rp.permission_id = old_p.id AND old_p.code = 'TIMETABLE_CAPACITY_PLANNER_BATCH_CREATE'
CROSS JOIN (SELECT id FROM permissions WHERE code = 'TIMETABLE_CAPACITY_PLANNER_REBALANCE') new_p
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
