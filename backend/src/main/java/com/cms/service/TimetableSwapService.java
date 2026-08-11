package com.cms.service;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.SwapCandidateResponse;
import com.cms.dto.SwapRequest;
import com.cms.exception.LifecycleConflictException;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.BlockedPeriod;
import com.cms.model.ClassSchedule;
import com.cms.model.Period;
import com.cms.model.TermInstance;
import com.cms.model.enums.ClassScheduleStatus;
import com.cms.model.enums.ClassSessionType;
import com.cms.model.enums.DayOfWeek;
import com.cms.repository.BlockedPeriodRepository;
import com.cms.repository.ClassScheduleRepository;
import com.cms.repository.FacultyAvailabilityRepository;
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
    private final FacultyAvailabilityRepository facultyAvailabilityRepository;
    private final PeriodRepository periodRepository;
    private final BlockedPeriodRepository blockedPeriodRepository;

    public TimetableSwapService(ClassScheduleRepository classScheduleRepository,
                                 FacultyAvailabilityRepository facultyAvailabilityRepository,
                                 PeriodRepository periodRepository,
                                 BlockedPeriodRepository blockedPeriodRepository) {
        this.classScheduleRepository = classScheduleRepository;
        this.facultyAvailabilityRepository = facultyAvailabilityRepository;
        this.periodRepository = periodRepository;
        this.blockedPeriodRepository = blockedPeriodRepository;
    }

    /** Whether a (day,start,end) slot works for `moving` — availability-clean and, if occupied,
     *  occupied by at most one same-room session (the potential swap partner). */
    private record SlotEvaluation(boolean valid, ClassSchedule occupant) {
        static final SlotEvaluation BLOCKED = new SlotEvaluation(false, null);
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
        SlotEvaluation eval = evaluateSlot(source, day, start, end, periodId, null);
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
    public void swap(Long termInstanceId, Long sessionId, SwapRequest request) {
        ClassSchedule source = requireDraftSession(termInstanceId, sessionId);

        Period targetPeriod = periodRepository.findById(request.periodId())
            .orElseThrow(() -> new ResourceNotFoundException("Period not found with id: " + request.periodId()));
        LocalTime start = targetPeriod.getStartTime();
        LocalTime end = targetPeriod.getEndTime();
        if (request.dayOfWeek() == source.getDayOfWeek() && targetPeriod.getId().equals(source.getPeriod().getId())) {
            throw new IllegalArgumentException("Target slot is the same as the session's current slot");
        }

        // Never trust a stale candidate list — re-evaluate from scratch.
        SlotEvaluation eval = evaluateSlot(source, request.dayOfWeek(), start, end, targetPeriod.getId(), null);
        if (!eval.valid()) {
            throw slotUnavailable(sessionId);
        }

        if (eval.occupant() != null) {
            ClassSchedule occupant = eval.occupant();
            if (!evaluateReverse(source, occupant).valid()) {
                throw slotUnavailable(sessionId);
            }
            occupant.setDayOfWeek(source.getDayOfWeek());
            occupant.setPeriod(source.getPeriod());
            classScheduleRepository.save(occupant);
        }

        source.setDayOfWeek(request.dayOfWeek());
        source.setPeriod(targetPeriod);
        classScheduleRepository.save(source);
    }

    /** Confirms `occupant` could itself move back into `source`'s original slot without new
     *  conflicts, once `source` has vacated it — required before offering/accepting a two-way
     *  swap, not just a move into an empty slot. */
    private SlotEvaluation evaluateReverse(ClassSchedule source, ClassSchedule occupant) {
        return evaluateSlot(occupant, source.getDayOfWeek(),
            source.getPeriod().getStartTime(), source.getPeriod().getEndTime(), source.getPeriod().getId(), source.getId());
    }

    /** @param alsoExcludeId an additional row (besides `moving` itself, always excluded) to leave
     *                       out of the conflict scan — used for the reverse-swap check, where the
     *                       session vacating the slot must not count as a blocker. */
    private SlotEvaluation evaluateSlot(ClassSchedule moving, DayOfWeek day, LocalTime start, LocalTime end,
                                         Long periodId, Long alsoExcludeId) {
        if (isBlocked(day, periodId, moving.getTermInstance())) {
            return SlotEvaluation.BLOCKED;
        }

        // Null for an unstaffed R3 Phase 4 skeleton row -- nothing to check faculty-availability
        // or faculty-conflict against yet, so both checks below become no-ops for it.
        Long facultyId = moving.getFaculty() != null ? moving.getFaculty().getId() : null;
        if (facultyId != null && !facultyAvailabilityRepository.findOverlapping(facultyId, day, start, end).isEmpty()) {
            return SlotEvaluation.BLOCKED;
        }

        List<ClassSchedule> overlapping = classScheduleRepository.findOverlapping(
            day, moving.getTermInstance().getId(), start, end, ClassScheduleStatus.DRAFT, moving.getId());
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
                if (roomOccupant != null) {
                    return SlotEvaluation.BLOCKED; // more than one session in the same room shouldn't happen structurally
                }
                roomOccupant = cs;
                continue;
            }
            if (facultyId != null && cs.getFaculty() != null && cs.getFaculty().getId().equals(facultyId)) {
                return SlotEvaluation.BLOCKED;
            }
            Long csAudienceId = resolveAudienceId(cs);
            if (sameSessionType && audienceId != null && audienceId.equals(csAudienceId)) {
                return SlotEvaluation.BLOCKED;
            }
        }
        return new SlotEvaluation(true, roomOccupant);
    }

    /** Non-throwing sibling of {@link TimetableSkeletonService}'s equivalent check — a candidate
     *  slot that falls in a recurring institutional lock or a holiday-derived one-off block simply
     *  never shows up as valid, rather than surfacing a distinct error code (there's no dedicated
     *  UI affordance here to explain "why" a slot didn't appear, unlike the skeleton builder's
     *  explicit placement action). Manually-created ONE_OFF blocks never reach this check, matching
     *  the skeleton builder's own coarseness. */
    private boolean isBlocked(DayOfWeek dayOfWeek, Long periodId, TermInstance termInstance) {
        List<BlockedPeriod> conflicts = blockedPeriodRepository.findOverlappingRecurringBlocks(
            dayOfWeek, periodId, termInstance.getStartDate(), termInstance.getEndDate());
        if (!conflicts.isEmpty()) {
            return true;
        }
        java.time.DayOfWeek targetDay = java.time.DayOfWeek.valueOf(dayOfWeek.name());
        return blockedPeriodRepository.findHolidayOneOffBlocksInRange(
                periodId, termInstance.getStartDate(), termInstance.getEndDate())
            .stream()
            .anyMatch(bp -> bp.getSpecificDate().getDayOfWeek() == targetDay);
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

    private static LifecycleConflictException slotUnavailable(Long sessionId) {
        return new LifecycleConflictException(
            "This slot is no longer available for swap — it may have changed since candidates were loaded.",
            "SWAP_SLOT_UNAVAILABLE", "ClassSchedule", sessionId, null);
    }
}
