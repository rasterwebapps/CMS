-- V246: Phase 6 cleanup — drop the old application_number_sequences table.
-- All data was migrated to number_series_definitions + number_sequence_counters in V244.
-- The ApplicationNumberSequence JPA entity and its repository have been removed.
-- This migration is safe only after verifying all sequence generation flows use the new tables.
DROP TABLE IF EXISTS application_number_sequences;
