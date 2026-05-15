-- Grant DASHBOARD_CUSTOMIZE to all roles that should be able to assign it
-- and to personalise their own dashboard.
--
-- DEV_ADMIN / SUPPORT_ADMIN / ADMIN / COLLEGE_ADMIN → can assign the permission
--   to other roles AND personalise their own dashboard.
-- FACULTY / FRONT_OFFICE / CASHIER → may personalise their own dashboard only
--   (they cannot assign it further because they cannot edit roles at/above their level).
-- STUDENT intentionally excluded — students do not get dashboard customisation.

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM   app_roles  r
CROSS  JOIN permissions p
WHERE  p.code = 'DASHBOARD_CUSTOMIZE'
  AND  r.name IN (
         'DEV_ADMIN', 'SUPPORT_ADMIN', 'ADMIN', 'COLLEGE_ADMIN',
         'FACULTY', 'FRONT_OFFICE', 'CASHIER'
       )
  AND  NOT EXISTS (
         SELECT 1
         FROM   role_permissions rp
         WHERE  rp.role_id      = r.id
           AND  rp.permission_id = p.id
       );
