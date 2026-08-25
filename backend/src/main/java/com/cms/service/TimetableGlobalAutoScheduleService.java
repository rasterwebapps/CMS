package com.cms.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.AutoPlaceUnplacedItem;
import com.cms.dto.CohortPlacementSummary;
import com.cms.dto.ConstraintViolation;
import com.cms.dto.CourseOfferingDto;
import com.cms.dto.EligibleFacultyCandidateDto;
import com.cms.dto.FacultyCapacityCheckResult;
import com.cms.dto.FacultyOverCapacity;
import com.cms.dto.FacultyWorkloadDetail;
import com.cms.dto.FacultyWorkloadSummary;
import com.cms.dto.GlobalAutoScheduleResult;
import com.cms.dto.GlobalAutoSchedulePrerequisites;
import com.cms.dto.GlobalCapacityPrecheckResult;
import com.cms.dto.OverageContributor;
import com.cms.dto.RaiseCapSuggestion;
import com.cms.dto.SkeletonBuilderResponse;
import com.cms.dto.SkeletonCellPlacementRequest;
import com.cms.dto.SkeletonCellResponse;
import com.cms.dto.SkeletonSubjectBudget;
import com.cms.dto.SkeletonSubjectResponse;
import com.cms.dto.SpreadLoadSuggestion;
import com.cms.dto.StaffingAssignmentRequest;
import com.cms.dto.UnassignedOfferingSummary;
import com.cms.exception.LifecycleConflictException;
import com.cms.exception.ResourceNotFoundException;
import com.cms.exception.TimetableConstraintViolationException;
import com.cms.model.Batch;
import com.cms.model.ClassSchedule;
import com.cms.model.Classroom;
import com.cms.model.Cohort;
import com.cms.model.CohortSection;
import com.cms.model.CourseOffering;
import com.cms.model.CourseOfferingSectionFaculty;
import com.cms.model.CurriculumSemesterCourse;
import com.cms.model.Faculty;
import com.cms.model.Period;
import com.cms.model.Subject;
import com.cms.model.TermInstance;
import com.cms.model.enums.ClassSessionType;
import com.cms.model.enums.DayOfWeek;
import com.cms.model.enums.EnrollmentStatus;
import com.cms.model.enums.FacultyStatus;
import com.cms.model.enums.RegistrationStatus;
import com.cms.repository.BatchRepository;
import com.cms.repository.ClassScheduleRepository;
import com.cms.repository.ClassroomRepository;
import com.cms.repository.CohortRepository;
import com.cms.repository.CourseOfferingRepository;
import com.cms.repository.CourseOfferingSectionFacultyRepository;
import com.cms.repository.CourseRegistrationRepository;
import com.cms.repository.FacultyRepository;
import com.cms.repository.PeriodRepository;
import com.cms.repository.StudentTermEnrollmentRepository;
import com.cms.repository.TermInstanceRepository;

/**
 * Global multi-cohort "generate a first draft" auto-scheduler — extends the per-cohort {@link
 * TimetableSkeletonAutoPlaceService}/term-wide {@link TimetableStaffingAutoAssignService} tools
 * with a single action covering every cohort in a term at once, treating each budget row's own
 * bound faculty as authoritative (unlike {@link TimetableStaffingAutoAssignService}, which picks
 * freely from the eligible department pool) -- that row's own {@link CourseOfferingSectionFaculty}
 * assignment: the section-scoped override for a sectioned THEORY row, or the whole-cohort row for
 * an unsectioned THEORY row or a LAB/CLINICAL batch with no coordinator of its own (see {@link
 * #resolveBudgetFacultyId}). There is no offering-wide "primary" faculty anymore -- a single
 * CourseOffering can be shared by more than one cohort, each assigned independently. {@link
 * #checkPrerequisites} should be
 * called first so known-in-advance gaps (missing faculty, over-capacity faculty) are reported as
 * actionable links before a run is even attempted; {@link #precheckCapacity} is re-run defensively
 * inside {@link #runGlobalAutoSchedule} itself so a stale/bypassed prerequisite check can never let
 * an over-capacity run through even via a direct API call. {@link #runGlobalAutoSchedule} itself is
 * best-effort: it commits everything it successfully places/staffs and reports the rest via each
 * {@link CohortPlacementSummary}'s {@code unplaced} list (plus {@code electiveUnplaced} at the top
 * level), mirroring {@link TimetableSkeletonAutoPlaceService#autoPlace}'s existing reporting shape
 * rather than aborting the whole term-wide run over one unplaceable session.
 *
 * <p>Deliberately scoped down from the per-cohort tool in two ways, not oversights: (1) no
 * bounded-backtrack displacement — {@link TimetableSkeletonAutoPlaceService}'s heuristic only ever
 * displaces a *placement*, never an already-staffed cell (there is no "unstaff" capability to
 * cheaply undo), and since here every placement is immediately staffed as one unit, a failed
 * backtrack attempt would still mean total failure for that unit anyway, so the extra complexity
 * isn't worth it when the unit simply falls into {@code unplaced} instead; (2) elective groups are
 * only auto-scheduled for their one shared slot (mirroring the existing "Place Elective Block"
 * admin action, {@link TimetableSkeletonService#placeElectiveGroup}) — {@link
 * TimetableSkeletonService#checkElectiveGroupSlot} already requires *every* placement for a group's
 * members to match one single anchor day/period, so a member needing more than one session/week has
 * never been placeable beyond its first session by any existing mechanism in this codebase; this
 * class doesn't attempt to solve that pre-existing gap, it just automates what's already achievable.
 */
@Service
public class TimetableGlobalAutoScheduleService {

    private static final double CAPACITY_EPSILON = 0.001;

    private final TimetableSkeletonService timetableSkeletonService;
    private final TimetableStaffingService timetableStaffingService;
    private final TimetableCapacityPlanningService timetableCapacityPlanningService;
    private final CourseOfferingService courseOfferingService;
    private final CourseOfferingRepository courseOfferingRepository;
    private final ClassScheduleRepository classScheduleRepository;
    private final StudentTermEnrollmentRepository studentTermEnrollmentRepository;
    private final CohortRepository cohortRepository;
    private final BatchRepository batchRepository;
    private final CourseOfferingSectionFacultyRepository courseOfferingSectionFacultyRepository;
    private final FacultyRepository facultyRepository;
    private final TermInstanceRepository termInstanceRepository;
    private final PeriodRepository periodRepository;
    private final TimetableBlockedPeriodChecker blockedPeriodChecker;
    private final ClassroomRepository classroomRepository;
    private final CourseRegistrationRepository courseRegistrationRepository;

