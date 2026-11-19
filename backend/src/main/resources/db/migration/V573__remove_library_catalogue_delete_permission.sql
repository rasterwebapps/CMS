-- V573: Remove LIBRARY_CATALOGUE_DELETE permission.
-- Books in the Accession Register (library_books) are never hard-deleted; their
-- lifecycle is managed strictly via status edits (AVAILABLE, LOST, DAMAGED, WITHDRAWN).
-- role_permissions.permission_id has ON DELETE CASCADE (V87), so role assignments
-- are cleaned up automatically.
DELETE FROM permissions WHERE code = 'LIBRARY_CATALOGUE_DELETE';
