package com.cms.service;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.CohortSectionResponse;
import com.cms.dto.ConstraintViolation;
import com.cms.dto.CourseOfferingDto;
import com.cms.dto.ElectiveGroupMemberPlacement;
import com.cms.dto.ElectiveGroupPlacementRequest;
import com.cms.dto.ElectiveGroupScheduleResponse;
import com.cms.dto.SkeletonBuilderResponse;
import com.cms.dto.SkeletonCellMoveRequest;
import com.cms.dto.SkeletonCellPlacementRequest;
import com.cms.dto.SkeletonCellResponse;
import com.cms.dto.SkeletonPlacementCandidateResponse;
import com.cms.dto.SkeletonSubjectBudget;
import com.cms.dto.SkeletonSubjectResponse;
import com.cms.exception.LifecycleConflictException;
import com.cms.exception.ResourceNotFoundException;
import com.cms.exception.TimetableConstraintViolationException;
import com.cms.model.Batch;
import com.cms.model.Classroom;
import com.cms.model.ClassSchedule;
import com.cms.model.Cohort;
import com.cms.model.CohortSection;
import com.cms.model.CourseOffering;
import com.cms.model.CurriculumSemesterCourse;
import com.cms.model.Period;
import com.cms.model.TermInstance;
import com.cms.model.enums.ClassScheduleStatus;
import com.cms.model.enums.ClassSessionType;
import com.cms.model.enums.CohortRoomAllocationStatus;
import com.cms.model.enums.DayOfWeek;
import com.cms.repository.BatchRepository;
import com.cms.repository.ClassScheduleRepository;
import com.cms.repository.CohortRepository;
import com.cms.repository.CohortRoomAllocationRepository;
import com.cms.repository.CohortSectionRepository;
import com.cms.repository.CourseOfferingRepository;
import com.cms.repository.PeriodRepository;
import com.cms.repository.TermInstanceRepository;

/**
 * R3 Phase 4 (cohort-wide since R3.1, per-section since R3.2) — the manual "place period + session
 * type first, staff it later" builder that replaces the one-shot {@link TimetableGenerationService}
 * for placement decisions, which R3.1 retires entirely. Scoped per cohort/term: every non-elective
 * {@link CourseOffering} the cohort has that term is placed into one shared grid so cross-subject
 * placement is visible while building, not just at Staffing/Draft Review time. Rows created here
 * have no faculty/room ({@link ClassSchedule#getFaculty()} null, {@code status = DRAFT}) until
 * Phase 5's staffing pass fills them in — enforced at the database level by V335's relaxed
 * {@code chk_class_schedule_session_shape} CHECK.
 *
 * <p>Conflict detection: a THEORY session is mandatory for every student in its audience, so it
 * hard-blocks against ANY other session (THEORY/LAB/CLINICAL, any subject) already placed at the
 * same audience/day/period, and vice versa — see {@link #checkCohortExclusivity}. Since R3.2, the
 * audience is no longer always "the whole cohort": when Capacity Planner has committed a room
 * allocation with more than one {@link CohortSection} for this cohort/term (V364), each section is
 * its own audience with its own room, and two different sections' sessions never conflict with each
 * other — see {@link #scopesConflict}. LAB/CLINICAL sessions from different subjects sharing a slot
 * are NOT hard-blocked (batch rosters aren't tracked cross-subject, so real overlap can't be proven
 * server-side) — the frontend renders that case as an advisory instead. {@code Batch} itself stays
 * {@link CourseOffering}-scoped, not promoted to cohort-scoped — different subjects legitimately
 * split labs into different batch sizes, and Capacity Planner's committed venture batches never get
 * a populated roster, so roster-overlap detection would silently miss real clashes if attempted here.
 */
@Service
@Transactional(readOnly = true)
public class TimetableSkeletonService {

    private static final String WHOLE_COHORT_SCOPE = "WHOLE";

    private final CourseOfferingRepository courseOfferingRepository;
    private final ClassScheduleRepository classScheduleRepository;
    private final PeriodRepository periodRepository;
    private final BatchRepository batchRepository;
    private final BatchService batchService;
    private final TimetableBlockedPeriodChecker blockedPeriodChecker;
    private final com.cms.repository.RotationSlotRepository rotationSlotRepository;
    private final RotationResolverService rotationResolverService;
    private final CourseOfferingService courseOfferingService;
    private final CohortRepository cohortRepository;
    private final TermInstanceRepository termInstanceRepository;
    private final CohortRoomAllocationRepository cohortRoomAllocationRepository;
    private final CohortSectionRepository cohortSectionRepository;
    private final TimetableStaffingService timetableStaffingService;

