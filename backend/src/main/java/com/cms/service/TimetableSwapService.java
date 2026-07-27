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
import com.cms.model.ClassSchedule;
import com.cms.model.LabSlot;
import com.cms.model.Period;
import com.cms.model.enums.ClassScheduleStatus;
import com.cms.model.enums.ClassSessionType;
import com.cms.model.enums.DayOfWeek;
import com.cms.repository.ClassScheduleRepository;
import com.cms.repository.FacultyAvailabilityRepository;
import com.cms.repository.LabSlotRepository;
import com.cms.repository.PeriodRepository;

/**
 * Moves a DRAFT session to a different day/period (or day/labSlot), or — when the target slot is
 * already occupied by another DRAFT session in the same room — exchanges the two sessions'
 * day+slot. Room never changes; a candidate slot is only offered/accepted when the session's
 * existing classroom/lab is free at that day+time. Scoped to DRAFT sessions only — swapping a
 * PUBLISHED/live timetable isn't supported by this feature (see TimetableDraftReviewComponent,
 * the only consumer).
 */
@Service
@Transactional(readOnly = true)
public class TimetableSwapService {

    private final ClassScheduleRepository classScheduleRepository;
    private final FacultyAvailabilityRepository facultyAvailabilityRepository;
    private final PeriodRepository periodRepository;
    private final LabSlotRepository labSlotRepository;

    public TimetableSwapService(ClassScheduleRepository classScheduleRepository,
                                 FacultyAvailabilityRepository facultyAvailabilityRepository,
                                 PeriodRepository periodRepository,
                                 LabSlotRepository labSlotRepository) {
        this.classScheduleRepository = classScheduleRepository;
        this.facultyAvailabilityRepository = facultyAvailabilityRepository;
        this.periodRepository = periodRepository;
        this.labSlotRepository = labSlotRepository;
    }

    /** Whether a (day,start,end) slot works for `moving` — availability-clean and, if occupied,
     *  occupied by at most one same-room session (the potential swap partner). */
    private record SlotEvaluation(boolean valid, ClassSchedule occupant) {
        static final SlotEvaluation BLOCKED = new SlotEvaluation(false, null);
    }

    public List<SwapCandidateResponse> findCandidates(Long termInstanceId, Long sessionId) {
        ClassSchedule source = requireDraftSession(termInstanceId, sessionId);
        boolean isTheory = source.getSessionType() == ClassSessionType.THEORY;
        List<SwapCandidateResponse> candidates = new ArrayList<>();

        if (isTheory) {
            Long sourcePeriodId = source.getPeriod().getId();
            for (Period period : periodRepository.findByIsActiveTrueOrderByPeriodOrderAsc()) {
                for (DayOfWeek day : DayOfWeek.values()) {
                    if (day == source.getDayOfWeek() && period.getId().equals(sourcePeriodId)) continue;
                    addIfCandidate(candidates, source, day, period.getStartTime(), period.getEndTime(), period.getId(), null);
                }
            }
        } else {
            Long sourceSlotId = source.getLabSlot().getId();
            for (LabSlot slot : labSlotRepository.findByIsActiveTrueOrderBySlotOrderAsc()) {
                for (DayOfWeek day : DayOfWeek.values()) {
                    if (day == source.getDayOfWeek() && slot.getId().equals(sourceSlotId)) continue;
                    addIfCandidate(candidates, source, day, slot.getStartTime(), slot.getEndTime(), null, slot.getId());
                }
            }
        }
        return candidates;
    }

    private void addIfCandidate(List<SwapCandidateResponse> out, ClassSchedule source, DayOfWeek day,
                                 LocalTime start, LocalTime end, Long periodId, Long labSlotId) {
        SlotEvaluation eval = evaluateSlot(source, day, start, end, null);
        if (!eval.valid()) {
            return;
        }
        if (eval.occupant() == null) {
            out.add(new SwapCandidateResponse(day, start, end, periodId, labSlotId, false, null, null));
            return;
        }
        // A swap partner exists here — also confirm the reverse move (partner -> source's original
        // slot) is itself conflict-free before offering this as a candidate.
        if (!evaluateReverse(source, eval.occupant()).valid()) {
            return;
        }
        out.add(new SwapCandidateResponse(day, start, end, periodId, labSlotId, true,
            eval.occupant().getId(), eval.occupant().getSubject().getName()));
    }

