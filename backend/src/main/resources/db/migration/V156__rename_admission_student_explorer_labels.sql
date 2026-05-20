-- V156: Rename Admissions → Admission Explorer and Students → Student Explorer
-- Updates permission display names to match the new menu labels.
-- Also ensures the devadmin (DEV_ADMIN) user has all ADMISSION_* and STUDENT_*
-- permissions explicitly (V129 already covers this via a catch-all, but this
-- makes the intent visible and idempotent).

-- Update ADMISSION permission display names
UPDATE permissions SET display_name = 'View Admission Explorer'    WHERE code = 'ADMISSION_VIEW';
UPDATE permissions SET display_name = 'Create Admission Explorer'  WHERE code = 'ADMISSION_CREATE';
UPDATE permissions SET display_name = 'Edit Admission Explorer'    WHERE code = 'ADMISSION_EDIT';
UPDATE permissions SET display_name = 'Delete Admission Explorer'  WHERE code = 'ADMISSION_DELETE';

-- Update STUDENT permission display names
UPDATE permissions SET display_name = 'View Student Explorer'      WHERE code = 'STUDENT_VIEW';
UPDATE permissions SET display_name = 'Create Student Explorer'    WHERE code = 'STUDENT_CREATE';
UPDATE permissions SET display_name = 'Edit Student Explorer'      WHERE code = 'STUDENT_EDIT';
UPDATE permissions SET display_name = 'Delete Student Explorer'    WHERE code = 'STUDENT_DELETE';

-- Ensure devadmin (DEV_ADMIN role) holds all ADMISSION_* and STUDENT_* permissions.
-- Idempotent: NOT EXISTS guard prevents duplicate rows.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r
CROSS JOIN permissions p
WHERE r.name = 'DEV_ADMIN'
  AND p.code IN (
      'ADMISSION_VIEW', 'ADMISSION_CREATE', 'ADMISSION_EDIT', 'ADMISSION_DELETE',
      'STUDENT_VIEW',   'STUDENT_CREATE',   'STUDENT_EDIT',   'STUDENT_DELETE'
  )
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