    public TimetableGlobalAutoScheduleService(TimetableSkeletonService timetableSkeletonService,
                                               TimetableStaffingService timetableStaffingService,
                                               TimetableCapacityPlanningService timetableCapacityPlanningService,
                                               CourseOfferingService courseOfferingService,
                                               CourseOfferingRepository courseOfferingRepository,
                                               ClassScheduleRepository classScheduleRepository,
                                               StudentTermEnrollmentRepository studentTermEnrollmentRepository,
                                               CohortRepository cohortRepository,
                                               BatchRepository batchRepository,
                                               CourseOfferingSectionFacultyRepository courseOfferingSectionFacultyRepository,
                                               FacultyRepository facultyRepository,
                                               TermInstanceRepository termInstanceRepository,
                                               PeriodRepository periodRepository,
                                               TimetableBlockedPeriodChecker blockedPeriodChecker,
                                               ClassroomRepository classroomRepository,
                                               CourseRegistrationRepository courseRegistrationRepository) {
        this.timetableSkeletonService = timetableSkeletonService;
        this.timetableStaffingService = timetableStaffingService;
        this.timetableCapacityPlanningService = timetableCapacityPlanningService;
        this.courseOfferingService = courseOfferingService;
        this.courseOfferingRepository = courseOfferingRepository;
        this.classScheduleRepository = classScheduleRepository;
        this.studentTermEnrollmentRepository = studentTermEnrollmentRepository;
        this.cohortRepository = cohortRepository;
        this.batchRepository = batchRepository;
        this.courseOfferingSectionFacultyRepository = courseOfferingSectionFacultyRepository;
        this.facultyRepository = facultyRepository;
        this.termInstanceRepository = termInstanceRepository;
        this.periodRepository = periodRepository;
        this.blockedPeriodChecker = blockedPeriodChecker;
        this.classroomRepository = classroomRepository;
        this.courseRegistrationRepository = courseRegistrationRepository;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Capacity precheck
    // ─────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public GlobalCapacityPrecheckResult precheckCapacity(Long termInstanceId) {
        TermDemandAggregation demand = computeTermDemand(termInstanceId);

        List<FacultyOverCapacity> overCapacity = new ArrayList<>();
        for (Map.Entry<Long, Double> entry : demand.demandByFaculty().entrySet()) {
            Long facultyId = entry.getKey();
            double totalDemand = entry.getValue();
            Faculty faculty = facultyRepository.findById(facultyId).orElse(null);
            if (faculty == null) {
                continue;
            }
            CapacityResolution capacity = resolveEffectiveTermCapacity(faculty, demand.workingDaysInTerm(), demand.weeksInTerm());
            if (capacity == null || totalDemand <= capacity.termCapacityHours() + CAPACITY_EPSILON) {
                continue;
            }

            double shortfall = totalDemand - capacity.termCapacityHours();
            double suggestedMinDailyHours = Math.ceil(totalDemand / demand.workingDaysInTerm());
            List<OverageContributor> topContributors = demand.contributorsByFaculty().getOrDefault(facultyId, List.of()).stream()
                .sorted(Comparator.comparingDouble(OverageContributor::termHoursContributed).reversed())
                .limit(2)
                .toList();

            RaiseCapSuggestion raiseCap = new RaiseCapSuggestion(facultyId, capacity.dailyCapForDisplay(), capacity.tier(), suggestedMinDailyHours);
            List<SpreadLoadSuggestion> spreadLoad = buildSpreadLoadSuggestions(
                topContributors, demand.demandByFaculty(), facultyId, demand.workingDaysInTerm(), demand.weeksInTerm());

            overCapacity.add(new FacultyOverCapacity(facultyId, faculty.getFullName(), capacity.dailyCapForDisplay(), capacity.tier(),
                demand.workingDaysInTerm(), capacity.termCapacityHours(), totalDemand, shortfall, suggestedMinDailyHours,
                topContributors, raiseCap, spreadLoad));
        }
        overCapacity.sort(Comparator.comparing(FacultyOverCapacity::facultyName, String.CASE_INSENSITIVE_ORDER));
        return new GlobalCapacityPrecheckResult(overCapacity);
    }

    /** Live, single-(faculty, offering+cohort) counterpart to {@link #precheckCapacity} -- used by
     *  Assign Faculty to check, before save, whether assigning {@code candidateFacultyId} to this
     *  cohort's whole-cohort row would push their real term-wide load over capacity. Reuses the
     *  exact same aggregation {@link #precheckCapacity} runs (via {@link #computeTermDemand}) so
     *  the two can never disagree. Mirrors {@link #checkFacultyCapacityForSection}'s shape but
     *  projects the cohort's *whole* theory+lab+clinical hours (via {@link
     *  #termHoursForOfferingInCohort}) instead of just one section's theory hours. */
    @Transactional(readOnly = true)
    public FacultyCapacityCheckResult checkFacultyCapacityForCohort(Long offeringId, Long cohortId, Long candidateFacultyId) {
        CourseOffering offering = courseOfferingRepository.findById(offeringId)
            .orElseThrow(() -> new ResourceNotFoundException("Course offering not found with id: " + offeringId));
        Faculty candidate = facultyRepository.findById(candidateFacultyId)
            .orElseThrow(() -> new ResourceNotFoundException("Faculty not found with id: " + candidateFacultyId));
        Long currentCohortFacultyId = currentCohortFacultyId(offering, cohortId);
        double cohortHours = termHoursForOfferingInCohort(offering, cohortId, offering.getTermInstance().getId(), null).totalHours();

        TermDemandAggregation demand = computeTermDemand(offering.getTermInstance().getId());
        double currentDemand = demand.demandByFaculty().getOrDefault(candidateFacultyId, 0.0);
        boolean alreadyHoldsCohort = candidateFacultyId.equals(currentCohortFacultyId);
        double projectedTotal = alreadyHoldsCohort ? currentDemand : currentDemand + cohortHours;

        CapacityResolution capacity = resolveEffectiveTermCapacity(candidate, demand.workingDaysInTerm(), demand.weeksInTerm());
        boolean overCapacity = capacity != null && projectedTotal > capacity.termCapacityHours() + CAPACITY_EPSILON;

        List<SpreadLoadSuggestion> spreadLoad = List.of();
        double suggestedMinDailyHours = 0;
        if (overCapacity) {
            suggestedMinDailyHours = Math.ceil(projectedTotal / demand.workingDaysInTerm());
            if (offering.getSubject() != null) {
                OverageContributor asContributor = new OverageContributor(offeringId, offering.getSubject().getName(),
                    cohortId, null, cohortHours, null, null, null, null, null);
                spreadLoad = buildSpreadLoadSuggestions(List.of(asContributor),
                    demand.demandByFaculty(), candidateFacultyId, demand.workingDaysInTerm(), demand.weeksInTerm());
            }
        }

        return new FacultyCapacityCheckResult(overCapacity, currentDemand, cohortHours, projectedTotal,
            capacity != null ? capacity.termCapacityHours() : 0, capacity != null ? capacity.dailyCapForDisplay() : 0,
            capacity != null ? capacity.tier() : "NONE", demand.workingDaysInTerm(), suggestedMinDailyHours, spreadLoad);
    }

    /** One faculty's full, real term workload — every offering/section/batch contributing to their
     *  demand, unlimited (unlike {@link #precheckCapacity}'s {@code topContributors}, which only
     *  ever surfaces the top 2 per over-capacity faculty for its warning cards). Backs the Faculty
     *  Detail "Courses" tab so management can see exactly what's assigned to one person before
     *  deciding whether to raise their cap, reassign pieces of their load, or hire. Reuses the same
     *  {@link #computeTermDemand} aggregation everything else in this class runs off, so this view
     *  can never disagree with the precheck/global-run numbers for the same term. */
    @Transactional(readOnly = true)
    public FacultyWorkloadDetail getFacultyWorkload(Long facultyId, Long termInstanceId) {
        Faculty faculty = facultyRepository.findById(facultyId)
            .orElseThrow(() -> new ResourceNotFoundException("Faculty not found with id: " + facultyId));
        TermDemandAggregation demand = computeTermDemand(termInstanceId);

        List<OverageContributor> assignments = demand.contributorsByFaculty().getOrDefault(facultyId, List.of());
        double totalDemand = demand.demandByFaculty().getOrDefault(facultyId, 0.0);
        CapacityResolution capacity = resolveEffectiveTermCapacity(faculty, demand.workingDaysInTerm(), demand.weeksInTerm());
        boolean overCapacity = capacity != null && totalDemand > capacity.termCapacityHours() + CAPACITY_EPSILON;
        double shortfall = overCapacity ? totalDemand - capacity.termCapacityHours() : 0;

        return new FacultyWorkloadDetail(facultyId, faculty.getFullName(), termInstanceId,
            demand.workingDaysInTerm(), capacity != null ? capacity.dailyCapForDisplay() : 0,
            capacity != null ? capacity.tier() : "NONE", capacity != null ? capacity.termCapacityHours() : 0,
            totalDemand, overCapacity, shortfall, assignments);
    }

    /** Term-total (not per-week) demand hours per faculty, correctly attributed per-cohort and
     *  per-section/batch via {@link #computeTermDemand} -- exposed for {@link
     *  FacultyWorkloadCapacityService#getTermWorkloadReport}, which needs these same figures
     *  converted to its own per-week reporting granularity, rather than recomputing a coarser,
     *  cohort-blind version on its own. */
    @Transactional(readOnly = true)
    public Map<Long, Double> getTermTotalDemandByFaculty(Long termInstanceId) {
        return computeTermDemand(termInstanceId).demandByFaculty();
    }

