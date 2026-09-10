package com.cms.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.AutoPlaceUnplacedItem;
import com.cms.dto.SystemConfigurationResponse;
import com.cms.dto.ClinicalShiftPeriodAvailabilityResult;
import com.cms.dto.ClinicalShiftWindow;
import com.cms.dto.CohortPlacementSummary;
import com.cms.dto.ConstraintViolation;
import com.cms.dto.CourseOfferingDto;
import com.cms.dto.CourseOfferingFacultySummaryDto;
import com.cms.dto.SectionFacultyAssignment;
import com.cms.dto.FacultySubstitutionTip;
import com.cms.dto.SubstitutionAffectedSection;
import com.cms.dto.EligibleFacultyCandidateDto;
import com.cms.dto.FacultyCapacityCheckResult;
import com.cms.dto.FacultyOverCapacity;
import com.cms.dto.FacultyTightCapacity;
import com.cms.dto.FacultyWorkloadDetail;
import com.cms.dto.FacultyWorkloadOverviewReport;
import com.cms.dto.FacultyWorkloadOverviewRow;
import com.cms.dto.FacultyWorkloadSummary;
import com.cms.dto.GlobalAutoScheduleResult;
import com.cms.dto.GlobalAutoSchedulePrerequisites;
import com.cms.dto.GlobalCapacityPrecheckResult;
import com.cms.dto.LabClinicalVenueCapacityResult;
import com.cms.dto.OverageContributor;
import com.cms.dto.RaiseCapSuggestion;
import com.cms.dto.RotationGroupCreateRequest;
import com.cms.dto.SkeletonBuilderResponse;
import com.cms.dto.SkeletonCellPlacementRequest;
import com.cms.dto.SkeletonCellResponse;
import com.cms.dto.SkeletonSubjectBudget;
import com.cms.dto.SkeletonSubjectResponse;
import com.cms.dto.SkippedPublishedCohort;
import com.cms.dto.SpreadLoadSuggestion;
import com.cms.dto.StaffingAssignmentRequest;
import com.cms.dto.UnassignedOfferingSummary;
import com.cms.dto.VenueCapacityGap;
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
import com.cms.model.RotationGroup;
import com.cms.model.RotationMemberAssignment;
import com.cms.model.RotationSlot;
import com.cms.model.Subject;
import com.cms.model.TermInstance;
import com.cms.model.enums.ClassScheduleStatus;
import com.cms.model.enums.ClassSessionType;
import com.cms.model.enums.DayOfWeek;
import com.cms.model.enums.EnrollmentStatus;
import com.cms.model.enums.FacultyStatus;
import com.cms.model.enums.OfferingAssignmentStatus;
import com.cms.model.enums.PlanningBasis;
import com.cms.model.enums.RegistrationStatus;
import com.cms.model.enums.RoomPurposeCategoryCode;
import com.cms.repository.BatchRepository;
import com.cms.repository.ClassScheduleRepository;
import com.cms.repository.ClassroomRepository;
import com.cms.repository.CohortRepository;
import com.cms.repository.CourseOfferingRepository;
import com.cms.repository.CourseOfferingSectionFacultyRepository;
import com.cms.repository.CourseRegistrationRepository;
import com.cms.repository.FacultyRepository;
import com.cms.repository.PeriodRepository;
import com.cms.repository.RotationGroupRepository;
import com.cms.repository.RotationMemberAssignmentRepository;
import com.cms.repository.RotationMemberRepository;
import com.cms.repository.RotationSlotRepository;
import com.cms.repository.StudentTermEnrollmentRepository;
import com.cms.repository.SubjectRepository;
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
 * called first so known-in-advance gaps (missing faculty, over-capacity faculty, over-capacity
 * Lab/Clinical venue) are reported as actionable links before a run is even attempted; both
 * {@link #precheckCapacity} and {@link TimetableCapacityPlanningService#computeLabClinicalVenueCapacity}
 * are re-run defensively inside {@link #runGlobalAutoSchedule} itself so a stale/bypassed
 * prerequisite check can never let an over-capacity run through even via a direct API call.
 * {@link #runGlobalAutoSchedule} itself is
 * best-effort: it commits everything it successfully places/staffs and reports the rest via each
 * {@link CohortPlacementSummary}'s {@code unplaced} list (plus {@code electiveUnplaced} at the top
 * level) rather than aborting the whole term-wide run over one unplaceable session.
 *
 * <p>Scoped down from the per-cohort tool in one deliberate way, not an oversight: elective groups
 * are only auto-scheduled for their one shared slot (mirroring the existing "Place Elective Block"
 * admin action, {@link TimetableSkeletonService#placeElectiveGroup}) — {@link
 * TimetableSkeletonService#checkElectiveGroupSlot} already requires *every* placement for a group's
 * members to match one single anchor day/period, so a member needing more than one session/week has
 * never been placeable beyond its first session by any existing mechanism in this codebase; this
 * class doesn't attempt to solve that pre-existing gap, it just automates what's already achievable.
 * ({@link #attemptBacktrack} below used to be a second deliberate omission here too — every
 * placement in this class is staffed in the same step it's placed, and there was no way to cheaply
 * undo a staffed cell. {@link TimetableSkeletonService#forceRemoveCell} closed that gap, so this
 * class now backtracks too.)
 *
 * <h2>Placement order — load-bearing, not incidental</h2>
 * A run rebuilds the whole DRAFT grid ({@link #purgeDraftCellsForRebuild}) and then places, in
 * this exact order:
 * <ol>
 * <li><b>Phase 1 — LAB/CLINICAL</b>, pooled across every cohort and sorted by largest remaining
 *     shortfall, since these are the only rows that contend for a venue another cohort also needs;
 * <li><b>Phase 2 — THEORY</b>, per cohort (each active {@link com.cms.model.CohortSection} has its
 *     own exclusive committed classroom, so cohort order is irrelevant here);
 * <li><b>Phase 3 — elective groups</b>, one shared slot each;
 * <li><b>Phase 4 — Library, then Self-Study/Co-curricular gap-fill;</b>
 * <li><b>Phase 5 — Saturday-upgrade mending</b> ({@link #attemptSaturdayUpgradeMending}), per
 *     cohort, after every other phase: a THEORY row that only found room on Saturday in Phase 2 is
 *     a real success as far as placement goes, but on a term whose Saturday pattern isn't every
 *     week that session fires far less often than a Monday-Friday one, so the row quietly finishes
 *     short on hours. This pass tries to reclaim a weekday slot for it by bumping whatever now
 *     sits there — Self-Study filler first, then another already-placed THEORY row (given a
 *     genuine fresh day-search of its own, never just shoved onto the vacated Saturday slot).
 * </ol>
 *
 * <p><b>Why this order cannot be casually rearranged.</b> A multi-period LAB/CLINICAL session needs
 * a run of consecutive periods that is unbroken in real clock time, and per {@link PeriodGapPolicy}
 * a CLINICAL block may cross a short recess but never the day's lunch break. On a typical 8-period
 * day that leaves exactly TWO legal positions for a 4-period Clinical block — forenoon and
 * afternoon — so the whole week offers only about a dozen. A single one-period THEORY or LIBRARY
 * session dropped anywhere inside such a run destroys that entire half-day window. The cheap,
 * flexible rows must therefore always be placed into the gaps the rigid ones leave, never the other
 * way round. Phases 3 and 4 in particular are greedy: Library claims its full weekly quota and
 * Self-Study backfills EVERY remaining weekday period, so anything scheduled after them gets
 * nothing.
 *
 * <p><b>Why the rebuild is what makes that order mean anything.</b> The ordering above only ever
 * governed cells a run places itself. {@link #attemptBacktrack} can displace a placement made
 * during the current run and nothing else, so before the rebuild every DRAFT cell inherited from an
 * earlier run was permanently immovable — run N's Phase 4 filler became run N+1's Phase 1
 * obstacle, and re-running made the week progressively worse instead of better. Real incident
 * (2026-09-02): 12 stray single periods had blocked 10 of one cohort's 12 weekly Clinical windows,
 * pinning Clinical at 2 of the 4 sessions/week it needed while the run report correctly insisted
 * there was nowhere left to put them. If a future change ever reintroduces "keep what's already
 * there," it must also give the placement pass a way to move those cells, or this failure returns.
 */
@Service
public class TimetableGlobalAutoScheduleService {

    private static final Logger log = LoggerFactory.getLogger(TimetableGlobalAutoScheduleService.class);

    private static final double CAPACITY_EPSILON = 0.001;
    /** A faculty at or above this fraction of their term capacity gets flagged as "tight" (see
     *  {@link com.cms.dto.FacultyTightCapacity}) even though they're not technically over — real
     *  day/period packing at near-100% utilization routinely fails even when the aggregate sum
     *  fits, since every other cohort/subject is competing for the same slots. Shared with {@link
     *  TimetableCapacityPlanningService}'s Lab/Clinical venue tight-capacity check — one literal,
     *  not two independently-typed copies. */
    private static final double TIGHT_CAPACITY_THRESHOLD = TimetableCapacityPlanningService.TIGHT_CAPACITY_THRESHOLD;
    private static final String LIBRARY_SUBJECT_CODE = "SYSTEM-LIBRARY";
    private static final String CONFIG_LIBRARY_SESSIONS_PER_WEEK = "timetable.library_sessions_per_week";
    private static final String CONFIG_LIBRARY_BLOCK_SIZE_PERIODS = "timetable.library_block_size_periods";
    private static final int DEFAULT_LIBRARY_SESSIONS_PER_WEEK = 2;
    private static final int DEFAULT_LIBRARY_BLOCK_SIZE_PERIODS = 2;
    /** Cap on how many different weekday candidates {@link #attemptSaturdayUpgradeMending} will
     *  try bumping for one Saturday-placed row before giving up on it -- keeps a term-wide run's
     *  added cost bounded (each attempt is 2 evictions plus up to 2 fresh placement attempts, all
     *  their own {@code REQUIRES_NEW} transactions) rather than scanning every other placement on
     *  the grid for every Saturday session found. */
    private static final int MAX_SATURDAY_MENDING_CANDIDATES = 3;

    /** Guards {@link #runGlobalAutoSchedule} against two overlapping runs on the same {@link
     *  com.cms.model.TermInstance} — a real production failure mode (a user double-clicking "Run"
     *  before the first request's spinner even shows, or two admins running it moments apart) since
     *  {@link AutoScheduleRunCache} is only a per-thread ThreadLocal and every actual DB write inside
     *  a run ({@code placeCell}/{@code staffCell}/{@code forceRemoveCell}, all {@code REQUIRES_NEW})
     *  commits and becomes visible to a concurrent run immediately, well before either request's own
     *  outer transaction finishes. Without this guard, run B's {@code purgeDraftCellsForRebuild} (or
     *  its own placements) can delete a {@link com.cms.model.ClassSchedule} row that run A already
     *  recorded in its in-memory {@code placedThisCohortRun}, so when run A's own {@link
     *  #attemptBacktrack}/{@link #tryUpgradeSwap} later tries to bump that row it 404s via {@link
     *  TimetableSkeletonService#forceRemoveCell} — surfaced in production as an uncaught {@code
     *  ResourceNotFoundException} ("Class schedule not found with id: ...") that aborts the whole
     *  run. A {@link java.util.concurrent.ConcurrentHashMap}-backed set keyed by termInstanceId is
     *  enough here: this only needs to reject a genuine overlap, not order or queue requests, and a
     *  run for one term must never block a run for a different term. */
    private static final Set<Long> ACTIVE_GLOBAL_AUTO_SCHEDULE_RUNS = java.util.concurrent.ConcurrentHashMap.newKeySet();

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
    private final SubjectRepository subjectRepository;
    private final SystemConfigurationService systemConfigurationService;
    private final ClinicalShiftGroupService clinicalShiftGroupService;
    private final RotationGroupService rotationGroupService;
    private final RotationGroupRepository rotationGroupRepository;
    private final RotationSlotRepository rotationSlotRepository;
    private final RotationMemberRepository rotationMemberRepository;
    private final RotationMemberAssignmentRepository rotationMemberAssignmentRepository;

    // Field injection with @Lazy breaks the circular dependency:
    // TimetableGlobalAutoScheduleService -> CourseOfferingSectionFacultyService -> TimetableGlobalAutoScheduleService
    @Autowired
    @Lazy
    private CourseOfferingSectionFacultyService courseOfferingSectionFacultyService;

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
                                               CourseRegistrationRepository courseRegistrationRepository,
                                               SubjectRepository subjectRepository,
                                               SystemConfigurationService systemConfigurationService,
                                               ClinicalShiftGroupService clinicalShiftGroupService,
                                               RotationGroupService rotationGroupService,
                                               RotationGroupRepository rotationGroupRepository,
                                               RotationSlotRepository rotationSlotRepository,
                                               RotationMemberRepository rotationMemberRepository,
                                               RotationMemberAssignmentRepository rotationMemberAssignmentRepository) {
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
        this.subjectRepository = subjectRepository;
        this.systemConfigurationService = systemConfigurationService;
        this.clinicalShiftGroupService = clinicalShiftGroupService;
        this.rotationGroupService = rotationGroupService;
        this.rotationGroupRepository = rotationGroupRepository;
        this.rotationSlotRepository = rotationSlotRepository;
        this.rotationMemberRepository = rotationMemberRepository;
        this.rotationMemberAssignmentRepository = rotationMemberAssignmentRepository;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Capacity precheck
    // ─────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public GlobalCapacityPrecheckResult precheckCapacity(Long termInstanceId) {
        TermDemandAggregation demand = computeTermDemand(termInstanceId);

        List<FacultyOverCapacity> overCapacity = new ArrayList<>();
        List<FacultyTightCapacity> tightCapacity = new ArrayList<>();
        for (Map.Entry<Long, Double> entry : demand.demandByFaculty().entrySet()) {
            Long facultyId = entry.getKey();
            double totalDemand = entry.getValue();
            Faculty faculty = facultyRepository.findById(facultyId).orElse(null);
            if (faculty == null) {
                continue;
            }
            CapacityResolution capacity = resolveEffectiveTermCapacity(faculty, demand.workingDaysInTerm(), demand.weeksInTerm());
            if (capacity == null) {
                continue;
            }

            if (totalDemand <= capacity.termCapacityHours() + CAPACITY_EPSILON) {
                // Not over capacity -- but "fits on paper" and "packs into a real grid every other
                // cohort/subject is also competing for" are different questions. Flag the ones
                // with near-zero slack so an admin can see the real risk before running, not just
                // after the fact as an unexplained unplaced session.
                if (totalDemand >= capacity.termCapacityHours() * TIGHT_CAPACITY_THRESHOLD) {
                    List<OverageContributor> topContributors = demand.contributorsByFaculty().getOrDefault(facultyId, List.of()).stream()
                        .sorted(Comparator.comparingDouble(OverageContributor::termHoursContributed).reversed())
                        .limit(2)
                        .toList();
                    double utilizationPercent = (totalDemand / capacity.termCapacityHours()) * 100;
                    tightCapacity.add(new FacultyTightCapacity(facultyId, faculty.getFullName(), capacity.dailyCapForDisplay(), capacity.tier(),
                        demand.workingDaysInTerm(), capacity.termCapacityHours(), totalDemand, utilizationPercent, topContributors));
                }
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
        tightCapacity.sort(Comparator.comparing(FacultyTightCapacity::facultyName, String.CASE_INSENSITIVE_ORDER));
        return new GlobalCapacityPrecheckResult(overCapacity, tightCapacity);
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
     *  sorted most-free-first -- backs the Assign Faculty dialog's per-row pickers directly (there is
     *  no separate pool-curation step: every offering derived from a subject automatically inherits
     *  that subject's eligible faculty, with no manual step in between). Grandfathered by "currently
     *  assigned somewhere on this offering" so an existing assignment predating a stricter
     *  subject/eligibility setup never silently disappears and becomes unreassignable. No speciality
     *  on the subject means no restriction at all (whole active roster returned). */
    /** Whether at least one active faculty member is eligible to teach {@code subject} -- Speciality
     *  match or the subject's admin-curated Eligible Faculty list, same rule as {@link
     *  FacultyEligibility#eligibleFaculty}. No grandfathering (unlike the picker-list methods above)
     *  since this gates offering *generation*, where no assignment exists yet to grandfather. A
     *  subject with no Speciality set is never restricted, so this is always true for it -- only a
     *  subject that has a Speciality but zero matching/listed active faculty returns false. Backs
     *  {@link CourseOfferingServiceImpl#generateOfferingsForTermInstance}'s hard gate. */
    @Transactional(readOnly = true)
    public boolean hasEligibleFacultyPool(Subject subject) {
        if (subject.getSpeciality() == null) {
            return true;
        }
        List<Faculty> activePool = facultyRepository.findByStatus(FacultyStatus.ACTIVE);
        return !FacultyEligibility.eligibleFaculty(subject, activePool).isEmpty();
    }

    @Transactional(readOnly = true)
    public List<EligibleFacultyCandidateDto> getEligibleFacultyForOffering(Long offeringId) {
        CourseOffering offering = courseOfferingRepository.findById(offeringId)
            .orElseThrow(() -> new ResourceNotFoundException("Course offering not found with id: " + offeringId));
        Subject subject = offering.getSubject();
        Set<Long> currentlyAssignedIds = courseOfferingSectionFacultyRepository.findByCourseOfferingId(offeringId).stream()
            .map(sf -> sf.getFaculty().getId()).collect(java.util.stream.Collectors.toSet());
        List<Faculty> pool = eligiblePoolGrandfathering(subject, currentlyAssignedIds);

        TermDemandAggregation demand = computeTermDemand(offering.getTermInstance().getId());
        List<EligibleFacultyCandidateDto> candidates = new ArrayList<>();
        for (Faculty faculty : pool) {
            boolean currentlyAssigned = currentlyAssignedIds.contains(faculty.getId());
            candidates.add(candidateDto(subject, faculty, demand, currentlyAssigned, 0));
        }
        return sortMostFreeFirst(candidates);
    }

    /** Section-scoped counterpart of {@link #getEligibleFacultyForOffering} -- candidates are every
     *  faculty eligible for the offering's subject, plus whoever currently holds this exact section
     *  even if they've since fallen out of eligibility, so an existing pick is never silently
     *  unrepresented. Each candidate's projected load is computed against just this section's own
     *  Theory hours rather than the whole offering's, since every section is assigned independently
     *  ({@link #checkFacultyCapacityForSection}). */
    @Transactional(readOnly = true)
    public List<EligibleFacultyCandidateDto> getEligibleFacultyForSection(Long offeringId, Long cohortSectionId) {
        CourseOffering offering = courseOfferingRepository.findById(offeringId)
            .orElseThrow(() -> new ResourceNotFoundException("Course offering not found with id: " + offeringId));
        Subject subject = offering.getSubject();
        Long currentSectionFacultyId = currentSectionFacultyId(offering, cohortSectionId);
        Set<Long> grandfatherIds = currentSectionFacultyId != null ? Set.of(currentSectionFacultyId) : Set.of();
        List<Faculty> pool = eligiblePoolGrandfathering(subject, grandfatherIds);

        double sectionHours = safe(offering.getCurriculumSemesterCourse() != null
            ? offering.getCurriculumSemesterCourse().getTheoryHours() : null);
        TermDemandAggregation demand = computeTermDemand(offering.getTermInstance().getId());
        List<EligibleFacultyCandidateDto> candidates = new ArrayList<>();
        for (Faculty faculty : pool) {
            boolean alreadyHoldsSection = faculty.getId().equals(currentSectionFacultyId);
            candidates.add(candidateDto(subject, faculty, demand, alreadyHoldsSection, sectionHours));
        }
        return sortMostFreeFirst(candidates);
    }

    /** Cohort-scoped counterpart of {@link #getEligibleFacultyForSection} -- for a cohort with no
     *  active section split. Candidates are every faculty eligible for the offering's subject, plus
     *  whoever currently holds the whole-cohort row even if they've since fallen out of eligibility,
     *  same grandfathering rule as the section-scoped variant. Each candidate's projected load is
     *  computed against this cohort's *whole* theory+lab+clinical hours ({@link
     *  #checkFacultyCapacityForCohort}) rather than one section's theory hours. */
    @Transactional(readOnly = true)
    public List<EligibleFacultyCandidateDto> getEligibleFacultyForCohort(Long offeringId, Long cohortId) {
        CourseOffering offering = courseOfferingRepository.findById(offeringId)
            .orElseThrow(() -> new ResourceNotFoundException("Course offering not found with id: " + offeringId));
        Subject subject = offering.getSubject();
        Long currentCohortFacultyId = currentCohortFacultyId(offering, cohortId);
        Set<Long> grandfatherIds = currentCohortFacultyId != null ? Set.of(currentCohortFacultyId) : Set.of();
        List<Faculty> pool = eligiblePoolGrandfathering(subject, grandfatherIds);

        double cohortHours = termHoursForOfferingInCohort(offering, cohortId, offering.getTermInstance().getId(), null).totalHours();
        TermDemandAggregation demand = computeTermDemand(offering.getTermInstance().getId());
        List<EligibleFacultyCandidateDto> candidates = new ArrayList<>();
        for (Faculty faculty : pool) {
            boolean alreadyHoldsCohort = faculty.getId().equals(currentCohortFacultyId);
            candidates.add(candidateDto(subject, faculty, demand, alreadyHoldsCohort, cohortHours));
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

    /** {@link FacultyEligibility#eligibleFaculty}'s active roster, with every id in {@code
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

    private EligibleFacultyCandidateDto candidateDto(Subject subject, Faculty faculty, TermDemandAggregation demand,
            boolean alreadyHoldsSlot, double slotHours) {
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
            alreadyHoldsSlot, currentDemand, capacityHours, tier, remaining, overCapacity);
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

    record TermDemandAggregation(int workingDaysInTerm, int weeksInTerm, Map<Long, Double> demandByFaculty,
                                          Map<Long, List<OverageContributor>> contributorsByFaculty, double totalRequiredHours) {}

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
        double totalRequiredHours = 0;

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
                // Counted regardless of whether any contribution below actually resolved a faculty --
                // this is "what the curriculum needs", not "what's currently bound to someone" (see
                // FacultyWorkloadOverviewReport#totalCurriculumRequiredHours).
                totalRequiredHours += split.totalHours();
                for (FacultyContribution contribution : split.contributions()) {
                    demandByFaculty.merge(contribution.facultyId(), contribution.hours(), Double::sum);
                    contributorsByFaculty.computeIfAbsent(contribution.facultyId(), k -> new ArrayList<>())
                        .add(new OverageContributor(offering.getId(), offeringDto.subjectName(), cohortId, cohort.getDisplayName(),
                            contribution.hours(), contribution.cohortSectionId(), contribution.batchId(),
                            contribution.cohortSectionLabel(), contribution.batchName(), contribution.sessionType()));
                }
            }
        }
        return new TermDemandAggregation(workingDaysInTerm, weeksInTerm, demandByFaculty, contributorsByFaculty, totalRequiredHours);
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
                    .filter(sf -> sf.getCohortSection() != null)
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

        // The same PARTIAL/NONE/FULL rollup Assign Faculty's badge and TimetableGenerationService
        // #approve's Publish gate already use -- so this checklist item can never show green on an
        // offering Publish would actually reject, and can never show a gap Publish wouldn't also
        // block on. Deliberately NOT skipped once the term is already PUBLISHED: a gap introduced by
        // a post-publish reassignment is exactly what this item exists to surface, even though a full
        // Global Auto-Schedule re-run is separately hard-blocked for a published term regardless (see
        // doRunGlobalAutoSchedule) -- "Assign Faculty" from this checklist still fixes it directly.
        Map<Long, OfferingAssignmentStatus> statusByOffering = courseOfferingSectionFacultyService
            .getAssignmentSummaryForTermInstance(termInstanceId).stream()
            .collect(Collectors.toMap(CourseOfferingFacultySummaryDto::offeringId, CourseOfferingFacultySummaryDto::assignmentStatus));

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
                OfferingAssignmentStatus status = statusByOffering.get(offering.getId());
                if (status == OfferingAssignmentStatus.NONE || status == OfferingAssignmentStatus.PARTIAL) {
                    unassigned.add(new UnassignedOfferingSummary(offering.getId(), subject.subjectName(), id, cohort.getDisplayName()));
                }
            }
        }

        for (Long electiveGroupId : electiveGroupIdsSeen) {
            for (CourseOffering member : courseOfferingRepository
                    .findByTermInstanceIdAndCurriculumSemesterCourse_ElectiveGroupId(termInstanceId, electiveGroupId)) {
                OfferingAssignmentStatus status = statusByOffering.get(member.getId());
                if (Boolean.TRUE.equals(member.getIsActive())
                        && (status == OfferingAssignmentStatus.NONE || status == OfferingAssignmentStatus.PARTIAL)) {
                    unassigned.add(new UnassignedOfferingSummary(member.getId(),
                        member.getSubject() != null ? member.getSubject().getName() + " (elective)" : "(elective)", null, null));
                }
            }
        }

        LabClinicalVenueCapacityResult venueCapacity =
            timetableCapacityPlanningService.computeLabClinicalVenueCapacity(termInstanceId, PlanningBasis.SANCTIONED);
        ClinicalShiftPeriodAvailabilityResult shiftAvailability =
            timetableCapacityPlanningService.computeClinicalShiftPeriodAvailability(termInstanceId);
        return new GlobalAutoSchedulePrerequisites(unassigned, precheckCapacity(termInstanceId), venueCapacity, shiftAvailability);
    }

    /** Every active faculty member's full term standing — "how many hours should they be carrying
     *  vs how many are they actually carrying" — regardless of whether they're anywhere near a
     *  capacity problem. Unlike {@link #precheckCapacity} (which only ever surfaces faculty already
     *  over or near their limit), this also reports faculty sitting well under capacity, so an
     *  admin can settle an "am I overworked" dispute or spot genuinely idle capacity before
     *  concluding the department is short-staffed. Reuses the exact same {@link
     *  #computeTermDemand}/{@link #resolveEffectiveTermCapacity} numbers every other capacity view
     *  in this class runs off, so this report, the checklist, and Faculty Detail's own workload tab
     *  can never disagree for the same term. */
    @Transactional(readOnly = true)
    public FacultyWorkloadOverviewReport getFullFacultyWorkloadOverview(Long termInstanceId) {
        TermDemandAggregation demand = computeTermDemand(termInstanceId);
        int unassignedOfferingsCount = checkPrerequisites(termInstanceId, null).offeringsWithoutFaculty().size();

        List<FacultyWorkloadOverviewRow> rows = new ArrayList<>();
        double totalAssignedHours = 0;
        double totalFacultyCapacityHours = 0;
        for (Faculty faculty : facultyRepository.findByStatus(FacultyStatus.ACTIVE)) {
            double totalDemand = demand.demandByFaculty().getOrDefault(faculty.getId(), 0.0);
            List<OverageContributor> contributors = demand.contributorsByFaculty().getOrDefault(faculty.getId(), List.of())
                .stream().sorted(Comparator.comparingDouble(OverageContributor::termHoursContributed).reversed()).toList();
            CapacityResolution capacity = resolveEffectiveTermCapacity(faculty, demand.workingDaysInTerm(), demand.weeksInTerm());
            boolean configured = capacity != null;
            double termCapacityHours = configured ? capacity.termCapacityHours() : 0;
            double utilizationPercent = configured && termCapacityHours > 0 ? (totalDemand / termCapacityHours) * 100 : 0;
            boolean overCapacity = configured && totalDemand > termCapacityHours + CAPACITY_EPSILON;
            boolean tightCapacity = configured && !overCapacity && totalDemand >= termCapacityHours * TIGHT_CAPACITY_THRESHOLD;

            rows.add(new FacultyWorkloadOverviewRow(faculty.getId(), faculty.getFullName(),
                faculty.getDesignation() != null ? faculty.getDesignation().getName() : null,
                faculty.getPlannedDailyHoursOverride(), configured,
                configured ? capacity.dailyCapForDisplay() : 0, configured ? capacity.tier() : "NONE",
                demand.workingDaysInTerm(), termCapacityHours, totalDemand, utilizationPercent,
                overCapacity ? totalDemand - termCapacityHours : 0,
                configured ? Math.max(0, termCapacityHours - totalDemand) : 0,
                overCapacity, tightCapacity, contributors));

            totalAssignedHours += totalDemand;
            if (configured) {
                totalFacultyCapacityHours += termCapacityHours;
            }
        }
        rows.sort(Comparator.comparing(FacultyWorkloadOverviewRow::facultyName, String.CASE_INSENSITIVE_ORDER));

        int recommendedAdditionalFacultyCount = recommendedAdditionalFacultyCount(
            demand.totalRequiredHours() - totalFacultyCapacityHours, rows, demand.workingDaysInTerm());

        return new FacultyWorkloadOverviewReport(termInstanceId, rows,
            demand.totalRequiredHours(), totalAssignedHours, totalFacultyCapacityHours, unassignedOfferingsCount,
            recommendedAdditionalFacultyCount);
    }

    /** See {@link FacultyWorkloadOverviewReport#recommendedAdditionalFacultyCount}'s own javadoc for
     *  the estimate's shape and limits. "One faculty" is the average configured daily capacity
     *  across every row that actually has one (0 — never flagged — otherwise, matching how an
     *  unconfigured cap is treated everywhere else in this class), so the estimate reflects this
     *  institution's real designation mix rather than a hardcoded constant. */
    private static int recommendedAdditionalFacultyCount(double gapHours, List<FacultyWorkloadOverviewRow> rows, int workingDaysInTerm) {
        if (gapHours <= 0.001 || workingDaysInTerm <= 0) {
            return 0;
        }
        double averageDailyCapacityHours = rows.stream()
            .filter(FacultyWorkloadOverviewRow::capacityConfigured)
            .mapToDouble(FacultyWorkloadOverviewRow::effectiveDailyCapacityHours)
            .filter(h -> h > 0)
            .average().orElse(0);
        if (averageDailyCapacityHours <= 0) {
            return 0;
        }
        double oneFacultyTermCapacity = averageDailyCapacityHours * workingDaysInTerm;
        return (int) Math.ceil(gapHours / oneFacultyTermCapacity);
    }

    // ─────────────────────────────────────────────────────────────────────
    // Placement + staffing run
    // ─────────────────────────────────────────────────────────────────────

    /** Best-effort — commits everything it successfully places/staffs and reports the rest, never
     *  rolling back a cohort's real progress over a different cohort's (or a different session's)
     *  failure. The capacity precheck stays a hard pre-flight gate (re-run defensively so a bad/
     *  stale prerequisite check can never be bypassed even via a direct API call) — that's a
     *  legitimate "don't even start" condition, distinct from the per-session best-effort behavior
     *  below it. {@code cohortId} null runs every cohort enrolled in the term, unless this term's
     *  timetable is already approved/{@code PUBLISHED} on Draft Review (today's existing behavior,
     *  minus a published term — see {@link #isTermTimetablePublished}); non-null scopes the run to
     *  just that cohort's shortfall, hard-blocked entirely once the term is published. */
    @Transactional
    public GlobalAutoScheduleResult runGlobalAutoSchedule(Long termInstanceId, Long cohortId) {
        // See ACTIVE_GLOBAL_AUTO_SCHEDULE_RUNS's own javadoc -- add() returns false if a run for
        // this exact term is already in flight on another thread, rejected here before touching the
        // database at all rather than racing it.
        if (!ACTIVE_GLOBAL_AUTO_SCHEDULE_RUNS.add(termInstanceId)) {
            throw new LifecycleConflictException(
                "A Global Auto-Schedule run is already in progress for this term — wait for it to finish before starting another",
                "GLOBAL_AUTO_SCHEDULE_ALREADY_RUNNING", "TermInstance", termInstanceId, null);
        }
        try {
            return AutoScheduleRunCache.run(termInstanceId, classScheduleRepository,
                () -> doRunGlobalAutoSchedule(termInstanceId, cohortId));
        } finally {
            ACTIVE_GLOBAL_AUTO_SCHEDULE_RUNS.remove(termInstanceId);
        }
    }

    /** The real run body, wrapped by {@link #runGlobalAutoSchedule} in an {@link AutoScheduleRunCache}
     *  so every placement/staffing attempt below reads/writes an in-memory mirror of this term's
     *  {@code ClassSchedule} rows instead of re-querying the database on every single (day, period)
     *  candidate — see that cache's own javadoc for why that was previously the dominant cost of an
     *  "All Cohorts" run (thousands of attempts × several fresh queries + a REQUIRES_NEW transaction
     *  each, not any one slow query). No placement/ordering behavior changes here — same inputs
     *  produce the same placed/unplaced result as before, just far fewer database round trips. */
    private GlobalAutoScheduleResult doRunGlobalAutoSchedule(Long termInstanceId, Long cohortId) {
        GlobalCapacityPrecheckResult precheck = precheckCapacity(termInstanceId);
        if (!precheck.overCapacityFaculty().isEmpty()) {
            List<ConstraintViolation> violations = precheck.overCapacityFaculty().stream()
                .map(f -> new ConstraintViolation("GLOBAL_AUTO_SCHEDULE_OVER_CAPACITY",
                    f.facultyName() + " needs " + formatHours(f.shortfallHours())
                        + " more capacity than currently configured — run the capacity precheck for remediation options"))
                .toList();
            throw new TimetableConstraintViolationException(violations);
        }

        LabClinicalVenueCapacityResult venueCapacity =
            timetableCapacityPlanningService.computeLabClinicalVenueCapacity(termInstanceId, PlanningBasis.SANCTIONED);
        if (!venueCapacity.overCapacityVenues().isEmpty()) {
            List<ConstraintViolation> violations = venueCapacity.overCapacityVenues().stream()
                .map(v -> new ConstraintViolation("GLOBAL_AUTO_SCHEDULE_VENUE_OVER_CAPACITY",
                    v.venueName() + " needs " + v.weeklyDemandPeriods() + " periods/week but only has "
                        + v.weeklyAvailablePeriods() + " available — run the capacity precheck for remediation options"))
                .toList();
            throw new TimetableConstraintViolationException(violations);
        }

        TermInstance term = requireTermInstance(termInstanceId);
        List<Period> periods = periodRepository.findByIsActiveTrueOrderByPeriodOrderAsc();
        Set<Long> cohortIds = resolveCohortIds(termInstanceId, cohortId);

        // Once this term's timetable is approved/PUBLISHED on Draft Review, that's the line past
        // which only manual period/staff edits (swap staff, swap sessions) are allowed -- never a
        // full automated re-run, at any cost. Room allocation being committed in Capacity Planner is
        // a prerequisite for placement, not a "stop touching it" signal, so it plays no part in this
        // gate. An explicit single-cohort request against a published term is a hard block (the
        // frontend also disables the action before this is ever reached, but this defensive re-check
        // can't be bypassed via a direct API call, same philosophy as the capacity/venue prechecks
        // above). An "All Cohorts" run instead reports every cohort back by name as skipped, since
        // publish is atomic term-wide -- either every cohort in the term is skipped, or none are.
        List<SkippedPublishedCohort> skippedPublishedCohorts = new ArrayList<>();
        boolean termPublished = isTermTimetablePublished(termInstanceId);
        if (cohortId != null) {
            if (termPublished) {
                Cohort thisCohort = cohortRepository.findById(cohortId).orElse(null);
                throw new TimetableConstraintViolationException(List.of(new ConstraintViolation(
                    "GLOBAL_AUTO_SCHEDULE_TERM_PUBLISHED",
                    (thisCohort != null ? thisCohort.getDisplayName() : "This cohort")
                        + " — this term's timetable is already approved; only manual period/staff edits are allowed now")));
            }
        } else if (termPublished) {
            for (Long id : cohortIds) {
                Cohort thisCohort = cohortRepository.findById(id).orElse(null);
                skippedPublishedCohorts.add(new SkippedPublishedCohort(id,
                    thisCohort != null ? thisCohort.getDisplayName() : ("Cohort " + id)));
            }
            cohortIds = Set.of();
        }

        PurgeOutcome purge = purgeDraftCellsForRebuild(termInstanceId, cohortIds);
        int staleDraftsCleared = purge.cleared();
        // One term-wide snapshot, reused for every cohort's self-study fallback ranking below --
        // computeTermDemand is O(cohorts x offerings) itself, so calling it once per cohort here
        // instead of once for the whole run would turn an already-expensive "All Cohorts" run
        // quadratic (see AutoScheduleRunCache's own javadoc for the perf history this class is
        // careful about). Slight staleness against sessions this same run places is acceptable --
        // this only feeds an advisory ranking; the real gate is tryStaffWithFallback's live check.
        TermDemandAggregation termDemand = computeTermDemand(termInstanceId);

        int totalPlaced = 0;
        int totalStaffed = 0;
        int totalUnfillableSelfStudyPeriods = 0;
        List<CohortPlacementSummary> summaries = new ArrayList<>();
        Set<Long> electiveGroupIdsSeen = new LinkedHashSet<>();

        // Phase 0: build every cohort's own context (skeleton, per-cohort dayLoad, unplaced list)
        // and flatten its still-short rows -- but LAB/CLINICAL rows go into one global queue instead
        // of this cohort's own list. THEORY rows stay per-cohort in the context since nothing about
        // them is ever shared across cohorts (see TaggedShortfallRow's javadoc).
        List<CohortRunContext> contexts = new ArrayList<>();
        Map<Long, CohortRunContext> contextsById = new LinkedHashMap<>();
        List<TaggedShortfallRow> globalLabClinicalQueue = new ArrayList<>();
        for (Long id : cohortIds) {
            Cohort cohort = cohortRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cohort not found with id: " + id));
            SkeletonBuilderResponse skeleton = timetableSkeletonService.getCohortSkeleton(termInstanceId, id);
            List<AutoPlaceUnplacedItem> unplacedForCohort = new ArrayList<>();

            List<ShortfallRow> theoryRows = new ArrayList<>();
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
                            occupantLabel(budget), "no faculty assigned on its Course Offering", subject.courseOfferingId()));
                        continue;
                    }
                    List<EligibleFacultyCandidateDto> fallbackCandidates = rankedFallbackCandidates(offering, facultyId, termDemand);
                    List<Long> candidateFacultyIds = new ArrayList<>();
                    candidateFacultyIds.add(facultyId);
                    candidateFacultyIds.addAll(fallbackCandidates.stream().map(EligibleFacultyCandidateDto::facultyId).toList());
                    ShortfallRow row = new ShortfallRow(subject.subjectName(), offering, budget, facultyId,
                        candidateFacultyIds, fallbackCandidates.stream()
                            .collect(Collectors.toMap(EligibleFacultyCandidateDto::facultyId, c -> c, (a, b) -> a)),
                        shortfall, CurriculumHoursCalculator.resolveBlockSize(offering.getSubject(), budget.sessionType()));
                    if (budget.sessionType() == ClassSessionType.THEORY) {
                        theoryRows.add(row);
                    } else {
                        globalLabClinicalQueue.add(new TaggedShortfallRow(id, row));
                    }
                }
            }
            theoryRows.sort(SHORTFALL_ROW_ORDER);

            // Whole-cohort daily load, seeded from every already-placed cell (any subject, any
            // session type, any section/batch) so the day-candidate order in tryPlaceAndStaff below
            // reflects how busy each day already looks to these students, not just to one subject's
            // own row. Without this, every row independently tries Monday first (see
            // tryPlaceAndStaff's old fixed DayOfWeek.values() order), so several single-session
            // subjects pile onto the same day's early periods and never come back to fill that same
            // day's afternoon -- leaving it structurally empty while a later, harder-to-place LAB/
            // CLINICAL block fills a different day to capacity. Least-loaded-day-first spreads the
            // week's real content evenly instead.
            Map<DayOfWeek, Integer> dayLoad = new EnumMap<>(DayOfWeek.class);
            for (DayOfWeek d : DayOfWeek.values()) {
                dayLoad.put(d, 0);
            }
            for (SkeletonCellResponse cell : skeleton.cells()) {
                dayLoad.merge(cell.dayOfWeek(), 1, Integer::sum);
            }

            // Seeded from cells already placed before this run started, not just ones this run adds
            // -- a batch whose sibling already has Monday+Tuesday from an earlier run must still
            // prefer those same days for its own remaining shortfall today.
            Map<String, Set<DayOfWeek>> siblingDaysByOfferingAndType = new LinkedHashMap<>();
            for (SkeletonCellResponse cell : skeleton.cells()) {
                if (cell.sessionType() == ClassSessionType.THEORY || cell.courseOfferingId() == null) {
                    continue;
                }
                siblingDaysByOfferingAndType
                    .computeIfAbsent(offeringSessionTypeKey(cell.courseOfferingId(), cell.sessionType()), k -> new HashSet<>())
                    .add(cell.dayOfWeek());
            }

            CohortRunContext context = new CohortRunContext(id, cohort, skeleton, unplacedForCohort, theoryRows,
                dayLoad, new ArrayList<>(), siblingDaysByOfferingAndType);
            contexts.add(context);
            contextsById.put(id, context);
        }

        // Phase 1: place LAB/CLINICAL across every cohort together, most-constrained-first GLOBALLY
        // -- whichever row (from any cohort) has the largest remaining shortfall gets first pick of
        // a shared venue's scarce day/period slots, instead of one cohort exhausting a shared Lab/
        // Clinical venue's whole week before the next cohort -- ordered only by {@code Set}
        // iteration, not by any real priority -- ever got a turn. Real seed data confirms venues ARE
        // shared across cohorts today (e.g. "Community Health Center" serves two different BSc
        // cohorts' Community Health postings in the same term) -- this phase is what makes that
        // sharing fair instead of first-iterated-wins. This does NOT create capacity that doesn't
        // exist: if combined demand across cohorts for one venue genuinely exceeds its weekly
        // ceiling, someone still comes up short -- this only decides who, by need rather than by
        // chance.
        double periodDurationHours = averagePeriodDurationHours(periods);
        Map<String, VenueGapAccumulator> venueGaps = new LinkedHashMap<>();
        // One entry per session actually placed with a fallback candidate instead of the row's own
        // bound faculty (see ShortfallRow#candidateFacultyIds) -- aggregated into
        // GlobalAutoScheduleResult#facultySubstitutionTips at the end of this run, surfaced as an
        // actionable tip rather than silently absorbed: if reassigning an offering to a different,
        // already-eligible faculty member would have avoided needing the fallback at all, an admin
        // should be told, not left to notice only if they happen to audit the timetable by hand.
        List<FacultySubstitutionEvent> facultySubstitutionEvents = new ArrayList<>();

        // Phase B: cross-offering LAB pairing -- runs before the main queue is sorted/consumed below,
        // since a pairing decision claims BOTH offerings' rows together (see
        // #attemptCrossOfferingPairing's javadoc). V1 only fires for the narrow, well-supported shape
        // confirmed this session: two offerings serving the exact same CohortSection, each split into
        // exactly 2 active batches sharing one Lab, each needing exactly one session/week of matching
        // block size. Any offering not meeting every gate is left untouched in the queue and falls
        // through to independent placement (plus Phase A's idle-batch fallback) exactly as before.
        int rotationGroupsCreated = 0;
        List<String> pairingSkipReasons = new ArrayList<>();
        for (CohortRunContext context : contexts) {
            rotationGroupsCreated += attemptCrossOfferingPairing(context, term, periods, globalLabClinicalQueue, pairingSkipReasons);
        }

        globalLabClinicalQueue.sort(Comparator.comparingInt((TaggedShortfallRow t) -> -t.row().shortfall()));
        for (TaggedShortfallRow tagged : globalLabClinicalQueue) {
            CohortRunContext context = contextsById.get(tagged.cohortId());
            placeShortfallRow(tagged.cohortId(), tagged.row(), term, periods, context, periodDurationHours, venueGaps, facultySubstitutionEvents);
        }

        // Phase 1.5: a cohort section split into batches for LAB/CLINICAL (one lab/venue can't hold
        // everyone at once) leaves every sibling batch NOT currently occupying that lab completely
        // unscheduled at the exact slot where one batch has its turn — Theory can't cover for them
        // (it's whole-section, never split), so those periods were dead time. Must run here, before
        // Phase 2 (THEORY): it needs the exact (day, period) each LAB/CLINICAL cell just landed on,
        // known only right after Phase 1 places it. Cannot take a slot Phase 2 would have legitimately
        // needed — that slot is already hard-blocked for THEORY at this cohort/section regardless.
        for (CohortRunContext context : contexts) {
            fillIdleBatchGaps(context.cohortId(), context.skeleton(), term, context.placedThisCohortRun(),
                context.unplacedForCohort(), termDemand);
        }

        // Phase 2: THEORY, per cohort -- never contends for a cross-cohort resource (each active
        // CohortSection has its own exclusive committed classroom), so cohort order here is
        // irrelevant the way it's load-bearing in Phase 1.
        for (CohortRunContext context : contexts) {
            for (ShortfallRow row : context.theoryRows()) {
                placeShortfallRow(context.cohortId(), row, term, periods, context, periodDurationHours, venueGaps, facultySubstitutionEvents);
            }
        }

        // Phase 3: electives -- only each group's one shared slot is automated (see class javadoc).
        // Counts fold into the totals but aren't attributed to any single cohort summary row, since
        // a group can span students from more than one cohort. This runs BEFORE the Library/
        // Self-Study passes below, not after: an elective is real curriculum content with its own
        // required weekly session, whereas Library and Self-Study are deliberately greedy filler
        // that claims every remaining empty Monday-Friday period. While every run inherited the
        // previous run's elective cells the order didn't matter (the group was already placed, so
        // this pass no-opped); now that purgeDraftCellsForRebuild clears the whole DRAFT grid first,
        // leaving it last would let filler swallow the entire week before the elective group ever
        // got a slot to ask for.
        List<AutoPlaceUnplacedItem> electiveUnplaced = new ArrayList<>();
        for (Long electiveGroupId : electiveGroupIdsSeen) {
            int placed = placeAndStaffElectiveGroup(termInstanceId, electiveGroupId, term, periods, electiveUnplaced);
            totalPlaced += placed;
            totalStaffed += placed;
        }

        // Library runs as its OWN pass, most-constrained-cohort-first, before the main per-cohort
        // loop below -- the exact same fairness principle Phase 1 already applies to a shared LAB/
        // CLINICAL venue, now extended to the one shared Library room every cohort in this term
        // competes for. Sequential per-cohort processing (the previous behavior) let whichever cohort
        // happened to iterate last get stuck with whatever room+slot overlaps the earlier cohorts
        // hadn't already claimed, even though the room itself sits well under half-utilized across
        // the week -- the real scarcity was never raw room-time, it was WHICH cohort got first pick
        // of the narrow overlap between "this room is free" and "this cohort is free" once other
        // cohorts had already taken the easy overlaps. A cohort with a heavier committed Theory/Lab/
        // Clinical week (more of {@code dayLoad} already spoken for) has fewer alternative free
        // days/periods to fall back on than a lightly-loaded cohort does, so it goes first.
        List<CohortRunContext> libraryFillOrder = contexts.stream()
            .sorted(Comparator.comparingInt((CohortRunContext c) ->
                c.dayLoad().values().stream().mapToInt(Integer::intValue).sum()).reversed())
            .toList();
        for (CohortRunContext context : libraryFillOrder) {
            LibraryGapFillOutcome libraryOutcome = fillLibraryGaps(context.cohortId(), term, periods,
                context.dayLoad(), context.unplacedForCohort(), context.skeleton().cells());
            context.placedThisCohortRun().addAll(libraryOutcome.filled());
        }

        for (CohortRunContext context : contexts) {
            // Self-Study fills whatever Library didn't claim -- both stay greedy filler, LAST of all
            // four phases (see the class javadoc's "Placement order" section for why), just no longer
            // sharing Library's own cross-cohort ordering concern (Self-Study never contends for a
            // shared resource the way the one Library room does).
            SelfStudyGapFillOutcome gapFillOutcome = fillSelfStudyGaps(context.cohortId(), context.skeleton(), term,
                periods, context.dayLoad(), context.unplacedForCohort(), termDemand);
            context.placedThisCohortRun().addAll(gapFillOutcome.filled());
            totalUnfillableSelfStudyPeriods += gapFillOutcome.unfillablePeriods();

            // Phase 5: now that filler exists to bump, try to upgrade any THEORY row that only
            // found room on Saturday earlier into a genuine Monday-Friday slot (see class javadoc).
            attemptSaturdayUpgradeMending(context.cohortId(), term, periods, context);

            int placedForCohort = context.placedThisCohortRun().size();
            boolean usedSaturdayForCohort = context.placedThisCohortRun().stream().anyMatch(p -> p.dayOfWeek() == DayOfWeek.SATURDAY);
            totalPlaced += placedForCohort;
            totalStaffed += placedForCohort;
            summaries.add(new CohortPlacementSummary(context.cohortId(), context.cohort().getDisplayName(),
                placedForCohort, placedForCohort, context.unplacedForCohort(), usedSaturdayForCohort));
        }

        double capacityCausedGapHours = totalUnfillableSelfStudyPeriods * averagePeriodDurationHours(periods);
        double averageDailyCapacityHours = averageConfiguredDailyCapacityHours(termDemand.workingDaysInTerm(), termDemand.weeksInTerm());
        int recommendedAdditionalFacultyCount = capacityCausedGapHours > 0.001 && averageDailyCapacityHours > 0 && termDemand.workingDaysInTerm() > 0
            ? (int) Math.ceil(capacityCausedGapHours / (averageDailyCapacityHours * termDemand.workingDaysInTerm()))
            : 0;

        List<VenueCapacityGap> venueCapacityGaps = venueGaps.values().stream()
            .filter(v -> v.unplacedHours > 0.001)
            .sorted(Comparator.comparingDouble((VenueGapAccumulator v) -> -v.unplacedHours))
            .map(v -> new VenueCapacityGap(v.venueId, v.venueType, v.venueName, v.capacity, v.unplacedHours,
                new ArrayList<>(v.subjects.values()), new ArrayList<>(v.subjects.keySet())))
            .toList();

        return new GlobalAutoScheduleResult(totalPlaced, totalStaffed, summaries, electiveUnplaced, staleDraftsCleared,
            purge.pinnedPreserved(), capacityCausedGapHours, recommendedAdditionalFacultyCount, venueCapacityGaps, skippedPublishedCohorts,
            rotationGroupsCreated, pairingSkipReasons, buildFacultySubstitutionTips(facultySubstitutionEvents));
    }

    /** This run's real, exact-count "still couldn't fill it, even after trying every eligible
     *  faculty" hours, converted from {@code totalUnfillableSelfStudyPeriods} via each period's own
     *  real duration rather than an assumed flat hour — distinct from {@link
     *  FacultyWorkloadOverviewReport#recommendedAdditionalFacultyCount}'s pre-run whole-pool
     *  estimate, which never reflects real day/period feasibility (block-size, contiguity, elective
     *  anchors), only raw aggregate hours. */
    private static double averagePeriodDurationHours(List<Period> periods) {
        return periods.stream()
            .mapToDouble(p -> java.time.Duration.between(p.getStartTime(), p.getEndTime()).toMinutes() / 60.0)
            .average().orElse(1.0);
    }

    /** Same "average configured daily cap across every faculty who actually has one" reference used
     *  for {@link FacultyWorkloadOverviewReport#recommendedAdditionalFacultyCount}, recomputed here
     *  from a live active-faculty scan rather than threaded through from that report (this run may
     *  be scoped to a single cohort, so the whole-term report isn't necessarily in scope/fresh). */
    private double averageConfiguredDailyCapacityHours(int workingDaysInTerm, int weeksInTerm) {
        List<Double> caps = new ArrayList<>();
        for (Faculty faculty : facultyRepository.findByStatus(FacultyStatus.ACTIVE)) {
            CapacityResolution capacity = resolveEffectiveTermCapacity(faculty, workingDaysInTerm, weeksInTerm);
            if (capacity != null) {
                caps.add(capacity.dailyCapForDisplay());
            }
        }
        return caps.isEmpty() ? 0 : caps.stream().mapToDouble(Double::doubleValue).average().orElse(0);
    }

    /** Clears EVERY DRAFT cell for the cohorts in scope, so each run re-packs the week from an
     *  empty grid instead of adding on top of whatever previous runs left behind. A PUBLISHED cell
     *  is never touched -- {@link #doRunGlobalAutoSchedule}'s own publish gate already refuses to
     *  run against a published term at all, so in practice everything reachable here is DRAFT.
     *
     *  <p>This replaced (2026-09-02) a narrower purge that only cleared cells a budget could prove
     *  were excess -- over-budget scopes, section-less THEORY ghosts, extra Library days, truncated
     *  multi-period blocks. That was strictly not enough, because the cells doing the real damage
     *  were all individually legitimate. The placement ORDER inside one run is already correct
     *  (LAB/CLINICAL blocks first, then THEORY, then electives, then Library/Self-Study filler), but
     *  it only holds for cells that run places itself: every DRAFT cell an EARLIER run left behind
     *  was immovable, since {@link #attemptBacktrack} can only displace placements from the current
     *  run. So run N's cheap, single-period THEORY and LIBRARY sessions became run N+1's permanent
     *  obstacles -- and a single 50-minute period landing at P4 or P5 destroys an entire half-day
     *  4-period CLINICAL window (see {@link PeriodGapPolicy}: a clinical block may cross a recess
     *  but never lunch, so a 6-period day offers exactly two legal 4-block positions, forenoon and
     *  afternoon). Real incident: BSc Nursing (2025-2029) had 10 of its 12 weekly clinical windows
     *  blocked by 12 stray THEORY/LIBRARY singles, leaving Clinical permanently stuck at 2 of the 4
     *  weekly sessions it needed while the report truthfully insisted there was nowhere to put them.
     *
     *  <p>Rebuilding makes a re-run idempotent: the same inputs now produce the same grid, and a
     *  fragmented week can always be recovered by simply running automation again.
     *
     *  <p>PINNED cells are the deliberate exception. This pass used to clear every DRAFT cell
     *  unconditionally, so an admin's manual drag-move or swap was destroyed by the next run — a
     *  documented tradeoff that is nonetheless incompatible with the draft-review model, where the
     *  generated grid is a starting point a human then reshapes before approving. A cell the admin
     *  positioned (manual place/move/swap, or an explicit pin) now survives, and automation packs
     *  the remaining week around it. Idempotence is preserved in the sense that matters: re-running
     *  with the same inputs AND the same pins reproduces the same grid.
     *
     *  <p>Returns how many were cleared, for {@link GlobalAutoScheduleResult#staleDraftsCleared()}'s
     *  visibility -- deliberately never silent. */
    private PurgeOutcome purgeDraftCellsForRebuild(Long termInstanceId, Set<Long> cohortIds) {
        Set<Long> idsToDeactivate = new LinkedHashSet<>();
        int pinnedPreserved = 0;
        for (Long cohortId : cohortIds) {
            SkeletonBuilderResponse skeleton = timetableSkeletonService.getCohortSkeleton(termInstanceId, cohortId);
            for (SkeletonCellResponse cell : skeleton.cells()) {
                if (cell.status() == com.cms.model.enums.ClassScheduleStatus.DRAFT && cell.pinned()) {
                    pinnedPreserved++;
                }
                // A pinned cell is one a human placed on purpose, so the rebuild leaves it standing
                // and re-packs the rest of the week around it. It still occupies its slot for every
                // downstream check (the skeleton snapshot below is read after this purge, so the
                // survivor shows up as an ordinary existing cell), which is exactly what makes
                // "automation works around my decisions" true rather than "automation ignores them".
                if (cell.status() == com.cms.model.enums.ClassScheduleStatus.DRAFT && !cell.pinned()) {
                    idsToDeactivate.add(cell.id());
                }
            }
        }

        if (idsToDeactivate.isEmpty()) {
            return new PurgeOutcome(0, pinnedPreserved);
        }
        purgeRotationRowsForCells(idsToDeactivate);
        List<ClassSchedule> toDeactivate = classScheduleRepository.findAllById(idsToDeactivate);
        for (ClassSchedule cs : toDeactivate) {
            cs.setIsActive(false);
            classScheduleRepository.save(cs);
            AutoScheduleRunCache.current().ifPresent(cache -> cache.recordRemoval(cs));
        }
        return new PurgeOutcome(toDeactivate.size(), pinnedPreserved);
    }

    /** Both halves of what the rebuild did to the existing DRAFT grid: how many cells it cleared,
     *  and how many it deliberately left standing because they were pinned. */
    private record PurgeOutcome(int cleared, int pinnedPreserved) {}

    /** Global Auto-Schedule fully owns creating a cross-offering {@link RotationSlot}-based
     *  rotation fresh every run (see the pairing sub-phase at the top of Phase 1) — so a rotation
     *  tied to a cell this rebuild is about to deactivate has no future life and must not be left
     *  pointing at a now-inactive cell. Hard-deleted here, not soft-deactivated: a {@link
     *  RotationSlot}/{@link RotationMemberAssignment}/{@link RotationGroup} row has no other
     *  consumer once orphaned ({@link RotationResolverService} is read-only, queried only by id
     *  from a live slot). Deletes bottom-up (assignments, then slots, then any now-empty group and
     *  its members) to respect the FK chain — a manually-created rotation from the "Set up
     *  Rotation" flyout is cleaned up the exact same way if any of its cells happen to be DRAFT and
     *  in this run's scope, since nothing here distinguishes who created it. */
    private void purgeRotationRowsForCells(Set<Long> classScheduleIds) {
        List<Long> ids = new ArrayList<>(classScheduleIds);
        List<RotationSlot> slots = rotationSlotRepository.findByClassScheduleIdIn(ids);
        if (slots.isEmpty()) {
            return;
        }
        Set<Long> affectedGroupIds = slots.stream().map(s -> s.getRotationGroup().getId()).collect(Collectors.toSet());

        List<RotationMemberAssignment> assignments = rotationMemberAssignmentRepository.findByRotationSlot_ClassSchedule_IdIn(ids);
        rotationMemberAssignmentRepository.deleteAllInBatch(assignments);
        rotationSlotRepository.deleteAllInBatch(slots);

        for (Long groupId : affectedGroupIds) {
            long remainingSlots = rotationSlotRepository.countByRotationGroupId(groupId);
            if (remainingSlots == 0) {
                rotationMemberRepository.deleteAllInBatch(rotationMemberRepository.findByRotationGroupIdOrderByMemberOrderAsc(groupId));
                rotationGroupRepository.deleteById(groupId);
            }
        }
    }

    /** One offering's LAB shortfall shape Phase B can consider pairing -- exactly 2 active batches
     *  sharing one Lab, both still needing their one weekly session, at this run's fixed block size.
     *  {@code row0}/{@code row1} are the exact {@link TaggedShortfallRow} instances from the caller's
     *  queue (by reference), so a successful pairing can remove them from it directly. */
    private record PairableOfferingGroup(Long cohortSectionId, CourseOffering offering, int blockSize, Long facultyId,
                                          Batch batch0, Batch batch1, TaggedShortfallRow row0, TaggedShortfallRow row1) {}

    private record RotationPairingOutcome(Placement placementA, Placement placementB) {}

    /** Phase B — cross-offering LAB pairing. When two offerings serving the exact same {@link
     *  CohortSection} each split into exactly 2 active batches sharing one (different) Lab, and both
     *  need exactly one session/week of the same block size, alternates the two physical groups
     *  between the two Labs week-to-week via a {@link RotationGroup} instead of leaving the "off-duty"
     *  batch idle at that slot (Phase A's generic Library/Self-Study filler would otherwise cover it).
     *  V1 is deliberately this narrow — see this session's specialist round: real fixture data
     *  (Child Health Nursing, 4 batches vs. Educational Technology, 2 batches, same cohort section)
     *  has a mismatched count and is correctly left unpaired, falling straight through to independent
     *  placement. A subject needing MORE than one session/week is also excluded outright: this pass
     *  only ever creates ONE shared weekly slot, so pairing a multi-session-per-week row would silently
     *  under-deliver the rest of its weekly requirement with nothing left in the queue to catch it.
     *
     * <p>Mutates {@code queue} in place — removes both rows of each offering it successfully pairs so
     *  the caller's own Phase 1 loop never independently re-places them on top of the rotation. Runs
     *  once per cohort context, entirely before that loop starts, since a pairing decision must claim
     *  two offerings' rows together — discovering this after either was already independently placed
     *  would be too late. Returns how many {@link RotationGroup}s this cohort's pass created. */
    private int attemptCrossOfferingPairing(CohortRunContext context, TermInstance term, List<Period> periods,
                                             List<TaggedShortfallRow> queue, List<String> skipReasons) {
        Long cohortId = context.cohortId();
        List<TaggedShortfallRow> candidates = queue.stream()
            .filter(t -> t.cohortId().equals(cohortId))
            .filter(t -> t.row().budget().sessionType() == ClassSessionType.LAB)
            .filter(t -> t.row().shortfall() == 1 && t.row().budget().requiredSessionsPerWeek() == 1)
            .filter(t -> t.row().budget().batchId() != null && t.row().budget().cohortSectionId() != null)
            .toList();

        Map<String, List<TaggedShortfallRow>> byOfferingWithinSection = candidates.stream()
            .collect(Collectors.groupingBy(
                t -> t.row().budget().cohortSectionId() + ":" + t.row().offering().getId(),
                LinkedHashMap::new, Collectors.toList()));

        List<PairableOfferingGroup> pairable = new ArrayList<>();
        for (List<TaggedShortfallRow> rows : byOfferingWithinSection.values()) {
            if (rows.size() != 2) {
                continue; // this run's queue doesn't show exactly 2 shortfall rows for the offering -- not this shape
            }
            Long sectionId = rows.get(0).row().budget().cohortSectionId();
            CourseOffering offering = rows.get(0).row().offering();
            List<Batch> activeBatches = batchRepository.findByCourseOfferingId(offering.getId()).stream()
                .filter(b -> Boolean.TRUE.equals(b.getIsActive()))
                .filter(b -> b.getCohortSection() != null && b.getCohortSection().getId().equals(sectionId))
                .toList();
            if (activeBatches.size() != 2) {
                continue; // real active batch count doesn't match -- never guess, skip
            }
            Batch batch0 = activeBatches.get(0);
            Batch batch1 = activeBatches.get(1);
            if (batch0.getLab() == null || batch1.getLab() == null
                    || !batch0.getLab().getId().equals(batch1.getLab().getId())) {
                continue; // offering's own 2 batches must already share ONE lab -- an already multi-venue-split offering isn't idle
            }
            int blockSize = rows.get(0).row().blockSize();
            if (rows.get(1).row().blockSize() != blockSize) {
                continue;
            }
            pairable.add(new PairableOfferingGroup(sectionId, offering, blockSize, rows.get(0).row().facultyId(),
                batch0, batch1, rows.get(0), rows.get(1)));
        }

        int created = 0;
        Set<Long> consumedOfferingIds = new HashSet<>();
        for (int i = 0; i < pairable.size(); i++) {
            PairableOfferingGroup a = pairable.get(i);
            if (consumedOfferingIds.contains(a.offering().getId())) {
                continue;
            }
            for (int j = i + 1; j < pairable.size(); j++) {
                PairableOfferingGroup b = pairable.get(j);
                if (consumedOfferingIds.contains(b.offering().getId())
                        || !a.cohortSectionId().equals(b.cohortSectionId())
                        || a.blockSize() != b.blockSize()
                        || a.batch0().getLab().getId().equals(b.batch0().getLab().getId())) {
                    continue;
                }
                RotationPairingOutcome outcome = tryPairOfferings(cohortId, a.offering(), a.facultyId(), a.batch0(), a.batch1(),
                    b.offering(), b.facultyId(), b.batch0(), b.batch1(), a.blockSize(), term, periods, context);
                if (outcome == null) {
                    skipReasons.add("Could not find a shared free slot to pair " + a.offering().getSubject().getName()
                        + " with " + b.offering().getSubject().getName() + " (cohort section " + a.cohortSectionId() + ")");
                    continue;
                }
                context.placedThisCohortRun().add(outcome.placementA());
                context.placedThisCohortRun().add(outcome.placementB());
                queue.removeIf(t -> t == a.row0() || t == a.row1() || t == b.row0() || t == b.row1());
                consumedOfferingIds.add(a.offering().getId());
                consumedOfferingIds.add(b.offering().getId());
                created++;
                break;
            }
        }
        return created;
    }

    /** Generalizes {@link #tryPlaceAndStaff}'s single-row day/period search to require BOTH
     *  offerings' representative cell (each offering's ordinal batch0) to land on the exact same
     *  (day, block) — the shared slot the two physical groups actually swap through week to week.
     *  Monday-Friday only, least-loaded-day-first (mirrors {@link #tryPlaceAndStaff}'s own ordering) —
     *  a shared cross-offering rotation landing on a Saturday that might not even run every week
     *  would be a worse outcome than simply not pairing, so V1 doesn't offer it that fallback.
     *
     * <p>On success, calls {@link RotationGroupService#create} with the two just-placed cells as its
     *  two slots and the two ordinal batch-pairs (batch0/batch0, batch1/batch1 — see class-level
     *  javadoc for why ordinal pairing is treated as safe here) as its two members. On ANY failure
     *  past this point — a hard rotation-side validation this method didn't already itself check,
     *  e.g. a real roster/venue mismatch — both cells are torn back down and {@code null} is returned;
     *  a caller must never be left with one real placed cell and no rotation covering it. */
    private RotationPairingOutcome tryPairOfferings(Long cohortId, CourseOffering offeringA, Long facultyA, Batch batchA0, Batch batchA1,
                                                     CourseOffering offeringB, Long facultyB, Batch batchB0, Batch batchB1,
                                                     int blockSize, TermInstance term, List<Period> periods, CohortRunContext context) {
        List<DayOfWeek> candidateDays = Arrays.stream(DayOfWeek.values())
            .filter(d -> d != DayOfWeek.SATURDAY)
            .sorted(Comparator.comparingInt(context.dayLoad()::get))
            .toList();

        for (DayOfWeek day : candidateDays) {
            for (int startIdx = 0; startIdx + blockSize <= periods.size(); startIdx++) {
                List<Period> block = periods.subList(startIdx, startIdx + blockSize);
                boolean hasGap = false;
                for (int k = 1; k < block.size(); k++) {
                    if (!block.get(k - 1).getEndTime().equals(block.get(k).getStartTime())) {
                        hasGap = true;
                        break;
                    }
                }
                if (hasGap) {
                    continue;
                }
                boolean anyBlocked = false;
                for (Period p : block) {
                    if (blockedPeriodChecker.blockReason(day, p.getStartTime(), p.getEndTime(), term).isPresent()) {
                        anyBlocked = true;
                        break;
                    }
                }
                if (anyBlocked) {
                    continue;
                }
                Period primary = block.get(0);
                List<Long> spanPeriodIds = block.size() > 1
                    ? block.subList(1, block.size()).stream().map(Period::getId).toList() : null;

                SkeletonCellResponse placedA;
                try {
                    placedA = timetableSkeletonService.placeCell(new SkeletonCellPlacementRequest(
                        offeringA.getId(), ClassSessionType.LAB, day, primary.getId(), batchA0.getId(), cohortId,
                        batchA0.getCohortSection() != null ? batchA0.getCohortSection().getId() : null, spanPeriodIds));
                } catch (TimetableConstraintViolationException | LifecycleConflictException | IllegalArgumentException ex) {
                    continue;
                }
                try {
                    timetableStaffingService.staffCell(placedA.id(), new StaffingAssignmentRequest(facultyA, null));
                } catch (TimetableConstraintViolationException | LifecycleConflictException | IllegalArgumentException ex) {
                    // placedA is real (placeCell above succeeded) -- must be torn down here, unlike
                    // the placeCell-failure branch above where there is nothing yet to remove.
                    timetableSkeletonService.removeCell(placedA.id());
                    continue;
                }

                SkeletonCellResponse placedB;
                try {
                    placedB = timetableSkeletonService.placeCell(new SkeletonCellPlacementRequest(
                        offeringB.getId(), ClassSessionType.LAB, day, primary.getId(), batchB0.getId(), cohortId,
                        batchB0.getCohortSection() != null ? batchB0.getCohortSection().getId() : null, spanPeriodIds));
                } catch (TimetableConstraintViolationException | LifecycleConflictException | IllegalArgumentException ex) {
                    timetableSkeletonService.removeCell(placedA.id());
                    continue;
                }
                try {
                    timetableStaffingService.staffCell(placedB.id(), new StaffingAssignmentRequest(facultyB, null));
                } catch (TimetableConstraintViolationException | LifecycleConflictException | IllegalArgumentException ex) {
                    // placedB is real here too (placeCell above succeeded) -- both cells must come
                    // down, not just placedA as before, or placedB leaks as an orphaned unstaffed
                    // DRAFT cell that silently occupies this exact day/period/venue for the rest of
                    // this cohort's pairing search (this was the actual bug behind the DIAG log: 18
                    // single removeCell calls tearing down only the "A" side while "B" leaked unlogged).
                    timetableSkeletonService.removeCell(placedB.id());
                    timetableSkeletonService.removeCell(placedA.id());
                    continue;
                }

                LocalDate anchor = firstOccurrenceOnOrAfter(term.getStartDate(), day);
                RotationGroupCreateRequest request = new RotationGroupCreateRequest(
                    term.getId(),
                    offeringA.getSubject().getName() + " / " + offeringB.getSubject().getName() + " rotation",
                    anchor,
                    List.of(new RotationGroupCreateRequest.RotationSlotInput(placedA.id(), 0),
                            new RotationGroupCreateRequest.RotationSlotInput(placedB.id(), 1)),
                    List.of(
                        new RotationGroupCreateRequest.RotationMemberInput(0, "Group 1", List.of(
                            new RotationGroupCreateRequest.RotationAssignmentInput(placedA.id(), batchA0.getId()),
                            new RotationGroupCreateRequest.RotationAssignmentInput(placedB.id(), batchB0.getId()))),
                        new RotationGroupCreateRequest.RotationMemberInput(1, "Group 2", List.of(
                            new RotationGroupCreateRequest.RotationAssignmentInput(placedA.id(), batchA1.getId()),
                            new RotationGroupCreateRequest.RotationAssignmentInput(placedB.id(), batchB1.getId())))));

                try {
                    rotationGroupService.create(request, "system:global-auto-schedule");
                } catch (RuntimeException ex) {
                    timetableSkeletonService.removeCell(placedB.id());
                    timetableSkeletonService.removeCell(placedA.id());
                    continue;
                }

                List<Long> periodIds = block.stream().map(Period::getId).toList();
                context.dayLoad().merge(day, blockSize, Integer::sum);
                // batchId left null on both Placements -- RotationGroupService#create already nulled
                // ClassSchedule#batch on both real cells, and Phase A's fillIdleBatchGaps explicitly
                // skips any Placement with a null batchId (it has no sibling-idle-time concept to
                // apply to a cell that's fully covered by a rotation every single week).
                Placement placementA = new Placement(placedA.id(), offeringA.getId(), ClassSessionType.LAB, null,
                    batchA0.getCohortSection() != null ? batchA0.getCohortSection().getId() : null, facultyA,
                    offeringA.getSubject().getName(), "Rotation", day, periodIds);
                Placement placementB = new Placement(placedB.id(), offeringB.getId(), ClassSessionType.LAB, null,
                    batchB0.getCohortSection() != null ? batchB0.getCohortSection().getId() : null, facultyB,
                    offeringB.getSubject().getName(), "Rotation", day, periodIds);
                return new RotationPairingOutcome(placementA, placementB);
            }
        }
        return null;
    }

    /** First date on or after {@code start} falling on {@code target} — matches {@code
     *  EscortRotationResolverService#nextOrSame}'s exact bridging of this file's {@code
     *  com.cms.model.enums.DayOfWeek} to {@code java.time.DayOfWeek} via name, the same pattern
     *  {@link RotationGroupService#create} itself uses to validate an anchor date's day of week. */
    private LocalDate firstOccurrenceOnOrAfter(LocalDate start, DayOfWeek target) {
        java.time.DayOfWeek javaTarget = java.time.DayOfWeek.valueOf(target.name());
        LocalDate date = start;
        while (date.getDayOfWeek() != javaTarget) {
            date = date.plusDays(1);
        }
        return date;
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

    /** True once this term's timetable has been approved on Draft Review — {@code
     *  TimetableGenerationService#approve} flips every {@code ClassSchedule} row for the whole term
     *  to {@code PUBLISHED} atomically, so this is a term-wide fact, not a per-cohort one: it can
     *  never be true for one cohort in a term and false for another. This is the line past which
     *  only manual period/staff edits (swap staff, swap sessions) are allowed, never a full
     *  automated re-run. Committing a Cohort Room Allocation in Capacity Planner does NOT trip this
     *  — that only unlocks placement (see {@code TimetableSkeletonService#resolveActiveSections}),
     *  it isn't itself a "done, stop touching this" signal. */
    private boolean isTermTimetablePublished(Long termInstanceId) {
        return classScheduleRepository.existsByTermInstanceIdAndStatus(termInstanceId, ClassScheduleStatus.PUBLISHED);
    }

    private String occupantLabel(SkeletonSubjectBudget budget) {
        return budget.cohortSectionLabel() != null ? budget.cohortSectionLabel() : budget.batchName();
    }

    /** The faculty who should actually staff this budget row. A LAB/CLINICAL row (always has a
     *  {@code batchId}) prefers its own {@link Batch#getCoordinatorFaculty()} first -- every
     *  parallel batch under a section used to be forced onto the SAME shared section-level
     *  faculty regardless of what {@code coordinatorFaculty} was actually set to, which is what
     *  silently capped real throughput (see OC-183): two batches in two venues at once genuinely
     *  need two different people at that moment, but with only one faculty ever resolved, the
     *  scheduler could only stagger them onto non-overlapping days instead of ever placing them
     *  in parallel. Per-batch resolution doesn't forbid assigning the same faculty to two
     *  batches -- that's legitimate as long as they're never placed at an overlapping day/time,
     *  which {@link TimetableStaffingService#checkFacultyFree} still enforces same as always; this
     *  change only lets a DIFFERENT faculty per batch actually take effect when one is set, which
     *  the old always-section-level lookup could never honor. Falls back to the section/cohort-level
     *  resolution below when the batch has no coordinator of its own yet, so pre-existing
     *  un-migrated batches keep scheduling exactly as before. A THEORY row split across active
     *  {@link CohortSection}s (no {@code batchId}) resolves its own {@link
     *  CourseOfferingSectionFaculty} section-level override; unsectioned THEORY resolves this
     *  cohort's whole-cohort row instead. Returns null (unplaced, reported as "no faculty
     *  assigned") when nothing has been assigned yet. */
    private Long resolveBudgetFacultyId(CourseOffering offering, SkeletonSubjectBudget budget, Long cohortId) {
        if (budget.batchId() != null) {
            Faculty coordinator = batchRepository.findById(budget.batchId())
                .map(Batch::getCoordinatorFaculty)
                .orElse(null);
            if (coordinator != null) {
                return coordinator.getId();
            }
        }
        if (budget.cohortSectionId() != null) {
            return currentSectionFacultyId(offering, budget.cohortSectionId());
        }
        return currentCohortFacultyId(offering, cohortId);
    }

    /** One still-short (subject, budget) row, already resolved to a real faculty id — flattened out
     *  of the per-subject/per-budget nested loop so the whole cohort's rows can be sorted together
     *  before any placement is attempted (see {@link #SHORTFALL_ROW_ORDER}). {@code blockSize} is
     *  how many consecutive periods one single session of this row must occupy (see {@link
     *  Subject#getLabSessionBlockPeriods()}/{@link Subject#getClinicalSessionBlockPeriods()}) —
     *  always 1 for THEORY, and defensively clamped to at least 1 for LAB/CLINICAL in case a
     *  subject's configured value is ever null/invalid. {@code candidateFacultyIds} is {@code
     *  facultyId} first, then every other eligible-and-not-over-capacity faculty for this subject,
     *  ranked least-remaining-capacity-first (see {@link #rankedFallbackFacultyIds}) — computed once
     *  here rather than per placement attempt, since a row's own eligible pool doesn't change mid-run.
     *  {@link #tryPlaceAndStaff} tries every one of these in order at each candidate slot before
     *  giving up on it, the same fallback the Self-Study filler pass already had, now extended to
     *  every ordinary curriculum row too — a mandatory subject whose sole bound faculty is booked
     *  elsewhere at every remaining slot no longer comes up unplaced when the audience's own week
     *  still has genuine free room and another eligible faculty member could cover it. {@code
     *  fallbackCandidatesById} is every id in {@code candidateFacultyIds} past the primary, keyed by
     *  id, still carrying its own {@code remainingHours}/{@code capacityTier} workload snapshot from
     *  {@link #rankedFallbackCandidates} — {@link #recordFacultySubstitutionIfAny} looks up whichever
     *  one actually got used so a substitution tip can report the substitute's own availability, not
     *  just their name. */
    private record ShortfallRow(String subjectName, CourseOffering offering, SkeletonSubjectBudget budget, Long facultyId,
                                 List<Long> candidateFacultyIds, Map<Long, EligibleFacultyCandidateDto> fallbackCandidatesById,
                                 int shortfall, int blockSize) {}

    /** A LAB/CLINICAL {@link ShortfallRow} paired with which cohort it belongs to, so every cohort's
     *  rows of this type can be pooled into one globally-sorted queue (see {@link
     *  #doRunGlobalAutoSchedule}'s Phase 1) instead of being placed strictly cohort-by-cohort. THEORY
     *  rows never need this: each active {@link CohortSection} has its own exclusive committed
     *  classroom ({@code ux_cohort_section_classroom_per_term}), so THEORY never contends for a
     *  resource another cohort also needs — only LAB/CLINICAL venues (and, per real seed data, even
     *  some Labs) are ever shared across cohorts. */
    private record TaggedShortfallRow(Long cohortId, ShortfallRow row) {}

    /** Everything one cohort's placement pass needs to carry across Phase 1 (global LAB/CLINICAL)
     *  and Phase 2 (per-cohort THEORY) of {@link #doRunGlobalAutoSchedule} — mutable {@code
     *  dayLoad}/{@code placedThisCohortRun}/{@code unplacedForCohort} so both phases (and {@link
     *  #fillSelfStudyGaps} afterward) accumulate into the exact same per-cohort state a single
     *  unified loop used to update in place. {@code siblingDaysByOfferingAndType} (keyed by {@link
     *  #offeringSessionTypeKey}) tracks which days each LAB/CLINICAL (offering, sessionType) pair
     *  has already landed a session on THIS run, seeded from cells already in {@code skeleton} too —
     *  see {@link #placeShortfallRow}'s sibling-batch-alignment step for why. */
    private record CohortRunContext(Long cohortId, Cohort cohort, SkeletonBuilderResponse skeleton,
                                     List<AutoPlaceUnplacedItem> unplacedForCohort, List<ShortfallRow> theoryRows,
                                     Map<DayOfWeek, Integer> dayLoad, List<Placement> placedThisCohortRun,
                                     Map<String, Set<DayOfWeek>> siblingDaysByOfferingAndType) {}

    /** Key for {@link CohortRunContext#siblingDaysByOfferingAndType()} — every batch splitting the
     *  same (offering, sessionType) across parallel venues shares this exact key. */
    private static String offeringSessionTypeKey(Long offeringId, ClassSessionType sessionType) {
        return offeringId + "|" + sessionType;
    }

    /** Mutable, run-scoped tally for one Lab or Clinical venue's unmet demand this run — accumulated
     *  by {@link #placeShortfallRow} every time a LAB/CLINICAL chunk genuinely fails to place (not
     *  merely "some other row happened to be tried first"), keyed by {@code sessionType + ":" +
     *  venueId} since Lab and ClinicalVenue are separate entities with separate id sequences. Turned
     *  into the public {@link VenueCapacityGap} list at the end of {@link #doRunGlobalAutoSchedule}. */
    private static final class VenueGapAccumulator {
        final Long venueId;
        final String venueType;
        final String venueName;
        final Integer capacity;
        double unplacedHours;
        /** id -> name, insertion-ordered -- see {@code TimetableCapacityPlanningService
         *  .VenueDemandAccumulator#subjects} for why this is keyed by id, not name. */
        final Map<Long, String> subjects = new LinkedHashMap<>();

        VenueGapAccumulator(Long venueId, String venueType, String venueName, Integer capacity) {
            this.venueId = venueId;
            this.venueType = venueType;
            this.venueName = venueName;
            this.capacity = capacity;
        }
    }

    /** Most-constrained-first: LAB/CLINICAL rows (venue- and batch-scoped, generally far fewer
     *  interchangeable slots than a THEORY lecture) are attempted before THEORY, and within the
     *  same session type, a row with a bigger remaining shortfall (more sessions still needing a
     *  home) goes before one with a smaller one. This is a cheap ordering heuristic, not an actual
     *  per-row feasible-slot count — a real constraint-count scan would be more precise but isn't
     *  worth the complexity for a first cut; {@link #attemptBacktrack} is what actually recovers
     *  from a wrong ordering guess, not this comparator alone. */
    private static final Comparator<ShortfallRow> SHORTFALL_ROW_ORDER = Comparator
        .comparing((ShortfallRow r) -> r.budget().sessionType() == ClassSessionType.THEORY ? 1 : 0)
        .thenComparing((ShortfallRow r) -> -r.shortfall());

    /** {@code dayPlaced} null means every day/period combination was exhausted — {@code
     *  failureReason} then names the constraint that blocked the largest share of attempts (see
     *  {@link #tryPlaceAndStaff}), instead of the old one-size-fits-all "no day/period found"
     *  message that gave an admin no way to tell a faculty-capacity problem from a room clash
     *  without re-deriving it from raw data by hand. {@code cellId}/{@code periodIds} are populated
     *  on success so a caller can track this placement for possible later backtracking (see
     *  {@link #attemptBacktrack}) without a second lookup — {@code periodIds} is every period in
     *  the placed block, ordered, primary first (a single-element list for an ordinary blockSize-1
     *  placement). {@code facultyId} (success only) is whichever candidate in the row's own {@code
     *  candidateFacultyIds} actually got staffed — not necessarily the row's primary bound faculty,
     *  now that {@link #tryPlaceAndStaff} tries fallbacks — so a caller building a {@link Placement}
     *  from this attempt must use this, not the row's own {@code facultyId()}, or a substituted
     *  session would be recorded (and later restored/bumped) under the wrong faculty. */
    private record PlacementAttempt(DayOfWeek dayPlaced, Long cellId, List<Long> periodIds, Long facultyId, String failureReason) {
        static PlacementAttempt success(DayOfWeek day, Long cellId, List<Long> periodIds, Long facultyId) {
            return new PlacementAttempt(day, cellId, periodIds, facultyId, null);
        }

        static PlacementAttempt failure(Map<String, Integer> failureTally) {
            return new PlacementAttempt(null, null, null, null, summarizeFailures(failureTally));
        }
    }

    /** Places every remaining chunk of one {@link ShortfallRow} against {@code context}'s own
     *  per-cohort state (dayLoad/placedThisCohortRun/unplacedForCohort) -- extracted so {@link
     *  #doRunGlobalAutoSchedule}'s Phase 1 (LAB/CLINICAL, pooled and globally ordered across every
     *  cohort) and Phase 2 (THEORY, per cohort) can share the exact same placement logic. {@code
     *  daysUsed} is resolved fresh per row from {@code context.skeleton().cells()} -- the initial
     *  per-cohort snapshot taken in Phase 0, unchanged in meaning from before this method was
     *  extracted: a row's OWN chunks placed during this same run still correctly extend it via the
     *  {@code daysUsed.add(...)} below, exactly as they did in the original single loop. {@code
     *  periodDurationHours}/{@code venueGaps} feed {@link #tallyVenueGap} -- a no-op for THEORY rows
     *  (never venue-scarce, see {@link TaggedShortfallRow}'s javadoc).
     *
     * <p>Reports at most ONE {@link AutoPlaceUnplacedItem} for this row, not one per failed chunk --
     *  a row needing 6 more blocks that fails on all 6 used to add 6 near-identical lines (same
     *  subject/reason, differing only in the trailing "N of M combinations tried" count), which
     *  read as noise rather than a signal an admin could act on. The single line now says how much
     *  is still short and the LAST attempt's reason (the most-exhausted, most-informative one, since
     *  earlier attempts in the same row are strictly less constrained as daysUsed/dayLoad fill up). */
    private void placeShortfallRow(Long cohortId, ShortfallRow row, TermInstance term, List<Period> periods,
                                    CohortRunContext context, double periodDurationHours,
                                    Map<String, VenueGapAccumulator> venueGaps,
                                    List<FacultySubstitutionEvent> facultySubstitutionEvents) {
        Set<DayOfWeek> daysUsed = existingDaysForBudgetRow(context.skeleton().cells(), row.offering().getId(), row.budget());
        // Second-session-per-day fallback (see #isEveryCandidateDayAlreadyUsed's javadoc): starts
        // empty and only ever gains a day once the one-session-per-day pass below has genuinely
        // exhausted every candidate day for this exact row -- a day only ever lands in here after
        // it's already in daysUsed, so this is additive to, never a replacement for, the normal cap.
        Set<DayOfWeek> daysUsedTwice = EnumSet.noneOf(DayOfWeek.class);
        // shortfall() is a count of SESSIONS still owed this week, NOT periods -- see
        // CurriculumHoursCalculator#sessionsPerWeek, which divides total hours by one whole
        // session's clock duration (slotDuration * blockSize). Each iteration below therefore places
        // exactly ONE session of the row's full blockSize, and decrements `remaining` by one
        // session. Spending `remaining` in PERIODS instead (the old `Math.min(blockSize, remaining)`
        // chunking, which then subtracted blockSize) silently under-delivered every LAB/CLINICAL row
        // by a factor of its own block size: a Clinical row owing 6 sessions of 4 periods (24
        // periods) placed a 4-period block plus a 2-period stub and called itself done -- 6 periods
        // instead of 24 -- while a Lab row owing 1 session of 2 periods placed a single lone period.
        // Real seed data confirmed both shapes exactly (one 4-cell Clinical group per batch, and
        // 1-cell Labs with no sessionGroupId at all). THEORY has blockSize 1, so sessions and
        // periods coincide there and its behavior is unchanged.
        // Sibling-batch alignment (LAB/CLINICAL only): a batch splitting the same offering across
        // parallel venues (see CohortRunContext#siblingDaysByOfferingAndType) delivers the SAME
        // curriculum hours at the same time as its sibling(s), just in a different room/venue -- it
        // should land on whatever day(s) a sibling already claimed, not spread onto a fresh day of
        // its own. Left null for THEORY, which has no such per-batch splitting.
        String siblingKey = row.budget().sessionType() == ClassSessionType.THEORY ? null
            : offeringSessionTypeKey(row.offering().getId(), row.budget().sessionType());
        // Real-world sequencing: a THEORY row for a subject that also has LAB/CLINICAL should prefer
        // landing on the same day as, or before, that subject's own LAB/CLINICAL day(s) -- students
        // shouldn't walk into a practical before the lecture behind it has even been taught that
        // week. Phase 1 (LAB/CLINICAL, every cohort) has always fully finished by the time Phase 2
        // (THEORY) reaches this row, so siblingDaysByOfferingAndType's LAB/CLINICAL entries for this
        // offering are already final -- safe to read once here rather than per candidate slot. Empty
        // (no preference, today's exact behavior) for a LAB/CLINICAL row itself or a THEORY subject
        // with no LAB/CLINICAL component at all.
        Set<DayOfWeek> preferredDays = EnumSet.noneOf(DayOfWeek.class);
        if (row.budget().sessionType() == ClassSessionType.THEORY) {
            Set<DayOfWeek> labClinicalDays = new HashSet<>();
            labClinicalDays.addAll(context.siblingDaysByOfferingAndType()
                .getOrDefault(offeringSessionTypeKey(row.offering().getId(), ClassSessionType.LAB), Set.of()));
            labClinicalDays.addAll(context.siblingDaysByOfferingAndType()
                .getOrDefault(offeringSessionTypeKey(row.offering().getId(), ClassSessionType.CLINICAL), Set.of()));
            if (!labClinicalDays.isEmpty()) {
                int earliestLabClinicalRank = labClinicalDays.stream().mapToInt(DayOfWeek::ordinal).min().orElseThrow();
                for (DayOfWeek d : DayOfWeek.values()) {
                    if (d != DayOfWeek.SATURDAY && d.ordinal() <= earliestLabClinicalRank) {
                        preferredDays.add(d);
                    }
                }
            }
        }
        int remaining = row.shortfall();
        int unplacedPeriods = 0;
        String lastFailureReason = null;
        while (remaining > 0) {
            int thisBlockSize = row.blockSize();
            PlacementAttempt attempt = null;
            if (siblingKey != null) {
                for (DayOfWeek siblingDay : context.siblingDaysByOfferingAndType().getOrDefault(siblingKey, Set.of())) {
                    if (daysUsed.contains(siblingDay)) {
                        continue;
                    }
                    PlacementAttempt siblingAttempt = tryPlaceAndStaff(cohortId, row.offering(), row.budget(), row.candidateFacultyIds(),
                        term, periods, allDaysExcept(siblingDay), thisBlockSize, context.dayLoad(), preferredDays);
                    if (siblingAttempt.dayPlaced() != null) {
                        attempt = siblingAttempt;
                        break;
                    }
                }
            }
            if (attempt == null) {
                attempt = tryPlaceAndStaff(cohortId, row.offering(), row.budget(), row.candidateFacultyIds(), term,
                    periods, daysUsed, thisBlockSize, context.dayLoad(), preferredDays);
            }
            if (attempt.dayPlaced() != null) {
                daysUsed.add(attempt.dayPlaced());
                context.dayLoad().merge(attempt.dayPlaced(), thisBlockSize, Integer::sum);
                context.placedThisCohortRun().add(new Placement(attempt.cellId(), row.offering().getId(), row.budget().sessionType(),
                    row.budget().batchId(), row.budget().cohortSectionId(), attempt.facultyId(), row.subjectName(),
                    occupantLabel(row.budget()), attempt.dayPlaced(), attempt.periodIds()));
                recordFacultySubstitutionIfAny(row, cohortId, attempt.facultyId(), facultySubstitutionEvents);
                if (siblingKey != null) {
                    context.siblingDaysByOfferingAndType().computeIfAbsent(siblingKey, k -> new HashSet<>()).add(attempt.dayPlaced());
                }
                remaining--; // one SESSION accounted for, whatever its blockSize in periods
                continue;
            }
            if (attemptBacktrack(cohortId, row, term, periods, daysUsed, context.placedThisCohortRun(),
                    context.unplacedForCohort(), thisBlockSize, context.dayLoad())) {
                remaining--; // one SESSION accounted for, whatever its blockSize in periods
                continue;
            }
            // One-session-per-day genuinely couldn't fit this chunk anywhere, AND every candidate day
            // already carries a session of this exact row -- the week structurally has no untouched
            // day left, not a room/faculty conflict a retry elsewhere could dodge (a curriculum-hours
            // rounding-up subject needing 7 weekly sessions in a 6-day week is the real-world case
            // this closes, see project_special_class_multiperiod_and_validations memory). Only now,
            // as the last resort, retry allowing ONE further session on an already-used day -- still
            // fully conflict-checked (real room/faculty/audience clashes at that exact day+period
            // still apply), just no longer excluded purely for already carrying one session today.
            PlacementAttempt secondPassAttempt = null;
            if (isEveryCandidateDayAlreadyUsed(daysUsed, term)) {
                secondPassAttempt = tryPlaceAndStaff(cohortId, row.offering(), row.budget(), row.candidateFacultyIds(), term,
                    periods, daysUsedTwice, thisBlockSize, context.dayLoad(), preferredDays);
            }
            if (secondPassAttempt != null && secondPassAttempt.dayPlaced() != null) {
                daysUsedTwice.add(secondPassAttempt.dayPlaced());
                context.dayLoad().merge(secondPassAttempt.dayPlaced(), thisBlockSize, Integer::sum);
                context.placedThisCohortRun().add(new Placement(secondPassAttempt.cellId(), row.offering().getId(), row.budget().sessionType(),
                    row.budget().batchId(), row.budget().cohortSectionId(), secondPassAttempt.facultyId(), row.subjectName(),
                    occupantLabel(row.budget()), secondPassAttempt.dayPlaced(), secondPassAttempt.periodIds()));
                recordFacultySubstitutionIfAny(row, cohortId, secondPassAttempt.facultyId(), facultySubstitutionEvents);
            } else {
                unplacedPeriods += thisBlockSize;
                lastFailureReason = secondPassAttempt != null ? secondPassAttempt.failureReason() : attempt.failureReason();
                tallyVenueGap(row, thisBlockSize, periodDurationHours, venueGaps);
            }
            remaining--; // one SESSION accounted for, whatever its blockSize in periods
        }
        if (unplacedPeriods > 0) {
            double unplacedHours = unplacedPeriods * periodDurationHours;
            context.unplacedForCohort().add(new AutoPlaceUnplacedItem(row.subjectName(), row.budget().sessionType(),
                occupantLabel(row.budget()), formatHours(unplacedHours) + " still unplaced — " + lastFailureReason,
                row.offering().getId()));
        }
    }

    /** True once {@code daysUsed} already covers every day {@link #tryPlaceAndStaff} would ever
     *  consider a real candidate for this term -- Saturday only counts when this term has actually
     *  opted into working-Saturday weeks, matching that method's own skip condition exactly. This
     *  is the trigger for {@link #placeShortfallRow}'s second-session-per-day fallback: it must
     *  only fire once every day has genuinely been tried once, never as a shortcut past a real
     *  room/faculty conflict on a day that's simply still untried. */
    private boolean isEveryCandidateDayAlreadyUsed(Set<DayOfWeek> daysUsed, TermInstance term) {
        for (DayOfWeek day : DayOfWeek.values()) {
            if (day == DayOfWeek.SATURDAY && term.getWorkingSaturdayWeeks().isEmpty()) {
                continue;
            }
            if (!daysUsed.contains(day)) {
                return false;
            }
        }
        return true;
    }

    /** Every day except {@code keep}, passed as {@code tryPlaceAndStaff}'s exclusion set to force it
     *  to only ever consider that one day — used by {@link #placeShortfallRow}'s sibling-batch
     *  alignment step to test "can THIS row's own venue/faculty land on the day a sibling batch
     *  already claimed" without disturbing that method's normal day-search logic at all. */
    private Set<DayOfWeek> allDaysExcept(DayOfWeek keep) {
        Set<DayOfWeek> excluded = EnumSet.allOf(DayOfWeek.class);
        excluded.remove(keep);
        return excluded;
    }

    /** Resolves {@code row}'s batch to its committed Lab or Clinical venue (a THEORY row, or a
     *  LAB/CLINICAL row with no batch/venue committed yet, has nothing to attribute — silently
     *  skipped, exactly as venue-agnostic as before this tracking existed) and adds {@code
     *  failedBlockSize} periods' worth of hours to that venue's running {@link VenueGapAccumulator}
     *  in {@code venueGaps}, creating one on first use. This is purely a tally for {@link
     *  VenueCapacityGap} reporting -- it never affects placement itself. */
    private void tallyVenueGap(ShortfallRow row, int failedBlockSize, double periodDurationHours,
                                Map<String, VenueGapAccumulator> venueGaps) {
        if (row.budget().sessionType() == ClassSessionType.THEORY || row.budget().batchId() == null) {
            return;
        }
        Batch batch = batchRepository.findById(row.budget().batchId()).orElse(null);
        if (batch == null) {
            return;
        }
        Long venueId;
        String venueName;
        Integer capacity;
        if (row.budget().sessionType() == ClassSessionType.CLINICAL && batch.getClinicalVenue() != null) {
            venueId = batch.getClinicalVenue().getId();
            venueName = batch.getClinicalVenue().getName();
            capacity = batch.getClinicalVenue().getCapacity();
        } else if (row.budget().sessionType() == ClassSessionType.LAB && batch.getLab() != null) {
            venueId = batch.getLab().getId();
            venueName = batch.getLab().getName();
            capacity = batch.getLab().getCapacity();
        } else {
            return;
        }
        String key = row.budget().sessionType() + ":" + venueId;
        VenueGapAccumulator accumulator = venueGaps.computeIfAbsent(key,
            k -> new VenueGapAccumulator(venueId, row.budget().sessionType().name(), venueName, capacity));
        accumulator.unplacedHours += failedBlockSize * periodDurationHours;
        if (row.offering().getSubject() != null) {
            accumulator.subjects.put(row.offering().getSubject().getId(), row.subjectName());
        }
    }

    /** Scans every free day for a run of {@code blockSize} immediately-consecutive periods (by
     *  {@link Period#getPeriodOrder()}) where placement AND staffing the offering's bound faculty
     *  both succeed for every period in the block, undoing the placement and trying the next
     *  candidate on a staffing failure (that faculty is busy at that slot, not a shared resource
     *  another row could be nudged out of here — see {@link #attemptBacktrack} for the one bounded
     *  exception a caller may apply on top of this method's own result). {@code blockSize} is
     *  always 1 for THEORY (see {@link CurriculumHoursCalculator#resolveBlockSize}) — that case behaves exactly as before
     *  block-size support existed. Monday-Friday candidates are tried least-loaded-first ({@code
     *  dayLoad}, a running count of periods already placed for this cohort on each day — see the
     *  caller), not in fixed Monday-first order: a static order let every independently-processed
     *  row grab Monday's early periods first and then move on once its own weekly quota was met, so
     *  several one-session-a-week subjects piled onto the same day's morning and nothing ever came
     *  back to fill that day's afternoon, while a later, harder-to-place LAB/CLINICAL block packed a
     *  different day solid — an uneven week, not a defensible one. Saturday stays a true fallback,
     *  tried only after every Monday-Friday day/period combination has failed — real curriculum
     *  content for a cohort is typically well under a full Monday-Friday week (see {@link
     *  #fillSelfStudyGaps} for what closes that genuine remainder), so pulling Saturday in on equal
     *  footing would activate it far more than actually needed; it is still not the old "just 2-3
     *  periods" outcome, since whatever a genuine overflow does land there is still tried
     *  earliest-period-first within the day, not scattered. Tallies every {@link ConstraintViolation}
     *  code hit along the way so a total failure can report *which* constraint actually blocked it,
     *  not just that one did.
     *
     * <p>{@code preferredDays} is a soft, additive bias only — real-world sequencing (a Theory
     *  lecture teaching the concept before students walk into that same subject's Lab/Clinical
     *  practical) means a THEORY row for a subject whose LAB/CLINICAL sessions already landed on,
     *  say, Wednesday should try Monday-Wednesday before ever reaching for Thursday/Friday, so the
     *  practical doesn't end up earlier in the week than the theory behind it. Every day is still
     *  eventually tried either way (this only reorders {@code candidateDays}, never filters it), so
     *  this can only improve which day a row lands on, never reduce whether it places at all. Empty
     *  for LAB/CLINICAL rows themselves (they're the anchor, nothing to prefer against) and for any
     *  THEORY row whose subject has no LAB/CLINICAL component — see {@link #placeShortfallRow}'s
     *  computation of this from {@link CohortRunContext#siblingDaysByOfferingAndType()}. */
    private PlacementAttempt tryPlaceAndStaff(Long cohortId, CourseOffering offering, SkeletonSubjectBudget budget,
                                        List<Long> candidateFacultyIds, TermInstance term, List<Period> periods,
                                        Set<DayOfWeek> daysUsed, int blockSize, Map<DayOfWeek, Integer> dayLoad,
                                        Set<DayOfWeek> preferredDays) {
        Map<String, Integer> failureTally = new LinkedHashMap<>();
        Map<DayOfWeek, List<ClinicalShiftWindow>> shiftWindowsByDay = resolveShiftWindowsByDay(cohortId, term);
        List<DayOfWeek> candidateDays = Arrays.stream(DayOfWeek.values())
            .filter(d -> d != DayOfWeek.SATURDAY)
            .sorted(Comparator.<DayOfWeek>comparingInt(d -> preferredDays.isEmpty() || preferredDays.contains(d) ? 0 : 1)
                .thenComparingInt(dayLoad::get))
            .toList();
        List<DayOfWeek> orderedWithSaturdayLast = new ArrayList<>(candidateDays);
        orderedWithSaturdayLast.add(DayOfWeek.SATURDAY);
        for (DayOfWeek day : orderedWithSaturdayLast) {
            // Saturday with no working-Saturday pattern configured is never a real candidate at
            // all (every period on it fails the exact same institutional gate) -- skip the whole
            // day rather than let 8 doomed attempts dilute the reported failure fraction down
            // below, the same way an already-used day is skipped outright rather than counted.
            if (daysUsed.contains(day) || (day == DayOfWeek.SATURDAY && term.getWorkingSaturdayWeeks().isEmpty())) {
                continue;
            }
            // Earliest-free-period-first within the chosen day. This is only defensible because of
            // the phase order (see the class javadoc): the rigid multi-period LAB/CLINICAL blocks
            // run first against a freshly rebuilt, empty grid, so taking the earliest slot costs
            // them nothing, and by the time single-period THEORY rows get here the big blocks have
            // already claimed their windows. Do NOT reuse this scan for a pass that runs after
            // filler has been placed without first teaching it to avoid splitting a contiguous run
            // some still-unplaced block needs -- that is exactly the fragmentation the rebuild
            // exists to prevent.
            for (int startIdx = 0; startIdx + blockSize <= periods.size(); startIdx++) {
                // periods is already active-only, periodOrder-sorted (see caller) -- a contiguous
                // subList of it IS, by construction, "the next blockSize real periods with none
                // skipped." No further periodOrder check is needed or correct here: periodOrder
                // itself can carry gaps left by long-retired period rows (e.g. the old standalone
                // LabSlot master's rows, inactive since V331 merged them into Period) that have no
                // bearing on anything real -- re-checking against those raw integers used to cap
                // every block size at whatever the retired rows' accidental gap pattern allowed.
                List<Period> block = periods.subList(startIdx, startIdx + blockSize);
                // Adjacent-by-position doesn't mean adjacent-in-time -- a recess/lunch break can
                // sit between two periods that are still next to each other in periods' active-list
                // ordering (no Period row models the break itself). Placing a block across that gap
                // would silently split the session around it, so require true back-to-back clock
                // times before this candidate is even attempted (matches the same requirement
                // TimetableSkeletonService.resolveSpanPeriods enforces on the manual-placement
                // side) -- UNLESS this is a CLINICAL block crossing a recess rather than the day's
                // lunch break, per PeriodGapPolicy: a half-day clinical posting runs straight
                // through a short recess in real institutional practice.
                boolean hasGap = false;
                for (int i = 1; i < block.size(); i++) {
                    if (!block.get(i - 1).getEndTime().equals(block.get(i).getStartTime())
                        && !PeriodGapPolicy.gapCrossableFor(budget.sessionType(), block.get(i - 1), block.get(i), periods)) {
                        hasGap = true;
                        break;
                    }
                }
                if (hasGap) {
                    failureTally.merge("PERIOD_NOT_CONTIGUOUS", 1, Integer::sum);
                    continue;
                }
                boolean anyBlocked = false;
                boolean anyShiftBlocked = false;
                List<ClinicalShiftWindow> windowsToday = shiftWindowsByDay.getOrDefault(day, List.of());
                for (Period p : block) {
                    if (blockedPeriodChecker.blockReason(day, p.getStartTime(), p.getEndTime(), term).isPresent()) {
                        anyBlocked = true;
                        break;
                    }
                    if (windowsToday.stream().anyMatch(w -> w.overlaps(p.getStartTime(), p.getEndTime()))) {
                        anyBlocked = true;
                        anyShiftBlocked = true;
                        break;
                    }
                }
                if (anyBlocked) {
                    failureTally.merge(anyShiftBlocked ? "CLINICAL_SHIFT_BLOCKED" : "PERIOD_BLOCKED", 1, Integer::sum);
                    continue;
                }
                Period primary = block.get(0);
                List<Long> spanPeriodIds = block.size() > 1
                    ? block.subList(1, block.size()).stream().map(Period::getId).toList()
                    : null;
                SkeletonCellResponse placed;
                try {
                    placed = timetableSkeletonService.placeCell(new SkeletonCellPlacementRequest(
                        offering.getId(), budget.sessionType(), day, primary.getId(),
                        budget.batchId(), cohortId, budget.cohortSectionId(), spanPeriodIds));
                } catch (TimetableConstraintViolationException ex) {
                    tallyViolations(failureTally, ex.getViolations());
                    continue;
                } catch (IllegalArgumentException ex) {
                    failureTally.merge("PLACEMENT_ERROR", 1, Integer::sum);
                    continue;
                }
                // Tries every candidate in order (the row's own bound faculty first, then ranked
                // eligible fallbacks -- see ShortfallRow#candidateFacultyIds/rankedFallbackFacultyIds)
                // at this exact (day, block) before giving up on the slot entirely. Previously this
                // only ever tried the one bound faculty: a mandatory subject whose sole assigned
                // faculty was booked elsewhere at every remaining slot came up unplaced even when the
                // audience's own week still had real free room -- exactly the Self-Study/Co-curricular
                // filler pass already avoided via its own tryStaffWithFallback, just never extended to
                // ordinary curriculum rows.
                List<ConstraintViolation> lastViolations = List.of();
                boolean lastWasStaffingError = false;
                Long staffedFacultyId = null;
                for (Long candidateFacultyId : candidateFacultyIds) {
                    try {
                        timetableStaffingService.staffCell(placed.id(), new StaffingAssignmentRequest(candidateFacultyId, null));
                        staffedFacultyId = candidateFacultyId;
                        break;
                    } catch (TimetableConstraintViolationException ex) {
                        lastViolations = ex.getViolations();
                        lastWasStaffingError = false;
                    } catch (LifecycleConflictException | IllegalArgumentException ex) {
                        lastWasStaffingError = true;
                    }
                }
                if (staffedFacultyId == null) {
                    timetableSkeletonService.removeCell(placed.id());
                    if (lastWasStaffingError) {
                        failureTally.merge("STAFFING_ERROR", 1, Integer::sum);
                    } else {
                        tallyViolations(failureTally, lastViolations);
                    }
                    continue;
                }
                return PlacementAttempt.success(day, placed.id(), block.stream().map(Period::getId).toList(), staffedFacultyId);
            }
        }
        return PlacementAttempt.failure(failureTally);
    }

    /** Every active Clinical Shift window bound to this cohort this term, grouped by day, when the
     *  cohort's Program has opted into Clinical Shift scheduling (empty map otherwise -- a plain
     *  no-op for every program that hasn't). Memoized via {@link AutoScheduleRunCache} since {@link
     *  #tryPlaceAndStaff} calls this on every placement <em>attempt</em> (hundreds per cohort per
     *  run) and a cohort's shift assignments never change mid-run. */
    private Map<DayOfWeek, List<ClinicalShiftWindow>> resolveShiftWindowsByDay(Long cohortId, TermInstance term) {
        Cohort cohort = cohortRepository.findById(cohortId).orElse(null);
        boolean shiftEnforced = cohort != null && cohort.getProgram() != null
            && Boolean.TRUE.equals(cohort.getProgram().getUsesClinicalShiftScheduling());
        if (!shiftEnforced) {
            return Map.of();
        }
        String memoKey = "shiftWindows|" + cohortId + "|" + term.getId();
        List<ClinicalShiftWindow> windows = AutoScheduleRunCache.current()
            .map(cache -> cache.memoizedShiftWindows(memoKey,
                () -> clinicalShiftGroupService.resolveActiveWindowsForCohort(cohortId, term.getId())))
            .orElseGet(() -> clinicalShiftGroupService.resolveActiveWindowsForCohort(cohortId, term.getId()));
        return windows.stream().collect(Collectors.groupingBy(ClinicalShiftWindow::dayOfWeek));
    }

    /** One Self-Study/Co-curricular budget row this cohort can use as gap-fill, pre-resolved to a
     *  ranked list of candidate faculty ids to try in order — see {@link
     *  #rankedFallbackFacultyIds}. Never empty when {@code offering}/{@code budget} came
     *  from a real skeleton row, but CAN be empty (no faculty bound and no eligible pool at all),
     *  in which case every attempt against this row correctly fails fast via {@link
     *  #tryStaffWithFallback}. */
    record SelfStudyRow(String subjectName, CourseOffering offering, SkeletonSubjectBudget budget, List<Long> candidateFacultyIds) {}

    /** {@code unfillablePeriods} is the exact count of Monday-Friday periods this cohort's pass left
     *  empty because every eligible Self-Study/Co-curricular faculty was at capacity — the caller
     *  sums this across cohorts to turn it into a real, this-run "how many more staff" figure on
     *  {@link GlobalAutoScheduleResult}, distinct from the pre-run whole-pool estimate on {@link
     *  FacultyWorkloadOverviewReport}. */
    record SelfStudyGapFillOutcome(List<Placement> filled, int unfillablePeriods) {}

    /** Real curriculum content for a semester routinely undershoots a full Monday-Friday week (a
     *  first-semester BSc Nursing cohort's actual theory+lab+clinical hours convert to well under
     *  40 periods — see the class's own capacity math discussion), so even a perfectly balanced
     *  run above still leaves genuine gaps. Rather than leave those blank, this pass backfills every
     *  Monday-Friday day/period still empty for each Self-Study/Co-curricular budget row (matched
     *  by subject name — see {@link #isSelfStudySubject}) with an EXTRA session of that same
     *  subject, deliberately exceeding its own curriculum-derived weekly quota via {@link
     *  TimetableSkeletonService#placeCell(SkeletonCellPlacementRequest, boolean)}'s budget-bypass
     *  overload — Self-Study/Co-curricular is curriculum-sanctioned flexible time, unlike any other
     *  subject here, so growing it to soak up real leftover capacity is the intended use, not an
     *  exploit of the cap. Every other placement rule still applies in full (already-placed,
     *  audience exclusivity, blocked periods) — a slot genuinely occupied by something else is
     *  correctly skipped, not overwritten. Saturday is included only once the admin has already
     *  opted this term into working Saturdays ({@code term.getWorkingSaturdayWeeks()} non-empty,
     *  the same signal {@link #placeShortfallRow}'s own Saturday fallback uses) -- Monday-Friday
     *  alone is never grown into Saturday just to generate filler content. But once Saturday IS
     *  active (opted in for a genuine shortfall elsewhere), a leftover empty Saturday period is
     *  exactly as unacceptable as a leftover empty weekday one, per the same "no gaps" requirement
     *  this pass exists to close -- so this loop covers it too rather than leaving it as the one
     *  exception.
     *
     * <p>Unlike the old version, a period is never silently left empty just because the row's own
     *  bound faculty happens to be at their cap: {@link #tryStaffWithFallback} walks a ranked list
     *  of every other eligible faculty member with real spare term capacity (least-remaining-first,
     *  so an already-committed faculty is topped off before a fresher person is pulled in — "extract
     *  maximum work before adding headcount") before giving up on that (day, period). A period that
     *  genuinely can't be staffed by anyone is now reported via {@code unplacedForCohort} — once per
     *  cohort with a count, not once per period, so a real capacity ceiling shows up on screen
     *  instead of reading as a silent, unexplained gap in the grid. */
    // Package-private (not private) so the Saturday-inclusion regression can be verified directly.
    SelfStudyGapFillOutcome fillSelfStudyGaps(Long cohortId, SkeletonBuilderResponse skeleton, TermInstance term,
                                               List<Period> periods, Map<DayOfWeek, Integer> dayLoad,
                                               List<AutoPlaceUnplacedItem> unplacedForCohort, TermDemandAggregation termDemand) {
        List<DayOfWeek> weekdays = term.getWorkingSaturdayWeeks().isEmpty()
            ? List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)
            : List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY,
                DayOfWeek.SATURDAY);
        List<Placement> filled = new ArrayList<>();

        // Every non-elective Theory offering this cohort actually runs -- Self-Study/Co-curricular
        // included, not privileged. Filling leftover periods from Self-Study ALONE produced a real,
        // visibly-wrong grid once the budget-cap bypass started working (a cohort's Thursday came
        // back as six consecutive Self-Study periods). User's explicit call, 2026-09-10: "fill them
        // equally for the offerings as providing extra hours" / "you can increase the hours for each
        // and every theory session if available and free" -- so the filler round-robins across all of
        // them instead. Deliberately does NOT touch CurriculumSemesterCourse.theoryHours: the
        // recorded, regulator-facing requirement stays exactly what the curriculum specifies, and
        // this only adds ADDITIONAL budget-uncapped sessions beyond it (placeCell's
        // enforceBudgetCap=false below).
        List<SelfStudyRow> rows = resolveExtraHoursFillerRows(cohortId, skeleton, termDemand);
        boolean extraHoursFallback = rows.stream().noneMatch(r -> isSelfStudySubject(r.subjectName()));
        if (rows.isEmpty()) {
            unplacedForCohort.add(new AutoPlaceUnplacedItem("Self-Study/Co-curricular", ClassSessionType.THEORY, null,
                "no Self-Study/Co-curricular offering and no other Theory offering exists for this cohort to use "
                    + "as gap-fill — every remaining Monday-Friday period stays empty until one is added", null));
            return new SelfStudyGapFillOutcome(filled, 0);
        }

        int unfillablePeriods = 0;
        int rotation = 0;
        // A period every row REJECTED (placeCell threw) used to be swallowed by the bare `continue`
        // below and counted nowhere -- so a genuinely empty period could sit in the grid with no
        // corresponding line anywhere in the run report, which is exactly the "empty periods but the
        // report claims nothing is wrong" symptom this tally closes. Keyed by the real violation code
        // so the report names the actual blocking rule instead of a generic catch-all. Counts ONLY
        // reasons that mean the period was genuinely FREE and still couldn't be used -- see
        // {@link #indicatesGenuinelyEmptyPeriod}; this loop walks every day/period in the week, so
        // most refusals are just "that slot already has a class / is clinical duty", which is normal
        // and must not be reported as an empty period.
        Map<String, Integer> blockedPeriodsByReason = new LinkedHashMap<>();
        for (DayOfWeek day : weekdays) {
            for (Period period : periods) {
                if (blockedPeriodChecker.blockReason(day, period.getStartTime(), period.getEndTime(), term).isPresent()) {
                    continue;
                }
                boolean periodFilled = false;
                boolean periodWasFreeButUnstaffable = false;
                String lastBlockCode = null;
                for (int attempt = 0; attempt < rows.size(); attempt++) {
                    // Round-robin: each successful fill advances the starting point, so extra hours
                    // spread evenly across the cohort's subjects instead of one subject absorbing
                    // every leftover period in the week.
                    SelfStudyRow row = rows.get((rotation + attempt) % rows.size());
                    SkeletonCellResponse placed;
                    try {
                        placed = timetableSkeletonService.placeCell(new SkeletonCellPlacementRequest(
                            row.offering().getId(), ClassSessionType.THEORY, day, period.getId(),
                            row.budget().batchId(), cohortId, row.budget().cohortSectionId(), null), false);
                    } catch (TimetableConstraintViolationException ex) {
                        lastBlockCode = ex.getViolations().isEmpty() ? "UNKNOWN" : ex.getViolations().get(0).code();
                        continue;
                    }
                    Long staffedBy = tryStaffWithFallback(placed.id(), row.candidateFacultyIds());
                    if (staffedBy != null) {
                        dayLoad.merge(day, 1, Integer::sum);
                        filled.add(new Placement(placed.id(), row.offering().getId(), ClassSessionType.THEORY,
                            row.budget().batchId(), row.budget().cohortSectionId(), staffedBy, row.subjectName(),
                            occupantLabel(row.budget()), day, List.of(period.getId())));
                        periodFilled = true;
                        rotation = (rotation + attempt + 1) % rows.size();
                        break;
                    }
                    timetableSkeletonService.removeCell(placed.id());
                    periodWasFreeButUnstaffable = true;
                }
                if (!periodFilled) {
                    if (periodWasFreeButUnstaffable) {
                        unfillablePeriods++;
                    } else if (lastBlockCode != null && indicatesGenuinelyEmptyPeriod(lastBlockCode)) {
                        blockedPeriodsByReason.merge(lastBlockCode, 1, Integer::sum);
                    }
                }
            }
        }
        if (unfillablePeriods > 0) {
            String fillerLabel = extraHoursFallback ? "eligible extra-hours" : "eligible Self-Study/Co-curricular";
            unplacedForCohort.add(new AutoPlaceUnplacedItem("Self-Study/Co-curricular", ClassSessionType.THEORY, null,
                unfillablePeriods + " period(s) left genuinely empty — every " + fillerLabel
                    + " faculty is unavailable, already teaching elsewhere, or at their capacity cap at that exact slot",
                rows.get(0).offering().getId()));
        }
        for (Map.Entry<String, Integer> blocked : blockedPeriodsByReason.entrySet()) {
            unplacedForCohort.add(new AutoPlaceUnplacedItem("Gap-fill", ClassSessionType.THEORY, null,
                blocked.getValue() + " period(s) left empty — " + friendlyFailureReason(blocked.getKey()),
                rows.get(0).offering().getId()));
        }
        return new SelfStudyGapFillOutcome(filled, unfillablePeriods);
    }

    /** Whether a gap-fill rejection means the period was genuinely FREE and still went unused (worth
     *  reporting as a real gap), or simply that the slot was already spoken for (normal, silent).
     *  {@link #fillSelfStudyGaps} walks every day/period in the week and tries to place into each, so
     *  the overwhelming majority of rejections are "there's already a class here" or "this cohort is
     *  on clinical duty" — reporting those as "N period(s) left empty" is flatly wrong and buries the
     *  handful of lines that do matter under a wall of false alarms. Only a refusal that leaves a
     *  genuinely usable slot unused (a budget cap, an unrecognised code) counts. */
    private static boolean indicatesGenuinelyEmptyPeriod(String violationCode) {
        return switch (violationCode) {
            // Slot already occupied by a real session for this audience.
            case "SKELETON_CELL_COHORT_CLASH",
            // Cohort is off campus on clinical duty for this window.
                 "SKELETON_CELL_CLINICAL_SHIFT_BLOCKED",
            // This subject already holds that exact day/period, so the slot isn't free either.
                 "SKELETON_CELL_ALREADY_PLACED",
            // Institutionally blocked (holiday, recurring lock) -- deliberately not teachable time.
                 "SKELETON_CELL_PERIOD_BLOCKED",
                 "PERIOD_NOT_CONTIGUOUS" -> false;
            default -> true;
        };
    }

    /** Every real, non-elective Theory offering in this cohort's curriculum, one {@link SelfStudyRow}
     *  per (offering, section) budget row — used only as {@link #fillSelfStudyGaps}'s fallback when
     *  no genuine Self-Study/Co-curricular subject exists for this cohort's term (see that method's
     *  own javadoc for why: extra practice/revision time on the cohort's own real subjects, never
     *  touching their recorded curriculum hours, round-robin so no single subject absorbs a
     *  disproportionate share). */
    private List<SelfStudyRow> resolveExtraHoursFillerRows(Long cohortId, SkeletonBuilderResponse skeleton, TermDemandAggregation termDemand) {
        List<SelfStudyRow> rows = new ArrayList<>();
        for (SkeletonSubjectResponse subject : skeleton.subjects()) {
            CourseOffering offering = courseOfferingRepository.findById(subject.courseOfferingId()).orElse(null);
            if (offering == null || timetableSkeletonService.isElectiveOffering(offering)) {
                continue;
            }
            for (SkeletonSubjectBudget budget : subject.budgets()) {
                if (budget.sessionType() != ClassSessionType.THEORY) {
                    continue;
                }
                Long primaryFacultyId = resolveBudgetFacultyId(offering, budget, cohortId);
                rows.add(new SelfStudyRow(subject.subjectName(), offering, budget,
                    rankedFallbackFacultyIds(offering, primaryFacultyId, termDemand)));
            }
        }
        return rows;
    }

    /** {@code unfillableSessions} is how many of this cohort's {@code timetable.library_sessions_per_week}
     *  quota couldn't be placed this run — either no Library classroom exists at all, or fewer than
     *  {@code sessionsPerWeek} distinct weekdays had a genuinely free, unblocked, contiguous
     *  {@code libraryBlockSizePeriods}-period window with a free Library classroom. */
    private record LibraryGapFillOutcome(List<Placement> filled, int unfillableSessions) {}

    /** Fixed-quota gap-fill pass, run BEFORE {@link #fillSelfStudyGaps} for every cohort (see the
     *  call site) — places exactly {@code timetable.library_sessions_per_week} sessions of
     *  {@code timetable.library_block_size_periods} contiguous periods each, per active
     *  CohortSection (or once for the whole cohort if it has no committed section split yet).
     *
     *  <p>Unlike every other session type in this class, no faculty is resolved or staffed — the
     *  saved rows keep {@code faculty} null. That is the entire point of this pass: {@link
     *  #fillSelfStudyGaps} can still leave a period empty when every eligible faculty is at
     *  capacity, but a strict "every period must be occupied" requirement needs at least one
     *  gap-filler that can never fail on faculty availability. A genuinely free slot with a Library
     *  classroom available always succeeds here.
     *
     *  <p>Monday-Friday only, matching {@link #fillSelfStudyGaps}'s own reasoning — Saturday stays
     *  real, occasional overflow capacity for content that doesn't fit, not filler for its own sake.
     *
     *  <p>Uses the shared system {@link #LIBRARY_SUBJECT_CODE} Subject (seeded once, V412) because
     *  {@code class_schedules.subject_id} is NOT NULL but Library is not curriculum data — it is
     *  never attached to a CourseOffering, curriculum term, or cohort's own credit/hour budget.
     *
     *  <p>{@code existingCells} (the skeleton snapshot taken before this run placed anything) is
     *  used to count each audience's ALREADY-placed Library days before adding more — without this,
     *  every run started {@code placedForAudience} back at 0 regardless of what a PREVIOUS run
     *  already placed, so a second run (not knowing Monday+Tuesday already existed) would try for
     *  {@code sessionsPerWeek} fresh sessions again, get blocked on those two days by {@code
     *  isSlotFreeForCohort}, and spill onto a 3rd/4th day instead of recognizing the quota was
     *  already met — permanent, silent over-placement with no cap, since Library has no
     *  CourseOffering and is invisible to {@link #purgeStaleOverBudgetDrafts}'s own cleanup (fixed
     *  alongside this to also cover Library, see that method's own doc). */
    private LibraryGapFillOutcome fillLibraryGaps(Long cohortId, TermInstance term, List<Period> periods,
                                                   Map<DayOfWeek, Integer> dayLoad, List<AutoPlaceUnplacedItem> unplacedForCohort,
                                                   List<SkeletonCellResponse> existingCells) {
        List<DayOfWeek> weekdays = List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY);
        List<Placement> filled = new ArrayList<>();

        Subject librarySubject = subjectRepository.findByCode(LIBRARY_SUBJECT_CODE).orElse(null);
        List<Classroom> libraryClassrooms = classroomRepository
            .findByIsActiveTrueAndRoom_PurposeCategory_CodeOrderByNameAsc(RoomPurposeCategoryCode.LIBRARY);
        if (librarySubject == null || libraryClassrooms.isEmpty()) {
            unplacedForCohort.add(new AutoPlaceUnplacedItem("Library", ClassSessionType.LIBRARY, null,
                "no Library classroom is configured (a Classroom linked to a Room tagged with the Library "
                    + "Purpose Category) — every cohort's Library quota stays unplaced until one is added", null));
            return new LibraryGapFillOutcome(filled, 0);
        }

        int sessionsPerWeek = resolveLibraryConfigInt(CONFIG_LIBRARY_SESSIONS_PER_WEEK, DEFAULT_LIBRARY_SESSIONS_PER_WEEK);
        int blockSize = resolveLibraryConfigInt(CONFIG_LIBRARY_BLOCK_SIZE_PERIODS, DEFAULT_LIBRARY_BLOCK_SIZE_PERIODS);
        List<List<Period>> candidateBlocks = contiguousPeriodBlocks(periods, blockSize);

        List<CohortSection> activeSections = timetableSkeletonService.resolveActiveSections(cohortId, term.getId());
        List<CohortSection> audiences = activeSections.isEmpty() ? Arrays.asList((CohortSection) null) : activeSections;

        int unfillableSessions = 0;
        for (CohortSection section : audiences) {
            Long sectionId = section != null ? section.getId() : null;
            Set<DayOfWeek> daysAlreadyUsed = existingCells.stream()
                .filter(c -> c.sessionType() == ClassSessionType.LIBRARY)
                .filter(c -> Objects.equals(c.cohortSectionId(), sectionId))
                .map(SkeletonCellResponse::dayOfWeek)
                .collect(java.util.stream.Collectors.toCollection(HashSet::new));
            List<DayOfWeek> orderedDays = weekdays.stream()
                .sorted(Comparator.comparingInt(dayLoad::get))
                .toList();
            int placedForAudience = daysAlreadyUsed.size();
            for (DayOfWeek day : orderedDays) {
                if (placedForAudience >= sessionsPerWeek) {
                    break;
                }
                if (daysAlreadyUsed.contains(day)) {
                    continue;
                }
                for (List<Period> block : candidateBlocks) {
                    boolean blocked = block.stream().anyMatch(p ->
                        blockedPeriodChecker.blockReason(day, p.getStartTime(), p.getEndTime(), term).isPresent());
                    if (blocked) {
                        continue;
                    }
                    if (overlapsClinicalShift(cohortId, term, day, block)) {
                        continue;
                    }
                    boolean slotFree = block.stream().allMatch(p ->
                        timetableSkeletonService.isSlotFreeForCohort(cohortId, term.getId(), day, p.getId()));
                    if (!slotFree) {
                        continue;
                    }
                    Classroom classroom = firstFreeLibraryClassroom(libraryClassrooms, term.getId(), day, block);
                    if (classroom == null) {
                        continue;
                    }
                    List<ClassSchedule> saved = placeLibraryBlock(librarySubject, term, day, block, section, classroom);
                    dayLoad.merge(day, 1, Integer::sum);
                    // periodIds must be real Period ids, not each saved ClassSchedule row's own id
                    // -- see the identical fix + explanation in saveIdleBatchLibraryCell.
                    filled.add(new Placement(saved.get(0).getId(), null, ClassSessionType.LIBRARY, null,
                        section != null ? section.getId() : null, null, "Library",
                        section != null ? section.getSectionLabel() : "Whole cohort",
                        day, block.stream().map(Period::getId).toList()));
                    daysAlreadyUsed.add(day);
                    placedForAudience++;
                    break;
                }
            }
            if (placedForAudience < sessionsPerWeek) {
                unfillableSessions += sessionsPerWeek - placedForAudience;
            }
        }
        if (unfillableSessions > 0) {
            unplacedForCohort.add(new AutoPlaceUnplacedItem("Library", ClassSessionType.LIBRARY, null,
                unfillableSessions + " of this cohort's weekly Library session(s) could not be placed — no "
                    + "weekday had " + blockSize + " genuinely free, unblocked, contiguous period(s) with a "
                    + "free Library classroom", null));
        }
        return new LibraryGapFillOutcome(filled, unfillableSessions);
    }

    /** True if any period in {@code block} overlaps this cohort's Clinical Shift wall-clock window
     *  (bus travel buffer included) on {@code day}. LIBRARY cells are written straight through {@code
     *  TimetableSkeletonService#saveLibraryBlockCells}, bypassing {@code placeCell} and therefore
     *  bypassing its {@code checkClinicalShiftBlocked} guard entirely — so without this the Library
     *  gap-fill would happily book a cohort into the library at 09:00 while that same cohort is on a
     *  07:00–13:00 clinical duty off campus (real symptom: a Wednesday Library block sitting in
     *  periods 1–2 underneath the duty bar). Checked per period rather than across the block's merged
     *  span so a block straddling a lunch gap isn't rejected for the gap itself, exactly matching
     *  {@code placeCell}'s own per-period evaluation. */
    private boolean overlapsClinicalShift(Long cohortId, TermInstance term, DayOfWeek day, List<Period> block) {
        List<ClinicalShiftWindow> windows = clinicalShiftGroupService.resolveActiveWindowsForCohort(cohortId, term.getId());
        if (windows.isEmpty()) {
            return false;
        }
        return block.stream().anyMatch(p -> windows.stream()
            .anyMatch(w -> w.dayOfWeek() == day && w.overlaps(p.getStartTime(), p.getEndTime())));
    }

    /** Every {@code blockSize}-period window of {@code periods} (already ordered by periodOrder)
     *  that is genuinely contiguous in real clock time — one period's end must exactly match the
     *  next's start, since periodOrder alone can span a real gap (e.g. a lunch break between Period
     *  4 and Period 5). Mirrors the adjacency care {@code TimetableSkeletonService#resolveSpanPeriods}
     *  takes for the same reason, for this class's own narrower Library-only need. */
    private List<List<Period>> contiguousPeriodBlocks(List<Period> periods, int blockSize) {
        List<List<Period>> result = new ArrayList<>();
        for (int i = 0; i + blockSize <= periods.size(); i++) {
            List<Period> window = periods.subList(i, i + blockSize);
            boolean contiguous = true;
            for (int j = 0; j < window.size() - 1; j++) {
                if (!window.get(j).getEndTime().equals(window.get(j + 1).getStartTime())) {
                    contiguous = false;
                    break;
                }
            }
            if (contiguous) {
                result.add(new ArrayList<>(window));
            }
        }
        return result;
    }

    /** First Library classroom free for this entire contiguous block — one call spanning the
     *  block's full start-to-end range (not one call per period) since {@link
     *  TimetableStaffingService#checkRoomFree}'s overlap check is a real time-range comparison, so
     *  checking the merged span is exactly equivalent to checking every period individually. */
    private Classroom firstFreeLibraryClassroom(List<Classroom> classrooms, Long termInstanceId, DayOfWeek day, List<Period> block) {
        LocalTime start = block.get(0).getStartTime();
        LocalTime end = block.get(block.size() - 1).getEndTime();
        for (Classroom classroom : classrooms) {
            boolean free = timetableStaffingService.checkRoomFree(ClassSessionType.LIBRARY, classroom.getId(),
                classroom.getRoom(), termInstanceId, null, day, start, end).isEmpty();
            if (free) {
                return classroom;
            }
        }
        return null;
    }

    /** Saves one {@link ClassSchedule} row per period in {@code block}, sharing one {@code
     *  sessionGroupId} when the block spans more than one period (mirrors {@code
     *  TimetableSkeletonService#placeCell}'s own periodSpan convention, so the grid/reports treat
     *  a multi-period Library session as the one linked block it is). Faculty is deliberately left
     *  null — see {@link #fillLibraryGaps}'s javadoc. */
    private List<ClassSchedule> placeLibraryBlock(Subject librarySubject, TermInstance term, DayOfWeek day,
                                                   List<Period> block, CohortSection section, Classroom classroom) {
        // See TimetableSkeletonService#saveLibraryBlockCells's javadoc: REQUIRES_NEW so these rows
        // commit immediately instead of sitting invisible, mid-run, in this method's own ambient
        // transaction.
        return timetableSkeletonService.saveLibraryBlockCells(librarySubject, term, day, block, section, classroom);
    }

    /** Outcome of one idle-batch fallback attempt: cells successfully filled, plus a count of idle
     *  batch instances that could not be given either Library or Self-Study (no free Library room
     *  of any size, or a Self-Study offering exists but no eligible faculty could staff it there). */
    private record IdleFillResult(List<Placement> filled, int unfillableCount) {}

    /** For every LAB/CLINICAL cell this cohort's run just placed (Phase 1), finds its sibling
     *  batches — same {@link CourseOffering}, same {@link CohortSection} (the same
     *  split-into-batches group) — that have no active cell at that exact day/period, and gives
     *  each idle batch a Library or classroom Self-Study fallback instead of leaving it blank (see
     *  {@link #placeIdleBatchFallback} for the confirmed algorithm). Deduplicated per (offering,
     *  sessionType, day, periodIds) so a multi-period block's several period-rows only trigger this
     *  once. Reads sibling occupancy from the live DB ({@link #classScheduleRepository}), not from
     *  an in-memory snapshot — every Phase 1 placement is already persisted by the time this runs,
     *  sibling-batch or not. */
    private void fillIdleBatchGaps(Long cohortId, SkeletonBuilderResponse skeleton, TermInstance term,
            List<Placement> placedThisCohortRun, List<AutoPlaceUnplacedItem> unplacedForCohort, TermDemandAggregation termDemand) {
        List<Placement> justPlaced = new ArrayList<>(placedThisCohortRun);
        Subject librarySubject = subjectRepository.findByCode(LIBRARY_SUBJECT_CODE).orElse(null);
        List<Classroom> libraryClassrooms = classroomRepository
            .findByIsActiveTrueAndRoom_PurposeCategory_CodeOrderByNameAsc(RoomPurposeCategoryCode.LIBRARY);

        Set<String> handledSlots = new HashSet<>();
        int unfillable = 0;
        for (Placement p : justPlaced) {
            if ((p.sessionType() != ClassSessionType.LAB && p.sessionType() != ClassSessionType.CLINICAL) || p.batchId() == null) {
                continue;
            }
            String slotKey = p.courseOfferingId() + ":" + p.sessionType() + ":" + p.dayOfWeek() + ":" + p.periodIds();
            if (!handledSlots.add(slotKey)) {
                continue;
            }
            Batch occupantBatch = batchRepository.findById(p.batchId()).orElse(null);
            if (occupantBatch == null || occupantBatch.getCohortSection() == null) {
                continue; // no section grouping to find siblings against -- leave as-is
            }
            Long sectionId = occupantBatch.getCohortSection().getId();
            List<Batch> siblingGroup = batchRepository.findByCourseOfferingId(p.courseOfferingId()).stream()
                .filter(b -> Boolean.TRUE.equals(b.getIsActive()))
                .filter(b -> b.getCohortSection() != null && b.getCohortSection().getId().equals(sectionId))
                .filter(b -> !b.getId().equals(occupantBatch.getId()))
                .toList();
            if (siblingGroup.isEmpty()) {
                continue;
            }
            List<Long> siblingIds = siblingGroup.stream().map(Batch::getId).toList();
            List<ClassSchedule> siblingCells = classScheduleRepository.findByBatchIdInAndIsActiveTrue(siblingIds);
            Set<Long> occupiedSiblingBatchIds = new HashSet<>();
            for (ClassSchedule cs : siblingCells) {
                if (cs.getDayOfWeek() == p.dayOfWeek() && cs.getPeriod() != null && p.periodIds().contains(cs.getPeriod().getId())
                        && cs.getBatch() != null) {
                    occupiedSiblingBatchIds.add(cs.getBatch().getId());
                }
            }
            List<Batch> idleBatches = siblingGroup.stream()
                .filter(b -> !occupiedSiblingBatchIds.contains(b.getId()))
                .toList();
            if (idleBatches.isEmpty()) {
                continue; // every sibling already occupied at this exact slot -- normal, nothing to do
            }

            IdleFillResult result = placeIdleBatchFallback(idleBatches, occupantBatch.getCohortSection(), term,
                p.dayOfWeek(), p.periodIds(), librarySubject, libraryClassrooms, cohortId, skeleton, termDemand);
            placedThisCohortRun.addAll(result.filled());
            unfillable += result.unfillableCount();
        }
        if (unfillable > 0) {
            unplacedForCohort.add(new AutoPlaceUnplacedItem("Idle batch fallback", ClassSessionType.LIBRARY, null,
                unfillable + " idle batch instance(s) could not be given a Library/Self-Study fallback -- no free "
                    + "Library classroom and no eligible Self-Study faculty available for that slot", null));
        }
    }

    /** Confirmed fallback algorithm: if a single free Library classroom's capacity covers the SUM
     *  of every idle batch's real headcount, all idle batches go there together (sharing one room
     *  concurrently — each still gets its own {@link ClassSchedule} row). Otherwise, give the
     *  largest idle batch a Library seat (if any free room fits it alone) and put the rest into
     *  unsupervised classroom Self-Study, staffed the same way {@link #fillSelfStudyGaps} staffs
     *  its own rows (a THEORY row needs a real faculty to ever be publishable — see {@link
     *  #saveIdleBatchSelfStudyCell}). Every batch is placed as a whole unit — never split. */
    private IdleFillResult placeIdleBatchFallback(List<Batch> idleBatches, CohortSection section, TermInstance term,
            DayOfWeek day, List<Long> periodIds, Subject librarySubject, List<Classroom> libraryClassrooms,
            Long cohortId, SkeletonBuilderResponse skeleton, TermDemandAggregation termDemand) {
        List<Period> block = periodIds.stream().map(id -> periodRepository.findById(id).orElseThrow()).toList();
        int totalIdleHeadcount = idleBatches.stream().mapToInt(this::realBatchHeadcount).sum();

        List<Placement> filled = new ArrayList<>();
        Classroom fittingLibraryRoom = librarySubject == null ? null
            : firstFreeLibraryClassroomForCapacity(libraryClassrooms, term.getId(), day, block, totalIdleHeadcount);
        if (fittingLibraryRoom != null) {
            for (Batch b : idleBatches) {
                filled.add(saveIdleBatchLibraryCell(librarySubject, term, day, block, section, fittingLibraryRoom, b));
            }
            return new IdleFillResult(filled, 0);
        }

        // No single room fits everyone: exactly one batch (largest, to relieve the most seats) to
        // Library if any free room exists (even one too small for the whole group but big enough
        // for one batch); the rest to classroom Self-Study.
        List<Batch> sortedByHeadcountDesc = idleBatches.stream()
            .sorted(Comparator.comparingInt(this::realBatchHeadcount).reversed())
            .toList();
        Batch toLibrary = null;
        Classroom singleFitRoom = null;
        if (librarySubject != null) {
            for (Batch candidate : sortedByHeadcountDesc) {
                Classroom room = firstFreeLibraryClassroomForCapacity(libraryClassrooms, term.getId(), day, block, realBatchHeadcount(candidate));
                if (room != null) {
                    toLibrary = candidate;
                    singleFitRoom = room;
                    break;
                }
            }
        }

        int unfillable = 0;
        for (Batch b : idleBatches) {
            if (b.equals(toLibrary)) {
                filled.add(saveIdleBatchLibraryCell(librarySubject, term, day, block, section, singleFitRoom, b));
                continue;
            }
            Placement selfStudy = saveIdleBatchSelfStudyCell(cohortId, skeleton, term, day, block, section, b, termDemand);
            if (selfStudy != null) {
                filled.add(selfStudy);
            } else {
                unfillable++;
            }
        }
        return new IdleFillResult(filled, unfillable);
    }

    /** Real enrolled headcount if any students are actually registered against this batch, else
     *  its planned {@link Batch#getCapacity()} — mirrors {@link TimetableStaffingService
     *  #resolveRequiredStrength}'s own real-enrollment-first preference. */
    private int realBatchHeadcount(Batch batch) {
        long enrolled = batchRepository.countStudents(batch.getId());
        return enrolled > 0 ? (int) enrolled : (batch.getCapacity() != null ? batch.getCapacity() : 0);
    }

    /** Like {@link #firstFreeLibraryClassroom} but also requires the room's own capacity to cover
     *  {@code requiredHeadcount}, and is called ONCE per idle-batch group rather than once per
     *  batch — {@link TimetableStaffingService#checkRoomFree} treats a classroom as exclusively
     *  single-occupant, so checking it again after the first idle batch's row is saved would reject
     *  the second batch's row against its own group-mate's freshly-placed session. */
    private Classroom firstFreeLibraryClassroomForCapacity(List<Classroom> classrooms, Long termInstanceId,
            DayOfWeek day, List<Period> block, int requiredHeadcount) {
        LocalTime start = block.get(0).getStartTime();
        LocalTime end = block.get(block.size() - 1).getEndTime();
        for (Classroom classroom : classrooms) {
            if (classroom.getCapacity() != null && classroom.getCapacity() < requiredHeadcount) {
                continue;
            }
            boolean free = timetableStaffingService.checkRoomFree(ClassSessionType.LIBRARY, classroom.getId(),
                classroom.getRoom(), termInstanceId, null, day, start, end).isEmpty();
            if (free) {
                return classroom;
            }
        }
        return null;
    }

    /** One idle batch's Library fallback cell — mirrors {@link #placeLibraryBlock} but scopes the
     *  row to this specific batch (today's ordinary Library filler never sets a batch) so it reads
     *  as this batch's own session, not the whole section's. */
    private Placement saveIdleBatchLibraryCell(Subject librarySubject, TermInstance term, DayOfWeek day,
            List<Period> block, CohortSection section, Classroom classroom, Batch idleBatch) {
        // Goes through TimetableSkeletonService#saveIdleBatchLibraryCells (REQUIRES_NEW) rather than
        // a plain classScheduleRepository.save(...) directly in this method's own ambient
        // transaction -- see that method's javadoc: an uncommitted row saved in the ambient
        // transaction (the whole global-auto-schedule run is one @Transactional that doesn't commit
        // until the run finishes) is invisible to a later REQUIRES_NEW call under READ_COMMITTED
        // isolation, which is exactly what made attemptBacktrack's own forceRemoveCell (also
        // REQUIRES_NEW) report every one of these idle-batch Library cells as "already missing from
        // the database" the instant it tried to bump one -- the row was real, just not committed yet.
        List<Long> ids = timetableSkeletonService.saveIdleBatchLibraryCells(
            librarySubject, term, day, block, idleBatch, section, classroom);
        // periodIds must be real Period ids (see Placement's own field, and attemptBacktrack's use
        // of it as tryRestoreAt's day/period target), not `ids` above (each saved ClassSchedule
        // row's own id) -- block.stream()...getId() is this group's actual period ids, primary
        // first, matching every other Placement construction site in this class.
        return new Placement(ids.get(0), null, ClassSessionType.LIBRARY, idleBatch.getId(),
            section != null ? section.getId() : null, null, "Library", idleBatch.getName(), day,
            block.stream().map(Period::getId).toList());
    }

    /** One idle batch's classroom Self-Study fallback cell. Constructed directly (bypassing {@link
     *  TimetableSkeletonService#placeCell}/its cohort-exclusivity check, which would hard-block
     *  this placement: the sibling LAB/CLINICAL cell already occupies the whole section's audience
     *  scope at this exact slot, even though only ONE batch of it is actually busy). Staffed the
     *  same way {@link #fillSelfStudyGaps} staffs its own rows — via {@link #tryStaffWithFallback}
     *  against the cohort's own Self-Study/Co-curricular offering's ranked eligible faculty —
     *  because a THEORY-typed row needs a real {@code faculty_id} (and, once staffed, {@link
     *  TimetableStaffingService} auto-resolves its classroom from {@link
     *  CohortSection#getClassroom()}) before it can ever be published; leaving it permanently
     *  unstaffed like Library would make it un-publishable (Library has its own DB-level bypass for
     *  this — {@code chk_class_schedule_session_shape} — THEORY does not). Returns null (after
     *  undoing the placement) if no eligible Self-Study faculty is free there — the caller reports
     *  this as a genuinely unfillable instance, never leaves a half-placed row behind. */
    private Placement saveIdleBatchSelfStudyCell(Long cohortId, SkeletonBuilderResponse skeleton, TermInstance term,
            DayOfWeek day, List<Period> block, CohortSection section, Batch idleBatch, TermDemandAggregation termDemand) {
        SelfStudyRow selfStudyRow = resolveSelfStudyRowForFallback(cohortId, section, skeleton, termDemand);
        if (selfStudyRow == null) {
            return null;
        }
        // saveIdleBatchTheoryCells runs (and commits) in its own REQUIRES_NEW transaction -- unlike
        // a direct classScheduleRepository.save() here in this method's own ambient transaction, an
        // uncommitted row from which tryStaffWithFallback's staffCell (also REQUIRES_NEW, its own
        // separate connection) could never actually see under READ_COMMITTED isolation. That
        // cross-transaction invisibility used to surface as a spurious "Class schedule not found"
        // the instant staffCell tried to load the row this method had just "saved."
        List<Long> ids = timetableSkeletonService.saveIdleBatchTheoryCells(
            selfStudyRow.offering(), term, day, block, idleBatch, section);
        Long primaryId = ids.get(0);
        Long staffedBy = tryStaffWithFallback(primaryId, selfStudyRow.candidateFacultyIds());
        if (staffedBy == null) {
            // removeCell -> deleteCellAndSiblings already deletes EVERY row sharing this group's
            // sessionGroupId in one call (periodSpan rows are one atomic unit) -- removing just the
            // primary is enough. Looping over every id in `ids` here used to call removeCell a
            // second time on an id its own first call had already deleted as a sibling, throwing a
            // spurious "Class schedule not found" for any multi-period block whose staffing failed.
            timetableSkeletonService.removeCell(primaryId);
            return null;
        }
        // periodIds must be real Period ids, not `ids` (each saved ClassSchedule row's own id) --
        // see the identical fix + explanation in saveIdleBatchLibraryCell just above.
        return new Placement(primaryId, selfStudyRow.offering().getId(), ClassSessionType.THEORY, idleBatch.getId(),
            section != null ? section.getId() : null, staffedBy, selfStudyRow.subjectName(), idleBatch.getName(), day,
            block.stream().map(Period::getId).toList());
    }

    /** First Self-Study/Co-curricular THEORY budget row configured for this cohort (scoped to
     *  {@code section} when the cohort has committed sections), with its ranked fallback faculty
     *  list — the same lookup {@link #fillSelfStudyGaps} performs across every self-study row it
     *  finds, simplified here to the first match since an idle-batch fallback only ever needs one
     *  usable offering, not an exhaustive per-period ranking across several. Reads from the
     *  already-fetched {@code skeleton} snapshot rather than re-querying — the Self-Study
     *  offering/budget's own identity is static configuration, unaffected by what this run has
     *  placed so far. */
    // Package-private (not private) so this regression can be verified directly.
    SelfStudyRow resolveSelfStudyRowForFallback(Long cohortId, CohortSection section,
            SkeletonBuilderResponse skeleton, TermDemandAggregation termDemand) {
        for (SkeletonSubjectResponse subject : skeleton.subjects()) {
            if (!isSelfStudySubject(subject.subjectName())) {
                continue;
            }
            CourseOffering offering = courseOfferingRepository.findById(subject.courseOfferingId()).orElse(null);
            if (offering == null || timetableSkeletonService.isElectiveOffering(offering)) {
                continue;
            }
            for (SkeletonSubjectBudget budget : subject.budgets()) {
                if (budget.sessionType() != ClassSessionType.THEORY) {
                    continue;
                }
                if (section != null && budget.cohortSectionId() != null && !budget.cohortSectionId().equals(section.getId())) {
                    continue;
                }
                if (budget.placedSessionsPerWeek() < budget.requiredSessionsPerWeek()) {
                    // This section's own regular curriculum requirement for this subject is NOT yet
                    // fully placed (per the pre-run skeleton snapshot) -- Phase 2, which runs right
                    // after this idle-batch pass, is what's responsible for closing that gap via its
                    // own ShortfallRow for this exact (offering, section). Using it as "extra" filler
                    // here would consume the same (offering, THEORY, section) budget bucket {@link
                    // #checkBudgetNotExceeded} enforces -- since saveIdleBatchTheoryCells stamps this
                    // filler cell with this same section, it counts identically to a real required
                    // session. Phase 2's later attempt would then find the budget already "met" by
                    // this filler and get hard-rejected, reporting the genuine requirement as
                    // permanently unplaced even though real capacity existed -- worse, if THIS filler
                    // cell later gets bumped by attemptBacktrack and can't relocate, the section ends
                    // up with fewer delivered Self-Study sessions than required and nothing left to
                    // retry it. Skip this row; try the next Self-Study subject instead (if the cohort
                    // configures more than one), or fall through to null so the caller counts this one
                    // idle batch as genuinely unfillable -- a small, honest gap, not a stolen budget
                    // slot.
                    continue;
                }
                Long primaryFacultyId = resolveBudgetFacultyId(offering, budget, cohortId);
                return new SelfStudyRow(subject.subjectName(), offering, budget,
                    rankedFallbackFacultyIds(offering, primaryFacultyId, termDemand));
            }
        }
        return null;
    }

    private int resolveLibraryConfigInt(String configKey, int defaultValue) {
        return systemConfigurationService.findByKey(configKey)
            .map(SystemConfigurationResponse::configValue)
            .filter(v -> v != null && !v.isBlank())
            .map(v -> {
                try {
                    int parsed = Integer.parseInt(v.trim());
                    return parsed > 0 ? parsed : defaultValue;
                } catch (NumberFormatException e) {
                    return defaultValue;
                }
            })
            .orElse(defaultValue);
    }

    /** Tries {@code candidateFacultyIds} in order against an already-placed (free) cell, stopping at
     *  the first one whose {@link TimetableStaffingService#staffCell} actually succeeds — a real,
     *  live check against that exact day/period (conflicts, daily/weekly/continuous caps), not the
     *  term-aggregate ranking that ordered the list. Returns the faculty id that succeeded, or
     *  {@code null} if every candidate failed (caller is responsible for removing the now-orphaned
     *  placed cell). */
    private Long tryStaffWithFallback(Long cellId, List<Long> candidateFacultyIds) {
        for (Long facultyId : candidateFacultyIds) {
            try {
                timetableStaffingService.staffCell(cellId, new StaffingAssignmentRequest(facultyId, null));
                return facultyId;
            } catch (TimetableConstraintViolationException | LifecycleConflictException | IllegalArgumentException ex) {
                // try the next candidate
            }
        }
        return null;
    }

    /** The row's own bound faculty first (unchanged default), then every other eligible-and-not-
     *  already-over-capacity faculty for this subject, ranked least-remaining-capacity-first — "top
     *  off" whoever is already closest to their configured cap before ever reaching for someone with
     *  lots of untouched spare time, so a fixed fallback pool concentrates onto as few people as it
     *  can rather than spreading thin. An uncapped candidate (no tier configured at all) sorts last:
     *  real, capped spare capacity is a scarcer resource to use up first than an open-ended "no limit
     *  configured" faculty member. Reuses {@link #eligiblePoolGrandfathering}/{@link #candidateDto} —
     *  the same machinery backing the offering/section/cohort candidate pickers — rather than a new
     *  eligibility rule, and a single caller-supplied {@link TermDemandAggregation} snapshot (computed
     *  once per run, not once per period) so this stays cheap at Global Auto-Schedule's whole-term
     *  scale. Originally Self-Study-filler-only; also backs every ordinary {@link ShortfallRow}'s own
     *  {@code candidateFacultyIds} now (see that record's javadoc) — the ranking logic itself was
     *  always subject-generic, only its old name and its one caller were Self-Study-specific. */
    private List<Long> rankedFallbackFacultyIds(CourseOffering offering, Long primaryFacultyId, TermDemandAggregation termDemand) {
        List<Long> candidateFacultyIds = new ArrayList<>();
        if (primaryFacultyId != null) {
            candidateFacultyIds.add(primaryFacultyId);
        }
        candidateFacultyIds.addAll(rankedFallbackCandidates(offering, primaryFacultyId, termDemand).stream()
            .map(EligibleFacultyCandidateDto::facultyId)
            .toList());
        return candidateFacultyIds;
    }

    /** The ranked fallback pool itself (primary excluded), each still carrying its own {@code
     *  remainingHours}/{@code capacityTier} snapshot at the moment of ranking — {@link
     *  #rankedFallbackFacultyIds} strips this down to bare ids for {@link #tryPlaceAndStaff}, but
     *  {@link ShortfallRow#fallbackCandidatesById()} keeps the full DTO around so a real substitution
     *  (see {@link #recordFacultySubstitutionIfAny}) can report the substitute's own workload at
     *  selection time, not just their name — "was this person already tight" is exactly what an
     *  admin needs before trusting a reassignment suggestion. */
    private List<EligibleFacultyCandidateDto> rankedFallbackCandidates(CourseOffering offering, Long primaryFacultyId, TermDemandAggregation termDemand) {
        Subject subject = offering.getSubject();
        List<Faculty> pool = eligiblePoolGrandfathering(subject,
            primaryFacultyId != null ? Set.of(primaryFacultyId) : Set.of());
        return pool.stream()
            .filter(f -> !f.getId().equals(primaryFacultyId))
            .map(f -> candidateDto(subject, f, termDemand, false, 0))
            .filter(c -> !c.overCapacity())
            .sorted(Comparator.comparingDouble(c -> "NONE".equals(c.capacityTier()) ? Double.MAX_VALUE : c.remainingHours()))
            .toList();
    }

    private static boolean isSelfStudySubject(String subjectName) {
        return com.cms.util.SelfStudySubjects.isSelfStudySubject(subjectName);
    }

    /** One cell (or, for a multi-period block, the whole linked group) this cohort's run has
     *  successfully placed+staffed — carries enough to identify its own row (for {@link
     *  #attemptBacktrack}'s "different row" check) and to fully restore it (exact slot, every
     *  period in its block, and the faculty who was staffing it) if a later row ends up displacing
     *  it. {@code periodIds} is ordered, primary first — a single-element list for an ordinary
     *  blockSize-1 placement. */
    record Placement(Long cellId, Long courseOfferingId, ClassSessionType sessionType, Long batchId,
                              Long cohortSectionId, Long facultyId, String subjectName, String occupantLabel,
                              DayOfWeek dayOfWeek, List<Long> periodIds) {
        // Section equality is only required for a THEORY row (batchId null) -- a LAB/CLINICAL row's
        // cohortSectionId carries its batch's own section (see resolveBudgetFacultyId), but a placed
        // LAB/CLINICAL cell's own section is always null (TimetableSkeletonService never persists
        // one for those types), so comparing it directly would always be null-vs-real and never match.
        // courseOfferingId is null-safe (Objects.equals, not a direct .equals()) because a LIBRARY
        // placement from the idle-batch fallback (see #saveIdleBatchLibraryCell) carries a null
        // courseOfferingId -- a real ShortfallRow (always backed by a genuine offering) can never
        // equal it either way, but must not NPE evaluating that against a genuine offering's id.
        boolean sameRowAs(ShortfallRow row) {
            return Objects.equals(courseOfferingId, row.offering().getId()) && sessionType == row.budget().sessionType()
                && Objects.equals(batchId, row.budget().batchId())
                && (row.budget().batchId() != null || Objects.equals(cohortSectionId, row.budget().cohortSectionId()));
        }
    }

    /** One session placed with a fallback faculty (see {@link ShortfallRow#candidateFacultyIds})
     *  instead of the row's own primary bound faculty — raw material for {@link
     *  #buildFacultySubstitutionTips}, which aggregates these into a session count per (subject,
     *  original, substitute) tuple at the end of the run. {@code substituteRemainingHours}/{@code
     *  substituteCapacityTier} are the substitute's own workload snapshot from {@link
     *  ShortfallRow#fallbackCandidatesById()} at the moment they were ranked as a fallback candidate
     *  — before this run added anything to them — so the resulting tip can answer "was this person
     *  already tight" honestly, not just report a name. {@code Double.NaN}/{@code null} only if the
     *  substitute's own snapshot somehow isn't in the row's map (defensive; should never happen since
     *  {@code actualFacultyId} can only be a candidate that came from that exact map). */
    private record FacultySubstitutionEvent(Long courseOfferingId, String subjectName, Long originalFacultyId,
                                              Long substituteFacultyId, double substituteRemainingHours, String substituteCapacityTier,
                                              Long cohortId, Long cohortSectionId, Long batchId) {}

    /** Appends a {@link FacultySubstitutionEvent} to {@code events} only when {@code
     *  actualFacultyId} differs from {@code row}'s own primary bound faculty — a plain no-op on the
     *  overwhelmingly common case where the first (and usually only) candidate in {@code
     *  row.candidateFacultyIds()} is the one that actually got staffed. {@code cohortId} is the
     *  cohort this placement belongs to (THEORY rows are always per-cohort — see {@link ShortfallRow}
     *  javadoc); paired with {@code row.budget().cohortSectionId()} (null for an unsplit cohort) this
     *  identifies exactly which live faculty-assignment row this event's fallback covered, so {@link
     *  #buildFacultySubstitutionTips} can scope each tip to precisely the row(s) it reports on.
     *  {@code row.budget().batchId()} is carried through too -- non-null for a LAB/CLINICAL row,
     *  where it (not {@code cohortSectionId}) is what a confirm action actually reassigns, since that
     *  row's own faculty lives on {@code Batch#coordinatorFaculty}, not {@code
     *  CourseOfferingSectionFaculty}. */
    private static void recordFacultySubstitutionIfAny(ShortfallRow row, Long cohortId, Long actualFacultyId, List<FacultySubstitutionEvent> events) {
        if (!Objects.equals(actualFacultyId, row.facultyId())) {
            EligibleFacultyCandidateDto snapshot = row.fallbackCandidatesById().get(actualFacultyId);
            events.add(new FacultySubstitutionEvent(row.offering().getId(), row.subjectName(), row.facultyId(), actualFacultyId,
                snapshot != null ? snapshot.remainingHours() : Double.NaN,
                snapshot != null ? snapshot.capacityTier() : "NONE",
                cohortId, row.budget().cohortSectionId(), row.budget().batchId()));
        }
    }

    /** Aggregates every {@link FacultySubstitutionEvent} this run recorded into one {@link
     *  FacultySubstitutionTip} per distinct (course offering, original faculty, substitute faculty)
     *  tuple, each carrying how many sessions actually needed the substitute — the concrete,
     *  actionable number behind a "consider reassigning this offering" suggestion, not a vague hint.
     *  Faculty names are resolved once here rather than per event; a since-deactivated/deleted
     *  faculty (id no longer resolvable) falls back to the raw id as its own display name rather than
     *  dropping the tip outright. Empty when this run needed no fallback at all — the common case,
     *  and never itself a sign anything went wrong.
     *
     * <p>Grouped by {@code courseOfferingId}, not just subject name: the same subject can have a
     *  separate {@code CourseOffering} row per cohort, and merging those into one tip would silently
     *  drop every offering but the first from {@code affectedSections} while still reporting their
     *  combined {@code sessionCount} — a session count Submit could never actually apply. {@code
     *  affectedSections} is the exact, disjoint set of (cohort, section) rows this tip's own events
     *  covered, carried through to {@link com.cms.dto.ConfirmFacultySubstitutionItem} on Submit so
     *  confirming this tip can never reassign a sibling section a different, separately-ticked tip
     *  already claimed (see {@code CourseOfferingSectionFacultyService#confirmSubstitutions}).
     *
     * <p>{@code substituteTotalSessionsThisRun} is that same substitute's grand total across EVERY
     *  subject they picked up this run, not just this one tip's own {@code sessionCount} — each
     *  individual event's capacity snapshot is a pre-run baseline (see {@link
     *  FacultySubstitutionEvent}'s javadoc), so it can't by itself see a substitute quietly
     *  accumulating several *different* subjects' fallback sessions across separate rows in the same
     *  run. Surfacing the cross-subject total lets an admin catch "this person is now the fallback
     *  for everything" even though no single ranking moment ever saw it happening. */
    private List<FacultySubstitutionTip> buildFacultySubstitutionTips(List<FacultySubstitutionEvent> events) {
        if (events.isEmpty()) {
            return List.of();
        }
        Map<Long, Integer> totalSessionsBySubstitute = new LinkedHashMap<>();
        for (FacultySubstitutionEvent e : events) {
            totalSessionsBySubstitute.merge(e.substituteFacultyId(), 1, Integer::sum);
        }
        Map<String, List<FacultySubstitutionEvent>> grouped = events.stream()
            .collect(Collectors.groupingBy(e -> e.courseOfferingId() + "|" + e.originalFacultyId() + "|" + e.substituteFacultyId(),
                LinkedHashMap::new, Collectors.toList()));
        return grouped.values().stream()
            .map(group -> {
                FacultySubstitutionEvent first = group.get(0);
                List<SubstitutionAffectedSection> affectedSections = group.stream()
                    .map(e -> new SubstitutionAffectedSection(e.cohortId(), e.cohortSectionId(), e.batchId()))
                    .distinct()
                    .toList();
                return new FacultySubstitutionTip(first.subjectName(), first.originalFacultyId(),
                    facultyDisplayName(first.originalFacultyId()), first.substituteFacultyId(),
                    facultyDisplayName(first.substituteFacultyId()), group.size(), first.courseOfferingId(),
                    Double.isNaN(first.substituteRemainingHours()) ? null : first.substituteRemainingHours(),
                    first.substituteCapacityTier(), totalSessionsBySubstitute.get(first.substituteFacultyId()),
                    affectedSections);
            })
            .sorted(Comparator.comparingInt((FacultySubstitutionTip t) -> -t.sessionCount()))
            .toList();
    }

    private String facultyDisplayName(Long facultyId) {
        return facultyRepository.findById(facultyId).map(Faculty::getFullName).orElse("Faculty #" + facultyId);
    }

    /** Bounded, single-attempt backtrack: displaces the single most-recently-placed cell from a
     *  *different* row this cohort's run itself placed, retries {@code row}, and — only if that
     *  retry succeeds — tries to restore the displaced cell to its exact original slot with its
     *  original faculty. Mirrors {@code TimetableSkeletonAutoPlaceService#attemptBacktrack}'s
     *  pattern (that service's own per-cohort tool has carried this since R3 Step 6), with one
     *  necessary divergence: {@code bumped} here is always already staffed (every placement in this
     *  class is staffed in the same step it's placed — see {@link #tryPlaceAndStaff}), so displacing
     *  it goes through {@link TimetableSkeletonService#forceRemoveCell} rather than the ordinary
     *  {@link TimetableSkeletonService#removeCell}, which would reject a staffed cell outright. Never
     *  leaves the run with fewer total placements than before the attempt: if the retry fails, the
     *  bumped cell is put straight back and nothing changes; if the retry succeeds but the restore
     *  fails, the bumped row is reported unplaced (a wash, not a loss) instead of silently
     *  disappearing from the report. */
    private boolean attemptBacktrack(Long cohortId, ShortfallRow row, TermInstance term, List<Period> periods,
                                      Set<DayOfWeek> daysUsed, List<Placement> placedThisCohortRun,
                                      List<AutoPlaceUnplacedItem> unplacedForCohort, int blockSize,
                                      Map<DayOfWeek, Integer> dayLoad) {
        for (int idx = placedThisCohortRun.size() - 1; idx >= 0; idx--) {
            Placement bumped = placedThisCohortRun.get(idx);
            if (bumped.sameRowAs(row)) {
                continue;
            }
            // Bumped is removed from both the database and this run's own tracking list up front,
            // unconditionally — its cell no longer exists either way once forceRemoveCell runs, so
            // the list must never keep a stale reference to it regardless of how the retry below
            // goes. forceRemoveCell (not the ordinary removeCell) because bumped is always staffed
            // by this point — every global-auto-schedule placement is staffed in the same step it's
            // placed in (see tryPlaceAndStaff), so there is never an unstaffed cell here to bump.
            //
            // Defensive: production has shown placedThisCohortRun occasionally holding a Placement
            // whose cell is already gone from the database by the time backtrack reaches it (root
            // cause still under investigation — every known addition path here writes a real,
            // just-verified id). Previously this threw ResourceNotFoundException uncaught, which
            // aborted the ENTIRE global-auto-schedule run (every other cohort's real, already-
            // committed progress included) over one stale bookkeeping entry. See {@link
            // #forceRemoveCellIfPresent}: logged with full context so a repeat pins down the
            // upstream cause; the run itself now just drops the dead entry and tries the next
            // backtrack candidate instead of crashing outright.
            if (!forceRemoveCellIfPresent(bumped.cellId(), cohortId, bumped)) {
                placedThisCohortRun.remove(idx);
                continue;
            }
            placedThisCohortRun.remove(idx);
            dayLoad.merge(bumped.dayOfWeek(), -bumped.periodIds().size(), Integer::sum);
            PlacementAttempt retry = tryPlaceAndStaff(cohortId, row.offering(), row.budget(), row.candidateFacultyIds(),
                term, periods, daysUsed, blockSize, dayLoad, Set.of());
            if (retry.dayPlaced() == null) {
                // No better off than before -- put the bumped cell straight back and give up on `row`.
                restoreBumpedOrReportUnplaced(bumped, cohortId, placedThisCohortRun, unplacedForCohort, dayLoad, term, periods);
                return false;
            }
            dayLoad.merge(retry.dayPlaced(), blockSize, Integer::sum);
            daysUsed.add(retry.dayPlaced());
            placedThisCohortRun.add(new Placement(retry.cellId(), row.offering().getId(), row.budget().sessionType(),
                row.budget().batchId(), row.budget().cohortSectionId(), retry.facultyId(), row.subjectName(),
                occupantLabel(row.budget()), retry.dayPlaced(), retry.periodIds()));
            restoreBumpedOrReportUnplaced(bumped, cohortId, placedThisCohortRun, unplacedForCohort, dayLoad, term, periods);
            // Whether or not the restore worked, `row` is now placed and the total count never
            // dropped below what it was before this attempt (net zero at worst, a genuine swap).
            return true;
        }
        return false;
    }

    /** Shared guard around every {@code forceRemoveCell} call in this class's backtrack/upgrade
     *  paths ({@link #attemptBacktrack}, {@link #tryUpgradeSwap}) — both bump a {@link Placement}
     *  this run recorded earlier and expect its cell to still be real, but production has shown
     *  that assumption occasionally doesn't hold (root cause still under investigation). Letting
     *  {@link com.cms.exception.ResourceNotFoundException} propagate uncaught here aborts the
     *  ENTIRE global-auto-schedule run — every other cohort's real, already-committed progress
     *  included — over one stale bookkeeping entry, which is worse than dropping the one entry.
     *  Returns {@code true} if the cell was genuinely removed, {@code false} if it was already gone
     *  (logged either way the caller can tell the two apart and react accordingly). */
    private boolean forceRemoveCellIfPresent(Long cellId, Long cohortId, Placement placement) {
        try {
            timetableSkeletonService.forceRemoveCell(cellId);
            return true;
        } catch (ResourceNotFoundException ex) {
            log.warn("Global Auto-Schedule: cell {} (offering {}, {}, batch {}, section {}) tracked in cohort {}'s "
                    + "run but already missing from the database — dropping the stale placement instead of "
                    + "aborting the run",
                cellId, placement.courseOfferingId(), placement.sessionType(), placement.batchId(),
                placement.cohortSectionId(), cohortId);
            return false;
        }
    }

    /** Shared tail of both {@link #attemptBacktrack} outcomes (and of {@link #tryUpgradeSwap}'s own
     *  bump attempts): try to put {@code bumped} back exactly where it was (already removed from
     *  {@code placedThisCohortRun} by the caller); if that exact slot is gone, fall back to a full
     *  day/period search — the same one a freshly-failing row gets via {@link #tryPlaceAndStaff} —
     *  before giving up. Restoring to the exact original slot only was too narrow: a bump can lose a
     *  session even when a different day/period is genuinely free for it, which is exactly what a
     *  real cohort run surfaced (several Theory rows reported unplaced with a free Monday-Friday
     *  slot still available elsewhere in the week). Only once both the exact restore and the full
     *  search fail is it recorded as a fresh unplaced item, instead of letting it silently vanish
     *  from the report. */
    private void restoreBumpedOrReportUnplaced(Placement bumped, Long cohortId, List<Placement> placedThisCohortRun,
                                                List<AutoPlaceUnplacedItem> unplacedForCohort, Map<DayOfWeek, Integer> dayLoad,
                                                TermInstance term, List<Period> periods) {
        Optional<Placement> restored = tryRestoreExact(bumped, cohortId);
        if (restored.isPresent()) {
            placedThisCohortRun.add(restored.get());
            dayLoad.merge(bumped.dayOfWeek(), bumped.periodIds().size(), Integer::sum);
            return;
        }
        // courseOfferingId is null for a LIBRARY placement (both the idle-batch fallback's own
        // #saveIdleBatchLibraryCell and #fillLibraryGaps) -- Spring Data's findById throws
        // IllegalArgumentException on a null id rather than returning empty, so it must be skipped
        // outright rather than passed in.
        CourseOffering offering = bumped.courseOfferingId() == null ? null
            : courseOfferingRepository.findById(bumped.courseOfferingId()).orElse(null);
        if (offering != null) {
            Set<DayOfWeek> daysUsed = placedThisCohortRun.stream()
                .filter(p -> sameRow(p, bumped))
                .map(Placement::dayOfWeek)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(DayOfWeek.class)));
            SkeletonSubjectBudget minimalBudget = new SkeletonSubjectBudget(bumped.sessionType(), bumped.batchId(), null,
                bumped.cohortSectionId(), null, 0, 0, 0, 0);
            // List.of(bumped.facultyId()) deliberately, not a ranked fallback list -- this restores
            // a displaced row back to its own original faculty, never a substitute, unlike a fresh
            // ShortfallRow's own candidateFacultyIds.
            PlacementAttempt retry = tryPlaceAndStaff(cohortId, offering, minimalBudget, List.of(bumped.facultyId()),
                term, periods, daysUsed, bumped.periodIds().size(), dayLoad, Set.of());
            if (retry.dayPlaced() != null) {
                dayLoad.merge(retry.dayPlaced(), bumped.periodIds().size(), Integer::sum);
                placedThisCohortRun.add(new Placement(retry.cellId(), bumped.courseOfferingId(), bumped.sessionType(),
                    bumped.batchId(), bumped.cohortSectionId(), retry.facultyId(), bumped.subjectName(),
                    bumped.occupantLabel(), retry.dayPlaced(), retry.periodIds()));
                return;
            }
        } else if (bumped.sessionType() == ClassSessionType.LIBRARY) {
            // No CourseOffering exists for Library, so the branch above can never run for it -- without
            // this, every bumped Library session was reported unplaced the instant its exact original
            // slot was gone, even when a different Monday-Friday day was still genuinely free (see
            // #tryRePlaceBumpedLibrarySession).
            Optional<Placement> relocated = tryRePlaceBumpedLibrarySession(bumped, cohortId, term, periods, dayLoad, placedThisCohortRun);
            if (relocated.isPresent()) {
                placedThisCohortRun.add(relocated.get());
                return;
            }
        }
        unplacedForCohort.add(new AutoPlaceUnplacedItem(bumped.subjectName(), bumped.sessionType(), bumped.occupantLabel(),
            "displaced during a backtrack attempt and could not be placed at its original slot or any other free day/period",
            bumped.courseOfferingId()));
    }

    /** Mirrors {@link #fillLibraryGaps}'s own free-day/free-classroom search, scoped to finding just
     *  ONE contiguous block matching {@code bumped}'s own period-span, for a Library session {@link
     *  #attemptBacktrack} displaced mid-run to make room for a higher-priority Theory/Lab/Clinical
     *  placement. Monday-Friday only, matching {@code fillLibraryGaps}' own reasoning -- Saturday
     *  stays real, occasional overflow capacity, not filler for its own sake. Excludes any day this
     *  same audience already has a (different, successfully-placed) Library session on this run, so
     *  a relocated session can never double up a day against the cohort's own weekly Library quota.
     *  Package-private (not private) so this regression can be verified directly. */
    Optional<Placement> tryRePlaceBumpedLibrarySession(Placement bumped, Long cohortId, TermInstance term,
                                                                 List<Period> periods, Map<DayOfWeek, Integer> dayLoad,
                                                                 List<Placement> placedThisCohortRun) {
        Subject librarySubject = subjectRepository.findByCode(LIBRARY_SUBJECT_CODE).orElse(null);
        List<Classroom> libraryClassrooms = classroomRepository
            .findByIsActiveTrueAndRoom_PurposeCategory_CodeOrderByNameAsc(RoomPurposeCategoryCode.LIBRARY);
        if (librarySubject == null || libraryClassrooms.isEmpty()) {
            return Optional.empty();
        }
        CohortSection section = null;
        if (bumped.cohortSectionId() != null) {
            section = timetableSkeletonService.resolveActiveSections(cohortId, term.getId()).stream()
                .filter(s -> s.getId().equals(bumped.cohortSectionId()))
                .findFirst().orElse(null);
            if (section == null) {
                return Optional.empty(); // section no longer active -- nothing sane to restore onto
            }
        }

        Set<DayOfWeek> daysUsed = placedThisCohortRun.stream()
            .filter(p -> p.sessionType() == ClassSessionType.LIBRARY && Objects.equals(p.cohortSectionId(), bumped.cohortSectionId()))
            .map(Placement::dayOfWeek)
            .collect(Collectors.toCollection(() -> EnumSet.noneOf(DayOfWeek.class)));
        List<DayOfWeek> orderedDays = List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY).stream()
            .filter(d -> !daysUsed.contains(d))
            .sorted(Comparator.comparingInt(dayLoad::get))
            .toList();

        List<List<Period>> candidateBlocks = contiguousPeriodBlocks(periods, bumped.periodIds().size());
        for (DayOfWeek day : orderedDays) {
            for (List<Period> block : candidateBlocks) {
                boolean blocked = block.stream().anyMatch(p ->
                    blockedPeriodChecker.blockReason(day, p.getStartTime(), p.getEndTime(), term).isPresent());
                if (blocked) {
                    continue;
                }
                if (overlapsClinicalShift(cohortId, term, day, block)) {
                    continue;
                }
                boolean slotFree = block.stream().allMatch(p ->
                    timetableSkeletonService.isSlotFreeForCohort(cohortId, term.getId(), day, p.getId()));
                if (!slotFree) {
                    continue;
                }
                Classroom classroom = firstFreeLibraryClassroom(libraryClassrooms, term.getId(), day, block);
                if (classroom == null) {
                    continue;
                }
                List<ClassSchedule> saved = placeLibraryBlock(librarySubject, term, day, block, section, classroom);
                dayLoad.merge(day, block.size(), Integer::sum);
                return Optional.of(new Placement(saved.get(0).getId(), null, ClassSessionType.LIBRARY, null,
                    bumped.cohortSectionId(), null, "Library", bumped.occupantLabel(), day,
                    block.stream().map(Period::getId).toList()));
            }
        }
        return Optional.empty();
    }

    /** Re-places {@code placement} at its exact original day/period(s) — the full block, not just
     *  its primary period, so a displaced multi-period Lab/Clinical session is restored whole, never
     *  collapsed down to a single period — and re-staffs it with its original faculty. Either half
     *  failing (the exact slot got taken by the retry itself — the genuine-swap case — or the
     *  original faculty is no longer free there) means the restore as a whole failed; a
     *  half-placed-but-unstaffed cell is never left behind. Thin wrapper over {@link #tryRestoreAt},
     *  which this and {@link #attemptSaturdayUpgradeMending} both need — the only difference between
     *  a plain restore and a mending swap is which day/period(s) the retry targets. */
    private Optional<Placement> tryRestoreExact(Placement placement, Long cohortId) {
        return tryRestoreAt(placement, placement.dayOfWeek(), placement.periodIds(), cohortId);
    }

    /** Places {@code placement}'s offering/batch/section at {@code day}/{@code periodIds} (which
     *  need not be its original slot — see {@link #attemptSaturdayUpgradeMending}) and re-staffs it
     *  with its original faculty. Either half failing means the whole attempt failed; a
     *  half-placed-but-unstaffed cell is never left behind. */
    private Optional<Placement> tryRestoreAt(Placement placement, DayOfWeek day, List<Long> periodIds, Long cohortId) {
        // A LIBRARY placement from the idle-batch fallback (see #saveIdleBatchLibraryCell) carries a
        // null courseOfferingId/facultyId -- it was never placed via placeCell/staffCell in the
        // first place (see TimetableSkeletonService#saveIdleBatchLibraryCells), so there's no
        // placeCell-based restore to attempt here. Spring Data's findById throws
        // IllegalArgumentException (not a normal "not found") on a null id, which previously escaped
        // uncaught here and aborted the ENTIRE global-auto-schedule run over one bumped filler cell.
        // Skip straight to "couldn't restore" so the caller's own null-safe fallback/report path
        // runs instead (see restoreBumpedOrReportUnplaced, which already guards its own
        // CourseOffering-based fallback the same way).
        if (placement.courseOfferingId() == null) {
            return Optional.empty();
        }
        Long primaryPeriodId = periodIds.get(0);
        List<Long> spanPeriodIds = periodIds.size() > 1 ? periodIds.subList(1, periodIds.size()) : null;
        SkeletonCellResponse restored;
        try {
            restored = timetableSkeletonService.placeCell(new SkeletonCellPlacementRequest(
                placement.courseOfferingId(), placement.sessionType(), day, primaryPeriodId,
                placement.batchId(), cohortId, placement.cohortSectionId(), spanPeriodIds));
        } catch (TimetableConstraintViolationException ex) {
            return Optional.empty();
        } catch (IllegalArgumentException ex) {
            // The idle-batch Self-Study counterpart of the LIBRARY case guarded just above: {@link
            // #saveIdleBatchSelfStudyCell} deliberately stamps its Placement's courseOfferingId as
            // the Self-Study offering while batchId stays the idle LAB/CLINICAL batch's own id (that
            // batch's REAL CourseOffering is whatever it was originally scheduled under) -- a
            // combination placeCell's own batch/offering ownership check (see
            // TimetableSkeletonService#placeCell) always rejects with IllegalArgumentException, since
            // it was never placed via placeCell in the first place. Same fix as the null-offering
            // guard above: skip straight to "couldn't restore" instead of letting this escape uncaught
            // and abort the entire run over one bumped filler cell.
            return Optional.empty();
        }
        try {
            timetableStaffingService.staffCell(restored.id(), new StaffingAssignmentRequest(placement.facultyId(), null));
        } catch (TimetableConstraintViolationException | LifecycleConflictException | IllegalArgumentException ex) {
            timetableSkeletonService.removeCell(restored.id());
            return Optional.empty();
        }
        return Optional.of(new Placement(restored.id(), placement.courseOfferingId(), placement.sessionType(),
            placement.batchId(), placement.cohortSectionId(), placement.facultyId(), placement.subjectName(),
            placement.occupantLabel(), day, periodIds));
    }

    /** Phase 5 — Saturday-upgrade mending, run once per cohort after every other phase (including
     *  the Library/Self-Study filler passes) has placed everything it's going to this run — see the
     *  class javadoc's "Placement order" section for why this must come last. A THEORY row that
     *  only found room on Saturday during Phase 2 is a genuine SUCCESS as far as {@link
     *  #placeShortfallRow} is concerned — {@link #attemptBacktrack} never even sees it, since
     *  nothing failed. But on a term whose {@code workingSaturdayWeeks} pattern isn't every week
     *  (see {@link WorkingSaturdayCalculator}), that weekly session actually fires far less often
     *  than a real Monday-Friday one, so the subject finishes the term genuinely short on hours even
     *  though the grid itself looks fully packed — the exact real-world case this closes (a cohort
     *  whose Mon-Fri week was fully saturated by Clinical/Lab/other Theory, so Self-Study/Co-
     *  curricular and a low-priority Theory subject only ever found Saturday). This pass tries, for
     *  each such placement, to reclaim a Monday-Friday slot by bumping whatever now sits on one —
     *  see {@link #tryUpgradeSwap} for exactly what gets bumped and how a failed attempt is
     *  guaranteed to leave the run no worse than before it. Out of scope by design: LAB/CLINICAL
     *  (rigid multi-period blocks needing their own contiguity-aware repair) and Library (a fixed
     *  weekly quota with its own classroom/section bookkeeping this pass doesn't reproduce) — both
     *  stay exactly as Phase 1/4 left them. */
    private void attemptSaturdayUpgradeMending(Long cohortId, TermInstance term, List<Period> periods, CohortRunContext context) {
        if (term.getWorkingSaturdayWeeks().isEmpty()) {
            return; // nothing could have landed on Saturday at all this run
        }
        List<Placement> saturdayTheoryPlacements = context.placedThisCohortRun().stream()
            .filter(p -> p.dayOfWeek() == DayOfWeek.SATURDAY && p.sessionType() == ClassSessionType.THEORY)
            .toList();
        for (Placement saturdayPlacement : saturdayTheoryPlacements) {
            if (!context.placedThisCohortRun().contains(saturdayPlacement)) {
                continue; // already moved off Saturday by an earlier candidate attempt below
            }
            upgradeOneSaturdayPlacement(cohortId, term, periods, context, saturdayPlacement);
        }
    }

    /** Picks up to {@link #MAX_SATURDAY_MENDING_CANDIDATES} weekday placements to try bumping for
     *  {@code saturdayPlacement}, Self-Study/Co-curricular filler first (cheapest to bump — see
     *  {@link #tryUpgradeSwap}), stopping at the first candidate whose swap actually succeeds. Only
     *  ever considers a same-block-size THEORY placement — LAB/CLINICAL and Library are handled by
     *  their own phases, not this one (see {@link #attemptSaturdayUpgradeMending}'s javadoc). */
    private void upgradeOneSaturdayPlacement(Long cohortId, TermInstance term, List<Period> periods,
                                              CohortRunContext context, Placement saturdayPlacement) {
        List<Placement> candidates = context.placedThisCohortRun().stream()
            .filter(p -> p != saturdayPlacement)
            .filter(p -> !sameRow(p, saturdayPlacement)) // bumping this row's own other session helps nothing
            .filter(p -> p.dayOfWeek() != DayOfWeek.SATURDAY)
            .filter(p -> p.sessionType() == ClassSessionType.THEORY)
            .filter(p -> p.periodIds().size() == saturdayPlacement.periodIds().size())
            .sorted(Comparator.comparing((Placement p) -> isSelfStudySubject(p.subjectName()) ? 0 : 1))
            .limit(MAX_SATURDAY_MENDING_CANDIDATES)
            .toList();
        Placement current = saturdayPlacement;
        for (Placement candidate : candidates) {
            if (tryUpgradeSwap(cohortId, term, periods, context, current, candidate)) {
                return;
            }
            // A failed attempt restores the Saturday row under a fresh cellId (deleted-then-
            // recreated via forceRemoveCell/placeCell) -- re-resolve it before trying the next
            // candidate so that attempt never targets a cellId this run already deleted.
            current = context.placedThisCohortRun().stream()
                .filter(p -> sameRow(p, saturdayPlacement) && p.dayOfWeek() == DayOfWeek.SATURDAY)
                .findFirst().orElse(null);
            if (current == null) {
                return; // couldn't even restore it -- already reported unplaced, nothing left to retry
            }
        }
    }

    /** One bump attempt: evicts both {@code saturdayPlacement} and {@code candidate}, then places
     *  {@code saturdayPlacement}'s row directly into {@code candidate}'s now-free weekday slot (a
     *  targeted attempt via {@link #tryRestoreAt}, not a fresh day-search — that slot is already
     *  known free). If that fails, both cells go straight back to exactly where they started and
     *  this candidate is abandoned. If it succeeds, {@code candidate} is either dropped for good (a
     *  Self-Study filler — deliberately extra capacity beyond its own curriculum quota, see {@link
     *  #fillSelfStudyGaps}'s own javadoc; losing one costs the term nothing) or given a genuine
     *  fresh day-search of its own via {@link #tryPlaceAndStaff} — never simply shoved onto the
     *  Saturday slot the other row just vacated, since that would only move the shortfall from one
     *  subject to another, not actually close it. A real curriculum row that can't be replaced
     *  anywhere (including back on Saturday) is reported unplaced rather than silently lost, exactly
     *  like {@link #attemptBacktrack}'s own restore-or-report path. */
    private boolean tryUpgradeSwap(Long cohortId, TermInstance term, List<Period> periods, CohortRunContext context,
                                    Placement saturdayPlacement, Placement candidate) {
        // Defensive: same "already gone from the database" guard as attemptBacktrack's own
        // forceRemoveCell call -- see that method's comment for why this can't be allowed to throw
        // uncaught here either. Abandons this candidate cleanly (caller moves on to the next one, or
        // gives up on this Saturday placement) rather than aborting the whole run.
        if (!forceRemoveCellIfPresent(candidate.cellId(), cohortId, candidate)) {
            context.placedThisCohortRun().remove(candidate);
            return false;
        }
        context.placedThisCohortRun().remove(candidate);
        context.dayLoad().merge(candidate.dayOfWeek(), -candidate.periodIds().size(), Integer::sum);
        if (!forceRemoveCellIfPresent(saturdayPlacement.cellId(), cohortId, saturdayPlacement)) {
            context.placedThisCohortRun().remove(saturdayPlacement);
            // candidate was genuinely removed above and must not just vanish -- put it back exactly
            // like the ordinary "swap didn't work out" failure path below does.
            restoreBumpedOrReportUnplaced(candidate, cohortId, context.placedThisCohortRun(), context.unplacedForCohort(), context.dayLoad(), term, periods);
            return false;
        }
        context.placedThisCohortRun().remove(saturdayPlacement);
        context.dayLoad().merge(saturdayPlacement.dayOfWeek(), -saturdayPlacement.periodIds().size(), Integer::sum);

        Optional<Placement> movedSaturdayRow = tryRestoreAt(saturdayPlacement, candidate.dayOfWeek(), candidate.periodIds(), cohortId);
        if (movedSaturdayRow.isEmpty()) {
            // The freed slot didn't actually work out for this row (its own faculty is busy there,
            // or something else raced in) -- put both original placements straight back and give up
            // on this candidate; the caller will try the next one, if any remain.
            restoreBumpedOrReportUnplaced(saturdayPlacement, cohortId, context.placedThisCohortRun(), context.unplacedForCohort(), context.dayLoad(), term, periods);
            restoreBumpedOrReportUnplaced(candidate, cohortId, context.placedThisCohortRun(), context.unplacedForCohort(), context.dayLoad(), term, periods);
            return false;
        }
        context.placedThisCohortRun().add(movedSaturdayRow.get());
        context.dayLoad().merge(candidate.dayOfWeek(), candidate.periodIds().size(), Integer::sum);

        if (isSelfStudySubject(candidate.subjectName())) {
            return true;
        }
        // Reuses restoreBumpedOrReportUnplaced's own exact-slot-then-full-search fallback rather
        // than duplicating it here: candidate's exact original slot is already occupied by
        // movedSaturdayRow, so that first attempt harmlessly fails fast before the full day search
        // runs, same as any other bumped placement.
        restoreBumpedOrReportUnplaced(candidate, cohortId, context.placedThisCohortRun(), context.unplacedForCohort(), context.dayLoad(), term, periods);
        return true; // the row this pass set out to help is placed either way
    }

    /** Same-row identity for two {@link Placement}s (as opposed to {@link Placement#sameRowAs},
     *  which compares against a {@link ShortfallRow} instead) — used by {@link #tryUpgradeSwap} to
     *  find a candidate's own other placements this run so its day-search retry doesn't land it
     *  twice on the same day. */
    private static boolean sameRow(Placement a, Placement b) {
        return Objects.equals(a.courseOfferingId(), b.courseOfferingId()) && a.sessionType() == b.sessionType()
            && Objects.equals(a.batchId(), b.batchId()) && Objects.equals(a.cohortSectionId(), b.cohortSectionId());
    }

    private static void tallyViolations(Map<String, Integer> failureTally, List<ConstraintViolation> violations) {
        for (ConstraintViolation violation : violations) {
            failureTally.merge(violation.code(), 1, Integer::sum);
        }
    }

    /** Picks the single most-frequent failure code across every attempted day/period combination
     *  and names it in plain language, with the count so an admin can judge how close/far this
     *  was from succeeding — one occupied slot reads very differently from every slot failing the
     *  same way. Falls back to the old generic message only when nothing was ever actually
     *  attempted (e.g. every day was already used by this row). */
    private static String summarizeFailures(Map<String, Integer> failureTally) {
        if (failureTally.isEmpty()) {
            return "no day/period found where both placement and staffing succeed";
        }
        Map.Entry<String, Integer> topReason = failureTally.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .orElseThrow();
        int totalAttempts = failureTally.values().stream().mapToInt(Integer::intValue).sum();
        return friendlyFailureReason(topReason.getKey())
            + " (" + topReason.getValue() + " of " + totalAttempts + " day/period combinations tried)";
    }

    private static String friendlyFailureReason(String violationCode) {
        return switch (violationCode) {
            case "STAFFING_WORKLOAD_DAILY_CAP_EXCEEDED" -> "the assigned faculty's daily workload cap was reached";
            case "STAFFING_WORKLOAD_WEEKLY_CAP_EXCEEDED" -> "the assigned faculty's weekly workload cap was reached";
            case "STAFFING_WORKLOAD_CONTINUOUS_CAP_EXCEEDED" -> "the assigned faculty's continuous-teaching cap was reached";
            case "STAFFING_FACULTY_CONFLICT" -> "the assigned faculty was already committed to another session at every remaining slot";
            case "STAFFING_FACULTY_UNAVAILABLE", "STAFFING_FACULTY_ABSENT" -> "the assigned faculty was marked unavailable or absent";
            case "STAFFING_ROOM_CONFLICT" -> "the committed venue was already booked at every remaining slot";
            case "STAFFING_VENUE_NOT_COMMITTED" -> "no committed venue exists for this session yet";
            case "SKELETON_CELL_COHORT_CLASH" -> "another mandatory session already occupies this audience's slot everywhere free";
            case "SKELETON_CELL_ALREADY_PLACED" -> "this subject already has a session at every remaining day/period";
            case "SKELETON_CELL_PERIOD_BLOCKED", "STAFFING_PERIOD_BLOCKED", "PERIOD_BLOCKED" ->
                "every remaining day/period is institutionally blocked (holiday, recurring lock, or Saturday not enabled for this term)";
            case "CLINICAL_SHIFT_BLOCKED" ->
                "every remaining day/period overlaps this cohort's active Clinical Shift window";
            case "PERIOD_NOT_CONTIGUOUS" ->
                "no remaining run of periods for this block size is free of a recess/lunch break in between";
            case "SKELETON_CELL_BUDGET_EXCEEDED" ->
                "this subject's curriculum-hours budget for this session type is already fully placed";
            default -> "a scheduling constraint (" + violationCode + ") blocked every remaining day/period";
        };
    }

    /** Mirrors {@code TimetableSkeletonAutoPlaceService#existingDaysForRow} — which days this exact
     *  budget row (subject/session-type/batch-or-section) already has a session on, so the shortfall
     *  loop never clusters two of that row's own sessions on the same day. Section equality is only
     *  required for a THEORY row (batchId null): a LAB/CLINICAL row's {@code cohortSectionId} now
     *  carries its batch's own section (populated by {@code TimetableSkeletonService#batchScopedBudgets}
     *  for faculty resolution), but the placed cell's own section is always null for LAB/CLINICAL
     *  (see {@code TimetableSkeletonService#checkAlreadyPlaced}) — batchId alone already uniquely
     *  identifies the row there, same as it does everywhere else in this class. */
    private Set<DayOfWeek> existingDaysForBudgetRow(List<SkeletonCellResponse> cells, Long courseOfferingId, SkeletonSubjectBudget budget) {
        Set<DayOfWeek> days = new HashSet<>();
        for (SkeletonCellResponse cell : cells) {
            // courseOfferingId() is null-safe from this side deliberately: a LIBRARY cell has no
            // CourseOffering at all (TimetableSkeletonService#toCellResponse), so it can never
            // match a real THEORY/LAB/CLINICAL row's courseOfferingId -- reversed so a Library cell
            // sitting in the skeleton snapshot compares false instead of NPE-ing here.
            if (courseOfferingId.equals(cell.courseOfferingId()) && cell.sessionType() == budget.sessionType()
                    && Objects.equals(cell.batchId(), budget.batchId())
                    && (budget.batchId() != null || Objects.equals(cell.cohortSectionId(), budget.cohortSectionId()))) {
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
                    "no faculty assigned on its Course Offering (elective)", member.getId()));
                return 0;
            }
        }

        List<Long> memberIds = members.stream().map(CourseOffering::getId).toList();
        List<ClassSchedule> existingGroupCells = classScheduleRepository
            .findByTermInstanceIdAndCourseOfferingIdIn(termInstanceId, memberIds).stream()
            .filter(cs -> Boolean.TRUE.equals(cs.getIsActive()))
            .toList();

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
                        + " — one or more new members can't join that exact slot", null));
                return 0;
            }
            return unplacedTheoryMembers.size();
        }

        for (DayOfWeek day : DayOfWeek.values()) {
            for (Period period : periods) {
                if (blockedPeriodChecker.blockReason(day, period.getStartTime(), period.getEndTime(), term).isPresent()) {
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
            "no day/period found where every member's bound faculty and a suitable room are all free", null));
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

    /** {@code cellIds} accumulates a member's id right after it's placed, before its own staffing
     *  attempt runs (see {@link #placeAndStaffElectiveMembers}) -- so by the time a later member's
     *  placement or staffing fails and this rollback runs, every earlier id in the list is already
     *  staffed, not a bare unstaffed draft. Must go through {@link
     *  TimetableSkeletonService#forceRemoveCell}, not the ordinary {@link
     *  TimetableSkeletonService#removeCell}, which rejects a staffed cell outright -- the same
     *  staffed-cell-undo need {@link #attemptBacktrack} has, just for a whole elective group instead
     *  of one row. Using the guarded {@code removeCell} here previously threw
     *  SKELETON_CELL_NOT_REMOVABLE uncaught, aborting the entire run the moment any elective group's
     *  second-or-later member failed after an earlier member had already placed and staffed. */
    private void rollbackElectiveCells(List<Long> cellIds) {
        for (Long id : cellIds) {
            timetableSkeletonService.forceRemoveCell(id);
        }
    }

    /** Elective member offerings don't loop per-cohort in this class -- a group's shared slot spans
     *  every enrolled cohort's students by design (see class javadoc), so there's no single
     *  cohortId in scope to resolve a per-cohort assignment against here. Delegates to {@link
     *  CourseOfferingSectionFacultyService#getForOffering} -- the same live-resolved view {@code
     *  checkPrerequisites} and the Assign Faculty screen already use for regular offerings -- rather
     *  than re-deriving faculty rows independently, so an elective can never pass the prerequisite
     *  checklist as staffed and then fail here at run time from a stale row the checklist already
     *  knows to ignore. Returns the shared faculty id if every resolved row agrees on exactly one
     *  (including the common case of just one row); returns null (treated as unassigned) if they
     *  disagree. Previously this queried {@link CourseOfferingSectionFaculty} rows directly and only
     *  excluded a row tied to a now-inactive {@link CohortSection} -- a stale whole-cohort (null
     *  section) row from before a cohort's section split was never excluded that way, since there is
     *  no section to check {@code isActive()} on, so it stayed permanently "disagreeing" with the
     *  newer section-scoped assignment and made the offering look unstaffed here even though {@code
     *  getForOffering} (which does know to prefer the active section once one exists) correctly
     *  reported it as fully staffed everywhere else. Package-private (not private) so this
     *  regression can be verified directly rather than only through the full placement pass. */
    Long resolveElectiveMemberFacultyId(CourseOffering member) {
        Set<Long> facultyIds = courseOfferingSectionFacultyService.getForOffering(member.getId()).sections().stream()
            .map(SectionFacultyAssignment::facultyId)
            .filter(Objects::nonNull)
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

    /** Package-private setter for test injection of the lazy-wired service. */
    void setCourseOfferingSectionFacultyService(CourseOfferingSectionFacultyService courseOfferingSectionFacultyService) {
        this.courseOfferingSectionFacultyService = courseOfferingSectionFacultyService;
    }
}
