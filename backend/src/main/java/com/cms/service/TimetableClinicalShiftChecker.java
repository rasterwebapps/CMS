package com.cms.service;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.cms.dto.ClinicalShiftWindow;
import com.cms.dto.ConstraintViolation;
import com.cms.model.ClassSchedule;
import com.cms.model.Cohort;
import com.cms.model.CohortSection;
import com.cms.model.Period;
import com.cms.model.TermInstance;
import com.cms.model.enums.DayOfWeek;
import com.cms.repository.CohortRepository;

/** Shared "is this cohort away on clinical duty then?" predicate, in the same spirit as {@link
 *  TimetableBlockedPeriodChecker} — one implementation used by every surface that needs it rather
 *  than a copy per caller.
 *
 *  <p>Deliberately NOT folded into {@link TimetableBlockedPeriodChecker}: that check is
 *  institution-wide and cohort-agnostic by design, while this one only means anything for a
 *  specific cohort, and only for a cohort whose Program has opted into Clinical Shift scheduling.
 *
 *  <p>Extracted when the Conflict Inspector was found not to run this check at all. Placement,
 *  moves, swaps and replaces had all enforced it for a while, but the whole-term scan that
 *  hard-gates Publish did not — so a session that landed inside a duty window (most easily by
 *  editing a shift group's start time AFTER the week was built, which revalidates nothing) stayed
 *  active, invisible in the grid, and passed the publish gate. Keeping the logic in one injectable
 *  place is what stops that gap reopening the next time a new surface needs it.
 */
@Service
public class TimetableClinicalShiftChecker {

    private final CohortRepository cohortRepository;
    private final ClinicalShiftGroupService clinicalShiftGroupService;

    public TimetableClinicalShiftChecker(CohortRepository cohortRepository,
                                          ClinicalShiftGroupService clinicalShiftGroupService) {
        this.cohortRepository = cohortRepository;
        this.clinicalShiftGroupService = clinicalShiftGroupService;
    }

    /** Violation if this cohort's Clinical Shift window (including bus travel buffer) covers any
     *  part of {@code period} on {@code dayOfWeek}. Empty when the cohort is unknown, its Program
     *  hasn't opted in, or the slot is genuinely free. */
    public Optional<ConstraintViolation> blockReason(Long cohortId, DayOfWeek dayOfWeek, Period period, TermInstance termInstance) {
        if (cohortId == null || period == null || termInstance == null) {
            return Optional.empty();
        }
        Cohort cohort = cohortRepository.findById(cohortId).orElse(null);
        if (cohort == null || cohort.getProgram() == null
            || !Boolean.TRUE.equals(cohort.getProgram().getUsesClinicalShiftScheduling())) {
            return Optional.empty();
        }
        List<ClinicalShiftWindow> windows = clinicalShiftGroupService
            .resolveActiveWindowsForCohort(cohortId, termInstance.getId());
        return windows.stream()
            .filter(w -> w.dayOfWeek() == dayOfWeek && w.overlaps(period.getStartTime(), period.getEndTime()))
            .findFirst()
            .map(w -> new ConstraintViolation("SKELETON_CELL_CLINICAL_SHIFT_BLOCKED",
                "This day and period falls within this cohort's Clinical Shift window (" + w.label() + ", "
                    + w.busDepart() + "–" + w.busReturn() + " incl. travel)"));
    }

    /** Row-oriented convenience for callers that hold a placed cell rather than a (cohort, day,
     *  period) triple.
     *
     *  <p>{@code fallbackCohortIds} covers the case {@link #audienceCohortId} cannot: a cell with
     *  no {@link CohortSection} at all. Elective sessions are exactly that — they are placed for a
     *  whole cohort rather than a section, so they carry neither a section nor a batch and the
     *  cohort is simply not reachable from the row. Skipping them would exempt the very sessions
     *  most likely to be misplaced: the automated elective pass was the one path that never
     *  checked duty windows, so electives are where the conflicts actually accumulated. The caller
     *  resolves which cohorts an offering serves (it can do so once per scan rather than per row)
     *  and passes them here; any one of them being on duty is enough. */
    public Optional<ConstraintViolation> blockReasonForCell(ClassSchedule cs, Set<Long> fallbackCohortIds) {
        if (cs == null) {
            return Optional.empty();
        }
        Long cohortId = audienceCohortId(cs);
        if (cohortId != null) {
            return blockReason(cohortId, cs.getDayOfWeek(), cs.getPeriod(), cs.getTermInstance());
        }
        if (fallbackCohortIds == null || fallbackCohortIds.isEmpty()) {
            return Optional.empty();
        }
        return fallbackCohortIds.stream()
            .map(id -> blockReason(id, cs.getDayOfWeek(), cs.getPeriod(), cs.getTermInstance()))
            .filter(Optional::isPresent)
            .findFirst()
            .orElse(Optional.empty());
    }

    /** True if ANY of these cohorts is on Clinical Shift duty during (day, period) — for a slot
     *  that several cohorts must all be able to attend, such as an elective group's one shared
     *  slot. One cohort being off-campus disqualifies it. */
    public boolean blocksAnyCohort(Set<Long> cohortIds, DayOfWeek day, Period period, TermInstance term) {
        if (cohortIds == null || cohortIds.isEmpty()) {
            return false;
        }
        return cohortIds.stream().anyMatch(id -> blockReason(id, day, period, term).isPresent());
    }

    /** The cohort a placed cell's audience belongs to, via its section's committed allocation.
     *  A LAB/CLINICAL row carries no {@code cohortSection} of its own, so it falls back to the
     *  batch's — the same fallback the Skeleton Builder's own cell mapping relies on. Null for a
     *  row with no section at all (unsectioned cohort, or a row predating section-scoped
     *  placement), where the duty check is skipped rather than guessed at. */
    public Long audienceCohortId(ClassSchedule cs) {
        CohortSection section = cs.getCohortSection() != null ? cs.getCohortSection()
            : (cs.getBatch() != null ? cs.getBatch().getCohortSection() : null);
        if (section == null || section.getCohortRoomAllocation() == null
            || section.getCohortRoomAllocation().getCohort() == null) {
            return null;
        }
        return section.getCohortRoomAllocation().getCohort().getId();
    }
}