    /** Lightweight per-faculty summaries for a list of faculty ids (e.g. one Faculty List page) —
     *  runs {@link #computeTermDemand} exactly once regardless of how many ids are requested, then
     *  extracts each one's numbers, so a paginated list screen can show a workload badge per row
     *  without an N+1 query pattern. A requested id with no demand this term still comes back with
     *  {@code totalDemandHours == 0} rather than being omitted, so every row gets a badge. */
    @Transactional(readOnly = true)
    public List<FacultyWorkloadSummary> getFacultyWorkloadSummaries(List<Long> facultyIds, Long termInstanceId) {
        TermDemandAggregation demand = computeTermDemand(termInstanceId);

        List<FacultyWorkloadSummary> summaries = new ArrayList<>();
        for (Long facultyId : facultyIds) {
            Faculty faculty = facultyRepository.findById(facultyId).orElse(null);
            if (faculty == null) {
                continue;
            }
            double totalDemand = demand.demandByFaculty().getOrDefault(facultyId, 0.0);
            CapacityResolution capacity = resolveEffectiveTermCapacity(faculty, demand.workingDaysInTerm(), demand.weeksInTerm());
            boolean overCapacity = capacity != null && totalDemand > capacity.termCapacityHours() + CAPACITY_EPSILON;
            double shortfall = overCapacity ? totalDemand - capacity.termCapacityHours() : 0;
            summaries.add(new FacultyWorkloadSummary(facultyId, totalDemand,
                capacity != null ? capacity.termCapacityHours() : 0, overCapacity, shortfall));
        }
        return summaries;
    }

    /** Every eligible (Speciality match OR the subject's Eligible Faculty list) active faculty for
     *  this offering's subject, annotated with real *standing* remaining term capacity (no
     *  hypothetical projection -- there's no single offering-wide slot to project against anymore),
     *  sorted most-free-first -- backs the Faculty Pool checklist. Anyone currently assigned to any
     *  cohort/section of this offering is grandfathered in even if they no longer pass eligibility,
     *  so an existing assignment predating a stricter subject/eligibility setup never silently
     *  disappears. No speciality on the subject means no restriction at all (whole active roster
     *  returned). */
    @Transactional(readOnly = true)
    public List<EligibleFacultyCandidateDto> getEligibleFacultyForOffering(Long offeringId) {
        CourseOffering offering = courseOfferingRepository.findById(offeringId)
            .orElseThrow(() -> new ResourceNotFoundException("Course offering not found with id: " + offeringId));
        Subject subject = offering.getSubject();
        Set<Long> currentlyAssignedIds = courseOfferingSectionFacultyRepository.findByCourseOfferingId(offeringId).stream()
            .map(sf -> sf.getFaculty().getId()).collect(java.util.stream.Collectors.toSet());
        List<Faculty> pool = eligiblePoolGrandfathering(subject, currentlyAssignedIds);
        Set<Long> poolFacultyIds = poolFacultyIds(offering);

        TermDemandAggregation demand = computeTermDemand(offering.getTermInstance().getId());
        List<EligibleFacultyCandidateDto> candidates = new ArrayList<>();
        for (Faculty faculty : pool) {
            boolean currentlyAssigned = currentlyAssignedIds.contains(faculty.getId());
            candidates.add(candidateDto(subject, faculty, demand, currentlyAssigned, 0, poolFacultyIds));
        }
        return sortMostFreeFirst(candidates);
    }

    /** Section-scoped counterpart of {@link #getEligibleFacultyForOffering} -- the candidate
     *  currently holding this section is grandfathered in the same way, and each candidate's
     *  projected load is computed against just this section's own Theory hours rather than the
     *  whole offering's, since every section is assigned independently ({@link
     *  #checkFacultyCapacityForSection}). */
    @Transactional(readOnly = true)
    public List<EligibleFacultyCandidateDto> getEligibleFacultyForSection(Long offeringId, Long cohortSectionId) {
        CourseOffering offering = courseOfferingRepository.findById(offeringId)
            .orElseThrow(() -> new ResourceNotFoundException("Course offering not found with id: " + offeringId));
        Subject subject = offering.getSubject();
        Long currentSectionFacultyId = currentSectionFacultyId(offering, cohortSectionId);
        List<Faculty> pool = eligiblePoolGrandfathering(subject,
            currentSectionFacultyId != null ? Set.of(currentSectionFacultyId) : Set.of());
        Set<Long> poolFacultyIds = poolFacultyIds(offering);

        double sectionHours = safe(offering.getCurriculumSemesterCourse() != null
            ? offering.getCurriculumSemesterCourse().getTheoryHours() : null);
        TermDemandAggregation demand = computeTermDemand(offering.getTermInstance().getId());
        List<EligibleFacultyCandidateDto> candidates = new ArrayList<>();
        for (Faculty faculty : pool) {
            boolean alreadyHoldsSection = faculty.getId().equals(currentSectionFacultyId);
            candidates.add(candidateDto(subject, faculty, demand, alreadyHoldsSection, sectionHours, poolFacultyIds));
        }
        return sortMostFreeFirst(candidates);
    }

    /** Cohort-scoped counterpart of {@link #getEligibleFacultyForSection} -- for a cohort with no
     *  active section split. The candidate currently holding the whole-cohort row is grandfathered
     *  in the same way, and each candidate's projected load is computed against this cohort's
     *  *whole* theory+lab+clinical hours ({@link #checkFacultyCapacityForCohort}) rather than one
     *  section's theory hours. */
    @Transactional(readOnly = true)
    public List<EligibleFacultyCandidateDto> getEligibleFacultyForCohort(Long offeringId, Long cohortId) {
        CourseOffering offering = courseOfferingRepository.findById(offeringId)
            .orElseThrow(() -> new ResourceNotFoundException("Course offering not found with id: " + offeringId));
        Subject subject = offering.getSubject();
        Long currentCohortFacultyId = currentCohortFacultyId(offering, cohortId);
        List<Faculty> pool = eligiblePoolGrandfathering(subject,
            currentCohortFacultyId != null ? Set.of(currentCohortFacultyId) : Set.of());
        Set<Long> poolFacultyIds = poolFacultyIds(offering);

        double cohortHours = termHoursForOfferingInCohort(offering, cohortId, offering.getTermInstance().getId(), null).totalHours();
        TermDemandAggregation demand = computeTermDemand(offering.getTermInstance().getId());
        List<EligibleFacultyCandidateDto> candidates = new ArrayList<>();
        for (Faculty faculty : pool) {
            boolean alreadyHoldsCohort = faculty.getId().equals(currentCohortFacultyId);
            candidates.add(candidateDto(subject, faculty, demand, alreadyHoldsCohort, cohortHours, poolFacultyIds));
        }
        return sortMostFreeFirst(candidates);
    }

    /** Live, single-(faculty, section) capacity check -- same math {@link
     *  CourseOfferingSectionFacultyService#upsert} hard-blocks on, surfaced early by the section
     *  picker before Save, mirroring {@link #checkFacultyCapacityForCohort}'s role for a
     *  whole-cohort row. */
    @Transactional(readOnly = true)
    public FacultyCapacityCheckResult checkFacultyCapacityForSection(Long offeringId, Long cohortSectionId, Long candidateFacultyId) {
        CourseOffering offering = courseOfferingRepository.findById(offeringId)
            .orElseThrow(() -> new ResourceNotFoundException("Course offering not found with id: " + offeringId));
        Faculty candidate = facultyRepository.findById(candidateFacultyId)
            .orElseThrow(() -> new ResourceNotFoundException("Faculty not found with id: " + candidateFacultyId));
        Long currentSectionFacultyId = currentSectionFacultyId(offering, cohortSectionId);
        double sectionHours = safe(offering.getCurriculumSemesterCourse() != null
            ? offering.getCurriculumSemesterCourse().getTheoryHours() : null);

        TermDemandAggregation demand = computeTermDemand(offering.getTermInstance().getId());
        double currentDemand = demand.demandByFaculty().getOrDefault(candidateFacultyId, 0.0);
        boolean alreadyHoldsSection = candidateFacultyId.equals(currentSectionFacultyId);
        double projectedTotal = alreadyHoldsSection ? currentDemand : currentDemand + sectionHours;

        CapacityResolution capacity = resolveEffectiveTermCapacity(candidate, demand.workingDaysInTerm(), demand.weeksInTerm());
        boolean overCapacity = capacity != null && projectedTotal > capacity.termCapacityHours() + CAPACITY_EPSILON;

        List<SpreadLoadSuggestion> spreadLoad = List.of();
        double suggestedMinDailyHours = 0;
        if (overCapacity) {
            suggestedMinDailyHours = Math.ceil(projectedTotal / demand.workingDaysInTerm());
            if (offering.getSubject() != null) {
                OverageContributor asContributor = new OverageContributor(offeringId, offering.getSubject().getName(),
                    null, null, sectionHours, cohortSectionId, null, null, null, "THEORY");
                spreadLoad = buildSpreadLoadSuggestions(List.of(asContributor),
                    demand.demandByFaculty(), candidateFacultyId, demand.workingDaysInTerm(), demand.weeksInTerm());
            }
        }

        return new FacultyCapacityCheckResult(overCapacity, currentDemand, sectionHours, projectedTotal,
            capacity != null ? capacity.termCapacityHours() : 0, capacity != null ? capacity.dailyCapForDisplay() : 0,
            capacity != null ? capacity.tier() : "NONE", demand.workingDaysInTerm(), suggestedMinDailyHours, spreadLoad);
    }

