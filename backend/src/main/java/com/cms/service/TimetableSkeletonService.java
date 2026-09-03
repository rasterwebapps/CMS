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
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.CohortSectionResponse;
import com.cms.dto.ConstraintViolation;
import com.cms.dto.CourseOfferingDto;
import com.cms.dto.ElectiveGroupMemberPlacement;
import com.cms.dto.ElectiveGroupPlacementRequest;
import com.cms.dto.ElectiveGroupScheduleResponse;
import com.cms.dto.SkeletonBuilderResponse;
import com.cms.dto.SkeletonCellMoveRequest;
import com.cms.dto.SkeletonCellSwapRequest;
import com.cms.dto.SkeletonCellPlacementRequest;
import com.cms.dto.SkeletonCellResponse;
import com.cms.dto.SkeletonClinicalShiftHours;
import com.cms.dto.SkeletonPlacementCandidateResponse;
import com.cms.dto.SkeletonSlotPreviewResponse;
import com.cms.dto.SkeletonSubjectBudget;
import com.cms.dto.SkeletonSubjectResponse;
import com.cms.exception.LifecycleConflictException;
import com.cms.exception.ResourceNotFoundException;
import com.cms.exception.TimetableConstraintViolationException;
import com.cms.model.Batch;
import com.cms.model.Classroom;
import com.cms.model.ClassSchedule;
import com.cms.model.ClinicalShiftGroup;
import com.cms.model.Cohort;
import com.cms.model.CohortSection;
import com.cms.model.CourseOffering;
import com.cms.model.CurriculumSemesterCourse;
import com.cms.model.Period;
import com.cms.model.Subject;
import com.cms.model.TermInstance;
import com.cms.model.enums.ClassScheduleStatus;
import com.cms.model.enums.ClassSessionType;
import com.cms.model.enums.CohortRoomAllocationStatus;
import com.cms.model.enums.DayOfWeek;
import com.cms.repository.BatchRepository;
import com.cms.repository.ClassScheduleRepository;
import com.cms.repository.ClinicalShiftGroupRepository;
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
    private final ClinicalShiftGroupRepository clinicalShiftGroupRepository;

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
                                     TimetableStaffingService timetableStaffingService,
                                     ClinicalShiftGroupRepository clinicalShiftGroupRepository) {
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
        this.clinicalShiftGroupRepository = clinicalShiftGroupRepository;
    }

    public SkeletonBuilderResponse getCohortSkeleton(Long termInstanceId, Long cohortId) {
        Cohort cohort = cohortRepository.findById(cohortId)
            .orElseThrow(() -> new ResourceNotFoundException("Cohort not found with id: " + cohortId));
        TermInstance termInstance = termInstanceRepository.findById(termInstanceId)
            .orElseThrow(() -> new ResourceNotFoundException("Term instance not found with id: " + termInstanceId));
        String termInstanceLabel = termInstance.getAcademicYear().getName() + " " + termInstance.getTermType();

        List<CohortSection> activeSections = resolveActiveSections(cohortId, termInstanceId);
        List<CohortSectionResponse> sectionResponses = activeSections.stream().map(this::toSectionResponse).toList();

        // LIBRARY cells have no CourseOffering (see TimetableGlobalAutoScheduleService#fillLibraryGaps),
        // so the offering-based query below never finds them -- resolved separately by this cohort's
        // own active CohortSections, same source cohortCellsAtSlot/isSlotFreeForCohort already use.
        List<Long> sectionIds = activeSections.stream().map(CohortSection::getId).toList();
        List<ClassSchedule> libraryCells = sectionIds.isEmpty() ? List.of()
            : classScheduleRepository.findByCohortSectionIdInAndIsActiveTrue(sectionIds).stream()
                .filter(cs -> cs.getSessionType() == ClassSessionType.LIBRARY)
                .toList();

        boolean termTimetablePublished = classScheduleRepository
            .existsByTermInstanceIdAndStatus(termInstanceId, ClassScheduleStatus.PUBLISHED);

        List<Long> offeringIds = new ArrayList<>(nonElectiveOfferingIds(termInstanceId, cohortId));
        offeringIds.addAll(electiveOfferingIds(termInstanceId, cohortId));
        if (offeringIds.isEmpty()) {
            List<SkeletonCellResponse> libraryOnlyCells = libraryCells.stream().map(this::toCellResponse).toList();
            return new SkeletonBuilderResponse(cohortId, cohort.getDisplayName(), termInstanceLabel, List.of(), libraryOnlyCells, List.of(), sectionResponses,
                CurriculumHoursCalculator.weeksInTerm(termInstance), WorkingSaturdayCalculator.workingSaturdayCount(termInstance), List.of(),
                termTimetablePublished);
        }

        Map<Long, CourseOffering> offeringById = new LinkedHashMap<>();
        for (Long id : offeringIds) {
            courseOfferingRepository.findById(id).ifPresent(o -> offeringById.put(id, o));
        }

        List<Period> periods = periodRepository.findByIsActiveTrueOrderByPeriodOrderAsc();
        int weeksInTerm = CurriculumHoursCalculator.weeksInTerm(termInstance);
        double periodDurationMinutes = CurriculumHoursCalculator.averageDurationMinutes(
            periods.stream().map(Period::getDurationMinutes).toList());

        // isActive=false filters out cells orphaned by a since-reverted CohortRoomAllocation --
        // riding on a batch/section that no longer exists in the currently-active plan; without
        // this they'd render as ghost cells in the grid and double up against freshly-placed ones.
        List<ClassSchedule> allCells = Stream.concat(
                classScheduleRepository.findByTermInstanceIdAndCourseOfferingIdIn(termInstanceId, offeringIds).stream(),
                libraryCells.stream())
            .filter(cs -> Boolean.TRUE.equals(cs.getIsActive()))
            .distinct()
            .toList();
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
                budgets.addAll(batchScopedBudgets(ClassSessionType.LAB, csc.getLabHours(), offeringBatches, existingForOffering, weeksInTerm, periodDurationMinutes, offering.getSubject()));
                Integer creditedClinicalHours = csc.getClinicalHours() != null
                    ? creditClinicalShiftHours(ClassSessionType.CLINICAL, csc.getClinicalHours(), offering, weeksInTerm)
                    : null;
                budgets.addAll(batchScopedBudgets(ClassSessionType.CLINICAL, csc.getClinicalHours(), creditedClinicalHours,
                    offeringBatches, existingForOffering, weeksInTerm, periodDurationMinutes, offering.getSubject()));
            }

            var electiveGroup = csc != null ? csc.getElectiveGroup() : null;
            subjects.add(new SkeletonSubjectResponse(offeringId, offering.getSubject().getName(), offering.getSubject().getCode(), budgets,
                electiveGroup != null ? electiveGroup.getId() : null,
                electiveGroup != null ? electiveGroup.getGroupName() : null));
        }

        List<SkeletonCellResponse> cells = allCells.stream().map(this::toCellResponse).toList();

        // Clinical Shift sessions (OC-177) never produce a ClassSchedule row -- they bypass the
        // period grid entirely via SessionOccurrence(CLINICAL_SHIFT) -- so they'd never appear in
        // `cells` above and the frontend's Clinical assigned-hours card would silently under-count
        // any cohort using them. Reported as a separate already-converted-to-hours list instead of
        // synthetic grid cells since a Clinical Shift has no periodId/day-column position to render.
        List<SkeletonClinicalShiftHours> clinicalShiftHours = clinicalShiftGroupRepository
            .findByTermInstanceIdAndIsActiveTrue(termInstanceId).stream()
            .filter(g -> g.getCourseOffering() != null && offeringIds.contains(g.getCourseOffering().getId()))
            .map(g -> toClinicalShiftHours(g, offeringById.get(g.getCourseOffering().getId()), weeksInTerm))
            .filter(h -> h.assignedHours() > 0)
            .toList();

        return new SkeletonBuilderResponse(cohortId, cohort.getDisplayName(), termInstanceLabel, subjects, cells, batches, sectionResponses,
            weeksInTerm, WorkingSaturdayCalculator.workingSaturdayCount(termInstance), clinicalShiftHours, termTimetablePublished);
    }

    /** One active {@link ClinicalShiftGroup} occurs once/week on its own {@code dayOfWeek}, so its
     *  real term-wide Clinical hours are simply its offering's configured shift duration converted
     *  to hours and multiplied by {@code weeksInTerm} -- 0 (filtered out by the caller) if the
     *  offering has no duration configured yet, matching how an unconfigured group can't actually
     *  generate occurrences either (see {@code ClinicalShiftOccurrenceService#generateForDate}). */
    /** The CLINICAL hours the grid still genuinely needs to deliver, after crediting whatever this
     *  offering's active {@link ClinicalShiftGroup}(s) already deliver off-grid (see {@link
     *  #toClinicalShiftHours}) — real hospital shift hours, never represented as grid cells at all.
     *  Without this, {@code sessionsPerWeek} demands the FULL raw curriculum hours on top of what a
     *  shift group is already covering, permanently over-demanding weekly grid periods for hours
     *  that are, in reality, already being delivered — the exact shape behind a subject's Clinical
     *  shortfall never clearing no matter how the grid is rearranged. A no-op (returns {@code
     *  rawHours} unchanged) for THEORY/LAB, which have no shift mechanism, and whenever the offering
     *  has no active Clinical Shift Group. Never negative. */
    private int creditClinicalShiftHours(ClassSessionType sessionType, int rawHours, CourseOffering offering, int weeksInTerm) {
        if (sessionType != ClassSessionType.CLINICAL || rawHours <= 0) {
            return rawHours;
        }
        double shiftHours = clinicalShiftGroupRepository.findByCourseOfferingId(offering.getId()).stream()
            .filter(g -> Boolean.TRUE.equals(g.getIsActive()))
            .mapToDouble(g -> toClinicalShiftHours(g, offering, weeksInTerm).assignedHours())
            .sum();
        return (int) Math.max(0, Math.ceil(rawHours - shiftHours));
    }

    private SkeletonClinicalShiftHours toClinicalShiftHours(ClinicalShiftGroup group, CourseOffering offering, int weeksInTerm) {
        Integer durationMinutes = offering != null ? offering.getClinicalShiftDurationMinutes() : null;
        double hours = durationMinutes != null ? (durationMinutes / 60.0) * weeksInTerm : 0.0;
        CohortSection section = group.getCohortSection();
        return new SkeletonClinicalShiftHours(group.getCourseOffering().getId(), section != null ? section.getId() : null, hours);
    }

    /** Active sections of the cohort's committed Cohort Room Allocation for this term, or empty if
     *  none has been committed — mirrors {@code TimetableStaffingService.resolveCommittedTheoryClassroom}'s
     *  exact repository chain. Empty means "whole cohort" (today's original behavior); one or more
     *  active sections means THEORY placement becomes per-section. */
    List<CohortSection> resolveActiveSections(Long cohortId, Long termInstanceId) {
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
        int required = CurriculumHoursCalculator.sessionsPerWeek(theoryHours, weeksInTerm, periodDurationMinutes, 1);

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
     *  hours are needed but there's nothing to place them against yet if no batch exists. Each
     *  row carries the batch's own {@link Batch#getCohortSection()} (set at commit time by {@link
     *  CohortRoomAllocationService#createVentureBatch}) so faculty resolution
     *  ({@code TimetableGlobalAutoScheduleService#resolveBudgetFacultyId}) can look up that
     *  section's own {@code CourseOfferingSectionFaculty} row exactly like a split Theory row does
     *  — a section-split cohort has no whole-cohort faculty row to fall back to, so leaving this
     *  null here made every Lab/Clinical row permanently unstaffable for a split cohort.
     *
     *  <p>{@code batches} is the offering's WHOLE active-batch pool (Lab and Clinical batches
     *  mixed together, e.g. Cohort Room Allocation committing both a "Lab - Section 1" and a
     *  "Clinical - Section 1" batch for the same offering) — a batch definitively wrong for {@code
     *  type} (it has the *other* type's venue committed) is skipped, or every Lab batch would also
     *  get a spurious Clinical budget row demanding hours against a batch with no clinical venue
     *  at all (and vice versa), inflating required-hours totals by however many unrelated batches
     *  exist and leaving automation trying to place sessions nothing can ever satisfy. A batch with
     *  neither venue committed yet (legacy/manual-create path, see {@link Batch#getLab()}'s own
     *  javadoc) is kept for both — no signal yet to say which it's meant to be. */
    private List<SkeletonSubjectBudget> batchScopedBudgets(ClassSessionType type, Integer hoursObj, List<Batch> batches,
                                                            List<ClassSchedule> existing, int weeksInTerm,
                                                            double periodDurationMinutes, Subject subject) {
        return batchScopedBudgets(type, hoursObj, hoursObj, batches, existing, weeksInTerm, periodDurationMinutes, subject);
    }

    /** Identity of the SESSION a row belongs to, for counting placed sessions against a
     *  session-denominated budget ({@code requiredSessionsPerWeek}). Every row of one multi-period
     *  block shares a {@code sessionGroupId} (see {@link #placeCell}'s OC-127 periodSpan handling),
     *  so they collapse to one entry; a single-period session has no group id and stands alone
     *  under its own row id. Never mix the two id spaces — hence the distinct prefixes. */
    private static String sessionKey(ClassSchedule cs) {
        if (cs.getSessionGroupId() != null) {
            return "g:" + cs.getSessionGroupId();
        }
        // Falls back to object identity for a row with no id yet (never persisted/flushed): keying
        // those on a null id would silently collapse every one of them into a single "session" and
        // under-count the budget, letting over-placement straight through the cap below.
        return cs.getId() != null ? "c:" + cs.getId() : "i:" + System.identityHashCode(cs);
    }

    /** {@code effectiveHoursForRequired} drives ONLY the weekly-sessions/grid-placement target
     *  ({@code required} below) — {@code displayHoursObj} (the raw curriculum figure) still drives
     *  the {@code totalHours} shown on the budget row, so a CLINICAL subject's real 480-hour
     *  requirement never reads as smaller than it actually is just because part of it is credited
     *  off-grid (see {@link #creditClinicalShiftHours}). Both are the same value for LAB, which has
     *  no such off-grid delivery mechanism. */
    private List<SkeletonSubjectBudget> batchScopedBudgets(ClassSessionType type, Integer displayHoursObj,
                                                            Integer effectiveHoursForRequired, List<Batch> batches,
                                                            List<ClassSchedule> existing, int weeksInTerm,
                                                            double periodDurationMinutes, Subject subject) {
        int hours = displayHoursObj != null ? displayHoursObj : 0;
        if (hours <= 0) {
            return List.of();
        }
        int effectiveHours = effectiveHoursForRequired != null ? effectiveHoursForRequired : 0;
        int blockSize = CurriculumHoursCalculator.resolveBlockSize(subject, type);
        int required = CurriculumHoursCalculator.sessionsPerWeek(effectiveHours, weeksInTerm, periodDurationMinutes, blockSize);

        // Counted in SESSIONS, not rows: a multi-period block is several ClassSchedule rows sharing
        // one sessionGroupId, and requiredSessionsPerWeek above is a session count -- comparing raw
        // row counts against it made a single placed 4-period Clinical block read as "4 of 6
        // sessions done" when it was 1, so the shortfall (and therefore the whole placement pass)
        // silently under-delivered every LAB/CLINICAL row by its own block size.
        Map<Long, Long> placedByBatchId = existing.stream()
            .filter(cs -> cs.getSessionType() == type && cs.getBatch() != null)
            .collect(java.util.stream.Collectors.groupingBy(cs -> cs.getBatch().getId(), LinkedHashMap::new,
                java.util.stream.Collectors.collectingAndThen(
                    java.util.stream.Collectors.mapping(TimetableSkeletonService::sessionKey, java.util.stream.Collectors.toSet()),
                    set -> (long) set.size())));

        List<Batch> matchingBatches = batches.stream()
            .filter(b -> !(type == ClassSessionType.LAB && b.getClinicalVenue() != null))
            .filter(b -> !(type == ClassSessionType.CLINICAL && b.getLab() != null))
            .toList();

        if (matchingBatches.isEmpty()) {
            return List.of(new SkeletonSubjectBudget(type, null, null, null, null, hours, weeksInTerm, required, 0));
        }
        List<SkeletonSubjectBudget> rows = new ArrayList<>();
        for (Batch batch : matchingBatches) {
            long placed = placedByBatchId.getOrDefault(batch.getId(), 0L);
            CohortSection section = batch.getCohortSection();
            rows.add(new SkeletonSubjectBudget(type, batch.getId(), batch.getName(),
                section != null ? section.getId() : null, section != null ? section.getSectionLabel() : null,
                hours, weeksInTerm, required, (int) placed));
        }
        return rows;
    }

    /** Hard-blocks a placement that would push a subject's placed-sessions-per-week past its
     *  curriculum-derived {@code requiredSessionsPerWeek} budget (the same number shown on the
     *  Skeleton Builder summary cards) -- applies uniformly to manual drag/drop and both
     *  auto-schedulers, since every placement path funnels through {@link #placeCell}. Automated
     *  placement was already self-limiting (the {@code ShortfallRow} queue stops enqueueing a row
     *  once its shortfall hits zero), but that stop condition was never enforced as an invariant on
     *  placement itself, so manual placement -- or automated re-placement against stale budgets
     *  after a curriculum-hours edit -- could freely push Assigned past Required with no warning.
     *  If more sessions are genuinely needed, the fix is to raise the subject's curriculum hours
     *  (which raises {@code requiredSessionsPerWeek} here too), not to bypass this check.
     *
     *  <p>Everything here is denominated in SESSIONS, never periods: one call to {@link #placeCell}
     *  creates exactly one session (however many periods it spans), so it costs exactly 1 against
     *  the budget. This used to charge {@code spanPeriods.size()} against a session-denominated
     *  budget while also counting already-placed ROWS, which double-punished multi-period blocks
     *  from both directions -- a 4-period Clinical block spent 4 of 6 and then read back as 4
     *  already placed, so the second legitimate block was rejected outright. Silently no-ops (no
     *  violation) when the offering has no resolved curriculum mapping or the session type's hours
     *  are 0/unset, matching how {@link #batchScopedBudgets} treats the same case. */
    private Optional<ConstraintViolation> checkBudgetNotExceeded(CourseOffering offering, ClassSessionType sessionType,
                                                                   Batch batch, CohortSection cohortSection) {
        CurriculumSemesterCourse csc = offering.getCurriculumSemesterCourse();
        if (csc == null) {
            return Optional.empty();
        }
        Integer hoursObj = switch (sessionType) {
            case THEORY -> csc.getTheoryHours();
            case LAB -> csc.getLabHours();
            case CLINICAL -> csc.getClinicalHours();
            case LIBRARY -> throw new IllegalStateException(
                "Library sessions have no CourseOffering/curriculum-hours budget to check against");
        };
        int hours = hoursObj != null ? hoursObj : 0;
        if (hours <= 0) {
            return Optional.empty();
        }

        TermInstance term = offering.getTermInstance();
        int weeksInTerm = CurriculumHoursCalculator.weeksInTerm(term);
        List<Period> activePeriods = periodRepository.findByIsActiveTrueOrderByPeriodOrderAsc();
        double periodDurationMinutes = CurriculumHoursCalculator.averageDurationMinutes(
            activePeriods.stream().map(Period::getDurationMinutes).toList());
        int blockSize = CurriculumHoursCalculator.resolveBlockSize(offering.getSubject(), sessionType);
        int effectiveHours = creditClinicalShiftHours(sessionType, hours, offering, weeksInTerm);
        int required = CurriculumHoursCalculator.sessionsPerWeek(effectiveHours, weeksInTerm, periodDurationMinutes, blockSize);

        Long scopeBatchId = batch != null ? batch.getId() : null;
        Long scopeSectionId = cohortSection != null ? cohortSection.getId() : null;
        List<ClassSchedule> candidates = AutoScheduleRunCache.current()
            .map(cache -> cache.byCourseOfferingId(offering.getId()))
            .orElseGet(() -> classScheduleRepository.findByCourseOfferingId(offering.getId()));
        // Sessions, not rows -- see sessionKey. `required` is a session count, so counting rows here
        // made a 4-period Clinical block consume 4 of its 6-session budget instead of 1, and would
        // now reject the second legitimate block outright.
        long placed = candidates.stream()
            .filter(cs -> Boolean.TRUE.equals(cs.getIsActive()))
            .filter(cs -> cs.getSessionType() == sessionType)
            .filter(cs -> sessionType == ClassSessionType.THEORY
                ? Objects.equals(cs.getCohortSection() != null ? cs.getCohortSection().getId() : null, scopeSectionId)
                : Objects.equals(cs.getBatch() != null ? cs.getBatch().getId() : null, scopeBatchId))
            .map(TimetableSkeletonService::sessionKey)
            .distinct()
            .count();

        if (placed + 1 > required) {
            return Optional.of(new ConstraintViolation("SKELETON_CELL_BUDGET_EXCEEDED",
                offering.getSubject().getName() + "'s " + sessionType + " budget is already met (" + placed + "/" + required
                    + " sessions/week) — increase the subject's curriculum hours first if more sessions are genuinely needed."));
        }
        return Optional.empty();
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
    private Optional<ConstraintViolation> checkBlocked(DayOfWeek dayOfWeek, Period period, TermInstance termInstance) {
        return blockedPeriodChecker.blockReason(dayOfWeek, period.getStartTime(), period.getEndTime(), termInstance)
            .map(reason -> new ConstraintViolation("SKELETON_CELL_PERIOD_BLOCKED", "This day and period is blocked: " + reason));
    }

    /** Used by {@link #suggestCandidates} to silently skip a blocked slot rather than surfacing a
     *  distinct violation — there's no per-candidate UI affordance to explain "why" a slot didn't
     *  appear. Returns the block reason, or null if the slot is free. */
    private String blockReason(DayOfWeek dayOfWeek, Period period, TermInstance termInstance) {
        return blockedPeriodChecker.blockReason(dayOfWeek, period.getStartTime(), period.getEndTime(), termInstance)
            .orElse(null);
    }

    private SkeletonCellResponse toCellResponse(ClassSchedule cs) {
        Period period = cs.getPeriod();
        Batch batch = cs.getBatch();
        // THEORY cells carry their own cohortSection directly; a LAB/CLINICAL cell's is always
        // null there (placement never sets it for those types — see SkeletonCellPlacementRequest),
        // so it falls back to the batch's own real cohortSection FK instead, the same fallback
        // scopeKeyForSectionId's callers already rely on elsewhere in this class.
        CohortSection cohortSection = cs.getCohortSection() != null ? cs.getCohortSection()
            : (batch != null ? batch.getCohortSection() : null);
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
            electiveGroup != null ? electiveGroup.getGroupName() : null,
            cs.getSessionGroupId()
        );
    }

    /** {@code REQUIRES_NEW}: both auto-schedulers ({@code TimetableGlobalAutoScheduleService},
     *  {@code TimetableSkeletonAutoPlaceService}) call this in a loop, catching {@link
     *  TimetableConstraintViolationException} as routine "try the next candidate slot" control
     *  flow — a candidate failing is the expected common case, not exceptional. With the default
     *  {@code REQUIRED} propagation, any exception thrown by a nested {@code @Transactional} call
     *  marks the *whole* enclosing transaction rollback-only the instant it propagates out of this
     *  method's proxy, regardless of whether the caller catches it — so the very first unplaceable
     *  candidate in an auto-schedule run (near-certain on real data) silently doomed the entire run
     *  to roll back everything, while the algorithm kept burning through the rest of the search
     *  space unaware it was already discarded. {@code REQUIRES_NEW} gives this call its own
     *  independent physical transaction, so a routine failure here can never poison a caller's
     *  broader unit of work — matching every actual caller's real intent (none rely on this
     *  method's failure rolling back anything beyond itself). */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SkeletonCellResponse placeCell(SkeletonCellPlacementRequest request) {
        return placeCell(request, true);
    }

    /** {@code enforceBudgetCap=false} skips {@link #checkBudgetNotExceeded} only — every other
     *  check (already-placed, audience exclusivity, blocked period) still applies in full. This
     *  exists for exactly one caller: {@code TimetableGlobalAutoScheduleService#fillSelfStudyGaps},
     *  which deliberately places Self-Study/Co-curricular sessions beyond that offering's own
     *  curriculum-derived weekly quota to soak up periods nothing else needs — the whole point of
     *  that pass is to exceed the normal budget, so the cap would defeat it outright. Never call
     *  this with {@code false} from anywhere else; the cap exists to keep every other subject
     *  honest against its real curriculum hours, and weakening it generally would silently let a
     *  future caller over-schedule real curriculum content instead of filler time. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    SkeletonCellResponse placeCell(SkeletonCellPlacementRequest request, boolean enforceBudgetCap) {
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

        List<Period> spanPeriods = resolveSpanPeriods(request.sessionType(), period, request.spanPeriodIds());

        List<ConstraintViolation> violations = new ArrayList<>();
        for (Period spanPeriod : spanPeriods) {
            SkeletonCellPlacementRequest perPeriodRequest = spanPeriod.getId().equals(period.getId())
                ? request
                : requestForPeriod(request, spanPeriod.getId());

            checkAlreadyPlaced(offering, perPeriodRequest).ifPresent(violations::add);

            if (isElectiveOffering(offering)) {
                checkElectiveGroupSlot(offering, perPeriodRequest).ifPresent(violations::add);
            } else {
                checkCohortExclusivity(perPeriodRequest, offering, batch, cohortSection).ifPresent(violations::add);
            }

            checkBlocked(request.dayOfWeek(), spanPeriod, offering.getTermInstance()).ifPresent(violations::add);
        }

        checkBudgetNotExceeded(offering, request.sessionType(), batch, cohortSection)
            .ifPresent(violations::add);

        if (!violations.isEmpty()) {
            throw new TimetableConstraintViolationException(violations);
        }

        // OC-127 periodSpan: every row in the span shares one groupId so staffing/removal can treat
        // them as one atomic unit -- null (not generated) for the ordinary single-period case, so
        // existing single-period rows/queries see no behavior change at all.
        java.util.UUID sessionGroupId = spanPeriods.size() > 1 ? java.util.UUID.randomUUID() : null;
        ClassSchedule primary = null;
        for (Period spanPeriod : spanPeriods) {
            ClassSchedule cs = new ClassSchedule();
            cs.setSessionType(request.sessionType());
            cs.setStatus(ClassScheduleStatus.DRAFT);
            cs.setSubject(offering.getSubject());
            cs.setDayOfWeek(request.dayOfWeek());
            cs.setTermInstance(offering.getTermInstance());
            cs.setCourseOffering(offering);
            cs.setPeriod(spanPeriod);
            cs.setBatch(batch);
            cs.setBatchName(batch != null ? batch.getName() : null);
            cs.setCohortSection(cohortSection);
            cs.setIsActive(true);
            cs.setSessionGroupId(sessionGroupId);
            ClassSchedule saved = classScheduleRepository.save(cs);
            AutoScheduleRunCache.current().ifPresent(cache -> cache.recordPlacement(saved));
            if (primary == null) {
                primary = saved;
            }
        }

        return toCellResponse(primary);
    }

    /** OC-127 periodSpan: resolves {@code primary} + every {@code spanPeriodIds} period into one
     *  periodOrder-sorted list, hard-requiring they form an unbroken consecutive run starting at
     *  {@code primary} -- a gap (e.g. periods 2 and 4 without 3) would silently place a session
     *  across a period nobody selected, so it's rejected rather than guessed. Empty/null
     *  {@code spanPeriodIds} returns just {@code primary} (the ordinary single-period case).
     *
     *  <p>Adjacency is checked against this term's real, currently-active teaching periods only --
     *  never against the raw {@code periodOrder} integer. {@code periodOrder} still carries gaps
     *  left by long-retired period rows (e.g. the old standalone LabSlot master's rows, still
     *  sitting in the table with {@code isActive=false} since V331 merged them into {@link Period})
     *  that have no bearing on anything real. Checking raw {@code periodOrder+1} instead of
     *  position-in-the-active-list would wrongly reject (and used to reject) a perfectly valid span
     *  that only "skips" one of those dead rows -- capping every block size at whatever the
     *  accidental gap pattern of retired rows happened to allow, regardless of how many real
     *  consecutive periods the day actually has.
     *
     *  <p>List-position adjacency alone isn't sufficient, though: two periods can be next to each
     *  other in the active list yet still have a real clock-time gap between them (a recess/lunch
     *  break that isn't itself modeled as a {@link Period} row). Placing a block across that gap
     *  would silently split the session in half around the break, so each pair of adjacent periods
     *  in the span must also have back-to-back clock times ({@code endTime == startTime}) — UNLESS
     *  {@code sessionType} is CLINICAL and the gap is a recess rather than the day's lunch break,
     *  per {@link PeriodGapPolicy#gapCrossableFor} (a half-day clinical posting runs straight
     *  through a short recess; it still never crosses lunch). */
    private List<Period> resolveSpanPeriods(ClassSessionType sessionType, Period primary, List<Long> spanPeriodIds) {
        if (spanPeriodIds == null || spanPeriodIds.isEmpty()) {
            return List.of(primary);
        }
        List<Period> all = new ArrayList<>();
        all.add(primary);
        for (Long id : spanPeriodIds) {
            all.add(periodRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Period not found with id: " + id)));
        }
        all.sort(Comparator.comparing(Period::getPeriodOrder));

        List<Period> activeOrderedPeriods = periodRepository.findByIsActiveTrueOrderByPeriodOrderAsc();
        List<Long> activeOrderedIds = activeOrderedPeriods.stream().map(Period::getId).toList();
        int previousPosition = -1;
        Period previousPeriod = null;
        for (Period p : all) {
            int position = activeOrderedIds.indexOf(p.getId());
            if (position < 0) {
                throw new IllegalArgumentException("Spanned periods must be currently active");
            }
            if (previousPosition >= 0 && position != previousPosition + 1) {
                throw new IllegalArgumentException("Spanned periods must be immediately consecutive");
            }
            if (previousPeriod != null && !previousPeriod.getEndTime().equals(p.getStartTime())
                && !PeriodGapPolicy.gapCrossableFor(sessionType, previousPeriod, p, activeOrderedPeriods)) {
                throw new IllegalArgumentException(
                    "Spanned periods must be back-to-back with no break in between");
            }
            previousPosition = position;
            previousPeriod = p;
        }
        return all;
    }

    private SkeletonCellPlacementRequest requestForPeriod(SkeletonCellPlacementRequest request, Long periodId) {
        return new SkeletonCellPlacementRequest(request.courseOfferingId(), request.sessionType(), request.dayOfWeek(),
            periodId, request.batchId(), request.cohortId(), request.cohortSectionId(), null);
    }

    /** Non-throwing: returns a violation if this course offering already has another session of
     *  the exact same type/day/period/batch/section combination — checked by both {@link
     *  #placeCell} (against a not-yet-created row) and {@link #moveCell} (against the target slot;
     *  the moving cell itself always sits at its *old* slot when this runs, so it never spuriously
     *  matches itself here). Section equality is only required for THEORY: {@link #placeCell} only
     *  ever persists a {@code ClassSchedule.cohortSection} for THEORY rows (LAB/CLINICAL's real
     *  scope comes from its batch's own section — see {@link #scopeKeyForCell}), so a placed
     *  LAB/CLINICAL cell's section is always null even once {@code request.cohortSectionId()}
     *  carries the batch's real section (populated by {@link #batchScopedBudgets} for faculty
     *  resolution) — requiring section equality there would always be null-vs-real and silently
     *  stop matching the batch's own already-placed cell. */
    private Optional<ConstraintViolation> checkAlreadyPlaced(CourseOffering offering, SkeletonCellPlacementRequest request) {
        return checkAlreadyPlaced(offering, request, null);
    }

    /** {@code excludeCellId} lets {@link #swapCells} validate each side moving into the *other's*
     *  current slot without that other cell (which is vacating the slot as part of the very same
     *  swap) spuriously counting as "already placed" there — every other caller passes null.
     *  isActive=false rows (ghosts orphaned by a since-reverted CohortRoomAllocation — see {@link
     *  #getCohortSkeleton}'s own filter) are excluded here too: without this, a ghost cell invisible
     *  in the grid would still silently claim its old slot as "already placed" forever. */
    private Optional<ConstraintViolation> checkAlreadyPlaced(CourseOffering offering, SkeletonCellPlacementRequest request, Long excludeCellId) {
        List<ClassSchedule> candidates = AutoScheduleRunCache.current()
            .map(cache -> cache.byCourseOfferingId(offering.getId()))
            .orElseGet(() -> classScheduleRepository.findByCourseOfferingId(offering.getId()));
        boolean alreadyPlaced = candidates.stream()
            .filter(cs -> Boolean.TRUE.equals(cs.getIsActive()))
            .filter(cs -> excludeCellId == null || !cs.getId().equals(excludeCellId))
            .anyMatch(cs -> cs.getSessionType() == request.sessionType()
                && cs.getDayOfWeek() == request.dayOfWeek()
                && cs.getPeriod() != null && cs.getPeriod().getId().equals(request.periodId())
                && Objects.equals(cs.getBatch() != null ? cs.getBatch().getId() : null, request.batchId())
                && (request.sessionType() != ClassSessionType.THEORY
                    || Objects.equals(cs.getCohortSection() != null ? cs.getCohortSection().getId() : null, request.cohortSectionId())));
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

        if (cs.getSessionGroupId() != null) {
            // OC-127 periodSpan: group-aware moving is out of scope for this pass -- a spanned
            // session's drag handle is disabled in the frontend, this is the server-side backstop.
            throw new IllegalArgumentException("A multi-period session can't be moved here yet — remove and re-place it instead");
        }

        List<ConstraintViolation> violations = validateMoveTarget(cs, request.dayOfWeek(), targetPeriod, request.cohortId(), null);
        if (!violations.isEmpty()) {
            throw new TimetableConstraintViolationException(violations);
        }

        cs.setDayOfWeek(request.dayOfWeek());
        cs.setPeriod(targetPeriod);
        return toCellResponse(classScheduleRepository.save(cs));
    }

    /** Live drag-highlight support: reports every (day, period) grid slot's legality for moving
     *  {@code classScheduleId} there, by literally re-running {@link #validateMoveTarget} — the
     *  exact same check {@link #moveCell} uses to accept/reject a real drop — against each of the
     *  term's active periods across Monday-Saturday. Never promises a slot {@link #moveCell} would
     *  then reject, since it's the same code path; the slot's current own position is skipped
     *  (moving a cell onto itself isn't a real target). Returns an empty list for a non-DRAFT or
     *  periodSpan-grouped cell — nothing here is a legal move target for either (see {@link
     *  #moveCell}'s own restriction), so there's nothing useful to preview.
     *
     *  <p>Read-only and non-reserving: a slot reported valid here can still fail moments later if
     *  another admin places something into it first, or if the drag drags on long enough for a
     *  stale precheck — the real {@link #moveCell}/{@link #swapCells} call remains the sole source
     *  of truth and re-validates independently. This intentionally does NOT re-run the heavier
     *  faculty/room availability checks for a cell that has no faculty yet ({@code cs.getFaculty()
     *  == null}) — {@link #validateMoveTarget} already skips those in that case, since there's
     *  nothing assigned yet to check; staffing (and its own availability checks) happens later, on
     *  the separate Staffing screen. */
    @Transactional(readOnly = true)
    public List<SkeletonSlotPreviewResponse> previewMoveTargets(Long classScheduleId, Long cohortId) {
        ClassSchedule cs = classScheduleRepository.findById(classScheduleId)
            .orElseThrow(() -> new ResourceNotFoundException("Class schedule not found with id: " + classScheduleId));
        if (cs.getStatus() != ClassScheduleStatus.DRAFT || cs.getSessionGroupId() != null) {
            return List.of();
        }

        List<Period> activePeriods = periodRepository.findByIsActiveTrueOrderByPeriodOrderAsc();
        List<SkeletonSlotPreviewResponse> results = new ArrayList<>();
        for (DayOfWeek day : DayOfWeek.values()) {
            for (Period period : activePeriods) {
                boolean isCurrentSlot = day == cs.getDayOfWeek()
                    && cs.getPeriod() != null && cs.getPeriod().getId().equals(period.getId());
                if (isCurrentSlot) {
                    continue;
                }
                List<ConstraintViolation> violations = validateMoveTarget(cs, day, period, cohortId, null);
                results.add(new SkeletonSlotPreviewResponse(day, period.getId(), violations.isEmpty(),
                    violations.isEmpty() ? null : violations.get(0).message()));
            }
        }
        return results;
    }

    /** Atomically exchanges two already-placed DRAFT cells' day/period — e.g. dragging one cell
     *  onto another occupied slot in the grid, rather than the fragile remove-then-re-place-twice
     *  dance that was the only way to do this before. Each side is validated against the *other's*
     *  current slot via {@link #validateMoveTarget} with that other cell excluded from every
     *  check — it's vacating the slot as part of this very swap, so its own still-unmutated row
     *  must never count as a blocker against the side moving in. Scoped identically to {@link
     *  #moveCell}: both cells must be DRAFT, and neither may belong to a periodSpan group (OC-127
     *  group-aware swapping is out of scope for this pass, same restriction as moving one). */
    @Transactional
    public List<SkeletonCellResponse> swapCells(Long cellAId, SkeletonCellSwapRequest request) {
        Long cellBId = request.targetCellId();
        if (cellAId.equals(cellBId)) {
            throw new IllegalArgumentException("Cannot swap a cell with itself");
        }
        ClassSchedule csA = classScheduleRepository.findById(cellAId)
            .orElseThrow(() -> new ResourceNotFoundException("Class schedule not found with id: " + cellAId));
        ClassSchedule csB = classScheduleRepository.findById(cellBId)
            .orElseThrow(() -> new ResourceNotFoundException("Class schedule not found with id: " + cellBId));

        if (csA.getStatus() != ClassScheduleStatus.DRAFT || csB.getStatus() != ClassScheduleStatus.DRAFT) {
            throw new LifecycleConflictException(
                "Only draft skeleton cells can be swapped here.",
                "SKELETON_CELL_NOT_DRAFT", "ClassSchedule", cellAId, null);
        }
        if (csA.getSessionGroupId() != null || csB.getSessionGroupId() != null) {
            throw new IllegalArgumentException("A multi-period session can't be swapped here yet — remove and re-place it instead");
        }

        DayOfWeek dayA = csA.getDayOfWeek();
        Period periodA = csA.getPeriod();
        DayOfWeek dayB = csB.getDayOfWeek();
        Period periodB = csB.getPeriod();

        List<ConstraintViolation> violations = new ArrayList<>();
        violations.addAll(validateMoveTarget(csA, dayB, periodB, request.cohortId(), csB.getId()));
        violations.addAll(validateMoveTarget(csB, dayA, periodA, request.cohortId(), csA.getId()));
        if (!violations.isEmpty()) {
            throw new TimetableConstraintViolationException(violations);
        }

        csA.setDayOfWeek(dayB);
        csA.setPeriod(periodB);
        csB.setDayOfWeek(dayA);
        csB.setPeriod(periodA);
        ClassSchedule savedA = classScheduleRepository.save(csA);
        ClassSchedule savedB = classScheduleRepository.save(csB);
        return List.of(toCellResponse(savedA), toCellResponse(savedB));
    }

    /** Every check a placed cell moving to (day, targetPeriod) must pass — shared by {@link
     *  #moveCell} (excludeCellId null) and {@link #swapCells} (excludeCellId = the swap partner's
     *  id, so its about-to-vacate row is never mistaken for a blocker). Room/capacity/faculty-
     *  eligibility are deliberately NOT rechecked: none of them change on a pure day/period move
     *  (the room, audience, and faculty all stay exactly what they already were). */
    private List<ConstraintViolation> validateMoveTarget(ClassSchedule cs, DayOfWeek day, Period targetPeriod, Long cohortId, Long excludeCellId) {
        CourseOffering offering = cs.getCourseOffering();
        SkeletonCellPlacementRequest asPlacementRequest = new SkeletonCellPlacementRequest(
            offering.getId(), cs.getSessionType(), day, targetPeriod.getId(),
            cs.getBatch() != null ? cs.getBatch().getId() : null,
            cohortId,
            cs.getCohortSection() != null ? cs.getCohortSection().getId() : null,
            null);

        List<ConstraintViolation> violations = new ArrayList<>();
        checkAlreadyPlaced(offering, asPlacementRequest, excludeCellId).ifPresent(violations::add);
        if (isElectiveOffering(offering)) {
            checkElectiveGroupSlot(offering, asPlacementRequest).ifPresent(violations::add);
        } else {
            checkCohortExclusivity(asPlacementRequest, offering, cs.getBatch(), cs.getCohortSection(), excludeCellId).ifPresent(violations::add);
        }
        checkBlocked(day, targetPeriod, offering.getTermInstance()).ifPresent(violations::add);

        if (cs.getFaculty() != null) {
            LocalTime start = targetPeriod.getStartTime();
            LocalTime end = targetPeriod.getEndTime();
            Long venueId = TimetableStaffingService.venueIdOf(cs);
            TimetableStaffingService.RoomCheckSpec roomCheck = venueId != null
                ? new TimetableStaffingService.RoomCheckSpec(cs.getSessionType(), venueId, TimetableStaffingService.physicalRoomOf(cs),
                    TimetableStaffingService.RoomMode.STRICT)
                : null;
            violations.addAll(timetableStaffingService.validateAssignment(
                cs, day, start, end, cs.getFaculty(), excludeCellId, roomCheck, null, null).violations());
        }
        return violations;
    }

    private String scopeKeyForSectionId(Long cohortSectionId) {
        return cohortSectionId != null ? cohortSectionId.toString() : WHOLE_COHORT_SCOPE;
    }

    /** THEORY's scope is its own CohortSection (or WHOLE if the cohort has no committed sections);
     *  LAB/CLINICAL's scope is derived from its batch's own CohortSection (or WHOLE if that batch
     *  predates Capacity Planner section-scoping, or the cohort has none). */
    private String scopeKeyForCell(ClassSchedule cs) {
        if (cs.getSessionType() == ClassSessionType.THEORY || cs.getSessionType() == ClassSessionType.LIBRARY) {
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

    /** Every active {@link ClassSchedule} row belonging to this cohort at this exact day/period,
     *  from either of the two disjoint ways a row can belong to a cohort: (1) its CourseOffering is
     *  one of this cohort's real curriculum offerings (THEORY/LAB/CLINICAL — {@link
     *  #nonElectiveOfferingIds}), or (2) its {@code cohortSection} directly matches one of this
     *  cohort's active sections — the only path a LIBRARY row has, since it has no CourseOffering at
     *  all (see {@code TimetableGlobalAutoScheduleService#fillLibraryGaps}). Shared by {@link
     *  #checkCohortExclusivity} (hard-block check) and {@link #isSlotFreeForCohort} (Library's own
     *  "is this slot genuinely empty" scan) so both agree on exactly the same definition of
     *  "occupied," rather than two independently-maintained copies drifting apart. */
    private List<ClassSchedule> cohortCellsAtSlot(Long cohortId, Long termInstanceId, DayOfWeek day, Long periodId, Long excludeCellId) {
        List<Long> cohortOfferingIds = nonElectiveOfferingIds(termInstanceId, cohortId);
        List<ClassSchedule> offeringCellsAtSlot = cohortOfferingIds.isEmpty() ? List.of() : AutoScheduleRunCache.current()
            .map(cache -> cache.byCourseOfferingIdIn(cohortOfferingIds))
            .orElseGet(() -> classScheduleRepository.findByTermInstanceIdAndCourseOfferingIdIn(termInstanceId, cohortOfferingIds))
            .stream()
            .toList();

        List<Long> sectionIds = resolveActiveSections(cohortId, termInstanceId).stream().map(CohortSection::getId).toList();
        List<ClassSchedule> sectionCellsAtSlot = sectionIds.isEmpty() ? List.of() : AutoScheduleRunCache.current()
            .map(cache -> cache.byCohortSectionIdIn(sectionIds))
            .orElseGet(() -> classScheduleRepository.findByCohortSectionIdInAndIsActiveTrue(sectionIds))
            .stream()
            .toList();

        return Stream.concat(offeringCellsAtSlot.stream(), sectionCellsAtSlot.stream())
            .filter(cs -> Boolean.TRUE.equals(cs.getIsActive()))
            .filter(cs -> excludeCellId == null || !cs.getId().equals(excludeCellId))
            .filter(cs -> cs.getDayOfWeek() == day && cs.getPeriod() != null && cs.getPeriod().getId().equals(periodId))
            .distinct()
            .toList();
    }

    /** Whether this cohort has NO active session at all (any offering, or a Library row) at this
     *  exact day/period — the "is this genuinely free" scan {@code fillLibraryGaps} needs before
     *  claiming a slot, reusing {@link #cohortCellsAtSlot} so it agrees exactly with what {@link
     *  #checkCohortExclusivity} would hard-block. */
    boolean isSlotFreeForCohort(Long cohortId, Long termInstanceId, DayOfWeek day, Long periodId) {
        return cohortCellsAtSlot(cohortId, termInstanceId, day, periodId, null).isEmpty();
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
        return checkCohortExclusivity(request, offering, batch, cohortSection, null);
    }

    /** {@code excludeCellId}: see {@link #checkAlreadyPlaced(CourseOffering, SkeletonCellPlacementRequest, Long)} —
     *  same reason, same swap-only use. */
    private Optional<ConstraintViolation> checkCohortExclusivity(SkeletonCellPlacementRequest request, CourseOffering offering,
                                         Batch batch, CohortSection cohortSection, Long excludeCellId) {
        List<ClassSchedule> cohortCellsAtSlot = cohortCellsAtSlot(request.cohortId(), offering.getTermInstance().getId(),
            request.dayOfWeek(), request.periodId(), excludeCellId);
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
        // LAB/CLINICAL vs a pre-existing LIBRARY cell: hard-blocked, same as THEORY -- Library
        // occupies its whole CohortSection audience just like a mandatory Theory session does.
        return cohortCellsAtSlot.stream()
            .filter(cs -> cs.getSessionType() == ClassSessionType.THEORY || cs.getSessionType() == ClassSessionType.LIBRARY)
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
            .findByTermInstanceIdAndCourseOfferingIdIn(offering.getTermInstance().getId(), siblingIds).stream()
            .filter(cs -> Boolean.TRUE.equals(cs.getIsActive()))
            .toList();
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
            : classScheduleRepository.findByTermInstanceIdAndCourseOfferingIdIn(request.termInstanceId(), siblingIds).stream()
                .filter(cs -> Boolean.TRUE.equals(cs.getIsActive()))
                .toList();
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
                member.batchId(), request.cohortId(), member.cohortSectionId(), null);

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
            checkBlocked(request.dayOfWeek(), period, offering.getTermInstance()).ifPresent(violations::add);

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
            : classScheduleRepository.findByTermInstanceIdAndCourseOfferingIdIn(termInstanceId, siblingIds).stream()
                .filter(cs -> Boolean.TRUE.equals(cs.getIsActive()))
                .toList();
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
            case LIBRARY -> throw new IllegalStateException(
                "Library sessions have no CourseOffering/curriculum-hours budget to suggest candidates for");
        };
        int hours = hoursObj != null ? hoursObj : 0;
        if (hours <= 0) {
            return List.of();
        }
        int blockSize = CurriculumHoursCalculator.resolveBlockSize(offering.getSubject(), sessionType);
        int effectiveHours = creditClinicalShiftHours(sessionType, hours, offering, weeksInTerm);
        int required = CurriculumHoursCalculator.sessionsPerWeek(effectiveHours, weeksInTerm, periodDurationMinutes, blockSize);

        List<ClassSchedule> existingForOffering = classScheduleRepository.findByCourseOfferingId(courseOfferingId);
        List<ClassSchedule> existingForThis = existingForOffering.stream()
            .filter(cs -> Boolean.TRUE.equals(cs.getIsActive()))
            .filter(cs -> cs.getSessionType() == sessionType
                && Objects.equals(cs.getBatch() != null ? cs.getBatch().getId() : null, batchId)
                && Objects.equals(cs.getCohortSection() != null ? cs.getCohortSection().getId() : null, cohortSectionId))
            .toList();
        // Sessions, not rows (see sessionKey) -- `required` is session-denominated, so a placed
        // multi-period block counts once here, not once per period it spans.
        int placedSessions = (int) existingForThis.stream().map(TimetableSkeletonService::sessionKey).distinct().count();
        int shortfall = required - placedSessions;
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

    /** {@code REQUIRES_NEW} — see {@link #placeCell}'s javadoc: both auto-schedulers call this to
     *  undo a just-placed cell after its staffing attempt failed, and that undo must not depend on
     *  (or be undone by) whatever rollback state the caller's own broader transaction is in. Guarded
     *  to an unstaffed draft only — this is the manual "click a skeleton cell to remove it" path
     *  (and the auto-schedulers' own undo-on-staffing-failure path, which is always unstaffed by
     *  construction), so a staffed/published session can never be destroyed by a stray click here;
     *  {@link TimetableGlobalAutoScheduleService#attemptBacktrack} — which does need to remove one
     *  of its own already-staffed placements — uses {@link #forceRemoveCell} instead, never this. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void removeCell(Long classScheduleId) {
        ClassSchedule cs = classScheduleRepository.findById(classScheduleId)
            .orElseThrow(() -> new ResourceNotFoundException("Class schedule not found with id: " + classScheduleId));
        if (cs.getFaculty() != null || cs.getStatus() != ClassScheduleStatus.DRAFT) {
            throw new LifecycleConflictException(
                "Only an unstaffed draft skeleton cell can be removed here — edit or delete a staffed session from the Class Schedule screen instead",
                "SKELETON_CELL_NOT_REMOVABLE", "ClassSchedule", classScheduleId, null);
        }
        deleteCellAndSiblings(cs);
    }

    /** Package-private escape hatch from {@link #removeCell}'s staffed-cell guard — for
     *  {@link TimetableGlobalAutoScheduleService#attemptBacktrack} (displacing a cell its own run
     *  just placed *and staffed* in one step) and {@code TimetableGlobalAutoScheduleService
     *  #rollbackElectiveCells} (unwinding an elective group's earlier, already-staffed members after
     *  a later member fails). Both need this because every global-auto-schedule placement is staffed
     *  immediately, so by the time either needs to undo an earlier placement, that earlier one is
     *  never still a bare unstaffed draft the ordinary {@link #removeCell} guard would allow.
     *  No controller exposes this — it only ever runs against a cell the calling run itself placed a
     *  moment earlier as part of the same best-effort pass, restorable via a fresh {@code
     *  placeCell}+{@code staffCell} if the backtrack/rollback doesn't pan out, never against a
     *  pre-existing published/committed session from before the run started. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void forceRemoveCell(Long classScheduleId) {
        ClassSchedule cs = classScheduleRepository.findById(classScheduleId)
            .orElseThrow(() -> new ResourceNotFoundException("Class schedule not found with id: " + classScheduleId));
        deleteCellAndSiblings(cs);
    }

    /** Shared tail of {@link #removeCell}/{@link #forceRemoveCell} — keeps the {@link
     *  AutoScheduleRunCache} sync and the periodSpan sibling-group delete in exactly one place. */
    private void deleteCellAndSiblings(ClassSchedule cs) {
        AutoScheduleRunCache.current().ifPresent(cache -> cache.recordRemoval(cs));
        // OC-127 periodSpan: a multi-period session's rows are one atomic unit -- removing any one
        // of them removes every sibling sharing the same groupId.
        if (cs.getSessionGroupId() != null) {
            classScheduleRepository.findBySessionGroupIdOrderByPeriod_PeriodOrderAsc(cs.getSessionGroupId())
                .forEach(sibling -> classScheduleRepository.deleteById(sibling.getId()));
            return;
        }
        classScheduleRepository.deleteById(cs.getId());
    }
}
