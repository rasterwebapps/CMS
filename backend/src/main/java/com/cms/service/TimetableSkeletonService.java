package com.cms.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.CourseOfferingDto;
import com.cms.dto.SkeletonBuilderResponse;
import com.cms.dto.SkeletonCellPlacementRequest;
import com.cms.dto.SkeletonCellResponse;
import com.cms.dto.SkeletonPlacementCandidateResponse;
import com.cms.dto.SkeletonSubjectBudget;
import com.cms.dto.SkeletonSubjectResponse;
import com.cms.exception.LifecycleConflictException;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Batch;
import com.cms.model.BlockedPeriod;
import com.cms.model.ClassSchedule;
import com.cms.model.Cohort;
import com.cms.model.CourseOffering;
import com.cms.model.CurriculumSemesterCourse;
import com.cms.model.Period;
import com.cms.model.TermInstance;
import com.cms.model.enums.ClassScheduleStatus;
import com.cms.model.enums.ClassSessionType;
import com.cms.model.enums.DayOfWeek;
import com.cms.repository.BatchRepository;
import com.cms.repository.BlockedPeriodRepository;
import com.cms.repository.ClassScheduleRepository;
import com.cms.repository.CohortRepository;
import com.cms.repository.CourseOfferingRepository;
import com.cms.repository.PeriodRepository;
import com.cms.repository.TermInstanceRepository;

/**
 * R3 Phase 4 (cohort-wide since R3.1) — the manual "place period + session type first, staff it
 * later" builder that replaces the one-shot {@link TimetableGenerationService} for placement
 * decisions, which R3.1 retires entirely. Scoped per cohort/term: every non-elective
 * {@link CourseOffering} the cohort has that term is placed into one shared grid so cross-subject
 * placement is visible while building, not just at Staffing/Draft Review time. Rows created here
 * have no faculty/room ({@link ClassSchedule#getFaculty()} null, {@code status = DRAFT}) until
 * Phase 5's staffing pass fills them in — enforced at the database level by V335's relaxed
 * {@code chk_class_schedule_session_shape} CHECK.
 *
 * <p>Conflict detection: a THEORY session is mandatory for every student in the cohort, so it
 * hard-blocks against ANY other session (THEORY/LAB/CLINICAL, any subject) already placed at the
 * same cohort/day/period, and vice versa — see {@link #checkCohortExclusivity}. LAB/CLINICAL
 * sessions from different subjects sharing a slot are NOT hard-blocked (batch rosters aren't
 * tracked cross-subject, so real overlap can't be proven server-side) — the frontend renders that
 * case as an advisory instead. {@code Batch} itself stays {@link CourseOffering}-scoped, not
 * promoted to cohort-scoped — different subjects legitimately split labs into different batch
 * sizes, and Capacity Planner's committed venture batches never get a populated roster, so
 * roster-overlap detection would silently miss real clashes if attempted here.
 */
@Service
@Transactional(readOnly = true)
public class TimetableSkeletonService {

    private final CourseOfferingRepository courseOfferingRepository;
    private final ClassScheduleRepository classScheduleRepository;
    private final PeriodRepository periodRepository;
    private final BatchRepository batchRepository;
    private final BatchService batchService;
    private final BlockedPeriodRepository blockedPeriodRepository;
    private final com.cms.repository.RotationSlotRepository rotationSlotRepository;
    private final RotationResolverService rotationResolverService;
    private final CourseOfferingService courseOfferingService;
    private final CohortRepository cohortRepository;
    private final TermInstanceRepository termInstanceRepository;