    /** This section's current faculty -- its own {@link CourseOfferingSectionFaculty} override, or
     *  null if unassigned. No offering-wide fallback anymore -- a split cohort's sections are
     *  assigned independently, with no single "primary" to fall back to. */
    private Long currentSectionFacultyId(CourseOffering offering, Long cohortSectionId) {
        return courseOfferingSectionFacultyRepository
            .findByCourseOfferingIdAndCohortSectionId(offering.getId(), cohortSectionId)
            .map(sf -> sf.getFaculty().getId())
            .orElse(null);
    }

    /** This cohort's whole-cohort faculty -- the {@link CourseOfferingSectionFaculty} row with no
     *  section (only meaningful/settable for a cohort with no active section split), or null if
     *  unassigned. */
    private Long currentCohortFacultyId(CourseOffering offering, Long cohortId) {
        return courseOfferingSectionFacultyRepository
            .findByCourseOfferingIdAndCohortIdAndCohortSectionIdIsNull(offering.getId(), cohortId)
            .map(sf -> sf.getFaculty().getId())
            .orElse(null);
    }

    /** {@link FacultyEligibility#eligibleFaculty}'s active pool, with every id in {@code
     *  currentHolderIds} added back in if eligibility alone would have excluded them -- shared
     *  grandfathering helper for the offering-level, section-level, and cohort-level candidate
     *  lists. */
    private List<Faculty> eligiblePoolGrandfathering(Subject subject, Set<Long> currentHolderIds) {
        List<Faculty> activePool = facultyRepository.findByStatus(FacultyStatus.ACTIVE);
        List<Faculty> eligible = subject != null
            ? new ArrayList<>(FacultyEligibility.eligibleFaculty(subject, activePool))
            : new ArrayList<>(activePool);
        Set<Long> eligibleIds = eligible.stream().map(Faculty::getId).collect(java.util.stream.Collectors.toSet());
        for (Long holderId : currentHolderIds) {
            if (holderId != null && !eligibleIds.contains(holderId)) {
                facultyRepository.findById(holderId).ifPresent(eligible::add);
            }
        }
        return eligible;
    }

    private static Set<Long> poolFacultyIds(CourseOffering offering) {
        return offering.getFacultyPool().stream().map(Faculty::getId).collect(java.util.stream.Collectors.toSet());
    }

    private EligibleFacultyCandidateDto candidateDto(Subject subject, Faculty faculty, TermDemandAggregation demand,
            boolean alreadyHoldsSlot, double slotHours, Set<Long> poolFacultyIds) {
        double currentDemand = demand.demandByFaculty().getOrDefault(faculty.getId(), 0.0);
        double projectedTotal = alreadyHoldsSlot ? currentDemand : currentDemand + slotHours;
        CapacityResolution capacity = resolveEffectiveTermCapacity(faculty, demand.workingDaysInTerm(), demand.weeksInTerm());
        double capacityHours = capacity != null ? capacity.termCapacityHours() : 0;
        String tier = capacity != null ? capacity.tier() : "NONE";
        double remaining = capacity != null ? capacityHours - projectedTotal : 0;
        boolean overCapacity = capacity != null && remaining < -CAPACITY_EPSILON;
        boolean specialityMatch = subject != null && FacultyEligibility.specialityMatches(subject, faculty);
        boolean viaEligibleList = subject != null && FacultyEligibility.viaEligibleList(subject, faculty);
        return new EligibleFacultyCandidateDto(faculty.getId(), faculty.getFullName(), specialityMatch, viaEligibleList,
            alreadyHoldsSlot, poolFacultyIds.contains(faculty.getId()), currentDemand, capacityHours, tier, remaining, overCapacity);
    }

    /** Uncapped candidates ({@code capacityTier == "NONE"}) sort first -- no configured limit reads
     *  as "most free" -- then the rest by descending remaining hours. */
    private static List<EligibleFacultyCandidateDto> sortMostFreeFirst(List<EligibleFacultyCandidateDto> candidates) {
        return candidates.stream()
            .sorted((a, b) -> {
                boolean aUncapped = "NONE".equals(a.capacityTier());
                boolean bUncapped = "NONE".equals(b.capacityTier());
                if (aUncapped != bUncapped) {
                    return aUncapped ? -1 : 1;
                }
                return Double.compare(b.remainingHours(), a.remainingHours());
            })
            .toList();
    }

    private record TermDemandAggregation(int workingDaysInTerm, int weeksInTerm, Map<Long, Double> demandByFaculty,
                                          Map<Long, List<OverageContributor>> contributorsByFaculty) {}

    /** The shared per-term aggregation every capacity check in this class runs off, so none of them
     *  can ever compute a faculty's demand differently. Loops every cohort active in the term, then
     *  every offering that cohort has, summing each offering+cohort pair's contribution per bound
     *  faculty (an offering shared across cohorts on the same curriculum version contributes once
     *  per cohort, correctly -- see class javadoc / {@link #precheckCapacity}'s original
     *  double-counting note). */
    private TermDemandAggregation computeTermDemand(Long termInstanceId) {
        TermInstance term = requireTermInstance(termInstanceId);
        int workingDaysInTerm = timetableCapacityPlanningService.countWorkingDays(
            term, timetableCapacityPlanningService.nonTeachingDates(term));
        int weeksInTerm = CurriculumHoursCalculator.weeksInTerm(term);

        Map<Long, Double> demandByFaculty = new LinkedHashMap<>();
        Map<Long, List<OverageContributor>> contributorsByFaculty = new LinkedHashMap<>();

        for (Long cohortId : enumerateCohortIds(termInstanceId)) {
            Cohort cohort = cohortRepository.findById(cohortId).orElse(null);
            if (cohort == null) {
                continue;
            }
            for (CourseOfferingDto offeringDto : courseOfferingService.getOfferingsByTermInstanceAndCohort(termInstanceId, cohortId)) {
                CourseOffering offering = courseOfferingRepository.findById(offeringDto.id()).orElse(null);
                if (offering == null || offering.getCurriculumSemesterCourse() == null) {
                    continue;
                }
                Long wholeCohortFacultyId = currentCohortFacultyId(offering, cohortId);
                OfferingHoursSplit split = termHoursForOfferingInCohort(offering, cohortId, termInstanceId, wholeCohortFacultyId);
                if (split.totalHours() <= 0) {
                    continue;
                }
                for (FacultyContribution contribution : split.contributions()) {
                    demandByFaculty.merge(contribution.facultyId(), contribution.hours(), Double::sum);
                    contributorsByFaculty.computeIfAbsent(contribution.facultyId(), k -> new ArrayList<>())
                        .add(new OverageContributor(offering.getId(), offeringDto.subjectName(), cohortId, cohort.getDisplayName(),
                            contribution.hours(), contribution.cohortSectionId(), contribution.batchId(),
                            contribution.cohortSectionLabel(), contribution.batchName(), contribution.sessionType()));
                }
            }
        }
        return new TermDemandAggregation(workingDaysInTerm, weeksInTerm, demandByFaculty, contributorsByFaculty);
    }

    /** One faculty's share of an offering+cohort's term hours, attributed to exactly one of: a
     *  specific {@link CohortSection} (THEORY), a specific {@link Batch} (LAB/CLINICAL), or neither
     *  (both null — the offering's whole-cohort primary, when there are no active sections/batches
     *  to split across). This granularity is what lets a "spread load" suggestion be turned into a
     *  real reassignment (§ {@link #termHoursForOfferingInCohort}) rather than just advisory text.
     *  {@code cohortSectionLabel}/{@code batchName} carry the display name so two rows for the same
     *  subject+cohort don't render as unexplained-looking duplicates. {@code sessionType} is
     *  "THEORY"/"LAB"/"CLINICAL", or "LAB_CLINICAL" for the untyped-legacy-batch/unbatched-fallback
     *  case where lab and clinical hours are combined and can't be split further. */
    private record FacultyContribution(Long facultyId, double hours, Long cohortSectionId, Long batchId,
                                        String cohortSectionLabel, String batchName, String sessionType) {}