    @Transactional
    public void swap(Long termInstanceId, Long sessionId, SwapRequest request) {
        ClassSchedule source = requireDraftSession(termInstanceId, sessionId);
        boolean isTheory = source.getSessionType() == ClassSessionType.THEORY;

        Period targetPeriod = null;
        LabSlot targetLabSlot = null;
        LocalTime start;
        LocalTime end;
        if (isTheory) {
            if (request.periodId() == null) {
                throw new IllegalArgumentException("periodId is required to swap a THEORY session");
            }
            targetPeriod = periodRepository.findById(request.periodId())
                .orElseThrow(() -> new ResourceNotFoundException("Period not found with id: " + request.periodId()));
            start = targetPeriod.getStartTime();
            end = targetPeriod.getEndTime();
            if (request.dayOfWeek() == source.getDayOfWeek() && targetPeriod.getId().equals(source.getPeriod().getId())) {
                throw new IllegalArgumentException("Target slot is the same as the session's current slot");
            }
        } else {
            if (request.labSlotId() == null) {
                throw new IllegalArgumentException("labSlotId is required to swap a LAB session");
            }
            targetLabSlot = labSlotRepository.findById(request.labSlotId())
                .orElseThrow(() -> new ResourceNotFoundException("Lab slot not found with id: " + request.labSlotId()));
            start = targetLabSlot.getStartTime();
            end = targetLabSlot.getEndTime();
            if (request.dayOfWeek() == source.getDayOfWeek() && targetLabSlot.getId().equals(source.getLabSlot().getId())) {
                throw new IllegalArgumentException("Target slot is the same as the session's current slot");
            }
        }

        // Never trust a stale candidate list — re-evaluate from scratch.
        SlotEvaluation eval = evaluateSlot(source, request.dayOfWeek(), start, end, null);
        if (!eval.valid()) {
            throw slotUnavailable(sessionId);
        }

        if (eval.occupant() != null) {
            ClassSchedule occupant = eval.occupant();
            if (!evaluateReverse(source, occupant).valid()) {
                throw slotUnavailable(sessionId);
            }
            DayOfWeek sourceDay = source.getDayOfWeek();
            occupant.setDayOfWeek(sourceDay);
            if (isTheory) {
                occupant.setPeriod(source.getPeriod());
            } else {
                occupant.setLabSlot(source.getLabSlot());
            }
            classScheduleRepository.save(occupant);
        }

        source.setDayOfWeek(request.dayOfWeek());
        if (isTheory) {
            source.setPeriod(targetPeriod);
        } else {
            source.setLabSlot(targetLabSlot);
        }
        classScheduleRepository.save(source);
    }

    /** Confirms `occupant` could itself move back into `source`'s original slot without new
     *  conflicts, once `source` has vacated it — required before offering/accepting a two-way
     *  swap, not just a move into an empty slot. */
    private SlotEvaluation evaluateReverse(ClassSchedule source, ClassSchedule occupant) {
        boolean isTheory = source.getSessionType() == ClassSessionType.THEORY;
        LocalTime sourceStart = isTheory ? source.getPeriod().getStartTime() : source.getLabSlot().getStartTime();
        LocalTime sourceEnd = isTheory ? source.getPeriod().getEndTime() : source.getLabSlot().getEndTime();
        return evaluateSlot(occupant, source.getDayOfWeek(), sourceStart, sourceEnd, source.getId());
    }

    /** @param alsoExcludeId an additional row (besides `moving` itself, always excluded) to leave
     *                       out of the conflict scan — used for the reverse-swap check, where the
     *                       session vacating the slot must not count as a blocker. */
    private SlotEvaluation evaluateSlot(ClassSchedule moving, DayOfWeek day, LocalTime start, LocalTime end,
                                         Long alsoExcludeId) {
        Long facultyId = moving.getFaculty().getId();
        if (!facultyAvailabilityRepository.findOverlapping(facultyId, day, start, end).isEmpty()) {
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
        Long audienceId = isTheory
            ? (moving.getCourseOffering() != null ? moving.getCourseOffering().getId() : null)
            : (moving.getBatch() != null ? moving.getBatch().getId() : null);

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
            if (cs.getFaculty().getId().equals(facultyId)) {
                return SlotEvaluation.BLOCKED;
            }
            Long csAudienceId = isTheory
                ? (cs.getCourseOffering() != null ? cs.getCourseOffering().getId() : null)
                : (cs.getBatch() != null ? cs.getBatch().getId() : null);
            if (sameSessionType && audienceId != null && audienceId.equals(csAudienceId)) {
                return SlotEvaluation.BLOCKED;
            }
        }
        return new SlotEvaluation(true, roomOccupant);
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