    public TimetableSkeletonService(CourseOfferingRepository courseOfferingRepository,
                                     ClassScheduleRepository classScheduleRepository,
                                     PeriodRepository periodRepository,
                                     BatchRepository batchRepository,
                                     BatchService batchService,
                                     BlockedPeriodRepository blockedPeriodRepository,
                                     com.cms.repository.RotationSlotRepository rotationSlotRepository,
                                     RotationResolverService rotationResolverService,
                                     CourseOfferingService courseOfferingService,
                                     CohortRepository cohortRepository,
                                     TermInstanceRepository termInstanceRepository) {
        this.courseOfferingRepository = courseOfferingRepository;
        this.classScheduleRepository = classScheduleRepository;
        this.periodRepository = periodRepository;
        this.batchRepository = batchRepository;
        this.batchService = batchService;
        this.blockedPeriodRepository = blockedPeriodRepository;
        this.rotationSlotRepository = rotationSlotRepository;
        this.rotationResolverService = rotationResolverService;
        this.courseOfferingService = courseOfferingService;
        this.cohortRepository = cohortRepository;
        this.termInstanceRepository = termInstanceRepository;
    }

    public SkeletonBuilderResponse getCohortSkeleton(Long termInstanceId, Long cohortId) {
        Cohort cohort = cohortRepository.findById(cohortId)
            .orElseThrow(() -> new ResourceNotFoundException("Cohort not found with id: " + cohortId));
        TermInstance termInstance = termInstanceRepository.findById(termInstanceId)
            .orElseThrow(() -> new ResourceNotFoundException("Term instance not found with id: " + termInstanceId));
        String termInstanceLabel = termInstance.getAcademicYear().getName() + " " + termInstance.getTermType();

        List<Long> offeringIds = nonElectiveOfferingIds(termInstanceId, cohortId);
        if (offeringIds.isEmpty()) {
            return new SkeletonBuilderResponse(cohortId, cohort.getDisplayName(), termInstanceLabel, List.of(), List.of(), List.of());
        }

        Map<Long, CourseOffering> offeringById = new LinkedHashMap<>();
        for (Long id : offeringIds) {
            courseOfferingRepository.findById(id).ifPresent(o -> offeringById.put(id, o));
        }

        List<Period> periods = periodRepository.findByIsActiveTrueOrderByPeriodOrderAsc();
        int weeksInTerm = CurriculumHoursCalculator.weeksInTerm(termInstance);
        double periodDurationMinutes = CurriculumHoursCalculator.averageDurationMinutes(
            periods.stream().map(Period::getDurationMinutes).toList());

        List<ClassSchedule> allCells = classScheduleRepository.findByTermInstanceIdAndCourseOfferingIdIn(termInstanceId, offeringIds);
        Map<Long, List<ClassSchedule>> cellsByOffering = allCells.stream()
            .filter(cs -> cs.getCourseOffering() != null)
            .collect(java.util.stream.Collectors.groupingBy(cs -> cs.getCourseOffering().getId(), LinkedHashMap::new, java.util.stream.Collectors.toList()));

        List<SkeletonSubjectResponse> subjects = new ArrayList<>();
        List<com.cms.dto.BatchDto> batches = new ArrayList<>();
        for (Long offeringId : offeringIds) {
            CourseOffering offering = offeringById.get(offeringId);
            if (offering == null) continue;

            List<ClassSchedule> existingForOffering = cellsByOffering.getOrDefault(offeringId, List.of());
            List<Batch> offeringBatches = batchRepository.findByCourseOfferingId(offeringId);
            batches.addAll(batchService.getBatchesForOffering(offeringId));

            CurriculumSemesterCourse csc = offering.getCurriculumSemesterCourse();
            List<SkeletonSubjectBudget> budgets;
            if (csc == null) {
                // No resolved curriculum mapping -- can't compute hour budgets, but still show the
                // subject and any cells it already has rather than dropping it from the cohort view.
                budgets = List.of();
            } else {
                budgets = new ArrayList<>();
                budgets.add(theoryBudget(csc, existingForOffering, weeksInTerm, periodDurationMinutes));
                budgets.addAll(batchScopedBudgets(ClassSessionType.LAB, csc.getLabHours(), offeringBatches, existingForOffering, weeksInTerm, periodDurationMinutes));
                budgets.addAll(batchScopedBudgets(ClassSessionType.CLINICAL, csc.getClinicalHours(), offeringBatches, existingForOffering, weeksInTerm, periodDurationMinutes));
            }

            subjects.add(new SkeletonSubjectResponse(offeringId, offering.getSubject().getName(), offering.getSubject().getCode(), budgets));
        }

        List<SkeletonCellResponse> cells = allCells.stream().map(this::toCellResponse).toList();

        return new SkeletonBuilderResponse(cohortId, cohort.getDisplayName(), termInstanceLabel, subjects, cells, batches);
    }