    public TimetableSkeletonService(CourseOfferingRepository courseOfferingRepository,
                                     ClassScheduleRepository classScheduleRepository,
                                     PeriodRepository periodRepository,
                                     BatchRepository batchRepository,
                                     BatchService batchService,
                                     TimetableBlockedPeriodChecker blockedPeriodChecker,
                                     com.cms.repository.RotationSlotRepository rotationSlotRepository,
                                     RotationResolverService rotationResolverService,
                                     CourseOfferingService courseOfferingService,
                                     CohortRepository cohortRepository,
                                     TermInstanceRepository termInstanceRepository,
                                     CohortRoomAllocationRepository cohortRoomAllocationRepository,
                                     CohortSectionRepository cohortSectionRepository,
                                     TimetableStaffingService timetableStaffingService) {
        this.courseOfferingRepository = courseOfferingRepository;
        this.classScheduleRepository = classScheduleRepository;
        this.periodRepository = periodRepository;
        this.batchRepository = batchRepository;
        this.batchService = batchService;
        this.blockedPeriodChecker = blockedPeriodChecker;
        this.rotationSlotRepository = rotationSlotRepository;
        this.rotationResolverService = rotationResolverService;
        this.courseOfferingService = courseOfferingService;
        this.cohortRepository = cohortRepository;
        this.termInstanceRepository = termInstanceRepository;
        this.cohortRoomAllocationRepository = cohortRoomAllocationRepository;
        this.cohortSectionRepository = cohortSectionRepository;
        this.timetableStaffingService = timetableStaffingService;
    }

    public SkeletonBuilderResponse getCohortSkeleton(Long termInstanceId, Long cohortId) {
        Cohort cohort = cohortRepository.findById(cohortId)
            .orElseThrow(() -> new ResourceNotFoundException("Cohort not found with id: " + cohortId));
        TermInstance termInstance = termInstanceRepository.findById(termInstanceId)
            .orElseThrow(() -> new ResourceNotFoundException("Term instance not found with id: " + termInstanceId));
        String termInstanceLabel = termInstance.getAcademicYear().getName() + " " + termInstance.getTermType();

        List<CohortSection> activeSections = resolveActiveSections(cohortId, termInstanceId);
        List<CohortSectionResponse> sectionResponses = activeSections.stream().map(this::toSectionResponse).toList();

        List<Long> offeringIds = new ArrayList<>(nonElectiveOfferingIds(termInstanceId, cohortId));
        offeringIds.addAll(electiveOfferingIds(termInstanceId, cohortId));
        if (offeringIds.isEmpty()) {
            return new SkeletonBuilderResponse(cohortId, cohort.getDisplayName(), termInstanceLabel, List.of(), List.of(), List.of(), sectionResponses);
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
            // Soft-deleted/inactive batches (e.g. superseded Capacity Planner batch splits) must
            // not surface as placeable budget rows or dropdown options here.
            List<Batch> offeringBatches = batchRepository.findByCourseOfferingId(offeringId).stream()
                .filter(b -> Boolean.TRUE.equals(b.getIsActive()))
                .toList();
            batches.addAll(batchService.getBatchesForOffering(offeringId).stream()
                .filter(b -> Boolean.TRUE.equals(b.isActive()))
                .toList());

            CurriculumSemesterCourse csc = offering.getCurriculumSemesterCourse();
            List<SkeletonSubjectBudget> budgets;
            if (csc == null) {
                // No resolved curriculum mapping -- can't compute hour budgets, but still show the
                // subject and any cells it already has rather than dropping it from the cohort view.
                budgets = List.of();
            } else {
                budgets = new ArrayList<>();
                budgets.addAll(theoryBudgets(csc, existingForOffering, weeksInTerm, periodDurationMinutes, activeSections));
                budgets.addAll(batchScopedBudgets(ClassSessionType.LAB, csc.getLabHours(), offeringBatches, existingForOffering, weeksInTerm, periodDurationMinutes));
                budgets.addAll(batchScopedBudgets(ClassSessionType.CLINICAL, csc.getClinicalHours(), offeringBatches, existingForOffering, weeksInTerm, periodDurationMinutes));
            }

            var electiveGroup = csc != null ? csc.getElectiveGroup() : null;
            subjects.add(new SkeletonSubjectResponse(offeringId, offering.getSubject().getName(), offering.getSubject().getCode(), budgets,
                electiveGroup != null ? electiveGroup.getId() : null,
                electiveGroup != null ? electiveGroup.getGroupName() : null));
        }

        List<SkeletonCellResponse> cells = allCells.stream().map(this::toCellResponse).toList();

        return new SkeletonBuilderResponse(cohortId, cohort.getDisplayName(), termInstanceLabel, subjects, cells, batches, sectionResponses);
    }

