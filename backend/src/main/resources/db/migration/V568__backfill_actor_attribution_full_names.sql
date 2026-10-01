-- ============================================================================
-- V568: Backfill actor-attribution columns that stored a raw Keycloak username
-- (enquiry_payments.collected_by, payment_receipts.collected_by,
-- fee_refunds.requested_by, fee_refunds.approved_by) with the matching
-- app_users.full_name, wherever a still-existing app_user row can be matched.
--
-- These columns are meant to show who actually collected a payment or
-- requested/approved a refund, but were historically populated from
-- jwt.getClaimAsString("preferred_username") instead of the user's real name.
-- A value that no longer matches any keycloak_username (an already-correct
-- full name, the "SYSTEM" literal used for auto-generated excess refunds, or
-- a username whose app_user account has since been deactivated/removed) is
-- left untouched by the exact-match join below, which also makes this
-- migration a no-op if it is ever re-run.
-- ============================================================================

UPDATE enquiry_payments ep
SET collected_by = au.full_name
FROM app_users au
WHERE ep.collected_by = au.keycloak_username;

UPDATE payment_receipts pr
SET collected_by = au.full_name
FROM app_users au
WHERE pr.collected_by = au.keycloak_username;

UPDATE fee_refunds fr
SET requested_by = au.full_name
FROM app_users au
WHERE fr.requested_by = au.keycloak_username;

UPDATE fee_refunds fr
SET approved_by = au.full_name
FROM app_users au
WHERE fr.approved_by = au.keycloak_username;