    /** Non-elective offering ids for a cohort/term — Skeleton Builder never places electives
     *  (left for manual Elective Assignment), matching the frontend's existing filter. */
    private List<Long> nonElectiveOfferingIds(Long termInstanceId, Long cohortId) {
        return courseOfferingService.getOfferingsByTermInstanceAndCohort(termInstanceId, cohortId).stream()
            .filter(o -> !Boolean.TRUE.equals(o.isElective()))
            .map(CourseOfferingDto::id)
            .toList();
    }

    private SkeletonSubjectBudget theoryBudget(CurriculumSemesterCourse csc, List<ClassSchedule> existing,
                                                int weeksInTerm, double periodDurationMinutes) {
        int theoryHours = csc.getTheoryHours() != null ? csc.getTheoryHours() : 0;
        int required = CurriculumHoursCalculator.sessionsPerWeek(theoryHours, weeksInTerm, periodDurationMinutes);
        long placed = existing.stream().filter(cs -> cs.getSessionType() == ClassSessionType.THEORY).count();
        return new SkeletonSubjectBudget(ClassSessionType.THEORY, null, null, theoryHours, weeksInTerm, required, (int) placed);
    }

    /** LAB/CLINICAL need their own full quota per batch (batches run in parallel, not shared) —
     *  one budget row per existing batch, or one placeholder row (batchId null) flagging the
     *  hours are needed but there's nothing to place them against yet if no batch exists. */
    private List<SkeletonSubjectBudget> batchScopedBudgets(ClassSessionType type, Integer hoursObj, List<Batch> batches,
                                                            List<ClassSchedule> existing, int weeksInTerm,
                                                            double periodDurationMinutes) {
        int hours = hoursObj != null ? hoursObj : 0;
        if (hours <= 0) {
            return List.of();
        }
        int required = CurriculumHoursCalculator.sessionsPerWeek(hours, weeksInTerm, periodDurationMinutes);

        Map<Long, Long> placedByBatchId = existing.stream()
            .filter(cs -> cs.getSessionType() == type && cs.getBatch() != null)
            .collect(java.util.stream.Collectors.groupingBy(cs -> cs.getBatch().getId(), LinkedHashMap::new, java.util.stream.Collectors.counting()));

        if (batches.isEmpty()) {
            return List.of(new SkeletonSubjectBudget(type, null, null, hours, weeksInTerm, required, 0));
        }
        List<SkeletonSubjectBudget> rows = new ArrayList<>();
        for (Batch batch : batches) {
            long placed = placedByBatchId.getOrDefault(batch.getId(), 0L);
            rows.add(new SkeletonSubjectBudget(type, batch.getId(), batch.getName(), hours, weeksInTerm, required, (int) placed));
        }
        return rows;
    }

    /** Hard-blocks placing a session at a day+period covered by a RECURRING blocked-period rule
     *  whose date range overlaps this offering's term at all -- deliberately coarse, since a
     *  recurring weekly-template placement can't represent "blocked some weeks, not others."
     *  Manually-created ONE_OFF blocks never reach this check -- they only affect Capacity
     *  Planner buffer-hours math and calendar display, not placement. Holiday-derived ONE_OFF
     *  blocks (auto-generated from a HOLIDAY CalendarEvent) DO hard-block here too, scoped
     *  strictly to {@code sourceCalendarEventId IS NOT NULL} so this is the same accepted
     *  coarseness RECURRING already has (one holiday Monday blocks every Monday of that period for
     *  the whole term), not a new behavior change for manual one-off blocks. */
    private void requireNotBlocked(DayOfWeek dayOfWeek, Period period, TermInstance termInstance) {
        String reason = blockReason(dayOfWeek, period, termInstance);
        if (reason != null) {
            throw new LifecycleConflictException(
                "This day and period is blocked: " + reason,
                "SKELETON_CELL_PERIOD_BLOCKED", "ClassSchedule", null, null);
        }
    }