    /** Active sections of the cohort's committed Cohort Room Allocation for this term, or empty if
     *  none has been committed — mirrors {@code TimetableStaffingService.resolveCommittedTheoryClassroom}'s
     *  exact repository chain. Empty means "whole cohort" (today's original behavior); one or more
     *  active sections means THEORY placement becomes per-section. */
    private List<CohortSection> resolveActiveSections(Long cohortId, Long termInstanceId) {
        return cohortRoomAllocationRepository
            .findByCohortIdAndTermInstanceIdAndStatus(cohortId, termInstanceId, CohortRoomAllocationStatus.COMMITTED)
            .map(a -> cohortSectionRepository.findByCohortRoomAllocationIdAndIsActiveTrue(a.getId()))
            .orElse(List.of());
    }

    private CohortSectionResponse toSectionResponse(CohortSection section) {
        Classroom classroom = section.getClassroom();
        return new CohortSectionResponse(
            section.getId(),
            section.getSectionLabel(),
            classroom.getId(),
            classroom.getName(),
            classroom.getCapacity(),
            section.getPlannedSize(),
            section.getIsActive()
        );
    }

    /** Non-elective offering ids for a cohort/term — Skeleton Builder never places electives
     *  (left for manual Elective Assignment), matching the frontend's existing filter. */
    private List<Long> nonElectiveOfferingIds(Long termInstanceId, Long cohortId) {
        return courseOfferingService.getOfferingsByTermInstanceAndCohort(termInstanceId, cohortId).stream()
            .filter(o -> !Boolean.TRUE.equals(o.isElective()))
            .map(CourseOfferingDto::id)
            .toList();
    }

    /** Elective offering ids for a cohort/term — since R3.3, these ARE placed in the skeleton
     *  grid alongside non-elective subjects (unlike {@link #nonElectiveOfferingIds}, which stays
     *  the audience for {@link #checkCohortExclusivity} — electives are exempt from that
     *  whole-cohort hard-lock, matching their existing exemption from Staffing's room lock; see
     *  {@link #checkElectiveGroupSlot} for what IS enforced on them instead). */
    private List<Long> electiveOfferingIds(Long termInstanceId, Long cohortId) {
        return courseOfferingService.getOfferingsByTermInstanceAndCohort(termInstanceId, cohortId).stream()
            .filter(o -> Boolean.TRUE.equals(o.isElective()))
            .map(CourseOfferingDto::id)
            .toList();
    }

