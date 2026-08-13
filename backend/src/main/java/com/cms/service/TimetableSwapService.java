package com.cms.service;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.ConstraintViolation;
import com.cms.dto.SwapCandidateResponse;
import com.cms.dto.SwapRequest;
import com.cms.exception.LifecycleConflictException;
import com.cms.exception.ResourceNotFoundException;
import com.cms.exception.TimetableConstraintViolationException;
import com.cms.model.ClassSchedule;
import com.cms.model.Period;
import com.cms.model.enums.ClassScheduleStatus;
import com.cms.model.enums.ClassSessionType;
import com.cms.model.enums.DayOfWeek;
import com.cms.repository.ClassScheduleRepository;
import com.cms.repository.PeriodRepository;

/**
 * Moves a DRAFT session to a different day/period, or — when the target slot is already occupied
 * by another DRAFT session in the same room — exchanges the two sessions' day+slot. Room never
 * changes; a candidate slot is only offered/accepted when the session's existing classroom/lab is
 * free at that day+time. THEORY, LAB, and CLINICAL rows all share the one Period master (V331
 * merged the formerly-separate LabSlot master into it). Room/audience resolution in {@link
 * #evaluateSlot} only branches THEORY vs non-THEORY (never reads {@code getClinicalVenue()}) —
 * safe today only because {@link TimetableGenerationService}, the sole producer of DRAFT rows,
 * never emits CLINICAL (see its class javadoc). Revisit this once R3 Phase 4's manual skeleton
 * builder can place a genuine DRAFT CLINICAL row. Scoped to DRAFT sessions only — swapping a
 * PUBLISHED/live timetable isn't supported by this feature (see TimetableDraftReviewComponent,
 * the only consumer).
 */
@Service
@Transactional(readOnly = true)
public class TimetableSwapService {

    private final ClassScheduleRepository classScheduleRepository;
    private final PeriodRepository periodRepository;
    private final TimetableBlockedPeriodChecker blockedPeriodChecker;
    private final AuditLogService auditLogService;
    private final TimetableStaffingService timetableStaffingService;

    public TimetableSwapService(ClassScheduleRepository classScheduleRepository,
                                 PeriodRepository periodRepository,
                                 TimetableBlockedPeriodChecker blockedPeriodChecker,
                                 AuditLogService auditLogService,
                                 TimetableStaffingService timetableStaffingService) {
        this.classScheduleRepository = classScheduleRepository;
        this.periodRepository = periodRepository;
        this.blockedPeriodChecker = blockedPeriodChecker;
        this.auditLogService = auditLogService;
        this.timetableStaffingService = timetableStaffingService;
    }

    /** Whether a (day,start,end) slot works for `moving` — availability-clean and, if occupied,
     *  occupied by at most one same-room session (the potential swap partner). `violations` is
     *  populated whenever `valid` is false, carrying the specific reason(s) so callers can surface
     *  more than "slot unavailable" at commit time. */
    private record SlotEvaluation(boolean valid, ClassSchedule occupant, List<ConstraintViolation> violations) {
        static SlotEvaluation blocked(List<ConstraintViolation> violations) {
            return new SlotEvaluation(false, null, violations);
        }
    }

    public List<SwapCandidateResponse> findCandidates(Long termInstanceId, Long sessionId) {
        ClassSchedule source = requireDraftSession(termInstanceId, sessionId);
        Long sourcePeriodId = source.getPeriod().getId();
        List<SwapCandidateResponse> candidates = new ArrayList<>();

        for (Period period : periodRepository.findByIsActiveTrueOrderByPeriodOrderAsc()) {
            for (DayOfWeek day : DayOfWeek.values()) {
                if (day == source.getDayOfWeek() && period.getId().equals(sourcePeriodId)) continue;
                addIfCandidate(candidates, source, day, period.getStartTime(), period.getEndTime(), period.getId());
            }
        }
        return candidates;
    }

    private void addIfCandidate(List<SwapCandidateResponse> out, ClassSchedule source, DayOfWeek day,
                                 LocalTime start, LocalTime end, Long periodId) {
        SlotEvaluation eval = evaluateSlot(source, day, start, end, null);
        if (!eval.valid()) {
            return;
        }
        if (eval.occupant() == null) {
            out.add(new SwapCandidateResponse(day, start, end, periodId, false, null, null));
            return;
        }
        // A swap partner exists here — also confirm the reverse move (partner -> source's original
        // slot) is itself conflict-free before offering this as a candidate.
        if (!evaluateReverse(source, eval.occupant()).valid()) {
            return;
        }
        out.add(new SwapCandidateResponse(day, start, end, periodId, true,
            eval.occupant().getId(), eval.occupant().getSubject().getName()));
    }

