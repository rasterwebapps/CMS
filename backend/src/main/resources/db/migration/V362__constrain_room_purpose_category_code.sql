-- Locks room_purpose_categories.code to a fixed, known set of values (RoomPurposeCategoryCode
-- enum), mirroring this codebase's existing VARCHAR+CHECK enum convention (see
-- chk_cohort_room_allocation_status, chk_library_books_status, etc.) rather than a native
-- Postgres ENUM type. Without this, `code` was a freely-typed text field an admin could rename at
-- any time -- and three backend services (ClassroomService/LabService/ClinicalVenueService) plus
-- two frontend screens already hardcode a literal 'ACADEMIC' string match against it, so a rename
-- would silently break every one of them. code itself also becomes immutable-after-creation in
-- RoomPurposeCategoryService going forward (application-level, not enforceable via CHECK) -- this
-- constraint only guards against an invalid/unknown value ever being written, from any path.
-- Extending this set later (per RoomPurposeCategoryCode's own javadoc) means adding both a new
-- enum value AND a new forward migration that redefines this constraint with the extra value.

ALTER TABLE room_purpose_categories
    ADD CONSTRAINT chk_room_purpose_categories_code
    CHECK (code IN ('ACADEMIC', 'RESIDENTIAL', 'ADMIN_STAFF', 'LIBRARY', 'DINING', 'UTILITY', 'SPORTS'));
