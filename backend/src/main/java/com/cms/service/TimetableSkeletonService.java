package com.cms.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.SkeletonBuilderResponse;
import com.cms.dto.SkeletonCellPlacementRequest;
import com.cms.dto.SkeletonCellResponse;
import com.cms.dto.SkeletonSubjectBudget;
import com.cms.exception.LifecycleConflictException;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Batch;
import com.cms.model.BlockedPeriod;
import com.cms.model.ClassSchedule;
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
import com.cms.repository.CourseOfferingRepository;
import com.cms.repository.PeriodRepository;

/**
 * R3 Phase 4 — the manual "place period + session type first, staff it later" builder that
 * replaces the one-shot {@link TimetableGenerationService} for placement decisions. Scoped per
 * {@link CourseOffering} (one subject in one term), matching every other per-subject screen in
 * this app (Elective Assignment, Progress Tracking) — there's no cross-subject "class/section"
 * entity in the data model to build a whole-week multi-subject grid against, so building one
 * subject's skeleton at a time is what's actually implementable without inventing one. Rows
 * created here have no faculty/room ({@link ClassSchedule#getFaculty()} null, {@code status =
 * DRAFT}) until Phase 5's staffing pass fills them in — enforced at the database level by V335's
 * relaxed {@code chk_class_schedule_session_shape} CHECK.
 *
 * <p>Deliberately NOT solved here: full cross-subject audience-conflict detection (whether
 * ANOTHER subject already occupies this day/period for the same cohort/section) — that requires
 * a real notion of "which students are in this class" that doesn't exist yet. Only exact
 * self-duplicate placement (same offering+type+batch+day+period) is blocked.
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

    public TimetableSkeletonService(CourseOfferingRepository courseOfferingRepository,
                                     ClassScheduleRepository classScheduleRepository,
                                     PeriodRepository periodRepository,
                                     BatchRepository batchRepository,
                                     BatchService batchService,
                                     BlockedPeriodRepository blockedPeriodRepository) {
        this.courseOfferingRepository = courseOfferingRepository;
        this.classScheduleRepository = classScheduleRepository;
        this.periodRepository = periodRepository;
        this.batchRepository = batchRepository;
        this.batchService = batchService;
        this.blockedPeriodRepository = blockedPeriodRepository;
    }

    public SkeletonBuilderResponse getSkeleton(Long courseOfferingId) {
        CourseOffering offering = courseOfferingRepository.findById(courseOfferingId)
            .orElseThrow(() -> new ResourceNotFoundException("Course offering not found with id: " + courseOfferingId));
        CurriculumSemesterCourse csc = offering.getCurriculumSemesterCourse();
        if (csc == null) {
            throw new IllegalArgumentException(
                "This course offering has no resolved curriculum mapping — it has no hour totals to build a skeleton against");
        }
        TermInstance termInstance = offering.getTermInstance();

        int weeksInTerm = CurriculumHoursCalculator.weeksInTerm(termInstance);
        List<Period> periods = periodRepository.findByIsActiveTrueOrderByPeriodOrderAsc();
        double periodDurationMinutes = CurriculumHoursCalculator.averageDurationMinutes(
            periods.stream().map(Period::getDurationMinutes).toList());

        List<ClassSchedule> existing = classScheduleRepository.findByCourseOfferingId(courseOfferingId);
        List<Batch> batches = batchRepository.findByCourseOfferingId(courseOfferingId);

        List<SkeletonSubjectBudget> budgets = new ArrayList<>();
        budgets.add(theoryBudget(csc, existing, weeksInTerm, periodDurationMinutes));
        budgets.addAll(batchScopedBudgets(ClassSessionType.LAB, csc.getLabHours(), batches, existing, weeksInTerm, periodDurationMinutes));
        budgets.addAll(batchScopedBudgets(ClassSessionType.CLINICAL, csc.getClinicalHours(), batches, existing, weeksInTerm, periodDurationMinutes));

        List<SkeletonCellResponse> cells = existing.stream().map(this::toCellResponse).toList();

        return new SkeletonBuilderResponse(
            offering.getId(),
            offering.getSubject().getName(),
            offering.getSubject().getCode(),
            termInstance.getAcademicYear().getName() + " " + termInstance.getTermType(),
            budgets,
            cells,
            batchService.getBatchesForOffering(courseOfferingId)
        );
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
     *  ONE_OFF blocks never reach this check -- they only affect Capacity Planner buffer-hours
     *  math and calendar display, not placement. */
    private void requireNotBlocked(DayOfWeek dayOfWeek, Period period, TermInstance termInstance) {
        List<BlockedPeriod> conflicts = blockedPeriodRepository.findOverlappingRecurringBlocks(
            dayOfWeek, period.getId(), termInstance.getStartDate(), termInstance.getEndDate());
        if (!conflicts.isEmpty()) {
            throw new LifecycleConflictException(
                "This day and period is blocked: " + conflicts.get(0).getReason(),
                "SKELETON_CELL_PERIOD_BLOCKED", "ClassSchedule", null, null);
        }
    }

    private SkeletonCellResponse toCellResponse(ClassSchedule cs) {
        Period period = cs.getPeriod();
        Batch batch = cs.getBatch();
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
            cs.getStatus()
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
                && java.util.Objects.equals(cs.getBatch() != null ? cs.getBatch().getId() : null, request.batchId()));
        if (alreadyPlaced) {
            throw new LifecycleConflictException(
                "This subject already has a session placed at this exact day and period",
                "SKELETON_CELL_ALREADY_PLACED", "ClassSchedule", null, null);
        }

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