    @Transactional
    public void swap(Long termInstanceId, Long sessionId, SwapRequest request, String actor) {
        ClassSchedule source = requireDraftSession(termInstanceId, sessionId);

        Period targetPeriod = periodRepository.findById(request.periodId())
            .orElseThrow(() -> new ResourceNotFoundException("Period not found with id: " + request.periodId()));
        LocalTime start = targetPeriod.getStartTime();
        LocalTime end = targetPeriod.getEndTime();
        if (request.dayOfWeek() == source.getDayOfWeek() && targetPeriod.getId().equals(source.getPeriod().getId())) {
            throw new IllegalArgumentException("Target slot is the same as the session's current slot");
        }

        // Never trust a stale candidate list — re-evaluate from scratch.
        SlotEvaluation eval = evaluateSlot(source, request.dayOfWeek(), start, end, null);
        if (!eval.valid()) {
            throw new TimetableConstraintViolationException(eval.violations());
        }

        if (eval.occupant() != null) {
            ClassSchedule occupant = eval.occupant();
            SlotEvaluation reverseEval = evaluateReverse(source, occupant);
            if (!reverseEval.valid()) {
                throw new TimetableConstraintViolationException(reverseEval.violations());
            }
            occupant.setDayOfWeek(source.getDayOfWeek());
            occupant.setPeriod(source.getPeriod());
            classScheduleRepository.save(occupant);
        }

        source.setDayOfWeek(request.dayOfWeek());
        source.setPeriod(targetPeriod);
        classScheduleRepository.save(source);
        auditLogService.record(actor, "TIMETABLE_SESSION_SWAPPED", "ClassSchedule", sessionId.toString(),
            "Moved to " + request.dayOfWeek() + " " + targetPeriod.getName());
    }

    /** Confirms `occupant` could itself move back into `source`'s original slot without new
     *  conflicts, once `source` has vacated it — required before offering/accepting a two-way
     *  swap, not just a move into an empty slot. */
    private SlotEvaluation evaluateReverse(ClassSchedule source, ClassSchedule occupant) {
        return evaluateSlot(occupant, source.getDayOfWeek(),
            source.getPeriod().getStartTime(), source.getPeriod().getEndTime(), source.getId());
    }

