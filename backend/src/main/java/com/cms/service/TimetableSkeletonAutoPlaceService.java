package com.cms.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.AutoPlaceResult;
import com.cms.dto.AutoPlaceUnplacedItem;
import com.cms.dto.SkeletonBuilderResponse;
import com.cms.dto.SkeletonCellPlacementRequest;
import com.cms.dto.SkeletonCellResponse;
import com.cms.dto.SkeletonSubjectBudget;
import com.cms.dto.SkeletonSubjectResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.exception.TimetableConstraintViolationException;
import com.cms.model.CourseOffering;
import com.cms.model.Period;
import com.cms.model.TermInstance;
import com.cms.model.enums.ClassSessionType;
import com.cms.model.enums.DayOfWeek;
import com.cms.repository.CourseOfferingRepository;
import com.cms.repository.PeriodRepository;
import com.cms.repository.TermInstanceRepository;

/** R3 Step 6 — fills a cohort's remaining Skeleton Builder shortfall automatically, augmenting
 *  (never replacing) manual placement: every constraint check is {@link TimetableSkeletonService}'s
 *  own (via {@link TimetableSkeletonService#placeCell}), never re-implemented here. Electives are
 *  skipped entirely, matching the retired {@code TimetableGenerationService}'s own precedent — the
 *  same-slot elective-group coordination and free room picks aren't worth the complexity for a
 *  first cut. The bounded backtrack (see {@link #attemptBacktrack}) only ever displaces the single
 *  most-recently-placed *other* row's cell, and only keeps the displacement if the displaced row
 *  can be restored to its exact original slot, or the run still ends up with the same-or-better
 *  total placed count — it never leaves the run worse off than before the backtrack attempt. */
@Service
public class TimetableSkeletonAutoPlaceService {

    private final TimetableSkeletonService timetableSkeletonService;
    private final CourseOfferingRepository courseOfferingRepository;
    private final PeriodRepository periodRepository;
    private final TimetableBlockedPeriodChecker blockedPeriodChecker;
    private final TermInstanceRepository termInstanceRepository;

    public TimetableSkeletonAutoPlaceService(TimetableSkeletonService timetableSkeletonService,
                                              CourseOfferingRepository courseOfferingRepository,
                                              PeriodRepository periodRepository,
                                              TimetableBlockedPeriodChecker blockedPeriodChecker,
                                              TermInstanceRepository termInstanceRepository) {
        this.timetableSkeletonService = timetableSkeletonService;
        this.courseOfferingRepository = courseOfferingRepository;
        this.periodRepository = periodRepository;
        this.blockedPeriodChecker = blockedPeriodChecker;
        this.termInstanceRepository = termInstanceRepository;
    }

    /** One (subject, sessionType, batch/section) budget row still short of its weekly quota. */
    private record ShortfallRow(Long courseOfferingId, ClassSessionType sessionType, Long batchId,
                                 Long cohortSectionId, int shortfall, String subjectName, String occupantLabel) {}

    /** One cell this run has placed, carrying enough to identify its row (for the backtrack's
     *  "different row" check) and its exact slot (for an exact-slot restore attempt). */
    private record Placement(Long cellId, Long courseOfferingId, ClassSessionType sessionType, Long batchId,
                              Long cohortSectionId, String subjectName, String occupantLabel,
                              DayOfWeek dayOfWeek, Long periodId) {
        boolean sameRowAs(ShortfallRow row) {
            return courseOfferingId.equals(row.courseOfferingId()) && sessionType == row.sessionType()
                && Objects.equals(batchId, row.batchId()) && Objects.equals(cohortSectionId, row.cohortSectionId());
        }
    }

    @Transactional
    public AutoPlaceResult autoPlace(Long termInstanceId, Long cohortId) {
        TermInstance termInstance = termInstanceRepository.findById(termInstanceId)
            .orElseThrow(() -> new ResourceNotFoundException("Term instance not found with id: " + termInstanceId));
        SkeletonBuilderResponse skeleton = timetableSkeletonService.getCohortSkeleton(termInstanceId, cohortId);
        List<Period> periods = periodRepository.findByIsActiveTrueOrderByPeriodOrderAsc();

        List<ShortfallRow> rows = new ArrayList<>();
        for (SkeletonSubjectResponse subject : skeleton.subjects()) {
            CourseOffering offering = courseOfferingRepository.findById(subject.courseOfferingId()).orElse(null);
            if (offering == null || timetableSkeletonService.isElectiveOffering(offering)) {
                continue;
            }
            for (SkeletonSubjectBudget budget : subject.budgets()) {
                int shortfall = budget.requiredSessionsPerWeek() - budget.placedSessionsPerWeek();
                if (shortfall <= 0) {
                    continue;
                }
                String occupantLabel = budget.cohortSectionLabel() != null ? budget.cohortSectionLabel() : budget.batchName();
                rows.add(new ShortfallRow(subject.courseOfferingId(), budget.sessionType(), budget.batchId(),
                    budget.cohortSectionId(), shortfall, subject.subjectName(), occupantLabel));
            }
        }

        List<Placement> placedThisRun = new ArrayList<>();
        List<AutoPlaceUnplacedItem> unplaced = new ArrayList<>();

        for (ShortfallRow row : rows) {
            Set<DayOfWeek> daysUsed = existingDaysForRow(skeleton.cells(), row);
            for (int i = 0; i < row.shortfall(); i++) {
                Optional<Placement> placement = tryPlace(row, cohortId, termInstance, periods, daysUsed);
                if (placement.isPresent()) {
                    placedThisRun.add(placement.get());
                    daysUsed.add(placement.get().dayOfWeek());
                    continue;
                }
                if (!attemptBacktrack(row, cohortId, termInstance, periods, daysUsed, placedThisRun)) {
                    unplaced.add(new AutoPlaceUnplacedItem(row.subjectName(), row.sessionType(), row.occupantLabel(),
                        "no free slot found across every day/period combination"));
                }
            }
        }
        return new AutoPlaceResult(placedThisRun.size(), unplaced);
    }

