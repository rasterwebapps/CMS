package com.cms.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.ConstraintViolation;
import com.cms.dto.RoomRelocationRequest;
import com.cms.dto.RoomRelocationResponse;
import com.cms.dto.VenueCandidate;
import com.cms.exception.LifecycleConflictException;
import com.cms.exception.ResourceNotFoundException;
import com.cms.exception.TimetableConstraintViolationException;
import com.cms.model.ClassSchedule;
import com.cms.model.Classroom;
import com.cms.model.ClinicalVenue;
import com.cms.model.Lab;
import com.cms.model.Period;
import com.cms.model.Room;
import com.cms.model.SessionOccurrence;
import com.cms.model.enums.ClassScheduleStatus;
import com.cms.model.enums.ClassSessionType;
import com.cms.model.enums.DayOfWeek;
import com.cms.model.enums.LabStatus;
import com.cms.model.enums.OccurrenceSource;
import com.cms.model.enums.OccurrenceStatus;
import com.cms.repository.ClassScheduleRepository;
import com.cms.repository.ClassroomRepository;
import com.cms.repository.ClinicalVenueRepository;
import com.cms.repository.LabRepository;
import com.cms.repository.SessionOccurrenceRepository;
import com.cms.service.SessionOccurrenceVenue.VenueResolution;

/**
 * Single-date room relocation — swaps the room for one specific calendar occurrence of a
 * recurring {@link ClassSchedule}, never mutating the recurring row's own venue. Mirrors {@link
 * FacultySessionSwapService}'s find-or-create idiom over {@link SessionOccurrence}, minus the
 * reciprocal {@code swapPartnerOccurrence} link (a room swap is one-sided).
 *
 * <p>No per-date room conflict-checking existed anywhere before this: {@link
 * TimetableStaffingService#checkRoomFree} only knows the weekly recurring pattern, and {@link
 * SpecialClassRequestService}'s date-specific checker explicitly excludes {@code REGULAR} rows.
 * {@link #checkConflicts} therefore combines both: the existing weekly-pattern check, plus a new
 * same-date/same-period scan over every {@link SessionOccurrence} (any source) for that date.
 */
@Service
@Transactional(readOnly = true)
public class RoomRelocationService {

    private final ClassScheduleRepository classScheduleRepository;
    private final ClassroomRepository classroomRepository;
    private final LabRepository labRepository;
    private final ClinicalVenueRepository clinicalVenueRepository;
    private final SessionOccurrenceRepository sessionOccurrenceRepository;
    private final ClassScheduleOccurrenceService occurrenceService;
    private final TimetableStaffingService timetableStaffingService;
    private final AuditLogService auditLogService;

    public RoomRelocationService(ClassScheduleRepository classScheduleRepository,
                                  ClassroomRepository classroomRepository,
                                  LabRepository labRepository,
                                  ClinicalVenueRepository clinicalVenueRepository,
                                  SessionOccurrenceRepository sessionOccurrenceRepository,
                                  ClassScheduleOccurrenceService occurrenceService,
                                  TimetableStaffingService timetableStaffingService,
                                  AuditLogService auditLogService) {
        this.classScheduleRepository = classScheduleRepository;
        this.classroomRepository = classroomRepository;
        this.labRepository = labRepository;
        this.clinicalVenueRepository = clinicalVenueRepository;
        this.sessionOccurrenceRepository = sessionOccurrenceRepository;
        this.occurrenceService = occurrenceService;
        this.timetableStaffingService = timetableStaffingService;
        this.auditLogService = auditLogService;
    }

    /** Every venue of this session's own type that wouldn't conflict on this date -- so the modal
     *  never offers a room that would just bounce with a conflict error. */
    public List<VenueCandidate> findCandidateVenues(Long classScheduleId, LocalDate date) {
        ClassSchedule schedule = requirePublishedRealOccurrence(classScheduleId, date);
        List<VenueCandidate> results = new ArrayList<>();
        switch (schedule.getSessionType()) {
            case THEORY -> {
                for (Classroom c : classroomRepository.findByIsActiveTrueOrderByNameAsc()) {
                    if (checkConflicts(schedule, date, c.getId(), c.getRoom()).isEmpty()) {
                        results.add(new VenueCandidate(c.getId(), c.getName(), c.getCapacity()));
                    }
                }
            }
            case LAB -> {
                for (Lab l : labRepository.findAll()) {
                    if (l.getStatus() != LabStatus.ACTIVE && l.getStatus() != LabStatus.AVAILABLE) continue;
                    if (checkConflicts(schedule, date, l.getId(), l.getRoom()).isEmpty()) {
                        results.add(new VenueCandidate(l.getId(), l.getName(), l.getCapacity()));
                    }
                }
            }
            case CLINICAL -> {
                for (ClinicalVenue v : clinicalVenueRepository.findByIsActiveTrueOrderByNameAsc()) {
                    if (checkConflicts(schedule, date, v.getId(), v.getRoom()).isEmpty()) {
                        results.add(new VenueCandidate(v.getId(), v.getName(), v.getCapacity()));
                    }
                }
            }
        }
        return results;
    }