    /** Non-throwing sibling of {@link #requireNotBlocked}, used by {@link #suggestCandidates} to
     *  silently skip a blocked slot rather than aborting the whole scan. Returns the block reason,
     *  or null if the slot is free. */
    private String blockReason(DayOfWeek dayOfWeek, Period period, TermInstance termInstance) {
        List<BlockedPeriod> conflicts = blockedPeriodRepository.findOverlappingRecurringBlocks(
            dayOfWeek, period.getId(), termInstance.getStartDate(), termInstance.getEndDate());
        if (!conflicts.isEmpty()) {
            return conflicts.get(0).getReason();
        }

        java.time.DayOfWeek targetDay = java.time.DayOfWeek.valueOf(dayOfWeek.name());
        List<BlockedPeriod> holidayConflicts = blockedPeriodRepository.findHolidayOneOffBlocksInRange(
                period.getId(), termInstance.getStartDate(), termInstance.getEndDate())
            .stream()
            .filter(bp -> bp.getSpecificDate().getDayOfWeek() == targetDay)
            .toList();
        if (!holidayConflicts.isEmpty()) {
            return holidayConflicts.get(0).getReason();
        }
        return null;
    }

    private SkeletonCellResponse toCellResponse(ClassSchedule cs) {
        Period period = cs.getPeriod();
        Batch batch = cs.getBatch();

        String rotationGroupLabel = null;
        List<String> rotatingBatchNames = List.of();
        if (batch == null) {
            var rotationSlot = rotationSlotRepository.findByClassScheduleId(cs.getId()).orElse(null);
            if (rotationSlot != null) {
                rotationGroupLabel = rotationSlot.getRotationGroup().getLabel();
                rotatingBatchNames = rotationResolverService.allAssignmentsForSlot(cs.getId()).stream()
                    .map(a -> a.getBatch().getName())
                    .toList();
            }
        }

        return new SkeletonCellResponse(
            cs.getId(),
            cs.getSessionType(),
            cs.getDayOfWeek(),
            period != null ? period.getId() : null,
            period != null ? period.getName() : null,
            period != null ? period.getStartTime() : null,
            period != null ? period.getEndTime() : null,
            batch != null ? batch.getId() : null,
            batch != null ? batch.getName() : null,
            cs.getFaculty() != null,
            cs.getStatus(),
            rotationGroupLabel,
            rotatingBatchNames,
            cs.getCourseOffering() != null ? cs.getCourseOffering().getId() : null,
            cs.getSubject() != null ? cs.getSubject().getName() : null,
            cs.getSubject() != null ? cs.getSubject().getCode() : null
        );
    }

