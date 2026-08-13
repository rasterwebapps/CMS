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
import com.cms.model.Room;
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
    private final AuditLogService auditLogService;
    private final TimetableStaffingService timetableStaffingService;

    /** OC-127: blocked-period checking for a candidate slot is now entirely delegated to {@link
     *  TimetableStaffingService#validateAssignment} — no longer needs its own {@link
     *  TimetableBlockedPeriodChecker} collaborator. */
    public TimetableSwapService(ClassScheduleRepository classScheduleRepository,
                                 PeriodRepository periodRepository,
                                 AuditLogService auditLogService,
                                 TimetableStaffingService timetableStaffingService) {
        this.classScheduleRepository = classScheduleRepository;
        this.periodRepository = periodRepository;
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
     *  <p>OC-127: funnels entirely through {@link TimetableStaffingService#validateAssignment} —
     *  blocked-period, faculty-availability, faculty-conflict, and workload-cap checks are the same
     *  ones {@code staffCell} uses (a candidate slot a faculty member can't actually be staffed into
     *  shouldn't be offered as swappable either); the room/audience scan is a swap-specific mode
     *  ({@code RoomMode.ALLOW_SINGLE_DRAFT_SWAP_PARTNER}) of the same shared method, since an occupied
     *  target slot in the *same room* can legitimately become a two-way swap partner here (not "must
     *  be entirely free"). This also folds in the physical-room cross-check {@code checkRoomFree}
     *  already had for staffing but this scan previously lacked (two different Classroom rows mapped
     *  to the same physical Room). Collects every applicable violation in one pass rather than the
     *  old first-found-wins short-circuit, matching the rest of the module's collect-all convention
     *  (OC-113) — a candidate slot that's both room- and faculty-conflicted now reports both. */
    private SlotEvaluation evaluateSlot(ClassSchedule moving, DayOfWeek day, LocalTime start, LocalTime end,
                                         Long alsoExcludeId) {
        boolean isTheory = moving.getSessionType() == ClassSessionType.THEORY;
        Long roomId = isTheory
            ? (moving.getClassroom() != null ? moving.getClassroom().getId() : null)
            : (moving.getLab() != null ? moving.getLab().getId() : null);
        Room physicalRoom = isTheory
            ? (moving.getClassroom() != null ? moving.getClassroom().getRoom() : null)
            : (moving.getLab() != null ? moving.getLab().getRoom() : null);
        Long audienceId = TimetableStaffingService.resolveAudienceId(moving);

        var roomCheck = new TimetableStaffingService.RoomCheckSpec(
            moving.getSessionType(), roomId, physicalRoom, TimetableStaffingService.RoomMode.ALLOW_SINGLE_DRAFT_SWAP_PARTNER);
        var result = timetableStaffingService.validateAssignment(
            moving, day, start, end, moving.getFaculty(), alsoExcludeId, roomCheck, audienceId);

        return result.isValid()
            ? new SlotEvaluation(true, result.swapPartnerOccupant(), List.of())
            : SlotEvaluation.blocked(result.violations());
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