    /** One whole-cohort row when the cohort has no committed room allocation (today's original
     *  behavior, unchanged); one row per active {@link CohortSection} when it does — mirroring
     *  {@link #batchScopedBudgets}'s one-row-per-occupant shape, but keeping this method's original
     *  quirk of always emitting at least one row regardless of hours (batchScopedBudgets instead
     *  returns nothing when hours <= 0). */
    private List<SkeletonSubjectBudget> theoryBudgets(CurriculumSemesterCourse csc, List<ClassSchedule> existing,
                                                        int weeksInTerm, double periodDurationMinutes,
                                                        List<CohortSection> sections) {
        int theoryHours = csc.getTheoryHours() != null ? csc.getTheoryHours() : 0;
        int required = CurriculumHoursCalculator.sessionsPerWeek(theoryHours, weeksInTerm, periodDurationMinutes);

        if (sections.isEmpty()) {
            long placed = existing.stream()
                .filter(cs -> cs.getSessionType() == ClassSessionType.THEORY && cs.getCohortSection() == null)
                .count();
            return List.of(new SkeletonSubjectBudget(ClassSessionType.THEORY, null, null, null, null,
                theoryHours, weeksInTerm, required, (int) placed));
        }

        Map<Long, Long> placedBySectionId = existing.stream()
            .filter(cs -> cs.getSessionType() == ClassSessionType.THEORY && cs.getCohortSection() != null)
            .collect(java.util.stream.Collectors.groupingBy(cs -> cs.getCohortSection().getId(), LinkedHashMap::new, java.util.stream.Collectors.counting()));

        List<SkeletonSubjectBudget> rows = new ArrayList<>();
        for (CohortSection section : sections) {
            long placed = placedBySectionId.getOrDefault(section.getId(), 0L);
            rows.add(new SkeletonSubjectBudget(ClassSessionType.THEORY, null, null,
                section.getId(), section.getSectionLabel(), theoryHours, weeksInTerm, required, (int) placed));
        }
        return rows;
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
            return List.of(new SkeletonSubjectBudget(type, null, null, null, null, hours, weeksInTerm, required, 0));
        }
        List<SkeletonSubjectBudget> rows = new ArrayList<>();
        for (Batch batch : batches) {
            long placed = placedByBatchId.getOrDefault(batch.getId(), 0L);
            rows.add(new SkeletonSubjectBudget(type, batch.getId(), batch.getName(), null, null, hours, weeksInTerm, required, (int) placed));
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
    /** Non-throwing: returns a violation if this day+period falls in a recurring institutional
     *  lock or a holiday-derived one-off block — backed by the shared {@link
     *  TimetableBlockedPeriodChecker} {@link TimetableStaffingService} and {@link
     *  TimetableSwapService} also use. */
    private Optional<ConstraintViolation> checkBlocked(DayOfWeek dayOfWeek, Long periodId, TermInstance termInstance) {
        return blockedPeriodChecker.blockReason(dayOfWeek, periodId, termInstance.getStartDate(), termInstance.getEndDate())
            .map(reason -> new ConstraintViolation("SKELETON_CELL_PERIOD_BLOCKED", "This day and period is blocked: " + reason));
    }

    /** Used by {@link #suggestCandidates} to silently skip a blocked slot rather than surfacing a
     *  distinct violation — there's no per-candidate UI affordance to explain "why" a slot didn't
     *  appear. Returns the block reason, or null if the slot is free. */
    private String blockReason(DayOfWeek dayOfWeek, Period period, TermInstance termInstance) {
        return blockedPeriodChecker.blockReason(dayOfWeek, period.getId(), termInstance.getStartDate(), termInstance.getEndDate())
            .orElse(null);
    }

    private SkeletonCellResponse toCellResponse(ClassSchedule cs) {
        Period period = cs.getPeriod();
        Batch batch = cs.getBatch();
        CohortSection cohortSection = cs.getCohortSection();
        var electiveGroup = cs.getCourseOffering() != null && cs.getCourseOffering().getCurriculumSemesterCourse() != null
            ? cs.getCourseOffering().getCurriculumSemesterCourse().getElectiveGroup()
            : null;

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
            cohortSection != null ? cohortSection.getId() : null,
            cohortSection != null ? cohortSection.getSectionLabel() : null,
            cs.getFaculty() != null,
            cs.getStatus(),
            rotationGroupLabel,
            rotatingBatchNames,
            cs.getCourseOffering() != null ? cs.getCourseOffering().getId() : null,
            cs.getSubject() != null ? cs.getSubject().getName() : null,
            cs.getSubject() != null ? cs.getSubject().getCode() : null,
            electiveGroup != null ? electiveGroup.getId() : null,
            electiveGroup != null ? electiveGroup.getGroupName() : null
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

        CohortSection cohortSection = null;
        if (request.sessionType() == ClassSessionType.THEORY) {
            List<CohortSection> activeSections = resolveActiveSections(request.cohortId(), offering.getTermInstance().getId());
            if (!activeSections.isEmpty()) {
                if (request.cohortSectionId() == null) {
                    throw new IllegalArgumentException(
                        "A cohort section is required to place a Theory session — this cohort has a committed room allocation");
                }
                cohortSection = activeSections.stream()
                    .filter(s -> s.getId().equals(request.cohortSectionId()))
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException(
                        "Cohort section not found with id: " + request.cohortSectionId() + " for this cohort/term"));
            }
        }

        List<ConstraintViolation> violations = new ArrayList<>();

        checkAlreadyPlaced(offering, request).ifPresent(violations::add);

        if (isElectiveOffering(offering)) {
            checkElectiveGroupSlot(offering, request).ifPresent(violations::add);
        } else {
            checkCohortExclusivity(request, offering, batch, cohortSection).ifPresent(violations::add);
        }

        checkBlocked(request.dayOfWeek(), period.getId(), offering.getTermInstance()).ifPresent(violations::add);

        if (!violations.isEmpty()) {
            throw new TimetableConstraintViolationException(violations);
        }

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
        cs.setCohortSection(cohortSection);
        cs.setIsActive(true);

        return toCellResponse(classScheduleRepository.save(cs));
    }

    /** Non-throwing: returns a violation if this course offering already has another session of
     *  the exact same type/day/period/batch/section combination — checked by both {@link
     *  #placeCell} (against a not-yet-created row) and {@link #moveCell} (against the target slot;
     *  the moving cell itself always sits at its *old* slot when this runs, so it never spuriously
     *  matches itself here). */
    private Optional<ConstraintViolation> checkAlreadyPlaced(CourseOffering offering, SkeletonCellPlacementRequest request) {
        boolean alreadyPlaced = classScheduleRepository.findByCourseOfferingId(offering.getId()).stream()
            .anyMatch(cs -> cs.getSessionType() == request.sessionType()
                && cs.getDayOfWeek() == request.dayOfWeek()
                && cs.getPeriod() != null && cs.getPeriod().getId().equals(request.periodId())
                && Objects.equals(cs.getBatch() != null ? cs.getBatch().getId() : null, request.batchId())
                && Objects.equals(cs.getCohortSection() != null ? cs.getCohortSection().getId() : null, request.cohortSectionId()));
        return alreadyPlaced ? Optional.of(new ConstraintViolation("SKELETON_CELL_ALREADY_PLACED",
            "This subject already has a session placed at this exact day and period")) : Optional.empty();
    }

