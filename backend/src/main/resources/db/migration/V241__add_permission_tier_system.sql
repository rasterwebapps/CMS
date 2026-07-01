-- ============================================================
-- V241 — Permission Tier System
--
-- Adds a `tier` column to `permissions` that controls:
--   (a) which roles can HOLD a permission
--   (b) which roles can DELEGATE a permission when assigning to sub-roles
--
-- Tier semantics:
--   1  Dev Only       — DEV_ADMIN holds and delegates; nobody else
--   2  Support+       — DEV_ADMIN + SUPPORT_ADMIN hold and delegate
--   3  Hold Only      — any senior role holds it; only Support+ can delegate it
--   4  Open           — any role holds and delegates (default for all permissions)
--
-- Also:
--   - Seeds PERMISSION_TIER_MANAGE (tier=1, DEV_ADMIN only)
--   - Removes ROLE_CREATE from SUPPORT_ADMIN (they cannot create new roles)
-- ============================================================

-- 1. Add tier column (default 4 = open, covers all existing permissions)
ALTER TABLE permissions
    ADD COLUMN IF NOT EXISTS tier INT NOT NULL DEFAULT 4
        CHECK (tier BETWEEN 1 AND 4);

-- 2. Tier 1 — Dev-only (logic-changing / irreversible operations)
UPDATE permissions SET tier = 1
WHERE code IN (
    'PERMISSION_ASSIGN',
    'IMPORT_DATA',
    'LEGACY_ADMIT',
    'RETRO_ADMIT'
);

-- 3. Tier 2 — Support-and-above only (client-level configuration)
UPDATE permissions SET tier = 2
WHERE code IN (
    'SETTINGS_VIEW',
    'SETTINGS_MANAGE',
    'INSTITUTION_VIEW',
    'INSTITUTION_MANAGE',
    'INDIA_LOCATION_MANAGE'
);

-- 4. Tier 3 — Senior roles hold it; only Support+ can delegate it
--    (COLLEGE_ADMIN can use these themselves but cannot assign to sub-roles)
UPDATE permissions SET tier = 3
WHERE code IN (
    'DOCUMENT_VERIFIED_OVERRIDE',
    'FEE_FINALIZE',
    'COMMISSION_SETTLE',
    'STUDENT_DELETE',
    'ADMISSION_DELETE',
    'ENQUIRY_DELETE'
);

-- 5. Seed PERMISSION_TIER_MANAGE (tier=1 — DEV_ADMIN only)
INSERT INTO permissions (code, display_name, category, tier, created_at)
VALUES ('PERMISSION_TIER_MANAGE', 'Manage Permission Tiers', 'SYSTEM', 1, CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

-- 6. Grant PERMISSION_TIER_MANAGE to DEV_ADMIN only
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r, permissions p
WHERE r.name = 'DEV_ADMIN'
  AND p.code = 'PERMISSION_TIER_MANAGE'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- 7. Remove ROLE_CREATE from SUPPORT_ADMIN
--    (Support team cannot create new roles; role creation is COLLEGE_ADMIN and above)
DELETE FROM role_permissions
WHERE role_id  = (SELECT id FROM app_roles WHERE name = 'SUPPORT_ADMIN')
  AND permission_id = (SELECT id FROM permissions WHERE code = 'ROLE_CREATE');
