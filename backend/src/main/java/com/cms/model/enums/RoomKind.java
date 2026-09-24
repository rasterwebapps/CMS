package com.cms.model.enums;

/** Which real table a Resource Timetable "room" row actually came from — Classroom, Lab, and
 *  ClinicalVenue are three separate tables, each with their own auto-increment id sequence, folded
 *  into one combined CLASSROOM {@code ResourceGridService.ResourceType} bucket purely for display
 *  (see ResourceGridService's own class javadoc). Two of those tables' ids can coincidentally
 *  collide (e.g. Classroom id 13 and ClinicalVenue id 13 both existing, unrelated to each other),
 *  so a raw resourceId alone is not enough to identify one specific room once drilling into its own
 *  full week -- see ResourceGridService#resourceMatcher/#shiftResourceMatcher, which used to OR all
 *  three tables' id checks together with no type check at all, so opening one room's week could
 *  silently pull in another, unrelated room's sessions whenever their ids happened to match. */
public enum RoomKind {
    CLASSROOM,
    LAB,
    CLINICAL_VENUE
}