    @Transactional
    public SkeletonCellResponse placeCell(SkeletonCellPlacementRequest request) {
        CourseOffering offering = courseOfferingRepository.findById(request.courseOfferingId())
            .orElseThrow(() -> new ResourceNotFoundException("Course offering not found with id: " + request.courseOfferingId()));
        Period period = periodRepository.findById(request.periodId())
            .orElseThrow(() -> new ResourceNotFoundException("Period not found with id: " + request.periodId()));

        Batch batch = null;
        if (request.sessionType() == ClassSessionType.LAB || request.sessionType() == ClassSessionType.CLINICAL) {
            if (request.batchId() == null) {
                throw new IllegalArgumentException("A batch is required to place a " + request.sessionType() + " session");
            }
        }
        if (request.batchId() != null) {
            batch = batchRepository.findById(request.batchId())
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found with id: " + request.batchId()));
            if (!batch.getCourseOffering().getId().equals(offering.getId())) {
                throw new IllegalArgumentException("This batch does not belong to the selected course offering");
            }
        }

        boolean alreadyPlaced = classScheduleRepository.findByCourseOfferingId(offering.getId()).stream()
            .anyMatch(cs -> cs.getSessionType() == request.sessionType()
                && cs.getDayOfWeek() == request.dayOfWeek()
                && cs.getPeriod() != null && cs.getPeriod().getId().equals(request.periodId())
                && Objects.equals(cs.getBatch() != null ? cs.getBatch().getId() : null, request.batchId()));
        if (alreadyPlaced) {
            throw new LifecycleConflictException(
                "This subject already has a session placed at this exact day and period",
                "SKELETON_CELL_ALREADY_PLACED", "ClassSchedule", null, null);
        }

        checkCohortExclusivity(request, offering);

        requireNotBlocked(request.dayOfWeek(), period, offering.getTermInstance());

        ClassSchedule cs = new ClassSchedule();
        cs.setSessionType(request.sessionType());
        cs.setStatus(ClassScheduleStatus.DRAFT);
        cs.setSubject(offering.getSubject());
        cs.setDayOfWeek(request.dayOfWeek());
        cs.setTermInstance(offering.getTermInstance());
        cs.setCourseOffering(offering);
        cs.setPeriod(period);
        cs.setBatch(batch);
        cs.setBatchName(batch != null ? batch.getName() : null);
        cs.setIsActive(true);

        return toCellResponse(classScheduleRepository.save(cs));
    }

    /** THEORY is mandatory for every student in the cohort, so it hard-blocks against any other
     *  session (any subject, any type) already placed at the same cohort/day/period, and vice
     *  versa. LAB/CLINICAL-vs-LAB/CLINICAL across different subjects is deliberately NOT blocked
     *  here — real roster overlap can't be proven without batch rosters that don't exist yet; the
     *  frontend surfaces that case as an advisory instead of a hard error. */
    private void checkCohortExclusivity(SkeletonCellPlacementRequest request, CourseOffering offering) {
        List<Long> cohortOfferingIds = nonElectiveOfferingIds(offering.getTermInstance().getId(), request.cohortId());
        if (cohortOfferingIds.isEmpty()) {
            return;
        }
        List<ClassSchedule> cohortCellsAtSlot = classScheduleRepository
            .findByTermInstanceIdAndCourseOfferingIdIn(offering.getTermInstance().getId(), cohortOfferingIds)
            .stream()
            .filter(cs -> cs.getDayOfWeek() == request.dayOfWeek()
                && cs.getPeriod() != null && cs.getPeriod().getId().equals(request.periodId()))
            .toList();
        if (cohortCellsAtSlot.isEmpty()) {
            return;
        }

        if (request.sessionType() == ClassSessionType.THEORY) {
            ClassSchedule other = cohortCellsAtSlot.get(0);
            throw new LifecycleConflictException(
                "A Theory session is mandatory for the whole cohort and can't share a slot with another session — "
                    + (other.getSubject() != null ? other.getSubject().getName() : "another subject")
                    + " already has a session placed here",
                "SKELETON_CELL_COHORT_CLASH", "ClassSchedule", null, null);
        }

        cohortCellsAtSlot.stream()
            .filter(cs -> cs.getSessionType() == ClassSessionType.THEORY)
            .findFirst()
            .ifPresent(theoryCell -> {
                throw new LifecycleConflictException(
                    (theoryCell.getSubject() != null ? theoryCell.getSubject().getName() : "Another subject")
                        + " has a mandatory Theory session in this slot for the whole cohort — no other session can be placed here",
                    "SKELETON_CELL_COHORT_CLASH", "ClassSchedule", null, null);
            });
        // LAB/CLINICAL vs LAB/CLINICAL from a different subject: allowed, advisory-only client-side.
    }