    /** @param alsoExcludeId an additional row (besides `moving` itself, always excluded) to leave
     *                       out of the conflict scan — used for the reverse-swap check, where the
     *                       session vacating the slot must not count as a blocker.
     *
     *  <p>Faculty-availability and workload-cap checks below delegate to {@link
     *  TimetableStaffingService}'s shared, already-validated methods (same ones {@code staffCell}
     *  uses) — a candidate slot a faculty member can't actually be staffed into shouldn't be
     *  offered as swappable either. Faculty-conflict/room/audience detection stays hand-rolled
     *  against one shared query, for two reasons neither shared method's contract covers: (1) an
     *  occupied target slot in the *same room* can legitimately become a two-way swap partner here
     *  (not "must be entirely free"), and (2) {@code alsoExcludeId} — the reverse-swap partner's own
     *  still-unmutated row — must never count as a blocker against itself, which {@link
     *  TimetableStaffingService#checkFacultyFree} has no way to express (single-excludeId contract). */
    private SlotEvaluation evaluateSlot(ClassSchedule moving, DayOfWeek day, LocalTime start, LocalTime end,
                                         Long alsoExcludeId) {
        List<ConstraintViolation> violations = new ArrayList<>();
        blockedPeriodChecker.blockReason(day, start, end, moving.getTermInstance().getStartDate(), moving.getTermInstance().getEndDate())
            .ifPresent(reason -> violations.add(new ConstraintViolation(
                "SWAP_PERIOD_BLOCKED", "This day and period is blocked: " + reason)));

        // Null for an unstaffed R3 Phase 4 skeleton row -- nothing to check faculty-availability,
        // workload caps, or faculty-conflict against yet, so all three become no-ops for it.
        Long facultyId = moving.getFaculty() != null ? moving.getFaculty().getId() : null;
        if (facultyId != null) {
            timetableStaffingService.checkFacultyAvailable(facultyId, day, start, end).ifPresent(violations::add);
            violations.addAll(timetableStaffingService.checkWithinWorkloadCaps(moving.getFaculty(), moving, day, start, end));
        }
        if (!violations.isEmpty()) {
            return SlotEvaluation.blocked(violations);
        }

        // Faculty/room/audience conflicts, checked against both PUBLISHED and other DRAFT rows in
        // one shared query -- widened from the old DRAFT-only scan, closing a gap where a draft swap
        // could otherwise land on a slot a live PUBLISHED session already occupies.
        List<ClassSchedule> overlapping = new ArrayList<>();
        for (ClassScheduleStatus status : List.of(ClassScheduleStatus.PUBLISHED, ClassScheduleStatus.DRAFT)) {
            overlapping.addAll(classScheduleRepository.findOverlapping(
                day, moving.getTermInstance().getId(), start, end, status, moving.getId()));
        }
        if (alsoExcludeId != null) {
            overlapping = overlapping.stream().filter(cs -> !cs.getId().equals(alsoExcludeId)).toList();
        }

        boolean isTheory = moving.getSessionType() == ClassSessionType.THEORY;
        Long roomId = isTheory
            ? (moving.getClassroom() != null ? moving.getClassroom().getId() : null)
            : (moving.getLab() != null ? moving.getLab().getId() : null);
        Long audienceId = resolveAudienceId(moving);

        ClassSchedule roomOccupant = null;
        for (ClassSchedule cs : overlapping) {
            boolean sameSessionType = cs.getSessionType() == moving.getSessionType();
            Long csRoomId = isTheory
                ? (cs.getClassroom() != null ? cs.getClassroom().getId() : null)
                : (cs.getLab() != null ? cs.getLab().getId() : null);
            if (sameSessionType && roomId != null && roomId.equals(csRoomId)) {
                // A PUBLISHED occupant can never be a swap partner -- this service only ever
                // reschedules DRAFT rows, so silently moving a live session would be a real bug.
                // More than one DRAFT occupant in the same room shouldn't happen structurally either.
                if (cs.getStatus() != ClassScheduleStatus.DRAFT || roomOccupant != null) {
                    violations.add(new ConstraintViolation("SWAP_ROOM_CONFLICT",
                        "This room is already occupied by another session at this exact day and time."));
                    return SlotEvaluation.blocked(violations);
                }
                roomOccupant = cs;
                continue;
            }
            if (facultyId != null && cs.getFaculty() != null && cs.getFaculty().getId().equals(facultyId)) {
                violations.add(new ConstraintViolation("SWAP_FACULTY_CONFLICT",
                    "This faculty member is already scheduled for another session at this exact day and time."));
                return SlotEvaluation.blocked(violations);
            }
            Long csAudienceId = resolveAudienceId(cs);
            if (sameSessionType && audienceId != null && audienceId.equals(csAudienceId)) {
                violations.add(new ConstraintViolation("SWAP_AUDIENCE_CONFLICT",
                    "This slot is already occupied by another session for the same cohort/batch."));
                return SlotEvaluation.blocked(violations);
            }
        }
        return new SlotEvaluation(true, roomOccupant, List.of());
    }

    /** THEORY audience is batch-scoped when a section was picked (R3 Phase 3), falling back to
     *  the whole-cohort courseOffering for un-sectioned rows; LAB/CLINICAL are always batch-scoped. */
    private Long resolveAudienceId(ClassSchedule cs) {
        if (cs.getSessionType() == ClassSessionType.THEORY) {
            return cs.getBatch() != null ? cs.getBatch().getId()
                : (cs.getCourseOffering() != null ? cs.getCourseOffering().getId() : null);
        }
        return cs.getBatch() != null ? cs.getBatch().getId() : null;
    }

    private ClassSchedule requireDraftSession(Long termInstanceId, Long sessionId) {
        ClassSchedule cs = classScheduleRepository.findById(sessionId)
            .orElseThrow(() -> new ResourceNotFoundException("Class schedule not found with id: " + sessionId));
        if (!cs.getTermInstance().getId().equals(termInstanceId)) {
            throw new ResourceNotFoundException("Class schedule not found with id: " + sessionId);
        }
        if (cs.getStatus() != ClassScheduleStatus.DRAFT) {
            throw new LifecycleConflictException("Only draft sessions can be swapped.",
                "SESSION_NOT_DRAFT", "ClassSchedule", sessionId, null);
        }
        return cs;
    }
}
