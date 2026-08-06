package com.cms.model.enums;

/** The fixed, code-relied-upon identity of a {@link com.cms.model.RoomPurposeCategory} — unlike
 *  its {@code name} (freely renamable by an admin), this set is closed and only grows via a new
 *  migration + enum value together, so anything in the codebase that keys off a specific category
 *  (e.g. "a Classroom's physical Room must be ACADEMIC") can never be silently broken by a
 *  same-screen rename or retyped value. Mirrors this table's own seed data (V343). */
public enum RoomPurposeCategoryCode {
    ACADEMIC,
    RESIDENTIAL,
    ADMIN_STAFF,
    LIBRARY,
    DINING,
    UTILITY,
    SPORTS
}
