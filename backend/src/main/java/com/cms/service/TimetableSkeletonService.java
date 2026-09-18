package com.cms.service;

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;
import com.cms.dto.ClinicalShiftWindow;
import com.cms.dto.DutyDayMovePreviewResponse;
import com.cms.dto.DutyDayMoveRequest;
import com.cms.dto.SkeletonPlannedMove;
import com.cms.dto.SkeletonRelocateRequest;
import com.cms.dto.SkeletonRelocationPlanResponse;
import com.cms.model.ClinicalShiftGroup;
import com.cms.model.ClinicalVenue;
import com.cms.model.Faculty;
import com.cms.model.RotationSlot;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.ClinicalShiftSummaryItem;
import com.cms.dto.ClinicalShiftWindow;
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
import com.cms.dto.SkeletonCellReplaceResponse;
import com.cms.dto.SkeletonCellReplaceRequest;
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
import com.cms.model.Faculty;
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
import com.cms.repository.FacultyRepository;
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
    private final com.cms.repository.RotationMemberAssignmentRepository rotationMemberAssignmentRepository;
    private final RotationResolverService rotationResolverService;
    private final CourseOfferingService courseOfferingService;
    private final CohortRepository cohortRepository;
    private final TermInstanceRepository termInstanceRepository;
    private final CohortRoomAllocationRepository cohortRoomAllocationRepository;
    private final CohortSectionRepository cohortSectionRepository;
    private final TimetableStaffingService timetableStaffingService;
    private final ClinicalShiftGroupRepository clinicalShiftGroupRepository;
    private final ClinicalShiftGroupService clinicalShiftGroupService;
    private final TimetableClinicalShiftChecker clinicalShiftChecker;
    private final FacultyRepository facultyRepository;

    public TimetableSkeletonService(CourseOfferingRepository courseOfferingRepository,
                                     ClassScheduleRepository classScheduleRepository,
                                     PeriodRepository periodRepository,
                                     BatchRepository batchRepository,
                                     BatchService batchService,
                                     TimetableBlockedPeriodChecker blockedPeriodChecker,
                                     com.cms.repository.RotationSlotRepository rotationSlotRepository,
                                     com.cms.repository.RotationMemberAssignmentRepository rotationMemberAssignmentRepository,
                                     RotationResolverService rotationResolverService,
                                     CourseOfferingService courseOfferingService,
                                     CohortRepository cohortRepository,
                                     TermInstanceRepository termInstanceRepository,
                                     CohortRoomAllocationRepository cohortRoomAllocationRepository,
                                     CohortSectionRepository cohortSectionRepository,
                                     TimetableStaffingService timetableStaffingService,
                                     ClinicalShiftGroupRepository clinicalShiftGroupRepository,
                                     ClinicalShiftGroupService clinicalShiftGroupService,
                                    TimetableClinicalShiftChecker clinicalShiftChecker,
                                     FacultyRepository facultyRepository) {
        this.courseOfferingRepository = courseOfferingRepository;
        this.classScheduleRepository = classScheduleRepository;
        this.periodRepository = periodRepository;
        this.batchRepository = batchRepository;
        this.batchService = batchService;
        this.blockedPeriodChecker = blockedPeriodChecker;
        this.rotationSlotRepository = rotationSlotRepository;
        this.rotationMemberAssignmentRepository = rotationMemberAssignmentRepository;
        this.rotationResolverService = rotationResolverService;
        this.courseOfferingService = courseOfferingService;
        this.cohortRepository = cohortRepository;
        this.termInstanceRepository = termInstanceRepository;
        this.cohortRoomAllocationRepository = cohortRoomAllocationRepository;
        this.cohortSectionRepository = cohortSectionRepository;
        this.timetableStaffingService = timetableStaffingService;
        this.clinicalShiftGroupRepository = clinicalShiftGroupRepository;
        this.clinicalShiftGroupService = clinicalShiftGroupService;
        this.clinicalShiftChecker = clinicalShiftChecker;
        this.facultyRepository = facultyRepository;
    }

    public SkeletonBuilderResponse getCohortSkeleton(Long termInstanceId, Long cohortId) {
        Cohort cohort = cohortRepository.findById(cohortId)
            .orElseThrow(() -> new ResourceNotFoundException("Cohort not found with id: " + cohortId));
        TermInstance termInstance = termInstanceRepository.findById(termInstanceId)
            .orElseThrow(() -> new ResourceNotFoundException("Term instance not found with id: " + termInstanceId));
        String termInstanceLabel = termInstance.getAcademicYear().getName() + " " + termInstance.getTermType();

        List<CohortSection> activeSections = resolveActiveSections(cohortId, termInstanceId);
        List<CohortSectionResponse> sectionResponses = activeSections.stream().map(this::toSectionResponse).toList();
        List<ClinicalShiftWindow> shiftWindows = clinicalShiftGroupService.resolveActiveWindowsForCohort(cohortId, termInstanceId);

        // LIBRARY and SPORTS cells have no CourseOffering (see TimetableGlobalAutoScheduleService
        // #fillLibraryGaps/#fillSportsGaps), so the offering-based query below never finds them --
        // resolved separately by this cohort's own active CohortSections, same source
        // cohortCellsAtSlot/isSlotFreeForCohort already use.
        List<Long> sectionIds = activeSections.stream().map(CohortSection::getId).toList();
        List<ClassSchedule> libraryCells = sectionIds.isEmpty() ? List.of()
            : classScheduleRepository.findByCohortSectionIdInAndIsActiveTrue(sectionIds).stream()
                .filter(cs -> cs.getSessionType() == ClassSessionType.LIBRARY || cs.getSessionType() == ClassSessionType.SPORTS)
                .toList();

        boolean termTimetablePublished = classScheduleRepository
            .existsByTermInstanceIdAndStatus(termInstanceId, ClassScheduleStatus.PUBLISHED);

        List<Long> offeringIds = new ArrayList<>(nonElectiveOfferingIds(termInstanceId, cohortId));
        offeringIds.addAll(electiveOfferingIds(termInstanceId, cohortId));
        if (offeringIds.isEmpty()) {
            List<SkeletonCellResponse> libraryOnlyCells = libraryCells.stream().map(this::toCellResponse).toList();
            return new SkeletonBuilderResponse(cohortId, cohort.getDisplayName(), termInstanceLabel, List.of(), libraryOnlyCells, List.of(), sectionResponses,
                CurriculumHoursCalculator.weeksInTerm(termInstance), WorkingSaturdayCalculator.workingSaturdayCount(termInstance), List.of(),
                termTimetablePublished, shiftWindows);
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
                budgets.addAll(theoryBudgets(csc, existingForOffering, termInstance, weeksInTerm, periodDurationMinutes, activeSections));
                budgets.addAll(batchScopedBudgets(ClassSessionType.LAB, csc.getLabHours(), offeringBatches, existingForOffering, termInstance, weeksInTerm, periodDurationMinutes, offering.getSubject()));
                Integer creditedClinicalHours = csc.getClinicalHours() != null
                    ? creditClinicalShiftHours(ClassSessionType.CLINICAL, csc.getClinicalHours(), offering, weeksInTerm)
                    : null;
                budgets.addAll(batchScopedBudgets(ClassSessionType.CLINICAL, csc.getClinicalHours(), creditedClinicalHours,
                    offeringBatches, existingForOffering, termInstance, weeksInTerm, periodDurationMinutes, offering.getSubject()));
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
            weeksInTerm, WorkingSaturdayCalculator.workingSaturdayCount(termInstance), clinicalShiftHours, termTimetablePublished, shiftWindows);
    }

    /** Term-wide Clinical Shift Group summary for Timetable Draft Review's duty-roster banner --
     *  one row per {@link CohortSection} with hours/week summed across however many active shift
     *  groups that section has, so a reviewer sees Clinical hours exist even though they never show
     *  up as grid cells (see the {@link #toClinicalShiftHours} comment above). Groups with no
     *  {@code cohortSection} (not yet room-sectioned via Capacity Auto-Plan) are skipped -- Draft
     *  Review has nothing scoped to show them against either. */
    public List<ClinicalShiftSummaryItem> findClinicalShiftSummaryForTerm(Long termInstanceId) {
        record SectionHours(CohortSection section, double hours) {}

        Map<Long, List<SectionHours>> bySectionId = clinicalShiftGroupRepository
            .findByTermInstanceIdAndIsActiveTrue(termInstanceId).stream()
            .filter(g -> g.getCohortSection() != null && g.getCourseOffering() != null)
            .map(g -> {
                Integer durationMinutes = g.getCourseOffering().getClinicalShiftDurationMinutes();
                return new SectionHours(g.getCohortSection(), durationMinutes != null ? durationMinutes / 60.0 : 0.0);
            })
            .filter(sh -> sh.hours() > 0)
            .collect(Collectors.groupingBy(sh -> sh.section().getId(), LinkedHashMap::new, Collectors.toList()));

        Map<Long, String> cohortNameBySectionId = new LinkedHashMap<>();
        Map<Long, String> sectionLabelBySectionId = new LinkedHashMap<>();
        Map<Long, Double> hoursBySectionId = new LinkedHashMap<>();
        for (var entry : bySectionId.entrySet()) {
            CohortSection section = entry.getValue().get(0).section();
            cohortNameBySectionId.put(entry.getKey(), section.getCohortRoomAllocation().getCohort().getDisplayName());
            sectionLabelBySectionId.put(entry.getKey(), section.getSectionLabel());
            hoursBySectionId.put(entry.getKey(), entry.getValue().stream().mapToDouble(SectionHours::hours).sum());
        }

        // Disambiguate sections sharing the same cohort display name by appending the section label.
        Map<String, Long> cohortNameCounts = cohortNameBySectionId.values().stream()
            .collect(Collectors.groupingBy(n -> n, Collectors.counting()));

        return cohortNameBySectionId.entrySet().stream()
            .map(e -> {
                String cohortName = e.getValue();
                String label = cohortNameCounts.get(cohortName) > 1
                    ? cohortName + " – " + sectionLabelBySectionId.get(e.getKey())
                    : cohortName;
                return new ClinicalShiftSummaryItem(e.getKey(), label, hoursBySectionId.get(e.getKey()));
            })
            .sorted(Comparator.comparing(ClinicalShiftSummaryItem::cohortLabel))
            .toList();
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
        double hours = durationMinutes != null
            ? (durationMinutes / 60.0) * effectiveWeeksFor(group, offering, weeksInTerm, durationMinutes / 60.0) : 0.0;
        CohortSection section = group.getCohortSection();
        return new SkeletonClinicalShiftHours(group.getCourseOffering().getId(), section != null ? section.getId() : null, hours);
    }

    /** A group bounded to a real sub-window (e.g. a 4-week internship block, see V419 migration)
     *  only actually delivers hours across those weeks, not the whole term -- crediting the full
     *  {@code weeksInTerm} for a group that only ran 4 of them would wildly over-credit and hide a
     *  genuine remaining requirement. But a manual date range (or its Both-null "whole term"
     *  default) is only ever a CEILING here, never a floor: {@link
     *  CurriculumHoursCalculator#weeksNeededFor} independently caps the same figure at however
     *  many weekly duty-length occurrences the offering's own curriculum Clinical hours actually
     *  need, and the tighter of the two always wins. Without this second cap, a shift duty
     *  configured longer than the subject's real average per-session need (only a FLOOR is
     *  enforced at save-time, see {@code ClinicalShiftGroupService}) silently over-credited hours
     *  every week for the group's whole run with nothing anywhere to stop it -- the real mechanism
     *  behind a genuine incident where three Clinical Shift subjects reported 468h assigned
     *  against a 400h combined requirement, term over term, with no admin action able to trigger
     *  or fix it (2026-09-05). {@link ClinicalShiftOccurrenceService#generateForDate} enforces the
     *  identical cap against real occurrence generation, not just this display/crediting figure —
     *  the two must never disagree. */
    private int effectiveWeeksFor(ClinicalShiftGroup group, CourseOffering offering, int weeksInTerm, double hoursPerOccurrence) {
        int datePatternWeeks;
        if (group.getEffectiveStartDate() == null || group.getEffectiveEndDate() == null) {
            datePatternWeeks = weeksInTerm;
        } else {
            long days = ChronoUnit.DAYS.between(group.getEffectiveStartDate(), group.getEffectiveEndDate()) + 1;
            datePatternWeeks = (int) Math.max(1, Math.ceil(days / 7.0));
        }
        CurriculumSemesterCourse csc = offering != null ? offering.getCurriculumSemesterCourse() : null;
        Integer rawHours = csc != null ? csc.getClinicalHours() : null;
        int hoursCapWeeks = CurriculumHoursCalculator.weeksNeededFor(rawHours != null ? rawHours : 0, hoursPerOccurrence);
        return hoursCapWeeks > 0 ? Math.min(datePatternWeeks, hoursCapWeeks) : datePatternWeeks;
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
                                                        TermInstance term, int weeksInTerm, double periodDurationMinutes,
                                                        List<CohortSection> sections) {
        int theoryHours = csc.getTheoryHours() != null ? csc.getTheoryHours() : 0;
        int required = CurriculumHoursCalculator.sessionsPerWeek(theoryHours, weeksInTerm, periodDurationMinutes, 1);
        int requiredRuns = CurriculumHoursCalculator.sessionsOverTerm(theoryHours, periodDurationMinutes, 1);

        if (sections.isEmpty()) {
            List<ClassSchedule> placed = existing.stream()
                .filter(cs -> cs.getSessionType() == ClassSessionType.THEORY && cs.getCohortSection() == null)
                .toList();
            int delivered = deliveredRuns(placed, term, weeksInTerm);
            return List.of(new SkeletonSubjectBudget(ClassSessionType.THEORY, null, null, null, null,
                theoryHours, weeksInTerm, required, placed.size(), requiredRuns, delivered,
                runsToHours(delivered, periodDurationMinutes)));
        }

        Map<Long, List<ClassSchedule>> placedBySectionId = existing.stream()
            .filter(cs -> cs.getSessionType() == ClassSessionType.THEORY && cs.getCohortSection() != null)
            .collect(java.util.stream.Collectors.groupingBy(cs -> cs.getCohortSection().getId(), LinkedHashMap::new, java.util.stream.Collectors.toList()));

        List<SkeletonSubjectBudget> rows = new ArrayList<>();
        for (CohortSection section : sections) {
            List<ClassSchedule> placed = placedBySectionId.getOrDefault(section.getId(), List.of());
            int delivered = deliveredRuns(placed, term, weeksInTerm);
            rows.add(new SkeletonSubjectBudget(ClassSessionType.THEORY, null, null,
                section.getId(), section.getSectionLabel(), theoryHours, weeksInTerm, required, placed.size(),
                requiredRuns, delivered, runsToHours(delivered, periodDurationMinutes)));
        }
        return rows;
    }

    /** What {@code rows} really deliver across the term, in session occurrences: each distinct
     *  session (see {@link #sessionKey}) runs as often as its day does — every week on
     *  Monday-Friday, only on the chosen working Saturdays on Saturday (see {@link
     *  WorkingSaturdayCalculator#runsInTerm}). Budgets are planned against the term's total hours
     *  (2026-09-15), so a Saturday session counts for exactly what it delivers, no more. */
    static int deliveredRuns(List<ClassSchedule> rows, TermInstance term, int weeksInTerm) {
        Map<String, DayOfWeek> dayBySession = new LinkedHashMap<>();
        for (ClassSchedule cs : rows) {
            dayBySession.putIfAbsent(sessionKey(cs), cs.getDayOfWeek());
        }
        return dayBySession.values().stream()
            .mapToInt(day -> WorkingSaturdayCalculator.runsInTerm(day, term, weeksInTerm))
            .sum();
    }

    /** Clock hours delivered by {@code runs} session occurrences of {@code sessionMinutes} each,
     *  to one decimal place. */
    private static double runsToHours(int runs, double sessionMinutes) {
        return Math.round(runs * sessionMinutes / 60.0 * 10) / 10.0;
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
                                                            List<ClassSchedule> existing, TermInstance term, int weeksInTerm,
                                                            double periodDurationMinutes, Subject subject) {
        return batchScopedBudgets(type, hoursObj, hoursObj, batches, existing, term, weeksInTerm, periodDurationMinutes, subject);
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
                                                            List<ClassSchedule> existing, TermInstance term, int weeksInTerm,
                                                            double periodDurationMinutes, Subject subject) {
        int hours = displayHoursObj != null ? displayHoursObj : 0;
        if (hours <= 0) {
            return List.of();
        }
        int effectiveHours = effectiveHoursForRequired != null ? effectiveHoursForRequired : 0;
        int blockSize = CurriculumHoursCalculator.resolveBlockSize(subject, type);
        int required = CurriculumHoursCalculator.sessionsPerWeek(effectiveHours, weeksInTerm, periodDurationMinutes, blockSize);
        int requiredRuns = CurriculumHoursCalculator.sessionsOverTerm(effectiveHours, periodDurationMinutes, blockSize);
        double sessionMinutes = periodDurationMinutes * blockSize;

        // sessionsPerWeek guarantees at least 1 recurring session/WEEK for the whole term once its
        // input is positive at all, which delivers periodDurationMinutes*blockSize*weeksInTerm
        // hours total (e.g. a 4-period block x 26 weeks = 86.7h) -- correct for a subject's own real
        // hours (a 3h subject still wants its normal weekly rhythm, even though 78h delivered for
        // 3h owed is itself a known, accepted tradeoff of this recurring-slot model). But wrong here:
        // effectiveHours is what's LEFT after creditClinicalShiftHours already credited most of the
        // requirement off-grid (e.g. 4h left after a 6h/week shift covers 156 of a 160h Clinical
        // requirement) -- that 4h isn't a real, separately-schedulable need, it's rounding noise from
        // the shift's own weekly cadence not dividing the curriculum figure evenly. Forcing one
        // recurring session to chase a residual smaller than what that ONE session alone delivers
        // across the whole term is never worth it -- compare against the term-wide delivery, not one
        // session's own length, or a residual just over one session's length (still wildly smaller
        // than 26 weeks of it) would wrongly still commit. Only fires for a genuinely shift-credited
        // row (effectiveHours < hours) -- a shift covering only a small fraction of a much larger
        // requirement still needs the grid for its real remainder and is untouched.
        if (required > 0 && effectiveHours < hours
            && effectiveHours * 60.0 < periodDurationMinutes * blockSize * weeksInTerm) {
            required = 0;
            requiredRuns = 0;
        }

        // Counted in SESSIONS, not rows: a multi-period block is several ClassSchedule rows sharing
        // one sessionGroupId, and requiredSessionsPerWeek above is a session count -- comparing raw
        // row counts against it made a single placed 4-period Clinical block read as "4 of 6
        // sessions done" when it was 1, so the shortfall (and therefore the whole placement pass)
        // silently under-delivered every LAB/CLINICAL row by its own block size.
        Map<Long, List<ClassSchedule>> placedByBatchId = existing.stream()
            .filter(cs -> cs.getSessionType() == type && cs.getBatch() != null)
            .collect(java.util.stream.Collectors.groupingBy(cs -> cs.getBatch().getId(), LinkedHashMap::new,
                java.util.stream.Collectors.toList()));

        List<Batch> matchingBatches = batches.stream()
            .filter(b -> !(type == ClassSessionType.LAB && b.getClinicalVenue() != null))
            .filter(b -> !(type == ClassSessionType.CLINICAL && b.getLab() != null))
            .toList();

        if (matchingBatches.isEmpty()) {
            return List.of(new SkeletonSubjectBudget(type, null, null, null, null, hours, weeksInTerm, required, 0,
                requiredRuns, 0, 0));
        }
        // A rotation-linked cell (see RotationGroupService#create) has its ClassSchedule#batch set
        // to null -- invisible to placedByBatchId above -- so a batch rotating through this session
        // type would otherwise always read as 0 placed, and the caller would keep trying to place
        // ANOTHER independent session for it on top of the rotation. Credits one placed session per
        // rotation assignment this batch holds for this exact session type (a batch is never a
        // member of more than one rotation slot of the same type at once by construction), running
        // as often as the rotation slot's own day does.
        Map<Long, List<DayOfWeek>> rotationDaysByBatchId = rotationMemberAssignmentRepository
            .findByBatchIdIn(matchingBatches.stream().map(Batch::getId).toList()).stream()
            .filter(a -> a.getRotationSlot().getClassSchedule().getSessionType() == type)
            .collect(java.util.stream.Collectors.groupingBy(a -> a.getBatch().getId(),
                java.util.stream.Collectors.mapping(a -> a.getRotationSlot().getClassSchedule().getDayOfWeek(),
                    java.util.stream.Collectors.toList())));
        List<SkeletonSubjectBudget> rows = new ArrayList<>();
        for (Batch batch : matchingBatches) {
            List<ClassSchedule> placedRows = placedByBatchId.getOrDefault(batch.getId(), List.of());
            List<DayOfWeek> rotationDays = rotationDaysByBatchId.getOrDefault(batch.getId(), List.of());
            long placed = placedRows.stream().map(TimetableSkeletonService::sessionKey).distinct().count()
                + rotationDays.size();
            int delivered = deliveredRuns(placedRows, term, weeksInTerm)
                + rotationDays.stream().mapToInt(day -> WorkingSaturdayCalculator.runsInTerm(day, term, weeksInTerm)).sum();
            CohortSection section = batch.getCohortSection();
            rows.add(new SkeletonSubjectBudget(type, batch.getId(), batch.getName(),
                section != null ? section.getId() : null, section != null ? section.getSectionLabel() : null,
                hours, weeksInTerm, required, (int) placed, requiredRuns, delivered, runsToHours(delivered, sessionMinutes)));
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
            case LIBRARY, SPORTS -> throw new IllegalStateException(
                "Library/Sports sessions have no CourseOffering/curriculum-hours budget to check against");
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
        int requiredRuns = CurriculumHoursCalculator.sessionsOverTerm(effectiveHours, periodDurationMinutes, blockSize);

        Long scopeBatchId = batch != null ? batch.getId() : null;
        Long scopeSectionId = cohortSection != null ? cohortSection.getId() : null;
        List<ClassSchedule> candidates = AutoScheduleRunCache.current()
            .map(cache -> cache.byCourseOfferingId(offering.getId()))
            .orElseGet(() -> classScheduleRepository.findByCourseOfferingId(offering.getId()));
        List<ClassSchedule> placed = candidates.stream()
            .filter(cs -> Boolean.TRUE.equals(cs.getIsActive()))
            .filter(cs -> cs.getSessionType() == sessionType)
            .filter(cs -> sessionType == ClassSessionType.THEORY
                ? Objects.equals(cs.getCohortSection() != null ? cs.getCohortSection().getId() : null, scopeSectionId)
                : Objects.equals(cs.getBatch() != null ? cs.getBatch().getId() : null, scopeBatchId))
            .toList();
        // Planned against the term's total hours (2026-09-15): a session counts for the runs its day
        // really has (a first-Saturday-only session runs 6 times, a weekday one 26), so another
        // session is allowed exactly while the placed ones still fall short of the curriculum hours
        // -- one session's worth of rounding over is the most this can ever overshoot.
        int delivered = deliveredRuns(placed, term, weeksInTerm);
        if (delivered >= requiredRuns) {
            double sessionMinutes = periodDurationMinutes * blockSize;
            return Optional.of(new ConstraintViolation("SKELETON_CELL_BUDGET_EXCEEDED",
                offering.getSubject().getName() + "'s " + sessionType + " budget is already met ("
                    + runsToHours(delivered, sessionMinutes) + " of " + effectiveHours
                    + " h placed across the term) — increase the subject's curriculum hours first if more sessions are genuinely needed."));
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

    /** Sibling to {@link #checkBlocked} for a cohort whose Program has opted into Clinical Shift
     *  scheduling (see {@link com.cms.model.Program#getUsesClinicalShiftScheduling()}) — rejects a
     *  manual placement/move/swap into a period overlapping that cohort's Clinical Shift wall-clock
     *  window (including bus travel buffer) on this day, mirroring the hard block {@link
     *  TimetableGlobalAutoScheduleService#tryPlaceAndStaff} already enforces for auto-schedule.
     *  Deliberately NOT folded into {@link TimetableBlockedPeriodChecker} — that check is
     *  institution-wide/cohort-agnostic by design, while this one is cohort-scoped. No-op (empty)
     *  when the cohort's Program hasn't opted in.
     *
     *  <p>{@code batchId} narrows the check to that batch's own duty window where the session has
     *  one — a LAB/CLINICAL row is attended by its batch alone, so another batch's shift should
     *  never block it. Null (a Theory/elective/Library row, whose audience is the whole
     *  cohort/section) keeps the conservative union of every window the cohort touches. */
    private Optional<ConstraintViolation> checkClinicalShiftBlocked(Long cohortId, Long batchId, DayOfWeek dayOfWeek, Period period, TermInstance termInstance) {
        return clinicalShiftChecker.blockReason(cohortId, batchId, dayOfWeek, period, termInstance);
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
            cs.getSessionGroupId(),
            cs.isPinned(),
            cs.getCourseOffering() != null && isCommonCohortElective(cs.getCourseOffering())
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

    /** Replace WHAT a placed Theory cell teaches, keeping its day/period/audience untouched — the
     *  "same slot, different content" edit that previously forced an admin to remove the session and
     *  place a new one, losing its faculty and room in between and briefly leaving a hole in the grid
     *  that a concurrent automation run could fill.
     *
     *  <p>Subject and faculty move together as one decision (see {@link SkeletonCellReplaceRequest}),
     *  and every check that guards an ordinary placement still applies to the incoming subject:
     *  it must not already sit at this exact day/period, it must not exceed its own curriculum-hours
     *  quota, the chosen faculty must be eligible for it, free at this time, and within their
     *  workload caps. Nothing is written until all of them pass.
     *
     *  <p>THEORY only, deliberately. A LAB/CLINICAL row's audience is a {@link Batch}, and a Batch
     *  belongs to exactly one CourseOffering — so "replace the subject" there really means "swap in
     *  a different batch with its own committed venue", which is a Capacity Planner decision rather
     *  than a per-session edit. Electives are excluded for the same class of reason: every member of
     *  an elective group must share one slot, so changing one member's subject in place would break
     *  that invariant.
     *
     *  <p>The room is deliberately NOT a parameter: it is re-derived from the section's committed
     *  allocation, exactly as {@code TimetableStaffingService#staffCell} does. */
    @Transactional
    public SkeletonCellReplaceResponse replaceCellSubject(Long classScheduleId, SkeletonCellReplaceRequest request) {
        ClassSchedule cs = classScheduleRepository.findById(classScheduleId)
            .orElseThrow(() -> new ResourceNotFoundException("Class schedule not found with id: " + classScheduleId));
        if (cs.getStatus() != ClassScheduleStatus.DRAFT) {
            throw new LifecycleConflictException(
                "Only a draft skeleton cell can be replaced — a published session is immutable.",
                "SKELETON_CELL_NOT_DRAFT", "ClassSchedule", classScheduleId, null);
        }
        if (cs.getSessionType() != ClassSessionType.THEORY) {
            throw new IllegalArgumentException(
                "Only a Theory session can have its subject replaced here — a Lab/Clinical session's audience is a "
                    + "batch tied to one offering, so changing its subject means changing the batch in Capacity Planner.");
        }
        CourseOffering newOffering = courseOfferingRepository.findById(request.courseOfferingId())
            .orElseThrow(() -> new ResourceNotFoundException("Course offering not found with id: " + request.courseOfferingId()));
        // An institution-decided elective is a common cohort subject (only its chosen option runs), so
        // it's replaced like any other; a student-choice group's options must keep sharing one slot.
        if (isSharedSlotElective(newOffering) || (cs.getCourseOffering() != null && isSharedSlotElective(cs.getCourseOffering()))) {
            throw new IllegalArgumentException(
                "Student-choice elective sessions can't be replaced in place — every option in the group shares one "
                    + "slot, so Run Automation places and moves the whole group.");
        }
        Faculty newFaculty = facultyRepository.findById(request.facultyId())
            .orElseThrow(() -> new ResourceNotFoundException("Faculty not found with id: " + request.facultyId()));

        Period period = cs.getPeriod();
        List<ConstraintViolation> violations = new ArrayList<>();

        // excludeCellId = this row: it is vacating the old subject as part of this very edit, so it
        // must never count as a blocker against the incoming one.
        SkeletonCellPlacementRequest asPlacementRequest = new SkeletonCellPlacementRequest(
            newOffering.getId(), ClassSessionType.THEORY, cs.getDayOfWeek(), period.getId(), null, null,
            cs.getCohortSection() != null ? cs.getCohortSection().getId() : null, null);
        checkAlreadyPlaced(newOffering, asPlacementRequest, cs.getId()).ifPresent(violations::add);

        // NO budget-cap check here, deliberately -- unlike placement, which adds a session to the
        // week, a replace is hour-neutral: one existing slot changes hands, so the incoming subject
        // gains exactly what the outgoing one loses and the term's total delivered hours do not
        // move. The over-delivery `checkBudgetNotExceeded` exists to prevent is therefore not
        // something this operation can cause.
        //
        // Enforcing it here also made the feature unusable in practice. The Global Auto-Schedule
        // extra-hours filler deliberately packs the grid by pushing every Theory offering PAST its
        // curriculum requirement (that is the whole point of the "no empty periods" policy), so on
        // a packed grid every candidate subject is already over quota and every replace was
        // rejected -- the cap was rejecting replacements on the grounds of a surplus the scheduler
        // had itself created on purpose.

        // Eligibility is reported as a violation alongside the rest rather than thrown on its own, so
        // the admin sees every reason the replacement was refused in one response instead of
        // discovering them one failed attempt at a time.
        if (FacultyEligibility.eligibleFaculty(newOffering.getSubject(), List.of(newFaculty)).isEmpty()) {
            violations.add(new ConstraintViolation("STAFFING_FACULTY_NOT_ELIGIBLE",
                newFaculty.getFullName() + " isn't eligible to teach " + newOffering.getSubject().getName() + "."));
        }
        // Physical location, staff, and period availability are all re-checked, not assumed. The
        // room does not change (a Theory room is the section's committed classroom, re-derived, not
        // a per-session choice), but it must still be re-scanned: the section's committed allocation
        // can have been changed or re-committed since this cell was placed, and the incoming faculty
        // is new to this slot regardless. Room spec mirrors validateMoveTarget's, so replace and
        // move/swap judge occupancy by exactly the same rule rather than two drifting copies.
        Long venueId = TimetableStaffingService.venueIdOf(cs);
        TimetableStaffingService.RoomCheckSpec roomCheck = venueId != null
            ? new TimetableStaffingService.RoomCheckSpec(cs.getSessionType(), venueId,
                TimetableStaffingService.physicalRoomOf(cs), TimetableStaffingService.RoomMode.STRICT)
            : null;
        violations.addAll(timetableStaffingService.validateAssignment(
            cs, cs.getDayOfWeek(), period.getStartTime(), period.getEndTime(), newFaculty,
            cs.getId(), roomCheck, null, null).violations());

        // Period availability also means "this cohort isn't away on clinical duty" -- a duty window
        // can be added or widened after a cell was placed, so the slot's legality is re-established
        // here rather than trusted because it was legal when originally placed.
        Long cohortId = audienceCohortId(cs);
        if (cohortId != null) {
            checkClinicalShiftBlocked(cohortId, cs.getBatch() != null ? cs.getBatch().getId() : null,
                cs.getDayOfWeek(), period, cs.getTermInstance())
                .ifPresent(violations::add);
        }

        if (!violations.isEmpty()) {
            throw new TimetableConstraintViolationException(violations);
        }

        // A multi-period Theory block is one session, so every row in the group is replaced together
        // -- replacing only the clicked period would leave a 2-period block teaching two subjects.
        CourseOffering displacedOffering = cs.getCourseOffering();
        CohortSection audience = cs.getCohortSection();

        List<ClassSchedule> group = cs.getSessionGroupId() == null ? List.of(cs)
            : classScheduleRepository.findBySessionGroupIdOrderByPeriod_PeriodOrderAsc(cs.getSessionGroupId());
        for (ClassSchedule row : group) {
            row.setCourseOffering(newOffering);
            row.setSubject(newOffering.getSubject());
            row.setFaculty(newFaculty);
            // Replacing is a deliberate human decision, so it pins for the same reason a drag-move
            // does -- otherwise the next automation run would simply undo it.
            row.setPinned(true);
            classScheduleRepository.save(row);
        }
        return new SkeletonCellReplaceResponse(toCellResponse(cs),
            describeDisplacedShortfall(displacedOffering, audience));
    }

    /** The cohort a placed Theory cell's audience belongs to, via its section's committed
     *  allocation — needed for the cohort-scoped Clinical Shift duty check, which cannot be derived
     *  from the row alone. Null for a row with no section (unsectioned cohort, or a row predating
     *  section-scoped placement), where the duty check is skipped rather than guessed at. */
    private Long audienceCohortId(ClassSchedule cs) {
        CohortSection section = cs.getCohortSection();
        if (section == null || section.getCohortRoomAllocation() == null) {
            return null;
        }
        return section.getCohortRoomAllocation().getCohort() != null
            ? section.getCohortRoomAllocation().getCohort().getId() : null;
    }

    /** What the subject we just displaced now owes, measured AFTER the replacement has been applied.
     *  Replacing hands a slot from one subject to another, so the loser silently drops below its
     *  weekly curriculum requirement somewhere else in the term — reported straight back so the
     *  admin knows to re-place it rather than finding out later from an hours card that stopped
     *  adding up. Null when nothing meaningful was displaced: no previous offering (a Library cell),
     *  no curriculum mapping to measure against, or the subject still meets its requirement without
     *  this slot because it was over quota or is covered elsewhere. */
    private SkeletonCellReplaceResponse.DisplacedSubjectShortfall describeDisplacedShortfall(
            CourseOffering displaced, CohortSection audience) {
        if (displaced == null || displaced.getCurriculumSemesterCourse() == null) {
            return null;
        }
        CurriculumSemesterCourse csc = displaced.getCurriculumSemesterCourse();
        int theoryHours = csc.getTheoryHours() != null ? csc.getTheoryHours() : 0;
        if (theoryHours <= 0) {
            return null;
        }
        TermInstance term = displaced.getTermInstance();
        int weeksInTerm = CurriculumHoursCalculator.weeksInTerm(term);
        List<Period> activePeriods = periodRepository.findByIsActiveTrueOrderByPeriodOrderAsc();
        double periodDurationMinutes = CurriculumHoursCalculator.averageDurationMinutes(
            activePeriods.stream().map(Period::getDurationMinutes).toList());
        int requiredRuns = CurriculumHoursCalculator.sessionsOverTerm(theoryHours, periodDurationMinutes, 1);

        Long audienceId = audience != null ? audience.getId() : null;
        List<ClassSchedule> placed = classScheduleRepository.findByCourseOfferingId(displaced.getId()).stream()
            .filter(row -> Boolean.TRUE.equals(row.getIsActive()))
            .filter(row -> row.getSessionType() == ClassSessionType.THEORY)
            .filter(row -> Objects.equals(
                row.getCohortSection() != null ? row.getCohortSection().getId() : null, audienceId))
            .toList();

        int delivered = deliveredRuns(placed, term, weeksInTerm);
        if (delivered >= requiredRuns) {
            return null;
        }
        return new SkeletonCellReplaceResponse.DisplacedSubjectShortfall(
            displaced.getId(),
            displaced.getSubject().getName(),
            displaced.getSubject().getCode(),
            audienceId,
            audience != null ? audience.getSectionLabel() : null,
            theoryHours, runsToHours(delivered, periodDurationMinutes),
            runsToHours(requiredRuns - delivered, periodDurationMinutes));
    }

    /** Manual placement from the Skeleton Builder — {@link #placeCell(SkeletonCellPlacementRequest)}
     *  plus a pin, so a cell an admin positioned by hand survives the next Global Auto-Schedule
     *  rebuild. Exists as its own method rather than a flag on {@code placeCell} because automation
     *  calls that same method thousands of times per run and must never pin anything; keeping the
     *  two entry points separate makes it impossible to confuse them. */
    @Transactional
    public SkeletonCellResponse placeCellManually(SkeletonCellPlacementRequest request) {
        SkeletonCellResponse placed = placeCell(request);
        return setPinned(placed.id(), true);
    }

    /** Pin or unpin a DRAFT cell. Unpinning is deliberately a real, separate decision: it hands the
     *  cell back to automation, so the next run may move or clear it. Applies to every row sharing
     *  the cell's {@code sessionGroupId}, since a multi-period session is one atomic unit — pinning
     *  only its first period would let a rebuild clear the rest and leave a truncated block. */
    @Transactional
    public SkeletonCellResponse setPinned(Long classScheduleId, boolean pinned) {
        ClassSchedule cs = classScheduleRepository.findById(classScheduleId)
            .orElseThrow(() -> new ResourceNotFoundException("Class schedule not found with id: " + classScheduleId));
        if (cs.getStatus() != ClassScheduleStatus.DRAFT) {
            throw new LifecycleConflictException(
                "Only a draft skeleton cell can be pinned — a published session is already immutable.",
                "SKELETON_CELL_NOT_DRAFT", "ClassSchedule", classScheduleId, null);
        }
        List<ClassSchedule> group = cs.getSessionGroupId() == null ? List.of(cs)
            : classScheduleRepository.findBySessionGroupIdOrderByPeriod_PeriodOrderAsc(cs.getSessionGroupId());
        for (ClassSchedule row : group) {
            row.setPinned(pinned);
            classScheduleRepository.save(row);
        }
        return toCellResponse(cs);
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

            if (isElectiveOffering(offering) && !isCommonCohortElective(offering)) {
                checkElectiveGroupSlot(offering, perPeriodRequest).ifPresent(violations::add);
            } else {
                checkCohortExclusivity(perPeriodRequest, offering, batch, cohortSection).ifPresent(violations::add);
            }

            checkBlocked(request.dayOfWeek(), spanPeriod, offering.getTermInstance()).ifPresent(violations::add);
            checkClinicalShiftBlocked(request.cohortId(), request.batchId(), request.dayOfWeek(), spanPeriod, offering.getTermInstance()).ifPresent(violations::add);
        }

        // enforceBudgetCap was accepted, documented, and then never actually read here -- the cap ran
        // unconditionally, so the ONE caller that passes false (the Self-Study/gap-fill pass, whose
        // entire purpose is to place filler BEYOND the curriculum quota) was capped by the very budget
        // it asks to bypass. Effect: gap-fill could only ever place as many sessions as the subject's
        // ordinary weekly quota, and every genuinely free period past that was rejected with
        // SKELETON_CELL_BUDGET_EXCEEDED and left blank in the grid -- the long-running "empty periods
        // the report can't explain" symptom.
        if (enforceBudgetCap) {
            checkBudgetNotExceeded(offering, request.sessionType(), batch, cohortSection)
                .ifPresent(violations::add);
        }

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

    /** {@code REQUIRES_NEW} -- commits {@code block}'s raw multi-period THEORY rows in their own
     *  transaction before returning, exactly like {@link #placeCell} already does for every other
     *  placement path. The one caller ({@code TimetableGlobalAutoScheduleService
     *  #saveIdleBatchSelfStudyCell}) constructs rows directly instead of going through {@link
     *  #placeCell} specifically to bypass its audience-exclusivity check (a sibling LAB/CLINICAL
     *  cell already occupies the whole section's audience scope at this exact slot for a different
     *  batch, even though this idle batch itself is genuinely free) -- but skipping {@code placeCell}
     *  also skips the {@code REQUIRES_NEW} commit that makes the row visible to a subsequent {@link
     *  TimetableStaffingService#staffCell} call, which runs in its own separate {@code REQUIRES_NEW}
     *  transaction/connection and cannot see a row that only exists, uncommitted, in the caller's
     *  still-open ambient transaction under READ_COMMITTED isolation -- surfacing as a spurious
     *  "Class schedule not found" the instant the caller tried to staff the row it had just "saved."
     *  Returns every saved row's id, primary period first. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<Long> saveIdleBatchTheoryCells(CourseOffering offering, TermInstance term, DayOfWeek day,
            List<Period> block, Batch batch, CohortSection cohortSection) {
        java.util.UUID sessionGroupId = block.size() > 1 ? java.util.UUID.randomUUID() : null;
        List<Long> ids = new ArrayList<>();
        for (Period period : block) {
            ClassSchedule cs = new ClassSchedule();
            cs.setSessionType(ClassSessionType.THEORY);
            cs.setStatus(ClassScheduleStatus.DRAFT);
            cs.setSubject(offering.getSubject());
            cs.setDayOfWeek(day);
            cs.setTermInstance(term);
            cs.setCourseOffering(offering);
            cs.setPeriod(period);
            cs.setBatch(batch);
            cs.setBatchName(batch.getName());
            cs.setCohortSection(cohortSection);
            cs.setIsActive(true);
            cs.setSessionGroupId(sessionGroupId);
            ClassSchedule saved = classScheduleRepository.save(cs);
            AutoScheduleRunCache.current().ifPresent(cache -> cache.recordPlacement(saved));
            ids.add(saved.getId());
        }
        return ids;
    }

    /** {@code REQUIRES_NEW} — the LIBRARY-typed twin of {@link #saveIdleBatchTheoryCells} just
     *  above, for the exact same reason: {@code TimetableGlobalAutoScheduleService
     *  #saveIdleBatchLibraryCell} used to save these rows directly via a plain {@code
     *  classScheduleRepository.save(...)} in its own long-lived ambient transaction (the whole
     *  global-auto-schedule run is one {@code @Transactional} that doesn't commit until the entire
     *  run finishes), which never made them visible to a LATER {@code REQUIRES_NEW} call on a
     *  separate connection under READ_COMMITTED isolation — concretely, {@link #forceRemoveCell},
     *  the very call {@code attemptBacktrack} makes when it later tries to bump one of these idle-
     *  batch Library placements. That invisibility surfaced as every such placement reporting
     *  "already missing from the database" the instant a run's own backtrack logic reached it, even
     *  though the row was sitting right there, just not yet committed. Returns every saved row's
     *  id, primary period first. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<Long> saveIdleBatchLibraryCells(Subject librarySubject, TermInstance term, DayOfWeek day,
            List<Period> block, Batch batch, CohortSection cohortSection, Classroom classroom) {
        java.util.UUID sessionGroupId = block.size() > 1 ? java.util.UUID.randomUUID() : null;
        List<Long> ids = new ArrayList<>();
        for (Period period : block) {
            ClassSchedule cs = new ClassSchedule();
            cs.setSessionType(ClassSessionType.LIBRARY);
            cs.setStatus(ClassScheduleStatus.DRAFT);
            cs.setSubject(librarySubject);
            cs.setDayOfWeek(day);
            cs.setTermInstance(term);
            cs.setCourseOffering(null);
            cs.setPeriod(period);
            cs.setClassroom(classroom);
            cs.setBatch(batch);
            cs.setBatchName(batch.getName());
            cs.setCohortSection(cohortSection);
            cs.setIsActive(true);
            cs.setSessionGroupId(sessionGroupId);
            ClassSchedule saved = classScheduleRepository.save(cs);
            AutoScheduleRunCache.current().ifPresent(cache -> cache.recordPlacement(saved));
            ids.add(saved.getId());
        }
        return ids;
    }

    /** {@code REQUIRES_NEW} — the whole-section/whole-cohort twin of {@link
     *  #saveIdleBatchLibraryCells} (no {@code batch}: {@code TimetableGlobalAutoScheduleService
     *  #fillLibraryGaps}'s regular Library filler places for a whole audience, not one idle batch),
     *  same reason: commits before returning so the row is visible to any later {@code REQUIRES_NEW}
     *  call on a separate connection, instead of sitting invisible in the caller's own long-lived
     *  ambient transaction under READ_COMMITTED isolation. Not currently reached by a same-run
     *  backtrack (see that method's own javadoc for the phase ordering that keeps it that way today),
     *  but kept consistent with every other idle-fill/filler placement path here rather than leaving
     *  this one as a dormant copy of the exact bug that {@link #saveIdleBatchLibraryCells} fixed.
     *  Returns every saved row entity, primary period first. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<ClassSchedule> saveLibraryBlockCells(Subject librarySubject, TermInstance term, DayOfWeek day,
            List<Period> block, CohortSection cohortSection, Classroom classroom) {
        return saveAudienceBlockCells(ClassSessionType.LIBRARY, librarySubject, term, day, block, cohortSection, classroom);
    }

    /** {@code REQUIRES_NEW} Sports twin of {@link #saveLibraryBlockCells}, for {@code
     *  TimetableGlobalAutoScheduleService#fillSportsGaps}: the same whole-audience block with its
     *  Sports-tagged classroom booked, saved unstaffed so the caller can then staff it with a PE
     *  faculty through the ordinary {@code staffCell} checks. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<ClassSchedule> saveSportsBlockCells(Subject sportsSubject, TermInstance term, DayOfWeek day,
            List<Period> block, CohortSection cohortSection, Classroom classroom) {
        return saveAudienceBlockCells(ClassSessionType.SPORTS, sportsSubject, term, day, block, cohortSection, classroom);
    }

    private List<ClassSchedule> saveAudienceBlockCells(ClassSessionType sessionType, Subject subject, TermInstance term,
            DayOfWeek day, List<Period> block, CohortSection cohortSection, Classroom classroom) {
        java.util.UUID sessionGroupId = block.size() > 1 ? java.util.UUID.randomUUID() : null;
        List<ClassSchedule> saved = new ArrayList<>();
        for (Period period : block) {
            ClassSchedule cs = new ClassSchedule();
            cs.setSessionType(sessionType);
            cs.setStatus(ClassScheduleStatus.DRAFT);
            cs.setSubject(subject);
            cs.setDayOfWeek(day);
            cs.setTermInstance(term);
            cs.setCourseOffering(null);
            cs.setPeriod(period);
            cs.setClassroom(classroom);
            cs.setCohortSection(cohortSection);
            cs.setIsActive(true);
            cs.setSessionGroupId(sessionGroupId);
            ClassSchedule persisted = classScheduleRepository.save(cs);
            AutoScheduleRunCache.current().ifPresent(cache -> cache.recordPlacement(persisted));
            saved.add(persisted);
        }
        return saved;
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
        return checkAlreadyPlacedExcluding(offering, request, excludeCellId == null ? Set.of() : Set.of(excludeCellId));
    }

    private Optional<ConstraintViolation> checkAlreadyPlacedExcluding(CourseOffering offering, SkeletonCellPlacementRequest request,
                                                                      Set<Long> excludeCellIds) {
        List<ClassSchedule> candidates = AutoScheduleRunCache.current()
            .map(cache -> cache.byCourseOfferingId(offering.getId()))
            .orElseGet(() -> classScheduleRepository.findByCourseOfferingId(offering.getId()));
        boolean alreadyPlaced = candidates.stream()
            .filter(cs -> Boolean.TRUE.equals(cs.getIsActive()))
            .filter(cs -> !isExcluded(excludeCellIds, cs.getId()))
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

        // validateRelocatedCell, not validateMoveTarget directly: a LIBRARY/SPORTS cell has no
        // CourseOffering by construction (see #saveLibraryBlockCells/#saveSportsBlockCells), and
        // validateMoveTargetExcluding unconditionally dereferences cs.getCourseOffering().getId() --
        // NPE'd on every attempt to drag-move one of those cells here before this used the same
        // null-safe branch #relocate already established.
        List<ConstraintViolation> violations = validateRelocatedCell(cs, request.dayOfWeek(), targetPeriod, request.cohortId(), Set.of());
        if (!violations.isEmpty()) {
            throw new TimetableConstraintViolationException(violations);
        }

        cs.setDayOfWeek(request.dayOfWeek());
        cs.setPeriod(targetPeriod);
        // A drag-move is a deliberate human decision, so it pins: the next Global Auto-Schedule
        // rebuild will pack the week around this cell instead of clearing it. Only this method and
        // #swapCells reach here (both are controller-only -- automation repositions cells through
        // forceRemoveCell + placeCell, never through a move), so pinning here can never mark an
        // automation-produced cell.
        cs.setPinned(true);
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
                // validateRelocatedCell: same null-CourseOffering NPE risk as #moveCell for a
                // LIBRARY/SPORTS cell — see that call site's comment.
                List<ConstraintViolation> violations = validateRelocatedCell(cs, day, period, cohortId, Set.of());
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

        // validateRelocatedCell: same null-CourseOffering NPE risk as #moveCell for a LIBRARY/SPORTS
        // cell — see that call site's comment.
        List<ConstraintViolation> violations = new ArrayList<>();
        violations.addAll(validateRelocatedCell(csA, dayB, periodB, request.cohortId(), Set.of(csB.getId())));
        violations.addAll(validateRelocatedCell(csB, dayA, periodA, request.cohortId(), Set.of(csA.getId())));
        if (!violations.isEmpty()) {
            throw new TimetableConstraintViolationException(violations);
        }

        csA.setDayOfWeek(dayB);
        csA.setPeriod(periodB);
        csB.setDayOfWeek(dayA);
        csB.setPeriod(periodA);
        // Both sides of a swap are deliberate placements -- pinning only the dragged one would let
        // the next rebuild clear its partner and silently undo half the admin's decision.
        csA.setPinned(true);
        csB.setPinned(true);
        ClassSchedule savedA = classScheduleRepository.save(csA);
        ClassSchedule savedB = classScheduleRepository.save(csB);
        return List.of(toCellResponse(savedA), toCellResponse(savedB));
    }

    // ── Block relocation and Clinical duty-day moves (2026-09-15) ──────────────────────────────────

    /** Everything that moves together when one session is dragged: every row of its multi-period
     *  block, every parallel batch of the same Lab/Clinical session (each in its own venue), an idle
     *  batch's Library/Self-Study fallback beside them, and a rotation partner sharing its slot — so
     *  a hand move keeps the cohort's batches aligned the way Run Automation places them.
     *  {@code periods} is the unit's own ordered run of periods on {@code day}. */
    private record MoveUnit(List<ClassSchedule> cells, DayOfWeek day, List<Period> periods) {
        Set<Long> ids() {
            return cells.stream().map(ClassSchedule::getId).collect(Collectors.toSet());
        }

        ClassSchedule anchor() {
            return cells.get(0);
        }
    }

    private record Slot(DayOfWeek day, Period period) {}

    private record CellState(DayOfWeek day, Period period, boolean pinned) {}

    /** {@code units} lists the dragged unit first, then every unit it swaps with; {@code newSlots}
     *  maps each of their rows to where it lands. */
    private record RelocationPlan(boolean valid, String reason, String kind, List<Period> targetPeriods,
                                  List<MoveUnit> units, Map<Long, Slot> newSlots) {
        static RelocationPlan refused(String reason) {
            return new RelocationPlan(false, reason, null, List.of(), List.of(), Map.of());
        }
    }

    private record DutyDayPlan(boolean valid, String reason, List<MoveUnit> units, Map<Long, Slot> newSlots) {
        static DutyDayPlan refused(String reason) {
            return new DutyDayPlan(false, reason, List.of(), Map.of());
        }
    }

    /** Live drag-highlight and Swap-menu data for moving {@code classScheduleId} together with its
     *  whole block: one entry per same-length window (every day × every possible start period), each
     *  saying whether it's legal, why not if it isn't, and — when it is — what moves where. A window
     *  that's empty is a MOVE; one already holding sessions is a SWAP, and those sessions take the
     *  periods the block frees. Judged with every moving row set aside; {@link #relocate} re-judges
     *  the real arrangement and remains the only source of truth. */
    public List<SkeletonRelocationPlanResponse> previewRelocation(Long classScheduleId, Long cohortId) {
        ClassSchedule anchor = classScheduleRepository.findById(classScheduleId)
            .orElseThrow(() -> new ResourceNotFoundException("Class schedule not found with id: " + classScheduleId));
        if (anchor.getStatus() != ClassScheduleStatus.DRAFT || isSharedSlotElective(anchor)) {
            return List.of();
        }
        MoveUnit unit = resolveUnit(anchor);
        List<Period> activePeriods = periodRepository.findByIsActiveTrueOrderByPeriodOrderAsc();
        List<SkeletonRelocationPlanResponse> results = new ArrayList<>();
        for (DayOfWeek day : DayOfWeek.values()) {
            for (Period start : activePeriods) {
                if (day == unit.day() && start.getId().equals(unit.periods().get(0).getId())) {
                    continue;
                }
                RelocationPlan plan = planRelocation(unit, day, start.getId(), cohortId, activePeriods);
                results.add(new SkeletonRelocationPlanResponse(day, start.getId(), periodIds(plan.targetPeriods()),
                    plan.kind(), plan.valid(), plan.reason(),
                    plan.valid() ? plannedMoves(plan.units(), plan.newSlots()) : List.of()));
            }
        }
        return results;
    }

    /** Moves a session with its whole block to the same-length window starting at the requested
     *  day/period — a move into empty periods, or a swap with the sessions already there, which take
     *  the periods the block frees in the same order. All-or-nothing: every row is placed first, then
     *  each is re-judged against the real resulting week (faculty clashes and daily caps included),
     *  and any failure rolls the whole change back. Every moved row is pinned, like any hand move. */
    @Transactional
    public List<SkeletonCellResponse> relocate(Long classScheduleId, SkeletonRelocateRequest request) {
        ClassSchedule anchor = classScheduleRepository.findById(classScheduleId)
            .orElseThrow(() -> new ResourceNotFoundException("Class schedule not found with id: " + classScheduleId));
        if (anchor.getStatus() != ClassScheduleStatus.DRAFT) {
            throw new LifecycleConflictException("Only a draft skeleton cell can be moved here.",
                "SKELETON_CELL_NOT_DRAFT", "ClassSchedule", classScheduleId, null);
        }
        if (isSharedSlotElective(anchor)) {
            throw new IllegalArgumentException(
                "Student-choice elective sessions move only with Run Automation — every option in the group shares one slot.");
        }
        MoveUnit unit = resolveUnit(anchor);
        List<Period> activePeriods = periodRepository.findByIsActiveTrueOrderByPeriodOrderAsc();
        RelocationPlan plan = planRelocation(unit, request.dayOfWeek(), request.startPeriodId(), request.cohortId(), activePeriods);
        if (!plan.valid()) {
            throw new TimetableConstraintViolationException(List.of(new ConstraintViolation("SKELETON_RELOCATE_REFUSED", plan.reason())));
        }
        applySlots(plan.units(), plan.newSlots());
        List<ConstraintViolation> violations = revalidateInPlace(plan.units(), request.cohortId());
        if (!violations.isEmpty()) {
            throw new TimetableConstraintViolationException(violations);
        }
        return plan.units().stream().flatMap(u -> u.cells().stream()).map(this::toCellResponse).toList();
    }

    /** For each other day, whether a Clinical Shift group's duty could move there and which of that
     *  day's sessions inside the duty window would swap into the day it leaves. Each candidate day is
     *  genuinely tried — the duty and the swapped sessions are moved, the week is re-judged exactly as
     *  {@link #moveDutyDay} would judge it, and everything is put back — and the whole transaction is
     *  rolled back at the end, so a preview can never change anything. */
    @Transactional
    public List<DutyDayMovePreviewResponse> previewDutyDayMove(Long shiftGroupId, Long cohortId) {
        ClinicalShiftGroup group = clinicalShiftGroupRepository.findById(shiftGroupId)
            .orElseThrow(() -> new ResourceNotFoundException("Clinical shift group not found with id: " + shiftGroupId));
        DayOfWeek originalDay = group.getDayOfWeek();
        List<Period> activePeriods = periodRepository.findByIsActiveTrueOrderByPeriodOrderAsc();
        List<DutyDayMovePreviewResponse> results = new ArrayList<>();
        try {
            for (DayOfWeek day : DayOfWeek.values()) {
                if (day == originalDay) {
                    continue;
                }
                DutyDayPlan plan = planDutyDayMove(group, day, cohortId, activePeriods);
                if (plan.valid()) {
                    Map<Long, CellState> before = snapshot(plan.units());
                    applyDutyDay(group, day, plan);
                    List<ConstraintViolation> violations = revalidateDutyDay(group, day, cohortId, plan.units());
                    restore(group, originalDay, plan.units(), before);
                    if (!violations.isEmpty()) {
                        plan = DutyDayPlan.refused(violations.get(0).message());
                    }
                }
                results.add(new DutyDayMovePreviewResponse(day, plan.valid(), plan.reason(),
                    plan.valid() ? plannedMoves(plan.units(), plan.newSlots()) : List.of()));
            }
        } finally {
            // Every tried day was already put back; this makes sure nothing it touched can commit.
            if (org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()) {
                org.springframework.transaction.interceptor.TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            }
        }
        return results;
    }

    /** Moves a Clinical Shift group's duty to another day for this cohort. The sessions that sit
     *  inside the duty window on the new day swap into the same periods of the day the duty leaves;
     *  the new day must pass every check Run Automation applies (blocked days, the cohort not already
     *  away on another duty then, the clinical venue's capacity that day, each coordinator's
     *  availability), and every swapped session is re-judged in its new slot. All-or-nothing, and the
     *  swapped sessions are pinned. A label naming the old day ("… (Friday)") is updated to the new one. */
    @Transactional
    public List<SkeletonCellResponse> moveDutyDay(Long shiftGroupId, DutyDayMoveRequest request) {
        ClinicalShiftGroup group = clinicalShiftGroupRepository.findById(shiftGroupId)
            .orElseThrow(() -> new ResourceNotFoundException("Clinical shift group not found with id: " + shiftGroupId));
        DayOfWeek oldDay = group.getDayOfWeek();
        if (request.dayOfWeek() == oldDay) {
            throw new IllegalArgumentException("This duty is already on " + dayLabel(oldDay) + ".");
        }
        DutyDayPlan plan = planDutyDayMove(group, request.dayOfWeek(), request.cohortId(),
            periodRepository.findByIsActiveTrueOrderByPeriodOrderAsc());
        if (!plan.valid()) {
            throw new TimetableConstraintViolationException(List.of(new ConstraintViolation("SKELETON_DUTY_DAY_REFUSED", plan.reason())));
        }
        applyDutyDay(group, request.dayOfWeek(), plan);
        List<ConstraintViolation> violations = revalidateDutyDay(group, request.dayOfWeek(), request.cohortId(), plan.units());
        if (!violations.isEmpty()) {
            throw new TimetableConstraintViolationException(violations);
        }
        String oldDaySuffix = "(" + dayLabel(oldDay) + ")";
        if (group.getLabel() != null && group.getLabel().endsWith(oldDaySuffix)) {
            group.setLabel(group.getLabel().substring(0, group.getLabel().length() - oldDaySuffix.length())
                + "(" + dayLabel(request.dayOfWeek()) + ")");
            clinicalShiftGroupRepository.save(group);
        }
        return plan.units().stream().flatMap(u -> u.cells().stream()).map(this::toCellResponse).toList();
    }

    private MoveUnit resolveUnit(ClassSchedule anchor) {
        Map<Long, ClassSchedule> cells = new LinkedHashMap<>();
        java.util.Deque<ClassSchedule> pending = new java.util.ArrayDeque<>();
        pending.add(anchor);
        while (!pending.isEmpty()) {
            ClassSchedule cs = pending.poll();
            if (cells.containsKey(cs.getId()) || !Boolean.TRUE.equals(cs.getIsActive())) {
                continue;
            }
            cells.put(cs.getId(), cs);
            if (cs.getSessionGroupId() != null) {
                pending.addAll(classScheduleRepository.findBySessionGroupIdOrderByPeriod_PeriodOrderAsc(cs.getSessionGroupId()));
            }
            pending.addAll(cellsAlongside(cs));
        }
        return new MoveUnit(new ArrayList<>(cells.values()), anchor.getDayOfWeek(),
            distinctOrderedPeriods(cells.values().stream().map(ClassSchedule::getPeriod).toList()));
    }

    /** Rows running in lockstep with {@code cs} at its exact day/period: the other batches of the
     *  same offering and section (parallel Lab/Clinical batches, and an idle batch's fallback beside
     *  them), and the rows sharing its Lab rotation slot. Only a venue batch has parallel siblings —
     *  a Theory row's batch is its section, whose other sections are separate classes. */
    private List<ClassSchedule> cellsAlongside(ClassSchedule cs) {
        List<ClassSchedule> alongside = new ArrayList<>();
        Batch batch = cs.getBatch();
        if (batch != null && batch.getCourseOffering() != null && (batch.getLab() != null || batch.getClinicalVenue() != null)) {
            Long sectionId = batch.getCohortSection() != null ? batch.getCohortSection().getId() : null;
            List<Long> siblingBatchIds = batchRepository.findByCourseOfferingId(batch.getCourseOffering().getId()).stream()
                .filter(b -> Boolean.TRUE.equals(b.getIsActive()))
                .filter(b -> Objects.equals(b.getCohortSection() != null ? b.getCohortSection().getId() : null, sectionId))
                .map(Batch::getId)
                .toList();
            if (!siblingBatchIds.isEmpty()) {
                classScheduleRepository.findByBatchIdInAndIsActiveTrue(siblingBatchIds).stream()
                    .filter(other -> sameDayAndPeriod(other, cs))
                    .forEach(alongside::add);
            }
        }
        rotationSlotRepository.findByClassScheduleId(cs.getId()).ifPresent(slot ->
            rotationSlotRepository.findByRotationGroupIdOrderBySlotOrderAsc(slot.getRotationGroup().getId()).stream()
                .map(RotationSlot::getClassSchedule)
                .filter(other -> other != null && Boolean.TRUE.equals(other.getIsActive()) && sameDayAndPeriod(other, cs))
                .forEach(alongside::add));
        return alongside;
    }

    private RelocationPlan planRelocation(MoveUnit unit, DayOfWeek day, Long startPeriodId, Long cohortId, List<Period> activePeriods) {
        int blockLength = unit.periods().size();
        int start = periodIds(activePeriods).indexOf(startPeriodId);
        if (start < 0) {
            return RelocationPlan.refused("That period isn't an active teaching period.");
        }
        if (start + blockLength > activePeriods.size()) {
            return RelocationPlan.refused("A " + blockLength + "-period session starting at " + activePeriods.get(start).getName()
                + " would run past the end of the day.");
        }
        List<Period> target = activePeriods.subList(start, start + blockLength);
        String breakReason = spanBreakReason(unit.anchor().getSessionType(), target, activePeriods);
        if (breakReason != null) {
            return RelocationPlan.refused(breakReason);
        }
        List<Long> unitPeriodIds = periodIds(unit.periods());
        List<Long> targetIds = periodIds(target);
        if (day == unit.day() && targetIds.equals(unitPeriodIds)) {
            return RelocationPlan.refused("The session is already here.");
        }

        // Every session in the window this one can't share a slot with has to trade places with it.
        Long termInstanceId = unit.anchor().getTermInstance().getId();
        Set<Long> unitIds = unit.ids();
        List<MoveUnit> occupants = new ArrayList<>();
        Set<Long> occupantIds = new HashSet<>();
        for (Period period : target) {
            for (ClassSchedule other : cohortCellsAtSlotExcluding(cohortId, termInstanceId, day, period.getId(), unitIds)) {
                if (occupantIds.contains(other.getId()) || unit.cells().stream().noneMatch(u -> cannotShareSlot(u, other))) {
                    continue;
                }
                if (other.getStatus() != ClassScheduleStatus.DRAFT) {
                    return RelocationPlan.refused(describe(other) + " is already approved and can't be moved.");
                }
                if (isSharedSlotElective(other)) {
                    return RelocationPlan.refused(describe(other) + " is a student-choice elective — only Run Automation moves it.");
                }
                MoveUnit occupant = resolveUnit(other);
                if (!java.util.Collections.disjoint(occupant.ids(), unitIds)) {
                    continue;
                }
                if (!targetIds.containsAll(periodIds(occupant.periods()))) {
                    return RelocationPlan.refused(describe(other) + " runs past this window — choose a window that covers the whole session.");
                }
                occupants.add(occupant);
                occupantIds.addAll(occupant.ids());
            }
        }

        // The block takes the window; whatever filled it takes the periods the block frees, in order.
        Map<Long, Slot> newSlots = new LinkedHashMap<>();
        for (ClassSchedule cs : unit.cells()) {
            newSlots.put(cs.getId(), new Slot(day, target.get(unitPeriodIds.indexOf(cs.getPeriod().getId()))));
        }
        List<Period> freed = day == unit.day()
            ? unit.periods().stream().filter(p -> !targetIds.contains(p.getId())).toList()
            : unit.periods();
        List<Long> taken = day == unit.day()
            ? targetIds.stream().filter(id -> !unitPeriodIds.contains(id)).toList()
            : targetIds;
        for (MoveUnit occupant : occupants) {
            List<Period> landing = new ArrayList<>();
            for (ClassSchedule cs : occupant.cells()) {
                int position = taken.indexOf(cs.getPeriod().getId());
                if (position < 0 || position >= freed.size()) {
                    return RelocationPlan.refused(describe(cs) + " can't be fitted into the periods this session frees.");
                }
                newSlots.put(cs.getId(), new Slot(unit.day(), freed.get(position)));
                landing.add(freed.get(position));
            }
            String landingBreak = spanBreakReason(occupant.anchor().getSessionType(), distinctOrderedPeriods(landing), activePeriods);
            if (landingBreak != null) {
                return RelocationPlan.refused(describe(occupant.anchor()) + ": " + landingBreak);
            }
        }

        List<MoveUnit> units = new ArrayList<>();
        units.add(unit);
        units.addAll(occupants);
        Set<Long> moving = new HashSet<>(newSlots.keySet());
        for (MoveUnit u : units) {
            for (ClassSchedule cs : u.cells()) {
                Slot slot = newSlots.get(cs.getId());
                List<ConstraintViolation> violations = validateRelocatedCell(cs, slot.day(), slot.period(), cohortId, moving);
                if (!violations.isEmpty()) {
                    return RelocationPlan.refused(describe(cs) + ": " + violations.get(0).message());
                }
            }
        }
        return new RelocationPlan(true, null, occupants.isEmpty() ? "MOVE" : "SWAP", target, units, newSlots);
    }

    private DutyDayPlan planDutyDayMove(ClinicalShiftGroup group, DayOfWeek newDay, Long cohortId, List<Period> activePeriods) {
        TermInstance term = group.getTermInstance();
        String dayName = dayLabel(newDay);
        if (classScheduleRepository.existsByTermInstanceIdAndStatus(term.getId(), ClassScheduleStatus.PUBLISHED)) {
            return DutyDayPlan.refused("This term's timetable is already approved — revert it to draft on Draft Review first.");
        }
        ClinicalShiftWindow window = ClinicalShiftWindow.from(group);
        if (window.busDepart() == null || window.busReturn() == null) {
            return DutyDayPlan.refused("Set this offering's clinical duty length and travel buffer first.");
        }
        Optional<String> blocked = blockedPeriodChecker.blockReason(newDay, window.busDepart(), window.busReturn(), term);
        if (blocked.isPresent()) {
            return DutyDayPlan.refused(dayName + " is blocked then: " + blocked.get());
        }

        String dutyScope = scopeKeyForSectionId(group.getCohortSection() != null ? group.getCohortSection().getId() : null);
        for (ClinicalShiftWindow other : clinicalShiftGroupService.resolveActiveWindowsForCohort(cohortId, term.getId())) {
            if (other.shiftGroupId().equals(group.getId()) || other.dayOfWeek() != newDay || !dutyTimesOverlap(window, other)) {
                continue;
            }
            ClinicalShiftGroup otherGroup = clinicalShiftGroupRepository.findById(other.shiftGroupId()).orElse(null);
            if (otherGroup != null
                    && scopesConflict(dutyScope, scopeKeyForSectionId(otherGroup.getCohortSection() != null ? otherGroup.getCohortSection().getId() : null))
                    && dutiesShareStudents(group, otherGroup)) {
                return DutyDayPlan.refused("This cohort is already away on " + other.label() + " on " + dayName + " at that time.");
            }
        }
        String venueGap = dutyVenueCapacityGap(group, newDay, window);
        if (venueGap != null) {
            return DutyDayPlan.refused(venueGap);
        }

        List<Period> windowPeriods = activePeriods.stream()
            .filter(p -> window.overlaps(p.getStartTime(), p.getEndTime()))
            .toList();
        List<Long> windowIds = periodIds(windowPeriods);
        List<MoveUnit> units = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        for (Period period : windowPeriods) {
            for (ClassSchedule cs : cohortCellsAtSlotExcluding(cohortId, term.getId(), newDay, period.getId(), Set.of())) {
                if (seen.contains(cs.getId()) || !affectedByDuty(group, dutyScope, cs)) {
                    continue;
                }
                if (cs.getStatus() != ClassScheduleStatus.DRAFT) {
                    return DutyDayPlan.refused(describe(cs) + " on " + dayName + " is already approved and can't be moved.");
                }
                if (isSharedSlotElective(cs)) {
                    return DutyDayPlan.refused(describe(cs) + " on " + dayName + " is a student-choice elective — only Run Automation moves it.");
                }
                MoveUnit unit = resolveUnit(cs);
                if (!windowIds.containsAll(periodIds(unit.periods()))) {
                    return DutyDayPlan.refused(describe(cs) + " on " + dayName + " runs past the duty window — move it first.");
                }
                units.add(unit);
                seen.addAll(unit.ids());
            }
        }
        DayOfWeek oldDay = group.getDayOfWeek();
        Map<Long, Slot> newSlots = new LinkedHashMap<>();
        units.forEach(u -> u.cells().forEach(cs -> newSlots.put(cs.getId(), new Slot(oldDay, cs.getPeriod()))));
        return new DutyDayPlan(true, null, units, newSlots);
    }

    /** A session the duty takes students away from: one whose audience overlaps the duty's section
     *  (or whole cohort), unless it belongs to a batch linked to a different duty group — the same
     *  narrowing {@link ClinicalShiftGroupService#resolveActiveWindowsForBatch} applies. */
    private boolean affectedByDuty(ClinicalShiftGroup group, String dutyScope, ClassSchedule cs) {
        if (!scopesConflict(dutyScope, scopeKeyForCell(cs))) {
            return false;
        }
        Batch batch = cs.getBatch();
        return batch == null || batch.getClinicalShiftGroup() == null || batch.getClinicalShiftGroup().getId().equals(group.getId());
    }

    /** Two duties take the same students unless both are linked to batches and those batches differ
     *  — an unlinked duty is treated as the whole audience, as the grid already does. */
    private boolean dutiesShareStudents(ClinicalShiftGroup a, ClinicalShiftGroup b) {
        Set<Long> aBatches = batchRepository.findByClinicalShiftGroupId(a.getId()).stream().map(Batch::getId).collect(Collectors.toSet());
        Set<Long> bBatches = batchRepository.findByClinicalShiftGroupId(b.getId()).stream().map(Batch::getId).collect(Collectors.toSet());
        return aBatches.isEmpty() || bBatches.isEmpty() || !java.util.Collections.disjoint(aBatches, bBatches);
    }

    private static boolean dutyTimesOverlap(ClinicalShiftWindow a, ClinicalShiftWindow b) {
        if (a.busDepart() == null || a.busReturn() == null || b.busDepart() == null || b.busReturn() == null) {
            return true;
        }
        return a.busDepart().isBefore(b.busReturn()) && b.busDepart().isBefore(a.busReturn());
    }

    /** The clinical venue check Run Automation's capacity precheck applies, for one day: every
     *  batch this duty sends to a venue, plus every other duty's batches at that venue the same day
     *  and time, must fit its capacity. Skipped for a duty with no linked batches — without them
     *  there's no venue to measure. */
    private String dutyVenueCapacityGap(ClinicalShiftGroup group, DayOfWeek newDay, ClinicalShiftWindow window) {
        Map<Long, List<Batch>> ownByVenue = batchRepository.findByClinicalShiftGroupId(group.getId()).stream()
            .filter(b -> Boolean.TRUE.equals(b.getIsActive()) && b.getClinicalVenue() != null && b.getClinicalVenue().getCapacity() != null)
            .collect(Collectors.groupingBy(b -> b.getClinicalVenue().getId()));
        if (ownByVenue.isEmpty()) {
            return null;
        }
        List<ClinicalShiftGroup> sameTime = clinicalShiftGroupRepository.findByTermInstanceIdAndIsActiveTrue(group.getTermInstance().getId())
            .stream()
            .filter(other -> !other.getId().equals(group.getId()) && other.getDayOfWeek() == newDay
                && dutyTimesOverlap(window, ClinicalShiftWindow.from(other)))
            .toList();
        for (List<Batch> own : ownByVenue.values()) {
            ClinicalVenue venue = own.get(0).getClinicalVenue();
            long students = own.stream().mapToLong(b -> batchRepository.countStudents(b.getId())).sum();
            for (ClinicalShiftGroup other : sameTime) {
                students += batchRepository.findByClinicalShiftGroupId(other.getId()).stream()
                    .filter(b -> Boolean.TRUE.equals(b.getIsActive()) && b.getClinicalVenue() != null
                        && b.getClinicalVenue().getId().equals(venue.getId()))
                    .mapToLong(b -> batchRepository.countStudents(b.getId()))
                    .sum();
            }
            if (students > venue.getCapacity()) {
                return venue.getName() + " would have " + students + " students on duty on " + dayLabel(newDay)
                    + " at that time, over its capacity of " + venue.getCapacity() + ".";
            }
        }
        return null;
    }

    /** Each coordinator of a batch on this duty must be available and not teaching during the duty
     *  on its new day. Checked after the swap is applied, so a session of theirs that just left the
     *  new day no longer counts against them. */
    private List<ConstraintViolation> dutyCoordinatorViolations(ClinicalShiftGroup group, DayOfWeek newDay) {
        ClinicalShiftWindow window = ClinicalShiftWindow.from(group);
        LocalTime dutyEnd = window.clinicalEnd() != null ? window.clinicalEnd() : window.busReturn();
        Map<Long, Faculty> coordinators = new LinkedHashMap<>();
        batchRepository.findByClinicalShiftGroupId(group.getId()).stream()
            .filter(b -> Boolean.TRUE.equals(b.getIsActive()) && b.getCoordinatorFaculty() != null)
            .forEach(b -> coordinators.putIfAbsent(b.getCoordinatorFaculty().getId(), b.getCoordinatorFaculty()));
        List<ConstraintViolation> violations = new ArrayList<>();
        for (Faculty coordinator : coordinators.values()) {
            timetableStaffingService.checkFacultyAvailable(coordinator.getId(), newDay, window.clinicalStart(), dutyEnd, null)
                .or(() -> timetableStaffingService.checkFacultyFree(coordinator.getId(), group.getTermInstance().getId(), null,
                    newDay, window.busDepart(), window.busReturn()))
                .map(v -> new ConstraintViolation(v.code(), "Coordinator " + coordinator.getFullName() + ": " + v.message()))
                .ifPresent(violations::add);
        }
        return violations;
    }

    private void applyDutyDay(ClinicalShiftGroup group, DayOfWeek newDay, DutyDayPlan plan) {
        group.setDayOfWeek(newDay);
        clinicalShiftGroupRepository.save(group);
        applySlots(plan.units(), plan.newSlots());
    }

    private List<ConstraintViolation> revalidateDutyDay(ClinicalShiftGroup group, DayOfWeek newDay, Long cohortId, List<MoveUnit> units) {
        List<ConstraintViolation> violations = revalidateInPlace(units, cohortId);
        violations.addAll(dutyCoordinatorViolations(group, newDay));
        return violations;
    }

    private Map<Long, CellState> snapshot(List<MoveUnit> units) {
        Map<Long, CellState> states = new LinkedHashMap<>();
        units.forEach(u -> u.cells().forEach(cs -> states.put(cs.getId(), new CellState(cs.getDayOfWeek(), cs.getPeriod(), cs.isPinned()))));
        return states;
    }

    private void restore(ClinicalShiftGroup group, DayOfWeek originalDay, List<MoveUnit> units, Map<Long, CellState> before) {
        group.setDayOfWeek(originalDay);
        clinicalShiftGroupRepository.save(group);
        for (MoveUnit u : units) {
            for (ClassSchedule cs : u.cells()) {
                CellState state = before.get(cs.getId());
                cs.setDayOfWeek(state.day());
                cs.setPeriod(state.period());
                cs.setPinned(state.pinned());
                classScheduleRepository.save(cs);
            }
        }
        classScheduleRepository.flush();
    }

    /** Puts every row where its plan lands it, pinned (a deliberate human arrangement), and flushes
     *  so the re-judging queries that follow see the real resulting week. */
    private void applySlots(List<MoveUnit> units, Map<Long, Slot> newSlots) {
        for (MoveUnit u : units) {
            for (ClassSchedule cs : u.cells()) {
                Slot slot = newSlots.get(cs.getId());
                cs.setDayOfWeek(slot.day());
                cs.setPeriod(slot.period());
                cs.setPinned(true);
                classScheduleRepository.save(cs);
            }
        }
        classScheduleRepository.flush();
    }

    /** Re-judges every moved row where it now sits, setting aside only its own unit's rows (which
     *  sit together by design) — everything else, including the unit it swapped with, is judged at
     *  its real new position. */
    private List<ConstraintViolation> revalidateInPlace(List<MoveUnit> units, Long cohortId) {
        Set<ConstraintViolation> violations = new LinkedHashSet<>();
        for (MoveUnit u : units) {
            Set<Long> ownRows = u.ids();
            for (ClassSchedule cs : u.cells()) {
                validateRelocatedCell(cs, cs.getDayOfWeek(), cs.getPeriod(), cohortId, ownRows).stream()
                    .map(v -> new ConstraintViolation(v.code(), describe(cs) + ": " + v.message()))
                    .forEach(violations::add);
            }
        }
        return new ArrayList<>(violations);
    }

    private List<ConstraintViolation> validateRelocatedCell(ClassSchedule cs, DayOfWeek day, Period period, Long cohortId,
                                                            Set<Long> excludeCellIds) {
        return cs.getCourseOffering() != null
            ? validateMoveTargetExcluding(cs, day, period, cohortId, excludeCellIds)
            : validateAudienceBlockTarget(cs, day, period, cohortId, excludeCellIds);
    }

    /** {@link #validateMoveTargetExcluding}'s counterpart for a row with no CourseOffering — a
     *  Library or Sports block, or an idle batch's Library fallback: its audience must be free, the
     *  slot unblocked and outside any Clinical duty, and its room (and a Sports block's PE faculty)
     *  free at the new time. */
    private List<ConstraintViolation> validateAudienceBlockTarget(ClassSchedule cs, DayOfWeek day, Period period, Long cohortId,
                                                                  Set<Long> excludeCellIds) {
        List<ConstraintViolation> violations = new ArrayList<>();
        TermInstance term = cs.getTermInstance();
        cohortCellsAtSlotExcluding(cohortId, term.getId(), day, period.getId(), excludeCellIds).stream()
            .filter(other -> cannotShareSlot(cs, other))
            .findFirst()
            .ifPresent(other -> violations.add(new ConstraintViolation("SKELETON_CELL_COHORT_CLASH",
                (other.getSubject() != null ? other.getSubject().getName() : "Another session")
                    + " already has a session in this slot for this audience")));
        checkBlocked(day, period, term).ifPresent(violations::add);
        checkClinicalShiftBlocked(cohortId, cs.getBatch() != null ? cs.getBatch().getId() : null, day, period, term)
            .ifPresent(violations::add);
        Long venueId = TimetableStaffingService.venueIdOf(cs);
        TimetableStaffingService.RoomCheckSpec roomCheck = venueId != null
            ? new TimetableStaffingService.RoomCheckSpec(cs.getSessionType(), venueId, TimetableStaffingService.physicalRoomOf(cs),
                TimetableStaffingService.RoomMode.STRICT)
            : null;
        if (roomCheck != null || cs.getFaculty() != null) {
            violations.addAll(timetableStaffingService.validateAssignmentExcluding(cs, day, period.getStartTime(), period.getEndTime(),
                cs.getFaculty(), excludeCellIds, roomCheck, null, null).violations());
        }
        return violations;
    }

    /** A row its whole section (or cohort) attends: Theory, and a section-level Library or Sports
     *  block. A batch-scoped row — a Lab/Clinical batch, or an idle batch's fallback — is attended
     *  by that batch alone. */
    private static boolean attendedByWholeAudience(ClassSchedule cs) {
        return cs.getSessionType() == ClassSessionType.THEORY
            || ((cs.getSessionType() == ClassSessionType.LIBRARY || cs.getSessionType() == ClassSessionType.SPORTS)
                && cs.getBatch() == null);
    }

    /** Two rows can't share a slot when their audiences overlap and at least one is attended by its
     *  whole audience — the same rule {@link #checkCohortExclusivity} applies at placement, where two
     *  different subjects' Lab batches may share a slot. */
    private boolean cannotShareSlot(ClassSchedule a, ClassSchedule b) {
        return scopesConflict(scopeKeyForCell(a), scopeKeyForCell(b))
            && (attendedByWholeAudience(a) || attendedByWholeAudience(b));
    }

    private boolean isSharedSlotElective(ClassSchedule cs) {
        return cs.getCourseOffering() != null && isSharedSlotElective(cs.getCourseOffering());
    }

    /** Null when {@code span} is a legal block for {@code sessionType}: back-to-back periods, except
     *  that a Clinical block may run through a short recess (never lunch) — {@link PeriodGapPolicy}. */
    private static String spanBreakReason(ClassSessionType sessionType, List<Period> span, List<Period> activePeriods) {
        for (int i = 1; i < span.size(); i++) {
            Period before = span.get(i - 1);
            Period after = span.get(i);
            if (!before.getEndTime().equals(after.getStartTime())
                    && !PeriodGapPolicy.gapCrossableFor(sessionType, before, after, activePeriods)) {
                return "A " + span.size() + "-period session can't run across the break between " + before.getName()
                    + " and " + after.getName() + ".";
            }
        }
        return null;
    }

    private List<SkeletonPlannedMove> plannedMoves(List<MoveUnit> units, Map<Long, Slot> newSlots) {
        return units.stream().map(u -> {
            List<Period> landing = distinctOrderedPeriods(u.cells().stream().map(cs -> newSlots.get(cs.getId()).period()).toList());
            String subjects = u.cells().stream().map(cs -> cs.getSubject() != null ? cs.getSubject().getCode() : null)
                .filter(Objects::nonNull).distinct().collect(Collectors.joining(" + "));
            String occupants = u.cells().stream()
                .map(cs -> cs.getBatch() != null ? cs.getBatch().getName()
                    : cs.getCohortSection() != null ? cs.getCohortSection().getSectionLabel() : null)
                .filter(Objects::nonNull).distinct().collect(Collectors.joining(", "));
            return new SkeletonPlannedMove(subjects, u.anchor().getSessionType(), occupants.isEmpty() ? null : occupants,
                u.day(), periodIds(u.periods()), newSlots.get(u.anchor().getId()).day(), periodIds(landing));
        }).toList();
    }

    private static String describe(ClassSchedule cs) {
        String type = cs.getSessionType().name();
        return (cs.getSubject() != null ? cs.getSubject().getName() : "A session")
            + " (" + type.charAt(0) + type.substring(1).toLowerCase() + ")";
    }

    private static String dayLabel(DayOfWeek day) {
        return day.name().charAt(0) + day.name().substring(1).toLowerCase();
    }

    /** Null-safe: an unsaved row (no id yet) is never one of the excluded rows. */
    private static boolean isExcluded(Set<Long> excludedIds, Long id) {
        return id != null && excludedIds.contains(id);
    }

    private static boolean sameDayAndPeriod(ClassSchedule a, ClassSchedule b) {
        return a.getDayOfWeek() == b.getDayOfWeek() && a.getPeriod() != null && b.getPeriod() != null
            && a.getPeriod().getId().equals(b.getPeriod().getId());
    }

    private static List<Long> periodIds(List<Period> periods) {
        return periods.stream().map(Period::getId).toList();
    }

    private static List<Period> distinctOrderedPeriods(List<Period> periods) {
        Map<Long, Period> byId = new LinkedHashMap<>();
        periods.stream().filter(Objects::nonNull).forEach(p -> byId.putIfAbsent(p.getId(), p));
        return byId.values().stream().sorted(Comparator.comparing(Period::getPeriodOrder)).toList();
    }

    /** Every check a placed cell moving to (day, targetPeriod) must pass, for a cell that has a
     *  CourseOffering — called only via {@link #validateRelocatedCell}, which routes a
     *  no-CourseOffering LIBRARY/SPORTS cell to {@link #validateAudienceBlockTarget} instead, since
     *  this method unconditionally dereferences {@code cs.getCourseOffering()}. Room/capacity/
     *  faculty-eligibility are deliberately NOT rechecked here: none of them change on a pure
     *  day/period move (the room, audience, and faculty all stay exactly what they already were). */
    private List<ConstraintViolation> validateMoveTargetExcluding(ClassSchedule cs, DayOfWeek day, Period targetPeriod, Long cohortId,
                                                                  Set<Long> excludeCellIds) {
        CourseOffering offering = cs.getCourseOffering();
        SkeletonCellPlacementRequest asPlacementRequest = new SkeletonCellPlacementRequest(
            offering.getId(), cs.getSessionType(), day, targetPeriod.getId(),
            cs.getBatch() != null ? cs.getBatch().getId() : null,
            cohortId,
            cs.getCohortSection() != null ? cs.getCohortSection().getId() : null,
            null);

        List<ConstraintViolation> violations = new ArrayList<>();
        checkAlreadyPlacedExcluding(offering, asPlacementRequest, excludeCellIds).ifPresent(violations::add);
        if (isElectiveOffering(offering) && !isCommonCohortElective(offering)) {
            checkElectiveGroupSlot(offering, asPlacementRequest).ifPresent(violations::add);
        } else {
            checkCohortExclusivityExcluding(asPlacementRequest, offering, cs.getBatch(), cs.getCohortSection(), excludeCellIds)
                .ifPresent(violations::add);
        }
        checkBlocked(day, targetPeriod, offering.getTermInstance()).ifPresent(violations::add);
        checkClinicalShiftBlocked(cohortId, cs.getBatch() != null ? cs.getBatch().getId() : null,
            day, targetPeriod, offering.getTermInstance()).ifPresent(violations::add);

        if (cs.getFaculty() != null) {
            LocalTime start = targetPeriod.getStartTime();
            LocalTime end = targetPeriod.getEndTime();
            Long venueId = TimetableStaffingService.venueIdOf(cs);
            TimetableStaffingService.RoomCheckSpec roomCheck = venueId != null
                ? new TimetableStaffingService.RoomCheckSpec(cs.getSessionType(), venueId, TimetableStaffingService.physicalRoomOf(cs),
                    TimetableStaffingService.RoomMode.STRICT)
                : null;
            violations.addAll(timetableStaffingService.validateAssignmentExcluding(
                cs, day, start, end, cs.getFaculty(), excludeCellIds, roomCheck, null, null).violations());
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
        if (cs.getSessionType() == ClassSessionType.THEORY || cs.getSessionType() == ClassSessionType.LIBRARY
                || cs.getSessionType() == ClassSessionType.SPORTS) {
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
        return cohortCellsAtSlotExcluding(cohortId, termInstanceId, day, periodId,
            excludeCellId == null ? Set.of() : Set.of(excludeCellId));
    }

    private List<ClassSchedule> cohortCellsAtSlotExcluding(Long cohortId, Long termInstanceId, DayOfWeek day, Long periodId,
                                                           Set<Long> excludeCellIds) {
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
            .filter(cs -> !isExcluded(excludeCellIds, cs.getId()))
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
        return checkCohortExclusivityExcluding(request, offering, batch, cohortSection,
            excludeCellId == null ? Set.of() : Set.of(excludeCellId));
    }

    private Optional<ConstraintViolation> checkCohortExclusivityExcluding(SkeletonCellPlacementRequest request, CourseOffering offering,
                                         Batch batch, CohortSection cohortSection, Set<Long> excludeCellIds) {
        List<ClassSchedule> cohortCellsAtSlot = cohortCellsAtSlotExcluding(request.cohortId(), offering.getTermInstance().getId(),
            request.dayOfWeek(), request.periodId(), excludeCellIds);
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
        // LAB/CLINICAL vs a pre-existing LIBRARY/SPORTS cell: hard-blocked, same as THEORY -- both
        // occupy their whole CohortSection audience just like a mandatory Theory session does.
        return cohortCellsAtSlot.stream()
            .filter(cs -> cs.getSessionType() == ClassSessionType.THEORY || cs.getSessionType() == ClassSessionType.LIBRARY
                || cs.getSessionType() == ClassSessionType.SPORTS)
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

    /** A management-selected elective ({@code INSTITUTION_DECIDED} group): the institution picks one
     *  option for the whole cohort, so the chosen option is a common cohort subject (OC-227) — it
     *  takes the ordinary cohort-exclusivity check and the section's own classroom, not the
     *  student-choice elective's shared-group-slot rule. Static and entity-only so the staffing and
     *  auto-schedule services apply the exact same test. */
    private boolean isSharedSlotElective(CourseOffering offering) {
        return isElectiveOffering(offering) && !isCommonCohortElective(offering);
    }

    static boolean isCommonCohortElective(CourseOffering offering) {
        CurriculumSemesterCourse csc = offering.getCurriculumSemesterCourse();
        return csc != null && Boolean.TRUE.equals(csc.getIsElective()) && csc.getElectiveGroup() != null
            && csc.getElectiveGroup().getSelectionMode() == com.cms.model.enums.ElectiveSelectionMode.INSTITUTION_DECIDED;
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
            checkClinicalShiftBlocked(request.cohortId(), null, request.dayOfWeek(), period, offering.getTermInstance()).ifPresent(violations::add);

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
            case LIBRARY, SPORTS -> throw new IllegalStateException(
                "Library/Sports sessions have no CourseOffering/curriculum-hours budget to suggest candidates for");
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