    /** {@code sessionType} is part of the merge key (not just carried data) so a THEORY
     *  contribution and a LAB/CLINICAL contribution never merge into one row even when both fall
     *  to the same primary faculty with no section/batch to attribute to (both null) — without
     *  this, an unsectioned/unbatched offering with both theory and lab/clinical hours would lose
     *  the type distinction entirely. */
    private record ContributionKey(Long facultyId, Long cohortSectionId, Long batchId, String sessionType) {}

    private record OfferingHoursSplit(double totalHours, List<FacultyContribution> contributions) {}

    /** This offering+cohort pair's real term hours, split by whichever faculty actually delivers
     *  each part -- not just a lump total credited to the offering's primary. THEORY hours are owed
     *  once per active {@link CohortSection} (a sectioned cohort needs the same course delivered
     *  once per section, exactly what {@link TimetableSkeletonService#resolveActiveSections} + its
     *  {@code theoryBudgets} caller already enforce one row at a time; reused here rather than
     *  re-derived so this can never disagree with what Skeleton Builder itself shows); each
     *  section's own {@link CourseOfferingSectionFaculty} override is credited for its hours
     *  (falling back to the primary for any section left unassigned), mirroring exactly how LAB/
     *  CLINICAL hours are owed once per active {@link Batch} and credited to each batch's own
     *  {@code coordinatorFacultyId} (falling back to the primary for any batch left uncoordinated).
     *  With no active sections (a single, unsectioned cohort) or no active batches, the whole
     *  theory/lab/clinical total for that part falls to the primary, same as before either
     *  mechanism existed. Contributions sharing the same (faculty, section, batch) key are merged
     *  so an unsectioned/unbatched offering still yields one combined contribution per faculty,
     *  matching pre-existing aggregate totals exactly. Each active batch owes only its OWN type's
     *  hours ({@link #batchHours} — a {@code Lab}-linked batch owes {@code labHours}, a {@code
     *  ClinicalVenue}-linked batch owes {@code clinicalHours}, a legacy untyped batch created
     *  outside the Cohort Room Allocation flow owes the combined total) -- not the full combined
     *  lab+clinical total per batch, which would double-charge a faculty coordinating separate Lab
     *  and Clinical batches for the same offering (confirmed against real data: an offering with 4
     *  Lab batches + 2 Clinical batches, all uncoordinated, was inflating one faculty's demand by
     *  ~2,500h by charging every batch the full lab+clinical sum instead of its own share). */
    private OfferingHoursSplit termHoursForOfferingInCohort(CourseOffering offering, Long cohortId, Long termInstanceId, Long primaryFacultyId) {
        CurriculumSemesterCourse csc = offering.getCurriculumSemesterCourse();
        int theoryHours = safe(csc.getTheoryHours());
        int labHours = safe(csc.getLabHours());
        int clinicalHours = safe(csc.getClinicalHours());
        int labClinicalHours = labHours + clinicalHours;

        List<CohortSection> activeSections = timetableSkeletonService.resolveActiveSections(cohortId, termInstanceId);
        List<Batch> activeBatches = batchRepository.findByCourseOfferingId(offering.getId()).stream()
            .filter(b -> Boolean.TRUE.equals(b.getIsActive()))
            .toList();

        Map<ContributionKey, Double> merged = new LinkedHashMap<>();

        double theoryTotal;
        if (activeSections.isEmpty()) {
            theoryTotal = theoryHours;
            if (theoryTotal > 0 && primaryFacultyId != null) {
                merged.merge(new ContributionKey(primaryFacultyId, null, null, "THEORY"), theoryTotal, Double::sum);
            }
        } else {
            theoryTotal = theoryHours * (double) activeSections.size();
            if (theoryHours > 0) {
                Map<Long, Long> sectionFacultyIdBySectionId = courseOfferingSectionFacultyRepository
                    .findByCourseOfferingId(offering.getId()).stream()
                    .collect(java.util.stream.Collectors.toMap(sf -> sf.getCohortSection().getId(), sf -> sf.getFaculty().getId()));
                for (CohortSection section : activeSections) {
                    Long facultyForSection = sectionFacultyIdBySectionId.getOrDefault(section.getId(), primaryFacultyId);
                    if (facultyForSection != null) {
                        merged.merge(new ContributionKey(facultyForSection, section.getId(), null, "THEORY"), (double) theoryHours, Double::sum);
                    }
                }
            }
        }

        double labClinicalTotal;
        if (activeBatches.isEmpty()) {
            labClinicalTotal = labClinicalHours;
            if (labClinicalTotal > 0 && primaryFacultyId != null) {
                merged.merge(new ContributionKey(primaryFacultyId, null, null, "LAB_CLINICAL"), labClinicalTotal, Double::sum);
            }
        } else {
            labClinicalTotal = 0;
            for (Batch batch : activeBatches) {
                double hoursForBatch = batchHours(batch, labHours, clinicalHours, labClinicalHours);
                if (hoursForBatch <= 0) {
                    continue;
                }
                labClinicalTotal += hoursForBatch;
                Long facultyForBatch = batch.getCoordinatorFaculty() != null
                    ? batch.getCoordinatorFaculty().getId() : primaryFacultyId;
                if (facultyForBatch != null) {
                    merged.merge(new ContributionKey(facultyForBatch, null, batch.getId(), batchSessionType(batch)), hoursForBatch, Double::sum);
                }
            }
        }

        // Plain loops, not Collectors.toMap -- its merge-based accumulator throws NPE on a null
        // value (sectionLabel/name can be null/unset on fixtures and legacy rows), which a
        // key->value lookup map should tolerate rather than reject.
        Map<Long, String> sectionLabelById = new LinkedHashMap<>();
        for (CohortSection section : activeSections) {
            sectionLabelById.put(section.getId(), section.getSectionLabel());
        }
        Map<Long, String> batchNameById = new LinkedHashMap<>();
        for (Batch batch : activeBatches) {
            batchNameById.put(batch.getId(), batch.getName());
        }

        List<FacultyContribution> contributions = merged.entrySet().stream()
            .map(e -> new FacultyContribution(e.getKey().facultyId(), e.getValue(), e.getKey().cohortSectionId(), e.getKey().batchId(),
                sectionLabelById.get(e.getKey().cohortSectionId()), batchNameById.get(e.getKey().batchId()), e.getKey().sessionType()))
            .toList();
        return new OfferingHoursSplit(theoryTotal + labClinicalTotal, contributions);
    }

    /** Mirrors {@link #batchHours}'s own type detection, for the {@code sessionType} carried on
     *  each contribution/row rather than the hours themselves. */
    private static String batchSessionType(Batch batch) {
        if (batch.getLab() != null) {
            return "LAB";
        }
        if (batch.getClinicalVenue() != null) {
            return "CLINICAL";
        }
        return "LAB_CLINICAL";
    }

    /** A batch's real hours owed, by its actual venue type -- {@link Batch#getLab()} set means it's
     *  a LAB batch (owes {@code labHours} only), {@link Batch#getClinicalVenue()} set means CLINICAL
     *  (owes {@code clinicalHours} only). Neither set means a legacy batch created outside the
     *  Cohort Room Allocation flow (see {@link Batch#getLab()}'s own javadoc) -- falls back to the
     *  full combined total, the only case where the pre-existing behavior was actually correct. */
    private static double batchHours(Batch batch, int labHours, int clinicalHours, int labClinicalHours) {
        if (batch.getLab() != null) {
            return labHours;
        }
        if (batch.getClinicalVenue() != null) {
            return clinicalHours;
        }
        return labClinicalHours;
    }

    private record CapacityResolution(double termCapacityHours, double dailyCapForDisplay, String tier) {}

