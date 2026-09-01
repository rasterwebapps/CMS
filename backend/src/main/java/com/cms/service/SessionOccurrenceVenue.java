package com.cms.service;

import com.cms.model.ClassSchedule;
import com.cms.model.Classroom;
import com.cms.model.ClinicalVenue;
import com.cms.model.Lab;
import com.cms.model.Room;
import com.cms.model.SessionOccurrence;
import com.cms.model.enums.ClassSessionType;
import com.cms.model.enums.OccurrenceSource;

/**
 * Shared {@code (venueId, physicalRoom, capacity, classroom, lab, clinicalVenue)} resolution for
 * whichever of a {@link SessionOccurrence}'s or {@link ClassSchedule}'s three venue FKs
 * (classroom/lab/clinicalVenue) actually applies for its session type. Extracted out of
 * {@code SpecialClassRequestService} (BR-55) once {@code RoomRelocationService} needed the exact
 * same resolution over {@code SessionOccurrence} rows -- a third near-copy of the switch was
 * judged worse than one shared utility. {@link com.cms.service.TimetableStaffingService#venueIdOf}/
 * {@code physicalRoomOf} are the same shape but operate on {@link ClassSchedule} fields that live
 * directly on that entity (not behind session-type branching against three separate FKs the way
 * this and {@link ClassSchedule}'s own three-way split otherwise are) and are left as-is.
 */
final class SessionOccurrenceVenue {

    private SessionOccurrenceVenue() {
    }

    record VenueResolution(Long venueId, Room physicalRoom, Integer capacity,
                            Classroom classroom, Lab lab, ClinicalVenue clinicalVenue) {
        static final VenueResolution NONE = new VenueResolution(null, null, null, null, null, null);
    }

    /** Carries over a REGULAR occurrence's room override (room relocation) or a SPECIAL_CLASS/
     *  DAY_REPEAT occurrence's own venue -- null-safe since neither is guaranteed to have one
     *  (an unstaffed/room-less skeleton row, or a REGULAR occurrence with no relocation applied).
     *
     *  <p>A REGULAR occurrence never has {@link SessionOccurrence#getSessionType()} populated
     *  (only SPECIAL_CLASS/DAY_REPEAT rows carry it, per the {@code chk_session_occurrences_special_shape}
     *  constraint) -- its session type can only come from the recurring {@code ClassSchedule} it
     *  belongs to, since that never changes via a room relocation. */
    static VenueResolution fromOccurrence(SessionOccurrence occurrence) {
        ClassSessionType sessionType = occurrence.getOccurrenceSource() == OccurrenceSource.REGULAR
            ? occurrence.getClassSchedule().getSessionType()
            : occurrence.getSessionType();
        if (sessionType == null) {
            return VenueResolution.NONE;
        }
        return switch (sessionType) {
            case THEORY, LIBRARY -> occurrence.getClassroom() != null
                ? new VenueResolution(occurrence.getClassroom().getId(), occurrence.getClassroom().getRoom(), occurrence.getClassroom().getCapacity(), occurrence.getClassroom(), null, null)
                : VenueResolution.NONE;
            case LAB -> occurrence.getLab() != null
                ? new VenueResolution(occurrence.getLab().getId(), occurrence.getLab().getRoom(), occurrence.getLab().getCapacity(), null, occurrence.getLab(), null)
                : VenueResolution.NONE;
            case CLINICAL -> occurrence.getClinicalVenue() != null
                ? new VenueResolution(occurrence.getClinicalVenue().getId(), occurrence.getClinicalVenue().getRoom(), occurrence.getClinicalVenue().getCapacity(), null, null, occurrence.getClinicalVenue())
                : VenueResolution.NONE;
        };
    }

    /** Carries over the recurring ClassSchedule row's already-committed venue -- null-safe since a
     *  row can be unstaffed/room-less (skeleton not yet fully staffed). */
    static VenueResolution fromClassSchedule(ClassSchedule cs) {
        return switch (cs.getSessionType()) {
            case THEORY, LIBRARY -> cs.getClassroom() != null
                ? new VenueResolution(cs.getClassroom().getId(), cs.getClassroom().getRoom(), cs.getClassroom().getCapacity(), cs.getClassroom(), null, null)
                : VenueResolution.NONE;
            case LAB -> cs.getLab() != null
                ? new VenueResolution(cs.getLab().getId(), cs.getLab().getRoom(), cs.getLab().getCapacity(), null, cs.getLab(), null)
                : VenueResolution.NONE;
            case CLINICAL -> cs.getClinicalVenue() != null
                ? new VenueResolution(cs.getClinicalVenue().getId(), cs.getClinicalVenue().getRoom(), cs.getClinicalVenue().getCapacity(), null, null, cs.getClinicalVenue())
                : VenueResolution.NONE;
        };
    }
}
