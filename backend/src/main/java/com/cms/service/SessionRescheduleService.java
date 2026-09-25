package com.cms.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.ApplyRescheduleRequest;
import com.cms.dto.ConstraintViolation;
import com.cms.dto.RescheduleResponse;
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
import com.cms.model.TermInstance;
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
import com.cms.repository.PeriodRepository;
import com.cms.repository.SessionOccurrenceRepository;
import com.cms.service.SessionOccurrenceVenue.VenueResolution;

/**
 * Moves one real calendar occurrence of a recurring {@link ClassSchedule} to a different
 * date/period/room — never mutating the recurring row itself, so every other occurrence keeps
 * firing on its normal schedule. Distinct from {@link RoomRelocationService} (room only, same
 * date) and {@link FacultySessionSwapService} (faculty only, same date/period): this is the one
 * action that can genuinely change *when* a session happens.
 *
 * <p>Modeled directly on {@link RoomRelocationService}'s find-or-create-{@link SessionOccurrence}
 * idiom, extended two ways: (1) it touches <em>two</em> dates per apply — the original (marked
 * {@link OccurrenceStatus#CANCELLED}, explaining where it moved) and the target (marked {@link
 * OccurrenceStatus#RESCHEDULED}, carrying the new period + venue); (2) unlike Room Relocation,
 * the target date is not guaranteed to be one of the schedule's own natural recurring dates, so
 * conflict-checking has to consider a genuinely different day-of-week/period, not just a
 * different room on the same day/period.
 *
 * <p>{@link #checkConflicts} therefore checks three dimensions this codebase never combined
 * before for a real calendar date: room (extends {@link RoomRelocationService}'s same-date scan
 * to also compare period), faculty (recurring pattern via {@link
 * TimetableStaffingService#checkFacultyFree}, plus the same same-date scan), and audience/cohort
 * (recurring pattern via {@link TimetableStaffingService#resolveAudienceId} against other {@link
 * ClassSchedule}s, plus the same same-date scan) — the audience dimension especially: nothing
 * else in the codebase checks whether a cohort/batch is already committed elsewhere on a specific
 * calendar date ({@link TimetableStaffingService}'s existing audience-conflict logic is only ever
 * invoked from the DRAFT-recurring {@code TimetableSwapService}/{@code TimetableSkeletonService},
 * never with a real date).
 */
@Service
@Transactional(readOnly = true)
public class SessionRescheduleService {

    private final ClassScheduleRepository classScheduleRepository;
    private final ClassroomRepository classroomRepository;
    private final LabRepository labRepository;
    private final ClinicalVenueRepository clinicalVenueRepository;
    private final PeriodRepository periodRepository;
    private final SessionOccurrenceRepository sessionOccurrenceRepository;
    private final ClassScheduleOccurrenceService occurrenceService;
    private final TimetableStaffingService timetableStaffingService;
    private final AuditLogService auditLogService;

    public SessionRescheduleService(ClassScheduleRepository classScheduleRepository,
                                     ClassroomRepository classroomRepository,
                                     LabRepository labRepository,
                                     ClinicalVenueRepository clinicalVenueRepository,
                                     PeriodRepository periodRepository,
                                     SessionOccurrenceRepository sessionOccurrenceRepository,
                                     ClassScheduleOccurrenceService occurrenceService,
                                     TimetableStaffingService timetableStaffingService,
                                     AuditLogService auditLogService) {
        this.classScheduleRepository = classScheduleRepository;
        this.classroomRepository = classroomRepository;
        this.labRepository = labRepository;
        this.clinicalVenueRepository = clinicalVenueRepository;
        this.periodRepository = periodRepository;
        this.sessionOccurrenceRepository = sessionOccurrenceRepository;
        this.occurrenceService = occurrenceService;
        this.timetableStaffingService = timetableStaffingService;
        this.auditLogService = auditLogService;
    }