    @Transactional
    public RoomRelocationResponse relocate(Long classScheduleId, RoomRelocationRequest request, String actor) {
        ClassSchedule schedule = requirePublishedRealOccurrence(classScheduleId, request.date());
        VenueResolution venue = resolveVenue(schedule.getSessionType(), request.venueId());

        SessionOccurrence occurrence = sessionOccurrenceRepository
            .findByClassScheduleIdAndOccurrenceDate(classScheduleId, request.date())
            .orElseGet(() -> new SessionOccurrence(schedule, request.date()));
        if (occurrence.getOccurrenceStatus() == OccurrenceStatus.CANCELLED) {
            throw new LifecycleConflictException("This occurrence is cancelled and can't be relocated.",
                "SESSION_OCCURRENCE_CANCELLED", "ClassSchedule", classScheduleId, null);
        }

        List<ConstraintViolation> violations = checkConflicts(schedule, request.date(), venue.venueId(), venue.physicalRoom());
        if (!violations.isEmpty()) {
            throw new TimetableConstraintViolationException(violations);
        }

        applyVenue(occurrence, schedule.getSessionType(), venue);
        occurrence.setOccurrenceStatus(OccurrenceStatus.SUBSTITUTED);
        occurrence = sessionOccurrenceRepository.save(occurrence);

        auditLogService.record(actor, "TIMETABLE_ROOM_RELOCATED", "ClassSchedule", classScheduleId.toString(),
            "Relocated to " + venueName(venue) + " for " + request.date());
        return new RoomRelocationResponse(classScheduleId, request.date(), venueName(venue), occurrence.getOccurrenceStatus());
    }

    @Transactional
    public RoomRelocationResponse revert(Long classScheduleId, LocalDate date, String actor) {
        ClassSchedule schedule = classScheduleRepository.findById(classScheduleId)
            .orElseThrow(() -> new ResourceNotFoundException("Class schedule not found with id: " + classScheduleId));
        SessionOccurrence occurrence = sessionOccurrenceRepository
            .findByClassScheduleIdAndOccurrenceDate(classScheduleId, date)
            .orElseThrow(() -> new ResourceNotFoundException("No room relocation found for this date."));

        occurrence.setClassroom(null);
        occurrence.setLab(null);
        occurrence.setClinicalVenue(null);
        // A faculty substitution can be independently active for the same date -- only clear
        // SUBSTITUTED back to HELD if nothing else is still overriding this occurrence.
        if (occurrence.getEffectiveFaculty() == null) {
            occurrence.setOccurrenceStatus(OccurrenceStatus.HELD);
        }
        occurrence = sessionOccurrenceRepository.save(occurrence);

        auditLogService.record(actor, "TIMETABLE_ROOM_RELOCATION_REVERTED", "ClassSchedule",
            classScheduleId.toString(), "Reverted to the recurring room for " + date);

        VenueResolution recurringVenue = SessionOccurrenceVenue.fromClassSchedule(schedule);
        return new RoomRelocationResponse(classScheduleId, date, venueName(recurringVenue), occurrence.getOccurrenceStatus());
    }

