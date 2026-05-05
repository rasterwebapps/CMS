-- ============================================================
-- V89: Fix role-permission gaps to align DB permissions
--       with the controller access rules already in production.
-- ============================================================

-- FACULTY: needs write perms for academic operations they're
-- already performing via Keycloak role checks.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r, permissions p
WHERE r.name = 'FACULTY'
  AND p.code IN (
      'EXAMINATION_MANAGE',
      'EXAM_RESULT_MANAGE',
      'EXPERIMENT_MANAGE',
      'SYLLABUS_MANAGE',
      'LAB_SCHEDULE_MANAGE',
      'CURRICULUM_MANAGE'
  )
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- LAB_INCHARGE: needs attendance + inventory + maintenance write access.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r, permissions p
WHERE r.name = 'LAB_INCHARGE'
  AND p.code IN (
      'ATTENDANCE_MANAGE',
      'INVENTORY_MANAGE',
      'MAINTENANCE_MANAGE'
  )
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- TECHNICIAN: needs inventory + maintenance write access.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r, permissions p
WHERE r.name = 'TECHNICIAN'
  AND p.code IN (
      'INVENTORY_MANAGE',
      'MAINTENANCE_MANAGE'
  )
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- FRONT_OFFICE: needs create/edit access for admissions and students
-- (previously controlled by Keycloak ROLE_FRONT_OFFICE checks).
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r, permissions p
WHERE r.name = 'FRONT_OFFICE'
  AND p.code IN (
      'ADMISSION_CREATE',
      'ADMISSION_EDIT',
      'STUDENT_CREATE',
      'STUDENT_EDIT',
      'COURSE_VIEW'
  )
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- CASHIER: needs FEE_COLLECT to record enquiry payments.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r, permissions p
WHERE r.name = 'CASHIER'
  AND p.code IN ('FEE_COLLECT')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- FACULTY: add COURSE_VIEW for course-offering and offering-list endpoints.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r, permissions p
WHERE r.name = 'FACULTY'
  AND p.code IN ('COURSE_VIEW', 'ADMISSION_VIEW')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