    /** Every venue of this session's own type that wouldn't conflict at the chosen target
     *  date+period — so the modal never offers a room that would just bounce with a conflict
     *  error on Apply. */
    public List<VenueCandidate> findCandidateVenues(Long classScheduleId, LocalDate date, LocalDate targetDate, Long periodId) {
        ClassSchedule schedule = requirePublishedRealOccurrence(classScheduleId, date);
        Period targetPeriod = periodRepository.findById(periodId)
            .orElseThrow(() -> new ResourceNotFoundException("Period not found with id: " + periodId));

        List<VenueCandidate> results = new ArrayList<>();
        switch (schedule.getSessionType()) {
            case THEORY, LIBRARY, SPORTS -> {
                for (Classroom c : classroomRepository.findByIsActiveTrueOrderByNameAsc()) {
                    if (checkConflicts(schedule, targetDate, targetPeriod, c.getId(), c.getRoom()).isEmpty()) {
                        results.add(new VenueCandidate(c.getId(), c.getName(), c.getCapacity()));
                    }
                }
            }
            case LAB -> {
                for (Lab l : labRepository.findAll()) {
                    if (l.getStatus() != LabStatus.ACTIVE && l.getStatus() != LabStatus.AVAILABLE) continue;
                    if (checkConflicts(schedule, targetDate, targetPeriod, l.getId(), l.getRoom()).isEmpty()) {
                        results.add(new VenueCandidate(l.getId(), l.getName(), l.getCapacity()));
                    }
                }
            }
            case CLINICAL -> {
                for (ClinicalVenue v : clinicalVenueRepository.findByIsActiveTrueOrderByNameAsc()) {
                    if (checkConflicts(schedule, targetDate, targetPeriod, v.getId(), v.getRoom()).isEmpty()) {
                        results.add(new VenueCandidate(v.getId(), v.getName(), v.getCapacity()));
                    }
                }
            }
        }
        return results;
    }

    @Transactional
    public RescheduleResponse apply(Long classScheduleId, ApplyRescheduleRequest request, String actor) {
        ClassSchedule schedule = requirePublishedRealOccurrence(classScheduleId, request.date());
        Period targetPeriod = periodRepository.findById(request.periodId())
            .orElseThrow(() -> new ResourceNotFoundException("Period not found with id: " + request.periodId()));
        VenueResolution venue = resolveVenue(schedule.getSessionType(), request.venueId());

        List<ConstraintViolation> violations =
            checkConflicts(schedule, request.targetDate(), targetPeriod, venue.venueId(), venue.physicalRoom());
        if (!violations.isEmpty()) {
            throw new TimetableConstraintViolationException(violations);
        }

        boolean sameDate = request.date().equals(request.targetDate());

        SessionOccurrence moved = sessionOccurrenceRepository
            .findByClassScheduleIdAndOccurrenceDate(classScheduleId, request.targetDate())
            .orElseGet(() -> new SessionOccurrence(schedule, request.targetDate()));
        if (!sameDate && moved.getOccurrenceStatus() == OccurrenceStatus.CANCELLED) {
            throw new LifecycleConflictException("The target date is cancelled and can't accept a rescheduled session.",
                "SESSION_OCCURRENCE_CANCELLED", "ClassSchedule", classScheduleId, null);
        }

        // Same date, different period only -- the "original" and "target" row are the same
        // identity, so there's nothing to separately cancel.
        if (!sameDate) {
            SessionOccurrence original = sessionOccurrenceRepository
                .findByClassScheduleIdAndOccurrenceDate(classScheduleId, request.date())
                .orElseGet(() -> new SessionOccurrence(schedule, request.date()));
            if (original.getOccurrenceStatus() == OccurrenceStatus.CANCELLED) {
                throw new LifecycleConflictException("This occurrence is already cancelled and can't be rescheduled.",
                    "SESSION_OCCURRENCE_CANCELLED", "ClassSchedule", classScheduleId, null);
            }
            original.setOccurrenceStatus(OccurrenceStatus.CANCELLED);
            original.setRemarks("Rescheduled to " + request.targetDate() + " " + targetPeriod.getName());
            sessionOccurrenceRepository.save(original);
        }

        moved.setPeriod(targetPeriod);
        applyVenue(moved, schedule.getSessionType(), venue);
        moved.setOccurrenceStatus(OccurrenceStatus.RESCHEDULED);
        moved.setRemarks(sameDate ? "Moved to a different period the same day" : "Rescheduled from " + request.date());
        moved = sessionOccurrenceRepository.save(moved);

        auditLogService.record(actor, "TIMETABLE_OCCURRENCE_RESCHEDULED", "ClassSchedule", classScheduleId.toString(),
            "Rescheduled from " + request.date() + " to " + request.targetDate() + " " + targetPeriod.getName()
                + " / " + venueName(venue));

        return new RescheduleResponse(classScheduleId, request.date(), request.targetDate(),
            targetPeriod.getName(), venueName(venue), moved.getOccurrenceStatus());
    }