    /** Daily cap (any tier) is the primary, always-reported dimension, matching the user's own
     *  framing ("raise to at least N hours/day"). Falls back to a weekly-derived term capacity
     *  (converted to an equivalent daily figure purely for display consistency) only when no daily
     *  cap is configured at any tier -- an institution that only ever configured the original
     *  weekly cap (added before daily/continuous existed) must still be checked, not silently
     *  skipped. A faculty with neither dimension configured has no cap at all and is never flagged,
     *  mirroring how {@link TimetableStaffingService#checkWithinWorkloadCaps} treats an unresolved
     *  cap today (no check, not an error). */
    private CapacityResolution resolveEffectiveTermCapacity(Faculty faculty, int workingDaysInTerm, int weeksInTerm) {
        Integer dailyOverrideOrDesignation = FacultyWorkloadCapacityService.resolveEffectiveDailyCapacity(faculty);
        if (dailyOverrideOrDesignation != null) {
            String tier = faculty.getPlannedDailyHoursOverride() != null ? "FACULTY_OVERRIDE" : "DESIGNATION_DEFAULT";
            return new CapacityResolution(dailyOverrideOrDesignation.doubleValue() * workingDaysInTerm,
                dailyOverrideOrDesignation.doubleValue(), tier);
        }
        Optional<Double> globalDaily = timetableStaffingService.resolveDailyCap(faculty);
        if (globalDaily.isPresent()) {
            return new CapacityResolution(globalDaily.get() * workingDaysInTerm, globalDaily.get(), "SYSTEM_CONFIGURATION");
        }

        Integer weeklyOverrideOrDesignation = FacultyWorkloadCapacityService.resolveEffectiveCapacity(faculty);
        if (weeklyOverrideOrDesignation != null) {
            String tier = faculty.getPlannedWeeklyHoursOverride() != null ? "FACULTY_OVERRIDE" : "DESIGNATION_DEFAULT";
            double termCapacity = weeklyOverrideOrDesignation.doubleValue() * weeksInTerm;
            return new CapacityResolution(termCapacity, termCapacity / workingDaysInTerm, tier);
        }
        Optional<Double> globalWeekly = timetableStaffingService.resolveWeeklyCap(faculty);
        if (globalWeekly.isPresent()) {
            double termCapacity = globalWeekly.get() * weeksInTerm;
            return new CapacityResolution(termCapacity, termCapacity / workingDaysInTerm, "SYSTEM_CONFIGURATION");
        }
        return null;
    }

    /** Advisory-only, nothing applied automatically -- for each of the faculty's top contributing
     *  offerings, scans the eligible candidate pool ({@link FacultyEligibility#eligibleFaculty},
     *  Speciality match OR the subject's Eligible Faculty list -- no restriction at all when the
     *  subject has no speciality tag, the common case in this data set), picking the first candidate
     *  whose own existing demand plus this offering's hours still fits their own capacity. */
    private List<SpreadLoadSuggestion> buildSpreadLoadSuggestions(List<OverageContributor> topContributors,
            Map<Long, Double> demandByFaculty, Long overCapacityFacultyId, int workingDaysInTerm, int weeksInTerm) {
        List<SpreadLoadSuggestion> suggestions = new ArrayList<>();
        for (OverageContributor contributor : topContributors) {
            CourseOffering offering = courseOfferingRepository.findById(contributor.courseOfferingId()).orElse(null);
            if (offering == null || offering.getSubject() == null) {
                continue;
            }
            List<Faculty> pool = FacultyEligibility.eligibleFaculty(
                offering.getSubject(), facultyRepository.findByStatus(FacultyStatus.ACTIVE));
            for (Faculty candidate : pool) {
                if (candidate.getId().equals(overCapacityFacultyId)) {
                    continue;
                }
                SpreadLoadSuggestion suggestion = spreadLoadSuggestionIfSpare(candidate, contributor, demandByFaculty, workingDaysInTerm, weeksInTerm);
                if (suggestion != null) {
                    suggestions.add(suggestion);
                    break;
                }
            }
        }
        return suggestions;
    }

    private SpreadLoadSuggestion spreadLoadSuggestionIfSpare(Faculty candidate, OverageContributor contributor,
            Map<Long, Double> demandByFaculty, int workingDaysInTerm, int weeksInTerm) {
        CapacityResolution capacity = resolveEffectiveTermCapacity(candidate, workingDaysInTerm, weeksInTerm);
        if (capacity == null) {
            return null;
        }
        double existingDemand = demandByFaculty.getOrDefault(candidate.getId(), 0.0);
        double spare = capacity.termCapacityHours() - existingDemand;
        if (spare + CAPACITY_EPSILON < contributor.termHoursContributed()) {
            return null;
        }
        return new SpreadLoadSuggestion(candidate.getId(), candidate.getFullName(), spare,
            contributor.courseOfferingId(), contributor.subjectName(), contributor.cohortSectionId(), contributor.batchId());
    }

    // ─────────────────────────────────────────────────────────────────────
    // Prerequisite check
    // ─────────────────────────────────────────────────────────────────────

    /** Consolidated, read-only "is this term/cohort ready for automation" report — combines every
     *  known-in-advance gap (offerings/elective members with no faculty bound, faculty over
     *  capacity) into one call so the frontend can show all shortfalls as actionable links up front
     *  instead of discovering them one gate at a time across multiple failed runs. Room-commit
     *  status is deliberately not part of this DTO — that's Capacity Planner's domain and is
     *  checked client-side against its own existing endpoints. */
    @Transactional(readOnly = true)
    public GlobalAutoSchedulePrerequisites checkPrerequisites(Long termInstanceId, Long cohortId) {
        List<UnassignedOfferingSummary> unassigned = new ArrayList<>();
        Set<Long> electiveGroupIdsSeen = new LinkedHashSet<>();

        for (Long id : resolveCohortIds(termInstanceId, cohortId)) {
            Cohort cohort = cohortRepository.findById(id).orElse(null);
            if (cohort == null) {
                continue;
            }
            SkeletonBuilderResponse skeleton = timetableSkeletonService.getCohortSkeleton(termInstanceId, id);
            for (SkeletonSubjectResponse subject : skeleton.subjects()) {
                CourseOffering offering = courseOfferingRepository.findById(subject.courseOfferingId()).orElse(null);
                if (offering == null) {
                    continue;
                }
                if (timetableSkeletonService.isElectiveOffering(offering)) {
                    if (subject.electiveGroupId() != null) {
                        electiveGroupIdsSeen.add(subject.electiveGroupId());
                    }
                    continue;
                }
                boolean hasShortfall = subject.budgets().stream()
                    .anyMatch(b -> b.requiredSessionsPerWeek() > b.placedSessionsPerWeek() && resolveBudgetFacultyId(offering, b, id) == null);
                if (hasShortfall) {
                    unassigned.add(new UnassignedOfferingSummary(offering.getId(), subject.subjectName(), id, cohort.getDisplayName()));
                }
            }
        }

        for (Long electiveGroupId : electiveGroupIdsSeen) {
            for (CourseOffering member : courseOfferingRepository
                    .findByTermInstanceIdAndCurriculumSemesterCourse_ElectiveGroupId(termInstanceId, electiveGroupId)) {
                if (Boolean.TRUE.equals(member.getIsActive()) && resolveElectiveMemberFacultyId(member) == null
                        && safe(member.getCurriculumSemesterCourse() != null ? member.getCurriculumSemesterCourse().getTheoryHours() : null) > 0) {
                    unassigned.add(new UnassignedOfferingSummary(member.getId(),
                        member.getSubject() != null ? member.getSubject().getName() + " (elective)" : "(elective)", null, null));
                }
            }
        }

        return new GlobalAutoSchedulePrerequisites(unassigned, precheckCapacity(termInstanceId));
    }

    // ─────────────────────────────────────────────────────────────────────
    // Placement + staffing run
    // ─────────────────────────────────────────────────────────────────────

