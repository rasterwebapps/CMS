package com.cms.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.ConstraintViolation;
import com.cms.dto.StaffSwapCandidateResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.exception.TimetableConstraintViolationException;
import com.cms.model.ClassSchedule;
import com.cms.model.SessionOccurrence;
import com.cms.model.enums.ClassScheduleStatus;
import com.cms.model.enums.DayOfWeek;
import com.cms.repository.ClassScheduleRepository;
import com.cms.repository.SessionOccurrenceRepository;

/**
 * Generalized single-date staff-to-staff session swap (Timetable planner Round 2, Phase 7) — two
 * PUBLISHED sessions on the same date trade faculty for that one date only, via {@link
 * SessionOccurrence} overrides (never mutating the recurring {@link ClassSchedule#getFaculty()}).
 * Deliberately a separate service from {@link TimetableSwapService}, which is explicitly DRAFT-only
 * and mutates day/period instead of faculty — conflating the two would violate that service's own
 * documented contract. Both directions of availability are checked (mutual swap), unlike Phase 6's
 * one-way absence substitute.
 */
@Service
@Transactional(readOnly = true)
public class FacultySessionSwapService {

    private final ClassScheduleRepository classScheduleRepository;
    private final SessionOccurrenceRepository sessionOccurrenceRepository;
    private final ClassScheduleOccurrenceService occurrenceService;
    private final AuditLogService auditLogService;
    private final TimetableStaffingService timetableStaffingService;

    public FacultySessionSwapService(ClassScheduleRepository classScheduleRepository,
                                      SessionOccurrenceRepository sessionOccurrenceRepository,
                                      ClassScheduleOccurrenceService occurrenceService,
                                      AuditLogService auditLogService,
                                      TimetableStaffingService timetableStaffingService) {
        this.classScheduleRepository = classScheduleRepository;
        this.sessionOccurrenceRepository = sessionOccurrenceRepository;
        this.occurrenceService = occurrenceService;
        this.auditLogService = auditLogService;
        this.timetableStaffingService = timetableStaffingService;
    }

    public List<StaffSwapCandidateResponse> findSwapCandidates(Long classScheduleId, LocalDate date) {
        ClassSchedule source = requirePublishedRealOccurrence(classScheduleId, date);
        LocalTime[] sourceTimes = resolveTimes(source);

        List<ClassSchedule> sameDay = classScheduleRepository.findByTermInstanceIdAndStatusAndDayOfWeek(
            source.getTermInstance().getId(), ClassScheduleStatus.PUBLISHED, source.getDayOfWeek());

        List<StaffSwapCandidateResponse> results = new ArrayList<>();
        for (ClassSchedule candidate : sameDay) {
            if (candidate.getId().equals(source.getId())) continue;
            if (candidate.getFaculty().getId().equals(source.getFaculty().getId())) continue;

            LocalTime[] candidateTimes = resolveTimes(candidate);
            boolean sourceFacultyFreeAtCandidateSlot =
                checkFacultyFreeToMove(source, source.getDayOfWeek(), candidateTimes[0], candidateTimes[1]).isEmpty();
            boolean candidateFacultyFreeAtSourceSlot =
                checkFacultyFreeToMove(candidate, source.getDayOfWeek(), sourceTimes[0], sourceTimes[1]).isEmpty();

            if (sourceFacultyFreeAtCandidateSlot && candidateFacultyFreeAtSourceSlot) {
                results.add(new StaffSwapCandidateResponse(candidate.getId(), candidate.getSubject().getName(),
                    candidate.getFaculty().getFullName(), candidateTimes[0], candidateTimes[1]));
            }
        }
        return results;
    }