    @Transactional
    public RescheduleResponse revert(Long classScheduleId, LocalDate date, LocalDate targetDate, String actor) {
        classScheduleRepository.findById(classScheduleId)
            .orElseThrow(() -> new ResourceNotFoundException("Class schedule not found with id: " + classScheduleId));

        SessionOccurrence moved = sessionOccurrenceRepository
            .findByClassScheduleIdAndOccurrenceDate(classScheduleId, targetDate)
            .orElseThrow(() -> new ResourceNotFoundException("No reschedule found for this date."));
        if (moved.getOccurrenceStatus() != OccurrenceStatus.RESCHEDULED) {
            throw new IllegalArgumentException(targetDate + " is not a rescheduled occurrence of this session");
        }
        // This row only ever existed because of the reschedule -- delete it outright, no "HELD"
        // state makes sense on a date the recurring schedule doesn't naturally fire on.
        sessionOccurrenceRepository.delete(moved);

        sessionOccurrenceRepository.findByClassScheduleIdAndOccurrenceDate(classScheduleId, date)
            .filter(original -> original.getOccurrenceStatus() == OccurrenceStatus.CANCELLED)
            .ifPresent(original -> {
                original.setOccurrenceStatus(OccurrenceStatus.HELD);
                original.setRemarks(null);
                sessionOccurrenceRepository.save(original);
            });

        auditLogService.record(actor, "TIMETABLE_OCCURRENCE_RESCHEDULE_REVERTED", "ClassSchedule",
            classScheduleId.toString(), "Reverted reschedule from " + date + " to " + targetDate);

        return new RescheduleResponse(classScheduleId, date, targetDate, null, null, OccurrenceStatus.HELD);
    }

    private List<ConstraintViolation> checkConflicts(ClassSchedule schedule, LocalDate targetDate, Period targetPeriod,
                                                       Long venueId, Room physicalRoom) {
        List<ConstraintViolation> violations = new ArrayList<>();
        LocalTime start = targetPeriod.getStartTime();
        LocalTime end = targetPeriod.getEndTime();

        if (targetDate.getDayOfWeek() == java.time.DayOfWeek.SUNDAY) {
            violations.add(new ConstraintViolation("RESCHEDULE_NO_TEACHING_ON_SUNDAY",
                "There are no teaching periods on Sunday."));
            return violations;
        }
        DayOfWeek targetDay = DayOfWeek.valueOf(targetDate.getDayOfWeek().name());

        TermInstance term = schedule.getTermInstance();
        if (targetDate.isBefore(term.getStartDate()) || targetDate.isAfter(term.getEndDate())) {
            violations.add(new ConstraintViolation("RESCHEDULE_OUTSIDE_TERM",
                "The target date falls outside this term's dates."));
            return violations;
        }
        if (WorkingSaturdayCalculator.isNonWorkingSaturday(targetDate, term)) {
            violations.add(new ConstraintViolation("RESCHEDULE_NON_WORKING_SATURDAY",
                "This Saturday isn't a working day for this term."));
            return violations;
        }

        Long facultyId = schedule.getFaculty() != null ? schedule.getFaculty().getId() : null;
        if (facultyId != null) {
            timetableStaffingService.checkFacultyAvailable(facultyId, targetDay, start, end, targetDate)
                .ifPresent(violations::add);
            timetableStaffingService.checkFacultyAbsent(facultyId, targetDate).ifPresent(violations::add);
            timetableStaffingService.checkFacultyFree(facultyId, schedule.getTermInstance().getId(), schedule.getId(),
                    targetDay, start, end)
                .ifPresent(violations::add);
        }

        timetableStaffingService.checkRoomFree(schedule.getSessionType(), venueId, physicalRoom,
                schedule.getTermInstance().getId(), schedule.getId(), targetDay, start, end)
            .ifPresent(violations::add);

        // Recurring-pattern audience dimension: another ClassSchedule already serving the same
        // batch/cohort at this day+time, every week.
        Long audienceId = TimetableStaffingService.resolveAudienceId(schedule);
        if (audienceId != null) {
            for (ClassSchedule other : classScheduleRepository.findOverlapping(
                    targetDay, schedule.getTermInstance().getId(), start, end, ClassScheduleStatus.PUBLISHED, schedule.getId())) {
                if (audienceId.equals(TimetableStaffingService.resolveAudienceId(other))) {
                    violations.add(new ConstraintViolation("RESCHEDULE_AUDIENCE_CONFLICT",
                        "This slot already has another session for the same cohort/batch, every week."));
                    break;
                }
            }
        }

        // Same-date dimension: room, faculty, and audience, scanned once over every occurrence
        // (any source) already sitting on this exact date -- mirrors RoomRelocationService's
        // same-date room scan, extended to also compare period/faculty/audience since reschedule,
        // unlike relocate, can move to a genuinely different period.
        Long ownTargetOccurrenceId = sessionOccurrenceRepository
            .findByClassScheduleIdAndOccurrenceDate(schedule.getId(), targetDate)
            .map(SessionOccurrence::getId)
            .orElse(null);
        boolean roomFlagged = false;
        boolean facultyFlagged = false;
        boolean audienceFlagged = false;
        for (SessionOccurrence other : sessionOccurrenceRepository.findByOccurrenceDate(targetDate)) {
            if (other.getClassSchedule() != null && other.getClassSchedule().getId().equals(schedule.getId())) {
                continue;
            }
            if (ownTargetOccurrenceId != null && other.getId().equals(ownTargetOccurrenceId)) {
                continue;
            }
            Period otherPeriod = other.getEffectivePeriod();
            if (otherPeriod == null || !otherPeriod.getId().equals(targetPeriod.getId())) {
                continue;
            }
            if (!roomFlagged) {
                VenueResolution otherVenue = SessionOccurrenceVenue.fromOccurrence(other);
                boolean sameVenue = otherVenue.venueId() != null && venueId.equals(otherVenue.venueId())
                    && effectiveSessionType(other) == schedule.getSessionType();
                boolean samePhysicalRoom = physicalRoom != null && otherVenue.physicalRoom() != null
                    && physicalRoom.getId().equals(otherVenue.physicalRoom().getId());
                if (sameVenue || samePhysicalRoom) {
                    violations.add(new ConstraintViolation("RESCHEDULE_ROOM_CONFLICT",
                        "This room is already occupied by another session at this exact date and period."));
                    roomFlagged = true;
                }
            }
            if (!facultyFlagged && facultyId != null && facultyId.equals(effectiveFacultyId(other))) {
                violations.add(new ConstraintViolation("RESCHEDULE_FACULTY_CONFLICT",
                    "This faculty member already has another session at this exact date and period."));
                facultyFlagged = true;
            }
            if (!audienceFlagged && audienceId != null && audienceId.equals(effectiveAudienceId(other))) {
                violations.add(new ConstraintViolation("RESCHEDULE_AUDIENCE_CONFLICT",
                    "This cohort/batch already has another session at this exact date and period."));
                audienceFlagged = true;
            }
        }

        return violations;
    }

