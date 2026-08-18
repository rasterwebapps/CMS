-- Permissions for the OC-136 Core Physical Infrastructure & Spatial Visualization Engine
-- service/controller layer (Phase 2). Two entities, two screens: Floor Plans (upload/calibrate
-- the plan asset) and Virtual Locations (place beds/workstations/geofences on a calibrated
-- plan) — split per the operation-wise permission mapping gate the same way V307 split Campus
-- Infrastructure from Hostel Rooms, since a floor plan's physical calibration and a location's
-- logical placement are plausibly different staff. Calibration and file replace stay folded
-- into FLOOR_PLAN_MANAGE (not their own permission) since they're both just configuring the one
-- floor-plan asset, matching how FacultyDocument folds upload/verify into one MANAGE permission.
-- First screens of their kind (new module) — nothing to auto-inherit from, so no old-code
-- carry-forward block, matching V307's precedent. NOTE: V307 itself grants to
-- ('ADMIN', 'COLLEGE_ADMIN') but both those role names were deleted by V125 (unreferenced
-- seed roles cleanup) — the real college-admin role is 'collegeadmin' (created by V123). Using
-- the real name here instead of copying V307's now-dead reference.

INSERT INTO permissions (code, display_name, category, screen_label, created_at) VALUES
    ('SPATIAL_FLOOR_PLAN_VIEW',        'View Floor Plans',        'MASTER', 'Floor Plans',        CURRENT_TIMESTAMP),
    ('SPATIAL_FLOOR_PLAN_MANAGE',      'Manage Floor Plans',      'MASTER', 'Floor Plans',        CURRENT_TIMESTAMP),
    ('SPATIAL_VIRTUAL_LOCATION_VIEW',  'View Virtual Locations',  'MASTER', 'Virtual Locations',  CURRENT_TIMESTAMP),
    ('SPATIAL_VIRTUAL_LOCATION_MANAGE','Manage Virtual Locations','MASTER', 'Virtual Locations',  CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r, permissions p
WHERE r.name IN ('DEV_ADMIN', 'SUPPORT_ADMIN', 'collegeadmin')
  AND p.code IN ('SPATIAL_FLOOR_PLAN_VIEW', 'SPATIAL_FLOOR_PLAN_MANAGE',
                 'SPATIAL_VIRTUAL_LOCATION_VIEW', 'SPATIAL_VIRTUAL_LOCATION_MANAGE')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
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