    /** Moves an already-placed cell (unstaffed or already-staffed) to a different day/period,
     *  re-running the same placement checks {@link #placeCell} uses against the target slot, plus
     *  — when the cell already carries a faculty — {@link TimetableStaffingService}'s faculty/room
     *  checks re-evaluated at that target slot (day-parameterized there for exactly this reason).
     *  Room/capacity/faculty-eligibility are deliberately NOT rechecked: none of them change on a
     *  pure day/period move (the room, audience, and faculty all stay exactly what they already
     *  were), so re-validating them would be redundant work re-proving something already true. */
    @Transactional
    public SkeletonCellResponse moveCell(Long classScheduleId, SkeletonCellMoveRequest request) {
        ClassSchedule cs = classScheduleRepository.findById(classScheduleId)
            .orElseThrow(() -> new ResourceNotFoundException("Class schedule not found with id: " + classScheduleId));
        if (cs.getStatus() != ClassScheduleStatus.DRAFT) {
            throw new LifecycleConflictException(
                "Only a draft skeleton cell can be moved here.",
                "SKELETON_CELL_NOT_DRAFT", "ClassSchedule", classScheduleId, null);
        }
        if (cs.getDayOfWeek() == request.dayOfWeek()
                && cs.getPeriod() != null && cs.getPeriod().getId().equals(request.periodId())) {
            throw new IllegalArgumentException("Target slot is the same as the cell's current slot");
        }
        Period targetPeriod = periodRepository.findById(request.periodId())
            .orElseThrow(() -> new ResourceNotFoundException("Period not found with id: " + request.periodId()));

        CourseOffering offering = cs.getCourseOffering();
        SkeletonCellPlacementRequest asPlacementRequest = new SkeletonCellPlacementRequest(
            offering.getId(), cs.getSessionType(), request.dayOfWeek(), targetPeriod.getId(),
            cs.getBatch() != null ? cs.getBatch().getId() : null,
            request.cohortId(),
            cs.getCohortSection() != null ? cs.getCohortSection().getId() : null);

        List<ConstraintViolation> violations = new ArrayList<>();
        checkAlreadyPlaced(offering, asPlacementRequest).ifPresent(violations::add);
        if (isElectiveOffering(offering)) {
            checkElectiveGroupSlot(offering, asPlacementRequest).ifPresent(violations::add);
        } else {
            checkCohortExclusivity(asPlacementRequest, offering, cs.getBatch(), cs.getCohortSection()).ifPresent(violations::add);
        }
        checkBlocked(request.dayOfWeek(), targetPeriod.getId(), offering.getTermInstance()).ifPresent(violations::add);

        if (cs.getFaculty() != null) {
            LocalTime start = targetPeriod.getStartTime();
            LocalTime end = targetPeriod.getEndTime();
            Long facultyId = cs.getFaculty().getId();
            timetableStaffingService.checkFacultyAvailable(facultyId, request.dayOfWeek(), start, end).ifPresent(violations::add);
            timetableStaffingService.checkFacultyFree(facultyId, cs, request.dayOfWeek(), start, end).ifPresent(violations::add);
            violations.addAll(timetableStaffingService.checkWithinWorkloadCaps(cs.getFaculty(), cs, request.dayOfWeek(), start, end));
            Long venueId = TimetableStaffingService.venueIdOf(cs);
            if (venueId != null) {
                timetableStaffingService.checkRoomFree(cs.getSessionType(), venueId, TimetableStaffingService.physicalRoomOf(cs),
                    cs, request.dayOfWeek(), start, end).ifPresent(violations::add);
            }
        }

        if (!violations.isEmpty()) {
            throw new TimetableConstraintViolationException(violations);
        }

        cs.setDayOfWeek(request.dayOfWeek());
        cs.setPeriod(targetPeriod);
        return toCellResponse(classScheduleRepository.save(cs));
    }

    private String scopeKeyForSectionId(Long cohortSectionId) {
        return cohortSectionId != null ? cohortSectionId.toString() : WHOLE_COHORT_SCOPE;
    }

    /** THEORY's scope is its own CohortSection (or WHOLE if the cohort has no committed sections);
     *  LAB/CLINICAL's scope is derived from its batch's own CohortSection (or WHOLE if that batch
     *  predates Capacity Planner section-scoping, or the cohort has none). */
    private String scopeKeyForCell(ClassSchedule cs) {
        if (cs.getSessionType() == ClassSessionType.THEORY) {
            return scopeKeyForSectionId(cs.getCohortSection() != null ? cs.getCohortSection().getId() : null);
        }
        Batch b = cs.getBatch();
        return scopeKeyForSectionId(b != null && b.getCohortSection() != null ? b.getCohortSection().getId() : null);
    }

    /** Two scopes conflict if they're literally the same section, or either side is WHOLE (a
     *  whole-cohort audience always overlaps with everything -- unsectioned cohorts, and any row
     *  predating section-scoping, stay exactly as exclusive as they are today). Two *different*,
     *  non-WHOLE sections never conflict -- Capacity Planner already guarantees they're different
     *  rooms with disjoint audiences once committed. */
    private boolean scopesConflict(String a, String b) {
        return a.equals(b) || WHOLE_COHORT_SCOPE.equals(a) || WHOLE_COHORT_SCOPE.equals(b);
    }

