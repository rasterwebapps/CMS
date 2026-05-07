-- V95: Rename LAB_FEE → LABORATORY_FEE and extend fee type catalogue.
--
-- Only the stored enum strings in fee_structures need updating.
-- New types (CLINICAL_FEE, BOOK_AND_PACKET_FEE, UNIFORM_AND_SHOES_FEE,
-- UNIVERSITY_REGISTRATION_FEE) are pure enum additions — no data to migrate.

UPDATE fee_structures
   SET fee_type = 'LABORATORY_FEE'
 WHERE fee_type = 'LAB_FEE';