    @Transactional
    public void applySwap(Long sessionAId, Long sessionBId, LocalDate date, String actor) {
        ClassSchedule a = requirePublishedRealOccurrence(sessionAId, date);
        ClassSchedule b = requirePublishedRealOccurrence(sessionBId, date);

        // Never trust a stale candidate list -- re-validate mutual availability from scratch,
        // directly rather than by re-deriving through findSwapCandidates' own list-membership check
        // (which only reported pass/fail, discarding the specific reason on failure).
        if (!a.getDayOfWeek().equals(b.getDayOfWeek())) {
            throw new IllegalArgumentException("Both sessions must fall on the same day to be swapped");
        }
        if (a.getFaculty().getId().equals(b.getFaculty().getId())) {
            throw new IllegalArgumentException("Cannot swap a session with another session taught by the same faculty member");
        }
        LocalTime[] aTimes = resolveTimes(a);
        LocalTime[] bTimes = resolveTimes(b);
        List<ConstraintViolation> violations = new ArrayList<>();
        violations.addAll(checkFacultyFreeToMove(a, a.getDayOfWeek(), bTimes[0], bTimes[1]));
        violations.addAll(checkFacultyFreeToMove(b, b.getDayOfWeek(), aTimes[0], aTimes[1]));
        if (!violations.isEmpty()) {
            throw new TimetableConstraintViolationException(violations);
        }

        SessionOccurrence occA = sessionOccurrenceRepository.findByClassScheduleIdAndOccurrenceDate(sessionAId, date)
            .orElseGet(() -> new SessionOccurrence(a, date));
        SessionOccurrence occB = sessionOccurrenceRepository.findByClassScheduleIdAndOccurrenceDate(sessionBId, date)
            .orElseGet(() -> new SessionOccurrence(b, date));

        occA.setEffectiveFaculty(b.getFaculty());
        occA.setOccurrenceStatus(com.cms.model.enums.OccurrenceStatus.SUBSTITUTED);
        occB.setEffectiveFaculty(a.getFaculty());
        occB.setOccurrenceStatus(com.cms.model.enums.OccurrenceStatus.SUBSTITUTED);

        SessionOccurrence savedA = sessionOccurrenceRepository.save(occA);
        SessionOccurrence savedB = sessionOccurrenceRepository.save(occB);
        savedA.setSwapPartnerOccurrence(savedB);
        savedB.setSwapPartnerOccurrence(savedA);
        sessionOccurrenceRepository.save(savedA);
        sessionOccurrenceRepository.save(savedB);
        auditLogService.record(actor, "TIMETABLE_STAFF_SWAPPED", "ClassSchedule",
            sessionAId + "," + sessionBId, "Faculty swapped for " + date + ": "
                + a.getFaculty().getFullName() + " <-> " + b.getFaculty().getFullName());
    }

    /** Non-throwing: reuses {@link TimetableStaffingService}'s shared, already-validated checks
     *  (same ones {@code staffCell} uses) instead of this service's own former hand-rolled
     *  availability/overlap query -- also closes a real gap where a single-date substitution never
     *  rechecked workload caps. Passing {@code cs} directly (not raw id/exclude params) mirrors the
     *  reuse pattern {@link TimetableSkeletonService#moveCell} already established for "re-check an
     *  existing entity at a target day/time without mutating it first". Widens from this service's
     *  old PUBLISHED-only scope to the shared checks' PUBLISHED+DRAFT scope -- a faculty member's own
     *  not-yet-approved DRAFT row at this day/time in the same term now also blocks the swap. */
    private List<ConstraintViolation> checkFacultyFreeToMove(ClassSchedule cs, DayOfWeek day, LocalTime start, LocalTime end) {
        List<ConstraintViolation> violations = new ArrayList<>();
        Long facultyId = cs.getFaculty().getId();
        timetableStaffingService.checkFacultyAvailable(facultyId, day, start, end).ifPresent(violations::add);
        timetableStaffingService.checkFacultyFree(facultyId, cs, day, start, end).ifPresent(violations::add);
        violations.addAll(timetableStaffingService.checkWithinWorkloadCaps(cs.getFaculty(), cs, day, start, end));
        return violations;
    }

    private ClassSchedule requirePublishedRealOccurrence(Long classScheduleId, LocalDate date) {
        ClassSchedule schedule = classScheduleRepository.findById(classScheduleId)
            .orElseThrow(() -> new ResourceNotFoundException("Class schedule not found with id: " + classScheduleId));
        if (schedule.getStatus() != ClassScheduleStatus.PUBLISHED) {
            throw new IllegalArgumentException("Only published sessions can be staff-swapped");
        }
        if (occurrenceService.occurrenceDatesFor(schedule, date, date).isEmpty()) {
            throw new IllegalArgumentException(date + " is not a real occurrence of this session");
        }
        return schedule;
    }

    private LocalTime[] resolveTimes(ClassSchedule cs) {
        return new LocalTime[]{cs.getPeriod().getStartTime(), cs.getPeriod().getEndTime()};
    }
}