    /** THEORY is mandatory for every student in its audience, so it hard-blocks against any other
     *  session (any subject, any type) already placed at the same audience/day/period, and vice
     *  versa — where "audience" is a specific {@link CohortSection} once the cohort's room
     *  allocation is sectioned, or the whole cohort otherwise; see {@link #scopesConflict}.
     *  LAB/CLINICAL-vs-LAB/CLINICAL across different subjects (same audience) is deliberately NOT
     *  blocked here — real roster overlap can't be proven without batch rosters that don't exist
     *  yet; the frontend surfaces that case as an advisory instead of a hard error. */
    private Optional<ConstraintViolation> checkCohortExclusivity(SkeletonCellPlacementRequest request, CourseOffering offering,
                                         Batch batch, CohortSection cohortSection) {
        List<Long> cohortOfferingIds = nonElectiveOfferingIds(offering.getTermInstance().getId(), request.cohortId());
        if (cohortOfferingIds.isEmpty()) {
            return Optional.empty();
        }
        List<ClassSchedule> cohortCellsAtSlot = classScheduleRepository
            .findByTermInstanceIdAndCourseOfferingIdIn(offering.getTermInstance().getId(), cohortOfferingIds)
            .stream()
            .filter(cs -> cs.getDayOfWeek() == request.dayOfWeek()
                && cs.getPeriod() != null && cs.getPeriod().getId().equals(request.periodId()))
            .toList();
        if (cohortCellsAtSlot.isEmpty()) {
            return Optional.empty();
        }

        String placingScope = request.sessionType() == ClassSessionType.THEORY
            ? scopeKeyForSectionId(cohortSection != null ? cohortSection.getId() : null)
            : scopeKeyForSectionId(batch != null && batch.getCohortSection() != null ? batch.getCohortSection().getId() : null);

        if (request.sessionType() == ClassSessionType.THEORY) {
            return cohortCellsAtSlot.stream()
                .filter(cs -> scopesConflict(placingScope, scopeKeyForCell(cs)))
                .findFirst()
                .map(other -> new ConstraintViolation("SKELETON_CELL_COHORT_CLASH",
                    "A Theory session is mandatory for this audience and can't share a slot with another session — "
                        + (other.getSubject() != null ? other.getSubject().getName() : "another subject")
                        + " already has a session placed here"));
        }

        // LAB/CLINICAL vs LAB/CLINICAL from a different subject, same audience: allowed, advisory-only client-side.
        return cohortCellsAtSlot.stream()
            .filter(cs -> cs.getSessionType() == ClassSessionType.THEORY)
            .filter(cs -> scopesConflict(placingScope, scopeKeyForCell(cs)))
            .findFirst()
            .map(theoryCell -> new ConstraintViolation("SKELETON_CELL_COHORT_CLASH",
                (theoryCell.getSubject() != null ? theoryCell.getSubject().getName() : "Another subject")
                    + " has a mandatory Theory session in this slot for this audience — no other session can be placed here"));
    }

    boolean isElectiveOffering(CourseOffering offering) {
        CurriculumSemesterCourse csc = offering.getCurriculumSemesterCourse();
        return csc != null && Boolean.TRUE.equals(csc.getIsElective());
    }

    /** Every subject sharing a {@code CurriculumElectiveGroup} must be placed in the exact same
     *  day/period this term — students pick one, so the options only work if they're all offered
     *  at once. {@code CurriculumElectiveGroup} itself is catalog-level (keyed to curriculumVersion
     *  + termNumber, reused across every calendar term that curriculum term recurs in), so the
     *  group's actual slot is never stored on it — it's derived here, purely from whichever
     *  {@link ClassSchedule} rows sibling offerings in this group already have THIS {@link
     *  TermInstance}. The first placement for a group in a given term defines its slot freely;
     *  every later placement in that group must match it exactly. Ungrouped electives ({@code
     *  isElective=true} but no {@code electiveGroup}) have nothing to enforce. Electives are
     *  otherwise exempt from {@link #checkCohortExclusivity} entirely — matching their existing
     *  exemption from Staffing's committed-room hard-lock, since they have no single owning
     *  cohort audience by design. */
    private Optional<ConstraintViolation> checkElectiveGroupSlot(CourseOffering offering, SkeletonCellPlacementRequest request) {
        CurriculumSemesterCourse csc = offering.getCurriculumSemesterCourse();
        if (csc == null || csc.getElectiveGroup() == null) {
            return Optional.empty();
        }
        Long groupId = csc.getElectiveGroup().getId();
        List<Long> siblingIds = courseOfferingRepository
            .findByTermInstanceIdAndCurriculumSemesterCourse_ElectiveGroupId(offering.getTermInstance().getId(), groupId)
            .stream().map(CourseOffering::getId).toList();
        if (siblingIds.isEmpty()) {
            return Optional.empty();
        }

        List<ClassSchedule> existingGroupCells = classScheduleRepository
            .findByTermInstanceIdAndCourseOfferingIdIn(offering.getTermInstance().getId(), siblingIds);
        ClassSchedule anyExisting = resolveGroupAnchor(existingGroupCells).orElse(null);
        if (anyExisting == null) {
            return Optional.empty();
        }
        boolean sameSlot = anyExisting.getDayOfWeek() == request.dayOfWeek()
            && anyExisting.getPeriod() != null && anyExisting.getPeriod().getId().equals(request.periodId());
        if (sameSlot) {
            return Optional.empty();
        }
        return Optional.of(new ConstraintViolation("SKELETON_ELECTIVE_GROUP_SLOT_MISMATCH",
            "This elective group is already scheduled for " + anyExisting.getDayOfWeek()
                + (anyExisting.getPeriod() != null ? ", " + anyExisting.getPeriod().getName() : "")
                + " — every subject in the group must share the same slot."));
    }