    /** Read-only candidate slots for a subject/session-type/batch still short of its weekly
     *  budget — mirrors the day/period scan shape of the retired
     *  {@code TimetableGenerationService.placeTheory}/{@code placeLab}, capping at one candidate
     *  per day (same clustering guard). Sources "already placed" from this offering's own rows
     *  only — it has no cohortId param, so it can't check sibling subjects' cells; {@link
     *  #placeCell}'s {@link #checkCohortExclusivity} remains the authoritative gate, this is
     *  purely a convenience nudge that may occasionally suggest a slot placeCell then rejects. */
    public List<SkeletonPlacementCandidateResponse> suggestCandidates(Long courseOfferingId, ClassSessionType sessionType, Long batchId) {
        CourseOffering offering = courseOfferingRepository.findById(courseOfferingId)
            .orElseThrow(() -> new ResourceNotFoundException("Course offering not found with id: " + courseOfferingId));
        CurriculumSemesterCourse csc = offering.getCurriculumSemesterCourse();
        if (csc == null) {
            return List.of();
        }
        TermInstance termInstance = offering.getTermInstance();
        int weeksInTerm = CurriculumHoursCalculator.weeksInTerm(termInstance);
        List<Period> periods = periodRepository.findByIsActiveTrueOrderByPeriodOrderAsc();
        double periodDurationMinutes = CurriculumHoursCalculator.averageDurationMinutes(
            periods.stream().map(Period::getDurationMinutes).toList());

        Integer hoursObj = switch (sessionType) {
            case THEORY -> csc.getTheoryHours();
            case LAB -> csc.getLabHours();
            case CLINICAL -> csc.getClinicalHours();
        };
        int hours = hoursObj != null ? hoursObj : 0;
        if (hours <= 0) {
            return List.of();
        }
        int required = CurriculumHoursCalculator.sessionsPerWeek(hours, weeksInTerm, periodDurationMinutes);

        List<ClassSchedule> existingForOffering = classScheduleRepository.findByCourseOfferingId(courseOfferingId);
        List<ClassSchedule> existingForThis = existingForOffering.stream()
            .filter(cs -> cs.getSessionType() == sessionType
                && Objects.equals(cs.getBatch() != null ? cs.getBatch().getId() : null, batchId))
            .toList();
        int shortfall = required - existingForThis.size();
        if (shortfall <= 0) {
            return List.of();
        }

        Set<DayOfWeek> daysUsed = existingForThis.stream().map(ClassSchedule::getDayOfWeek)
            .collect(java.util.stream.Collectors.toCollection(java.util.HashSet::new));

        List<SkeletonPlacementCandidateResponse> candidates = new ArrayList<>();
        for (DayOfWeek day : DayOfWeek.values()) {
            if (candidates.size() >= shortfall) break;
            if (daysUsed.contains(day)) continue;
            for (Period period : periods) {
                if (blockReason(day, period, termInstance) != null) continue;
                candidates.add(new SkeletonPlacementCandidateResponse(day, period.getId()));
                daysUsed.add(day);
                break;
            }
        }
        return candidates;
    }

    @Transactional
    public void removeCell(Long classScheduleId) {
        ClassSchedule cs = classScheduleRepository.findById(classScheduleId)
            .orElseThrow(() -> new ResourceNotFoundException("Class schedule not found with id: " + classScheduleId));
        if (cs.getFaculty() != null || cs.getStatus() != ClassScheduleStatus.DRAFT) {
            throw new LifecycleConflictException(
                "Only an unstaffed draft skeleton cell can be removed here — edit or delete a staffed session from the Class Schedule screen instead",
                "SKELETON_CELL_NOT_REMOVABLE", "ClassSchedule", classScheduleId, null);
        }
        classScheduleRepository.deleteById(classScheduleId);
    }
}