    private static ClassSessionType effectiveSessionType(SessionOccurrence occurrence) {
        return occurrence.getOccurrenceSource() == OccurrenceSource.REGULAR
            ? occurrence.getClassSchedule().getSessionType()
            : occurrence.getSessionType();
    }

    private static Long effectiveFacultyId(SessionOccurrence occurrence) {
        if (occurrence.getEffectiveFaculty() != null) {
            return occurrence.getEffectiveFaculty().getId();
        }
        if (occurrence.getOccurrenceSource() == OccurrenceSource.REGULAR) {
            return occurrence.getClassSchedule().getFaculty() != null
                ? occurrence.getClassSchedule().getFaculty().getId() : null;
        }
        return occurrence.getRequestedFaculty() != null ? occurrence.getRequestedFaculty().getId() : null;
    }

    private static Long effectiveAudienceId(SessionOccurrence occurrence) {
        if (occurrence.getOccurrenceSource() == OccurrenceSource.REGULAR) {
            return TimetableStaffingService.resolveAudienceId(occurrence.getClassSchedule());
        }
        if (occurrence.getBatch() != null) return occurrence.getBatch().getId();
        if (occurrence.getCohortSection() != null) return occurrence.getCohortSection().getId();
        return null;
    }

    private VenueResolution resolveVenue(ClassSessionType sessionType, Long venueId) {
        return switch (sessionType) {
            case THEORY, LIBRARY, SPORTS -> {
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
            case THEORY, LIBRARY, SPORTS -> occurrence.setClassroom(venue.classroom());
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

    /** Mirrors {@code RoomRelocationService.requirePublishedRealOccurrence} -- a reschedule only
     *  makes sense starting from a date the recurring schedule actually fires on (not a
     *  holiday/blocked date, not before the term starts, not already moved elsewhere). */
    private ClassSchedule requirePublishedRealOccurrence(Long classScheduleId, LocalDate date) {
        ClassSchedule schedule = classScheduleRepository.findById(classScheduleId)
            .orElseThrow(() -> new ResourceNotFoundException("Class schedule not found with id: " + classScheduleId));
        if (schedule.getStatus() != ClassScheduleStatus.PUBLISHED) {
            throw new IllegalArgumentException("Only published sessions can be rescheduled");
        }
        if (occurrenceService.occurrenceDatesFor(schedule, date, date).isEmpty()) {
            throw new IllegalArgumentException(date + " is not a real occurrence of this session");
        }
        return schedule;
    }
}