    /** Best-effort — commits everything it successfully places/staffs and reports the rest, never
     *  rolling back a cohort's real progress over a different cohort's (or a different session's)
     *  failure. The capacity precheck stays a hard pre-flight gate (re-run defensively so a bad/
     *  stale prerequisite check can never be bypassed even via a direct API call) — that's a
     *  legitimate "don't even start" condition, distinct from the per-session best-effort behavior
     *  below it. {@code cohortId} null runs every cohort enrolled in the term (today's existing
     *  behavior); non-null scopes the run to just that cohort's shortfall. */
    @Transactional
    public GlobalAutoScheduleResult runGlobalAutoSchedule(Long termInstanceId, Long cohortId) {
        GlobalCapacityPrecheckResult precheck = precheckCapacity(termInstanceId);
        if (!precheck.overCapacityFaculty().isEmpty()) {
            List<ConstraintViolation> violations = precheck.overCapacityFaculty().stream()
                .map(f -> new ConstraintViolation("GLOBAL_AUTO_SCHEDULE_OVER_CAPACITY",
                    f.facultyName() + " needs " + formatHours(f.shortfallHours())
                        + " more capacity than currently configured — run the capacity precheck for remediation options"))
                .toList();
            throw new TimetableConstraintViolationException(violations);
        }

        TermInstance term = requireTermInstance(termInstanceId);
        List<Period> periods = periodRepository.findByIsActiveTrueOrderByPeriodOrderAsc();
        Set<Long> cohortIds = resolveCohortIds(termInstanceId, cohortId);

        int totalPlaced = 0;
        int totalStaffed = 0;
        List<CohortPlacementSummary> summaries = new ArrayList<>();
        Set<Long> electiveGroupIdsSeen = new LinkedHashSet<>();

        for (Long id : cohortIds) {
            Cohort cohort = cohortRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cohort not found with id: " + id));
            SkeletonBuilderResponse skeleton = timetableSkeletonService.getCohortSkeleton(termInstanceId, id);
            int placedForCohort = 0;
            boolean usedSaturdayForCohort = false;
            List<AutoPlaceUnplacedItem> unplacedForCohort = new ArrayList<>();

            for (SkeletonSubjectResponse subject : skeleton.subjects()) {
                CourseOffering offering = courseOfferingRepository.findById(subject.courseOfferingId()).orElse(null);
                if (offering == null) {
                    continue;
                }
                if (timetableSkeletonService.isElectiveOffering(offering)) {
                    if (subject.electiveGroupId() != null) {
                        electiveGroupIdsSeen.add(subject.electiveGroupId());
                    }
                    continue;
                }
                for (SkeletonSubjectBudget budget : subject.budgets()) {
                    int shortfall = budget.requiredSessionsPerWeek() - budget.placedSessionsPerWeek();
                    if (shortfall <= 0) {
                        continue;
                    }
                    Long facultyId = resolveBudgetFacultyId(offering, budget, id);
                    if (facultyId == null) {
                        unplacedForCohort.add(new AutoPlaceUnplacedItem(subject.subjectName(), budget.sessionType(),
                            occupantLabel(budget), "no faculty assigned on its Course Offering"));
                        continue;
                    }
                    Set<DayOfWeek> daysUsed = existingDaysForBudgetRow(skeleton.cells(), subject.courseOfferingId(), budget);
                    for (int i = 0; i < shortfall; i++) {
                        DayOfWeek placedOn = tryPlaceAndStaff(id, offering, budget, facultyId, term, periods, daysUsed);
                        if (placedOn == null) {
                            unplacedForCohort.add(new AutoPlaceUnplacedItem(subject.subjectName(), budget.sessionType(),
                                occupantLabel(budget), "no day/period found where both placement and staffing succeed"));
                            continue;
                        }
                        if (placedOn == DayOfWeek.SATURDAY) {
                            usedSaturdayForCohort = true;
                        }
                        daysUsed.add(placedOn);
                        placedForCohort++;
                    }
                }
            }
            totalPlaced += placedForCohort;
            totalStaffed += placedForCohort;
            summaries.add(new CohortPlacementSummary(id, cohort.getDisplayName(), placedForCohort, placedForCohort,
                unplacedForCohort, usedSaturdayForCohort));
        }

        // Electives: only each group's one shared slot is automated (see class javadoc) -- counts
        // fold into the totals but aren't attributed to any single cohort summary row, since a
        // group can span students from more than one cohort.
        List<AutoPlaceUnplacedItem> electiveUnplaced = new ArrayList<>();
        for (Long electiveGroupId : electiveGroupIdsSeen) {
            int placed = placeAndStaffElectiveGroup(termInstanceId, electiveGroupId, term, periods, electiveUnplaced);
            totalPlaced += placed;
            totalStaffed += placed;
        }

        return new GlobalAutoScheduleResult(totalPlaced, totalStaffed, summaries, electiveUnplaced);
    }

    /** {@code cohortId} null = every cohort enrolled in the term (existing behavior); non-null must
     *  actually be enrolled in this term, else this is a caller error, not a "nothing to do." */
    private Set<Long> resolveCohortIds(Long termInstanceId, Long cohortId) {
        Set<Long> enrolled = enumerateCohortIds(termInstanceId);
        if (cohortId == null) {
            return enrolled;
        }
        if (!enrolled.contains(cohortId)) {
            throw new ResourceNotFoundException("Cohort " + cohortId + " is not enrolled in term " + termInstanceId);
        }
        return Set.of(cohortId);
    }

    private String occupantLabel(SkeletonSubjectBudget budget) {
        return budget.cohortSectionLabel() != null ? budget.cohortSectionLabel() : budget.batchName();
    }

    /** The faculty who should actually staff this budget row -- a THEORY row split across active
     *  {@link CohortSection}s resolves its own {@link CourseOfferingSectionFaculty} section-level
     *  override; every other row shape (unsectioned THEORY, or a LAB/CLINICAL batch with no
     *  coordinator of its own -- {@link Batch#getCoordinatorFaculty()} stays advisory-only,
     *  unaffected by this change) resolves this cohort's whole-cohort row instead. Returns null
     *  (unplaced, reported as "no faculty assigned") when nothing has been assigned yet. */
    private Long resolveBudgetFacultyId(CourseOffering offering, SkeletonSubjectBudget budget, Long cohortId) {
        if (budget.cohortSectionId() != null) {
            return currentSectionFacultyId(offering, budget.cohortSectionId());
        }
        return currentCohortFacultyId(offering, cohortId);
    }

    /** Scans every free day/period until one is found where placement AND staffing the offering's
     *  bound faculty both succeed, undoing the placement and trying the next candidate on a
     *  staffing failure (that faculty is busy at that slot, not a shared resource another row could
     *  be nudged out of — see class javadoc for why this doesn't attempt backtracking). {@code
     *  DayOfWeek.values()} is already Monday-first/Saturday-last by declaration order, so
     *  Monday–Friday is always exhausted before Saturday is ever tried — Saturday is fallback-only,
     *  never preferred. Returns the day it landed on, or null if every combination was exhausted. */
    private DayOfWeek tryPlaceAndStaff(Long cohortId, CourseOffering offering, SkeletonSubjectBudget budget, Long facultyId,
                                        TermInstance term, List<Period> periods, Set<DayOfWeek> daysUsed) {
        for (DayOfWeek day : DayOfWeek.values()) {
            if (daysUsed.contains(day)) {
                continue;
            }
            for (Period period : periods) {
                if (blockedPeriodChecker.blockReason(day, period.getStartTime(), period.getEndTime(),
                        term.getStartDate(), term.getEndDate()).isPresent()) {
                    continue;
                }
                SkeletonCellResponse placed;
                try {
                    placed = timetableSkeletonService.placeCell(new SkeletonCellPlacementRequest(
                        offering.getId(), budget.sessionType(), day, period.getId(),
                        budget.batchId(), cohortId, budget.cohortSectionId(), null));
                } catch (TimetableConstraintViolationException | IllegalArgumentException ex) {
                    continue;
                }
                try {
                    timetableStaffingService.staffCell(placed.id(), new StaffingAssignmentRequest(facultyId, null));
                } catch (TimetableConstraintViolationException | LifecycleConflictException | IllegalArgumentException ex) {
                    timetableSkeletonService.removeCell(placed.id());
                    continue;
                }
                return day;
            }
        }
        return null;
    }

    /** Mirrors {@code TimetableSkeletonAutoPlaceService#existingDaysForRow} — which days this exact
     *  budget row (subject/session-type/batch-or-section) already has a session on, so the shortfall
     *  loop never clusters two of that row's own sessions on the same day. */
    private Set<DayOfWeek> existingDaysForBudgetRow(List<SkeletonCellResponse> cells, Long courseOfferingId, SkeletonSubjectBudget budget) {
        Set<DayOfWeek> days = new HashSet<>();
        for (SkeletonCellResponse cell : cells) {
            if (cell.courseOfferingId().equals(courseOfferingId) && cell.sessionType() == budget.sessionType()
                    && Objects.equals(cell.batchId(), budget.batchId())
                    && Objects.equals(cell.cohortSectionId(), budget.cohortSectionId())) {
                days.add(cell.dayOfWeek());
            }
        }
        return days;
    }

