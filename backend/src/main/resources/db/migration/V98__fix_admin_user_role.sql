-- Fix admin user role from ADMIN to DEV_ADMIN so they can manage all users
-- This migration updates the existing admin user to have higher privileges

UPDATE app_users
SET app_role_id = (SELECT id FROM app_roles WHERE name = 'DEV_ADMIN'),
    updated_at = current_timestamp
WHERE keycloak_username = 'admin'
  AND app_role_id = (SELECT id FROM app_roles WHERE name = 'ADMIN');