    /** Combines the weekly-pattern check ({@link TimetableStaffingService#checkRoomFree}, reused
     *  as-is) with a new same-date/same-period scan over every occurrence on this date, since the
     *  weekly check has no awareness of one-off overrides (relocations, special classes, day-repeat
     *  batches) and the special-class checker explicitly excludes REGULAR rows. */
    private List<ConstraintViolation> checkConflicts(ClassSchedule schedule, LocalDate date, Long venueId, Room physicalRoom) {
        List<ConstraintViolation> violations = new ArrayList<>();
        Period period = schedule.getPeriod();
        LocalTime start = period.getStartTime();
        LocalTime end = period.getEndTime();
        DayOfWeek day = schedule.getDayOfWeek();

        timetableStaffingService.checkRoomFree(schedule.getSessionType(), venueId, physicalRoom,
                schedule.getTermInstance().getId(), schedule.getId(), day, start, end)
            .ifPresent(violations::add);

        Long existingOccurrenceId = sessionOccurrenceRepository
            .findByClassScheduleIdAndOccurrenceDate(schedule.getId(), date)
            .map(SessionOccurrence::getId)
            .orElse(null);

        for (SessionOccurrence other : sessionOccurrenceRepository.findByOccurrenceDate(date)) {
            if (existingOccurrenceId != null && other.getId().equals(existingOccurrenceId)) {
                continue;
            }
            Period otherPeriod = effectivePeriod(other);
            if (otherPeriod == null || !otherPeriod.getId().equals(period.getId())) {
                continue;
            }
            VenueResolution otherVenue = SessionOccurrenceVenue.fromOccurrence(other);
            if (otherVenue.venueId() == null) {
                continue;
            }
            boolean sameVenue = venueId.equals(otherVenue.venueId()) && effectiveSessionType(other) == schedule.getSessionType();
            boolean samePhysicalRoom = physicalRoom != null && otherVenue.physicalRoom() != null
                && physicalRoom.getId().equals(otherVenue.physicalRoom().getId());
            if (sameVenue || samePhysicalRoom) {
                violations.add(new ConstraintViolation("ROOM_RELOCATION_CONFLICT",
                    "This room is already occupied by another session at this exact date and period."));
            }
        }
        return violations;
    }

    private static Period effectivePeriod(SessionOccurrence occurrence) {
        return occurrence.getOccurrenceSource() == OccurrenceSource.REGULAR
            ? occurrence.getClassSchedule().getPeriod()
            : occurrence.getPeriod();
    }

    private static ClassSessionType effectiveSessionType(SessionOccurrence occurrence) {
        return occurrence.getOccurrenceSource() == OccurrenceSource.REGULAR
            ? occurrence.getClassSchedule().getSessionType()
            : occurrence.getSessionType();
    }

    private VenueResolution resolveVenue(ClassSessionType sessionType, Long venueId) {
        return switch (sessionType) {
            case THEORY -> {
                Classroom c = classroomRepository.findById(venueId)
                    .orElseThrow(() -> new ResourceNotFoundException("Classroom not found with id: " + venueId));
                yield new VenueResolution(c.getId(), c.getRoom(), c.getCapacity(), c, null, null);
            }
            case LAB -> {
                Lab l = labRepository.findById(venueId)
                    .orElseThrow(() -> new ResourceNotFoundException("Lab not found with id: " + venueId));
                yield new VenueResolution(l.getId(), l.getRoom(), l.getCapacity(), null, l, null);
            }
            case CLINICAL -> {
                ClinicalVenue v = clinicalVenueRepository.findById(venueId)
                    .orElseThrow(() -> new ResourceNotFoundException("Clinical venue not found with id: " + venueId));
                yield new VenueResolution(v.getId(), v.getRoom(), v.getCapacity(), null, null, v);
            }
        };
    }

    private void applyVenue(SessionOccurrence occurrence, ClassSessionType sessionType, VenueResolution venue) {
        switch (sessionType) {
            case THEORY -> occurrence.setClassroom(venue.classroom());
            case LAB -> occurrence.setLab(venue.lab());
            case CLINICAL -> occurrence.setClinicalVenue(venue.clinicalVenue());
        }
    }

    private static String venueName(VenueResolution venue) {
        if (venue.classroom() != null) return venue.classroom().getName();
        if (venue.lab() != null) return venue.lab().getName();
        if (venue.clinicalVenue() != null) return venue.clinicalVenue().getName();
        return null;
    }

    /** Mirrors {@code FacultySessionSwapService.requirePublishedRealOccurrence} -- a relocation
     *  only makes sense against a date the recurring schedule actually fires on (not a
     *  holiday/blocked date, not before the term starts). */
    private ClassSchedule requirePublishedRealOccurrence(Long classScheduleId, LocalDate date) {
        ClassSchedule schedule = classScheduleRepository.findById(classScheduleId)
            .orElseThrow(() -> new ResourceNotFoundException("Class schedule not found with id: " + classScheduleId));
        if (schedule.getStatus() != ClassScheduleStatus.PUBLISHED) {
            throw new IllegalArgumentException("Only published sessions can have their room relocated");
        }
        if (occurrenceService.occurrenceDatesFor(schedule, date, date).isEmpty()) {
            throw new IllegalArgumentException(date + " is not a real occurrence of this session");
        }
        return schedule;
    }
}