    /** Automates only a group's one shared slot (see class javadoc for why members needing more
     *  than one session/week are out of scope — the same limit {@code checkElectiveGroupSlot}
     *  already imposes on every other placement path). Only THEORY members are attempted — LAB/
     *  CLINICAL electives still need a Capacity-Planner-committed batch/venue exactly like
     *  non-electives, so there is no free-room search to build for them; they're simply left
     *  unplaced by this pass, same as any other structural prerequisite gap. Best-effort: any
     *  failure adds to {@code unplacedSink} and returns 0 instead of throwing, so one group's
     *  problem never aborts the whole term-wide run. */
    private int placeAndStaffElectiveGroup(Long termInstanceId, Long electiveGroupId, TermInstance term, List<Period> periods,
                                            List<AutoPlaceUnplacedItem> unplacedSink) {
        List<CourseOffering> members = courseOfferingRepository
            .findByTermInstanceIdAndCurriculumSemesterCourse_ElectiveGroupId(termInstanceId, electiveGroupId);
        if (members.isEmpty()) {
            return 0;
        }
        for (CourseOffering member : members) {
            if (Boolean.TRUE.equals(member.getIsActive()) && resolveElectiveMemberFacultyId(member) == null
                    && safe(member.getCurriculumSemesterCourse() != null ? member.getCurriculumSemesterCourse().getTheoryHours() : null) > 0) {
                unplacedSink.add(new AutoPlaceUnplacedItem(member.getSubject().getName(), ClassSessionType.THEORY, null,
                    "no faculty assigned on its Course Offering (elective)"));
                return 0;
            }
        }

        List<Long> memberIds = members.stream().map(CourseOffering::getId).toList();
        List<ClassSchedule> existingGroupCells = classScheduleRepository
            .findByTermInstanceIdAndCourseOfferingIdIn(termInstanceId, memberIds);

        List<CourseOffering> unplacedTheoryMembers = members.stream()
            .filter(m -> Boolean.TRUE.equals(m.getIsActive()))
            .filter(m -> m.getCurriculumSemesterCourse() != null && safe(m.getCurriculumSemesterCourse().getTheoryHours()) > 0)
            .filter(m -> existingGroupCells.stream().noneMatch(cs -> cs.getCourseOffering() != null && cs.getCourseOffering().getId().equals(m.getId())))
            .toList();
        if (unplacedTheoryMembers.isEmpty()) {
            return 0;
        }

        ClassSchedule anchor = existingGroupCells.stream().min(Comparator.comparing(ClassSchedule::getId)).orElse(null);
        int registeredStrength = unplacedTheoryMembers.stream()
            .mapToInt(m -> (int) courseRegistrationRepository.countByCourseOfferingIdAndStatus(m.getId(), RegistrationStatus.REGISTERED))
            .max().orElse(0);
        List<Classroom> activeClassrooms = classroomRepository.findByIsActiveTrueOrderByNameAsc();

        if (anchor != null) {
            // The group's slot is already fixed by an earlier (manual or automated) placement --
            // every unplaced member must join at that exact day/period, same as checkElectiveGroupSlot
            // already requires of any other placement path.
            DayOfWeek day = anchor.getDayOfWeek();
            Period period = anchor.getPeriod();
            Classroom classroom = period == null ? null
                : firstFreeClassroom(activeClassrooms, registeredStrength, day, period, term);
            if (period == null || classroom == null || !placeAndStaffElectiveMembers(unplacedTheoryMembers, day, period, classroom, term)) {
                unplacedSink.add(new AutoPlaceUnplacedItem("Elective group " + electiveGroupId, ClassSessionType.THEORY, null,
                    "already scheduled for " + day + (period != null ? ", " + period.getName() : "")
                        + " — one or more new members can't join that exact slot"));
                return 0;
            }
            return unplacedTheoryMembers.size();
        }

        for (DayOfWeek day : DayOfWeek.values()) {
            for (Period period : periods) {
                if (blockedPeriodChecker.blockReason(day, period.getStartTime(), period.getEndTime(),
                        term.getStartDate(), term.getEndDate()).isPresent()) {
                    continue;
                }
                Classroom classroom = firstFreeClassroom(activeClassrooms, registeredStrength, day, period, term);
                if (classroom == null) {
                    continue;
                }
                if (placeAndStaffElectiveMembers(unplacedTheoryMembers, day, period, classroom, term)) {
                    return unplacedTheoryMembers.size();
                }
            }
        }
        unplacedSink.add(new AutoPlaceUnplacedItem("Elective group " + electiveGroupId, ClassSessionType.THEORY, null,
            "no day/period found where every member's bound faculty and a suitable room are all free"));
        return 0;
    }

    /** Attempts every member at the given slot/room, undoing everything on the first failure so a
     *  partially-placed group is never left behind for the caller's next candidate slot to build on. */
    private boolean placeAndStaffElectiveMembers(List<CourseOffering> members, DayOfWeek day, Period period, Classroom classroom, TermInstance term) {
        List<Long> placedCellIds = new ArrayList<>();
        for (CourseOffering member : members) {
            SkeletonCellResponse placed;
            try {
                placed = timetableSkeletonService.placeCell(new SkeletonCellPlacementRequest(
                    member.getId(), ClassSessionType.THEORY, day, period.getId(), null, null, null, null));
            } catch (TimetableConstraintViolationException | IllegalArgumentException ex) {
                rollbackElectiveCells(placedCellIds);
                return false;
            }
            placedCellIds.add(placed.id());
            try {
                timetableStaffingService.staffCell(placed.id(), new StaffingAssignmentRequest(resolveElectiveMemberFacultyId(member), classroom.getId()));
            } catch (TimetableConstraintViolationException | LifecycleConflictException | IllegalArgumentException ex) {
                rollbackElectiveCells(placedCellIds);
                return false;
            }
        }
        return true;
    }

    private void rollbackElectiveCells(List<Long> cellIds) {
        for (Long id : cellIds) {
            timetableSkeletonService.removeCell(id);
        }
    }

    /** Elective member offerings don't loop per-cohort in this class -- a group's shared slot spans
     *  every enrolled cohort's students by design (see class javadoc), so there's no single
     *  cohortId in scope to resolve a per-cohort assignment against here. Resolves the member's own
     *  {@link CourseOfferingSectionFaculty} rows (whole-cohort and/or per-section) and returns their
     *  shared faculty if every row agrees on exactly one (including the common case of just one row);
     *  returns null (treated as unassigned) if they disagree, rather than guessing which one wins --
     *  a deliberately narrow exception to the per-cohort model, matching this class's existing
     *  elective scope limits. */
    private Long resolveElectiveMemberFacultyId(CourseOffering member) {
        Set<Long> facultyIds = courseOfferingSectionFacultyRepository.findByCourseOfferingId(member.getId()).stream()
            .map(sf -> sf.getFaculty().getId())
            .collect(java.util.stream.Collectors.toSet());
        return facultyIds.size() == 1 ? facultyIds.iterator().next() : null;
    }

    private Classroom firstFreeClassroom(List<Classroom> candidates, int requiredStrength, DayOfWeek day, Period period, TermInstance term) {
        for (Classroom classroom : candidates) {
            if (classroom.getCapacity() != null && classroom.getCapacity() < requiredStrength) {
                continue;
            }
            Optional<ConstraintViolation> conflict = timetableStaffingService.checkRoomFree(
                ClassSessionType.THEORY, classroom.getId(), classroom.getRoom(), term.getId(), null,
                day, period.getStartTime(), period.getEndTime());
            if (conflict.isEmpty()) {
                return classroom;
            }
        }
        return null;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Shared helpers
    // ─────────────────────────────────────────────────────────────────────

    private Set<Long> enumerateCohortIds(Long termInstanceId) {
        return studentTermEnrollmentRepository.findDistinctCohortIdsByTermInstanceId(termInstanceId, EnrollmentStatus.ENROLLED);
    }

    private TermInstance requireTermInstance(Long termInstanceId) {
        return termInstanceRepository.findById(termInstanceId)
            .orElseThrow(() -> new ResourceNotFoundException("Term instance not found with id: " + termInstanceId));
    }

    private static int safe(Integer value) {
        return value != null ? value : 0;
    }

    private static String formatHours(double hours) {
        return (Math.round(hours * 10) / 10.0) + "h";
    }
}