    /** The group's real "first" placement -- deterministically the lowest-id (earliest-created)
     *  cell, never an arbitrary list-order pick. Used both by {@link #checkElectiveGroupSlot}'s
     *  reactive per-cell check and by {@link #placeElectiveGroup}/{@link #getElectiveGroupSchedule}
     *  so all three agree on what "this group's slot" means. */
    private Optional<ClassSchedule> resolveGroupAnchor(List<ClassSchedule> groupCells) {
        return groupCells.stream().min(Comparator.comparing(ClassSchedule::getId));
    }

    /** Atomically places every member of an elective group's session at one shared day/period --
     *  the "visually bundle and place at once" action Skeleton Builder's per-cell {@link
     *  #placeCell} has no equivalent for (each elective subject there is placed one at a time,
     *  only reactively validated against {@link #checkElectiveGroupSlot} once a sibling already
     *  exists). Skips {@link #checkElectiveGroupSlot}/{@link #checkCohortExclusivity} entirely --
     *  this method IS the group-slot enforcement, atomically, for every member in one pass -- but
     *  still runs the same {@link #checkAlreadyPlaced}/{@link #checkBlocked} checks {@link
     *  #placeCell} does per member. Collects every violation across every member before throwing
     *  (all-or-nothing: nothing is saved if any member fails), matching {@code
     *  SpecialClassRequestService.requestDayRepeat}'s established batch-placement contract. */
    @Transactional
    public List<SkeletonCellResponse> placeElectiveGroup(ElectiveGroupPlacementRequest request) {
        List<CourseOffering> siblingOfferings = courseOfferingRepository
            .findByTermInstanceIdAndCurriculumSemesterCourse_ElectiveGroupId(request.termInstanceId(), request.electiveGroupId());
        Map<Long, CourseOffering> siblingsById = siblingOfferings.stream()
            .collect(java.util.stream.Collectors.toMap(CourseOffering::getId, o -> o));

        Period period = periodRepository.findById(request.periodId())
            .orElseThrow(() -> new ResourceNotFoundException("Period not found with id: " + request.periodId()));

        List<Long> siblingIds = siblingOfferings.stream().map(CourseOffering::getId).toList();
        List<ClassSchedule> existingGroupCells = siblingIds.isEmpty() ? List.of()
            : classScheduleRepository.findByTermInstanceIdAndCourseOfferingIdIn(request.termInstanceId(), siblingIds);
        ClassSchedule anchor = resolveGroupAnchor(existingGroupCells).orElse(null);
        if (anchor != null) {
            boolean matchesAnchor = anchor.getDayOfWeek() == request.dayOfWeek()
                && anchor.getPeriod() != null && anchor.getPeriod().getId().equals(request.periodId());
            if (!matchesAnchor) {
                throw new TimetableConstraintViolationException(List.of(new ConstraintViolation(
                    "SKELETON_ELECTIVE_GROUP_SLOT_MISMATCH",
                    "This elective group is already scheduled for " + anchor.getDayOfWeek()
                        + (anchor.getPeriod() != null ? ", " + anchor.getPeriod().getName() : "")
                        + " — a bulk placement can't move an already-scheduled group.")));
            }
        }

        List<CohortSection> activeSections = resolveActiveSections(request.cohortId(), request.termInstanceId());
        List<ConstraintViolation> violations = new ArrayList<>();
        List<ClassSchedule> toSave = new ArrayList<>();

        for (ElectiveGroupMemberPlacement member : request.members()) {
            CourseOffering offering = siblingsById.get(member.courseOfferingId());
            if (offering == null) {
                violations.add(new ConstraintViolation("SKELETON_ELECTIVE_GROUP_MEMBER_INVALID",
                    "Course offering " + member.courseOfferingId() + " is not a member of this elective group."));
                continue;
            }

            SkeletonCellPlacementRequest asPlacementRequest = new SkeletonCellPlacementRequest(
                member.courseOfferingId(), member.sessionType(), request.dayOfWeek(), request.periodId(),
                member.batchId(), request.cohortId(), member.cohortSectionId());

            Batch batch = null;
            if (member.sessionType() == ClassSessionType.LAB || member.sessionType() == ClassSessionType.CLINICAL) {
                if (member.batchId() == null) {
                    violations.add(new ConstraintViolation("SKELETON_CELL_BATCH_REQUIRED",
                        offering.getSubject().getName() + ": a batch is required for a " + member.sessionType() + " session"));
                    continue;
                }
                batch = batchRepository.findById(member.batchId())
                    .orElseThrow(() -> new ResourceNotFoundException("Batch not found with id: " + member.batchId()));
            }

            CohortSection cohortSection = null;
            if (member.sessionType() == ClassSessionType.THEORY && !activeSections.isEmpty()) {
                if (member.cohortSectionId() == null) {
                    violations.add(new ConstraintViolation("SKELETON_CELL_SECTION_REQUIRED",
                        offering.getSubject().getName() + ": a cohort section is required for this Theory session"));
                    continue;
                }
                cohortSection = activeSections.stream()
                    .filter(s -> s.getId().equals(member.cohortSectionId()))
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException(
                        "Cohort section not found with id: " + member.cohortSectionId() + " for this cohort/term"));
            }

            checkAlreadyPlaced(offering, asPlacementRequest).ifPresent(violations::add);
            checkBlocked(request.dayOfWeek(), period.getId(), offering.getTermInstance()).ifPresent(violations::add);

            ClassSchedule cs = new ClassSchedule();
            cs.setSessionType(member.sessionType());
            cs.setStatus(ClassScheduleStatus.DRAFT);
            cs.setSubject(offering.getSubject());
            cs.setDayOfWeek(request.dayOfWeek());
            cs.setTermInstance(offering.getTermInstance());
            cs.setCourseOffering(offering);
            cs.setPeriod(period);
            cs.setBatch(batch);
            cs.setBatchName(batch != null ? batch.getName() : null);
            cs.setCohortSection(cohortSection);
            cs.setIsActive(true);
            toSave.add(cs);
        }