    private Set<DayOfWeek> existingDaysForRow(List<SkeletonCellResponse> cells, ShortfallRow row) {
        Set<DayOfWeek> days = new HashSet<>();
        for (SkeletonCellResponse cell : cells) {
            if (cell.courseOfferingId().equals(row.courseOfferingId()) && cell.sessionType() == row.sessionType()
                    && Objects.equals(cell.batchId(), row.batchId())
                    && Objects.equals(cell.cohortSectionId(), row.cohortSectionId())) {
                days.add(cell.dayOfWeek());
            }
        }
        return days;
    }

    /** Bounded, single-attempt backtrack: displaces the single most-recently-placed cell from a
     *  *different* row, retries {@code row}, and — only if that retry succeeds — tries to restore
     *  the displaced cell to its exact original slot (guaranteed conflict-free unless the retry
     *  itself took that exact slot, which is the genuine-swap case). Returns whether {@code row}
     *  ended up placed; never leaves the run with fewer total placements than before the attempt. */
    private boolean attemptBacktrack(ShortfallRow row, Long cohortId, TermInstance termInstance, List<Period> periods,
                                      Set<DayOfWeek> daysUsed, List<Placement> placedThisRun) {
        for (int idx = placedThisRun.size() - 1; idx >= 0; idx--) {
            Placement bumped = placedThisRun.get(idx);
            if (bumped.sameRowAs(row)) {
                continue;
            }
            timetableSkeletonService.removeCell(bumped.cellId());
            Optional<Placement> retry = tryPlace(row, cohortId, termInstance, periods, daysUsed);
            if (retry.isEmpty()) {
                // No better off than before -- put the bumped cell straight back and give up on `row`.
                tryRestoreExact(bumped, cohortId);
                return false;
            }
            placedThisRun.remove(idx);
            placedThisRun.add(retry.get());
            daysUsed.add(retry.get().dayOfWeek());
            tryRestoreExact(bumped, cohortId).ifPresent(placedThisRun::add);
            // Whether or not the restore worked, `row` is now placed and the total count never
            // dropped below what it was before this attempt (net zero at worst, a genuine swap).
            return true;
        }
        return false;
    }

    private Optional<Placement> tryRestoreExact(Placement placement, Long cohortId) {
        try {
            SkeletonCellResponse restored = timetableSkeletonService.placeCell(new SkeletonCellPlacementRequest(
                placement.courseOfferingId(), placement.sessionType(), placement.dayOfWeek(), placement.periodId(),
                placement.batchId(), cohortId, placement.cohortSectionId(), null));
            return Optional.of(toPlacement(restored, placement.subjectName(), placement.occupantLabel()));
        } catch (TimetableConstraintViolationException ex) {
            return Optional.empty();
        }
    }

    private Optional<Placement> tryPlace(ShortfallRow row, Long cohortId, TermInstance termInstance,
                                          List<Period> periods, Set<DayOfWeek> daysUsed) {
        for (DayOfWeek day : DayOfWeek.values()) {
            if (daysUsed.contains(day)) {
                continue;
            }
            for (Period period : periods) {
                if (blockedPeriodChecker.blockReason(day, period.getStartTime(), period.getEndTime(),
                        termInstance.getStartDate(), termInstance.getEndDate()).isPresent()) {
                    continue;
                }
                try {
                    SkeletonCellResponse placed = timetableSkeletonService.placeCell(new SkeletonCellPlacementRequest(
                        row.courseOfferingId(), row.sessionType(), day, period.getId(),
                        row.batchId(), cohortId, row.cohortSectionId(), null));
                    return Optional.of(toPlacement(placed, row.subjectName(), row.occupantLabel()));
                } catch (TimetableConstraintViolationException ex) {
                    // try the next candidate
                }
            }
        }
        return Optional.empty();
    }

    private Placement toPlacement(SkeletonCellResponse cell, String subjectName, String occupantLabel) {
        return new Placement(cell.id(), cell.courseOfferingId(), cell.sessionType(), cell.batchId(),
            cell.cohortSectionId(), subjectName, occupantLabel, cell.dayOfWeek(), cell.periodId());
    }
}