        if (!violations.isEmpty()) {
            throw new TimetableConstraintViolationException(violations);
        }

        return classScheduleRepository.saveAll(toSave).stream().map(this::toCellResponse).toList();
    }

    /** Read-only lookup for the Elective Assignment screen (and anywhere else that needs to know
     *  "has this term's elective group been scheduled yet") -- same anchor resolution {@link
     *  #placeElectiveGroup}/{@link #checkElectiveGroupSlot} use, so this can never disagree with
     *  what placement actually enforces. */
    public ElectiveGroupScheduleResponse getElectiveGroupSchedule(Long electiveGroupId, Long termInstanceId) {
        List<Long> siblingIds = courseOfferingRepository
            .findByTermInstanceIdAndCurriculumSemesterCourse_ElectiveGroupId(termInstanceId, electiveGroupId)
            .stream().map(CourseOffering::getId).toList();
        List<ClassSchedule> existingGroupCells = siblingIds.isEmpty() ? List.of()
            : classScheduleRepository.findByTermInstanceIdAndCourseOfferingIdIn(termInstanceId, siblingIds);
        ClassSchedule anchor = resolveGroupAnchor(existingGroupCells).orElse(null);
        if (anchor == null || anchor.getPeriod() == null) {
            return new ElectiveGroupScheduleResponse(false, null, null, null, null);
        }
        Period period = anchor.getPeriod();
        return new ElectiveGroupScheduleResponse(true, anchor.getDayOfWeek(), period.getName(),
            period.getStartTime(), period.getEndTime());
    }

    /** Read-only candidate slots for a subject/session-type/batch (or, for THEORY, cohort section)
     *  still short of its weekly budget — mirrors the day/period scan shape of the retired
     *  {@code TimetableGenerationService.placeTheory}/{@code placeLab}, capping at one candidate
     *  per day (same clustering guard). Sources "already placed" from this offering's own rows
     *  only — it has no cohortId param, so it can't check sibling subjects' cells; {@link
     *  #placeCell}'s {@link #checkCohortExclusivity} remains the authoritative gate, this is
     *  purely a convenience nudge that may occasionally suggest a slot placeCell then rejects. */
    public List<SkeletonPlacementCandidateResponse> suggestCandidates(Long courseOfferingId, ClassSessionType sessionType,
                                                                        Long batchId, Long cohortSectionId) {
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
                && Objects.equals(cs.getBatch() != null ? cs.getBatch().getId() : null, batchId)
                && Objects.equals(cs.getCohortSection() != null ? cs.getCohortSection().getId() : null, cohortSectionId))
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
