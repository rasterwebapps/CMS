package com.cms.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.IdentityHashMap;
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
import com.cms.dto.ClinicalResidualItem;
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
import com.cms.dto.TimetableConflictRow;
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
import com.cms.model.enums.SubjectType;
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
 * <li><b>Phase 4 — Library (one session a week), Sports, a bonus second Library session when the
 *     week still has plenty of free periods, then the extra-hours gap-fill.</b>
 * </ol>
 *
 * <p><b>Saturday.</b> Every phase uses the same working days: Monday-Friday, plus Saturday as a
 * regular day whenever the term has chosen any working-Saturday pattern (see {@link
 * #saturdayIsWorkingDay}). There is no Saturday-specific fallback or mending pass.
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
    /** Not private -- {@link SubjectService#SYSTEM_MANAGED_CODES} references this and {@link
     *  #SPORTS_SUBJECT_CODE} directly rather than keeping its own copy of the two literals, same
     *  "one literal, not two copies" discipline as {@link #TIGHT_CAPACITY_THRESHOLD} above. */
    static final String LIBRARY_SUBJECT_CODE = "SYSTEM-LIBRARY";
    private static final String CONFIG_LIBRARY_SESSIONS_PER_WEEK = "timetable.library_sessions_per_week";
    private static final String CONFIG_LIBRARY_BLOCK_SIZE_PERIODS = "timetable.library_block_size_periods";
    private static final int DEFAULT_LIBRARY_SESSIONS_PER_WEEK = 1;
    private static final int DEFAULT_LIBRARY_BLOCK_SIZE_PERIODS = 2;
    private static final String CONFIG_LIBRARY_EXTRA_SESSION_MIN_FREE_PERIODS = "timetable.library_extra_session_min_free_periods";
    private static final int DEFAULT_LIBRARY_EXTRA_SESSION_MIN_FREE_PERIODS = 8;
    static final String SPORTS_SUBJECT_CODE = "SYSTEM-SPORTS";
    private static final String CONFIG_SPORTS_SESSIONS_PER_WEEK = "timetable.sports_sessions_per_week";
    private static final String CONFIG_SPORTS_BLOCK_SIZE_PERIODS = "timetable.sports_block_size_periods";
    private static final int DEFAULT_SPORTS_SESSIONS_PER_WEEK = 1;
    private static final int DEFAULT_SPORTS_BLOCK_SIZE_PERIODS = 2;
    /** User's rule: leftover periods go Library -> Sports -> genuine Self-Study (capped, here) ->
     *  only THEN the uncapped bonus-Theory filler ({@link #fillSelfStudyGaps}). Same config pattern
     *  as Library/Sports above -- see {@link #fillGenuineSelfStudyGaps}. */
    private static final String CONFIG_SELF_STUDY_SESSIONS_PER_WEEK = "timetable.self_study_sessions_per_week";
    private static final String CONFIG_SELF_STUDY_BLOCK_SIZE_PERIODS = "timetable.self_study_block_size_periods";
    private static final int DEFAULT_SELF_STUDY_SESSIONS_PER_WEEK = 1;
    private static final int DEFAULT_SELF_STUDY_BLOCK_SIZE_PERIODS = 2;

    /** Guards {@link #runGlobalAutoSchedule} against two overlapping runs on the same {@link
     *  com.cms.model.TermInstance} — a real production failure mode (a user double-clicking "Run"
     *  before the first request's spinner even shows, or two admins running it moments apart) since
     *  {@link AutoScheduleRunCache} is only a per-thread ThreadLocal and every actual DB write inside
     *  a run ({@code placeCell}/{@code staffCell}/{@code forceRemoveCell}, all {@code REQUIRES_NEW})
     *  commits and becomes visible to a concurrent run immediately, well before either request's own
     *  outer transaction finishes. Without this guard, run B's {@code purgeDraftCellsForRebuild} (or
     *  its own placements) can delete a {@link com.cms.model.ClassSchedule} row that run A already
     *  recorded in its in-memory {@code placedThisCohortRun}, so when run A's own {@link
     *  #attemptBacktrack} later tries to bump that row it 404s via {@link
     *  TimetableSkeletonService#forceRemoveCell} — surfaced in production as an uncaught {@code
     *  ResourceNotFoundException} ("Class schedule not found with id: ...") that aborts the whole
     *  run. A {@link java.util.concurrent.ConcurrentHashMap}-backed set keyed by termInstanceId is
     *  enough here: this only needs to reject a genuine overlap, not order or queue requests, and a
     *  run for one term must never block a run for a different term. */
    private static final Set<Long> ACTIVE_GLOBAL_AUTO_SCHEDULE_RUNS = java.util.concurrent.ConcurrentHashMap.newKeySet();

    private final TimetableSkeletonService timetableSkeletonService;
    private final TimetableStaffingService timetableStaffingService;
    private final TimetableClinicalShiftChecker clinicalShiftChecker;
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
    private final TimetableConflictInspectorService timetableConflictInspectorService;
    private final BatchService batchService;
    private final ClassScheduleCleanupService classScheduleCleanupService;

    // Field injection with @Lazy breaks the circular dependency:
    // TimetableGlobalAutoScheduleService -> CourseOfferingSectionFacultyService -> TimetableGlobalAutoScheduleService
    @Autowired
    @Lazy
    private CourseOfferingSectionFacultyService courseOfferingSectionFacultyService;

    public TimetableGlobalAutoScheduleService(TimetableSkeletonService timetableSkeletonService,
                                               TimetableStaffingService timetableStaffingService,
                                             TimetableClinicalShiftChecker clinicalShiftChecker,
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
                                               TimetableConflictInspectorService timetableConflictInspectorService,
                                               BatchService batchService,
                                               ClassScheduleCleanupService classScheduleCleanupService) {
        this.timetableSkeletonService = timetableSkeletonService;
        this.timetableStaffingService = timetableStaffingService;
        this.clinicalShiftChecker = clinicalShiftChecker;
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
        this.timetableConflictInspectorService = timetableConflictInspectorService;
        this.batchService = batchService;
        this.classScheduleCleanupService = classScheduleCleanupService;
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
        int suggestedMinDailySessions = 0;
        if (overCapacity) {
            suggestedMinDailyHours = Math.ceil(projectedTotal / demand.workingDaysInTerm());
            suggestedMinDailySessions = minDailySessionsFor(suggestedMinDailyHours);
            if (offering.getSubject() != null) {
                OverageContributor asContributor = new OverageContributor(offeringId, offering.getSubject().getName(),
                    cohortId, null, cohortHours, null, null, null, null, null);
                spreadLoad = buildSpreadLoadSuggestions(List.of(asContributor),
                    demand.demandByFaculty(), candidateFacultyId, demand.workingDaysInTerm(), demand.weeksInTerm());
            }
        }

        return new FacultyCapacityCheckResult(overCapacity, currentDemand, cohortHours, projectedTotal,
            capacity != null ? capacity.termCapacityHours() : 0, capacity != null ? capacity.dailyCapForDisplay() : 0,
            capacity != null ? capacity.tier() : "NONE", demand.workingDaysInTerm(), suggestedMinDailyHours,
            suggestedMinDailySessions, spreadLoad);
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
        return getEligibleFacultyForSection(offeringId, cohortSectionId, null);
    }

    /** {@code classScheduleId} non-null additionally reports, per candidate, whether staffing THAT
     *  session would be refused by the daily/weekly/continuous caps — see {@link
     *  EligibleFacultyCandidateDto#slotBlockedReason}. Omit it when picking faculty for a whole
     *  offering rather than one placed session. */
    @Transactional(readOnly = true)
    public List<EligibleFacultyCandidateDto> getEligibleFacultyForSection(Long offeringId, Long cohortSectionId, Long classScheduleId) {
        CourseOffering offering = courseOfferingRepository.findById(offeringId)
            .orElseThrow(() -> new ResourceNotFoundException("Course offering not found with id: " + offeringId));
        Subject subject = offering.getSubject();
        Long currentSectionFacultyId = currentSectionFacultyId(offering, cohortSectionId);
        Set<Long> grandfatherIds = currentSectionFacultyId != null ? Set.of(currentSectionFacultyId) : Set.of();
        List<Faculty> pool = eligiblePoolGrandfathering(subject, grandfatherIds);

        double sectionHours = safe(offering.getCurriculumSemesterCourse() != null
            ? offering.getCurriculumSemesterCourse().getTheoryHours() : null);
        TermDemandAggregation demand = computeTermDemand(offering.getTermInstance().getId());
        ClassSchedule targetCell = resolveTargetCell(classScheduleId);
        List<EligibleFacultyCandidateDto> candidates = new ArrayList<>();
        for (Faculty faculty : pool) {
            boolean alreadyHoldsSection = faculty.getId().equals(currentSectionFacultyId);
            candidates.add(candidateDto(subject, faculty, demand, alreadyHoldsSection, sectionHours, targetCell));
        }
        return sortMostFreeFirst(candidates);
    }

    /** Null id means "no specific session" and is the norm for the offering-level pickers; a
     *  non-null id that doesn't resolve is a caller error worth surfacing, not a silent null. */
    private ClassSchedule resolveTargetCell(Long classScheduleId) {
        return classScheduleId == null ? null
            : classScheduleRepository.findById(classScheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Class schedule not found with id: " + classScheduleId));
    }

    /** Cohort-scoped counterpart of {@link #getEligibleFacultyForSection} -- for a cohort with no
     *  active section split. Candidates are every faculty eligible for the offering's subject, plus
     *  whoever currently holds the whole-cohort row even if they've since fallen out of eligibility,
     *  same grandfathering rule as the section-scoped variant. Each candidate's projected load is
     *  computed against this cohort's *whole* theory+lab+clinical hours ({@link
     *  #checkFacultyCapacityForCohort}) rather than one section's theory hours. */
    @Transactional(readOnly = true)
    public List<EligibleFacultyCandidateDto> getEligibleFacultyForCohort(Long offeringId, Long cohortId) {
        return getEligibleFacultyForCohort(offeringId, cohortId, null);
    }

    /** See {@link #getEligibleFacultyForSection(Long, Long, Long)} for what {@code classScheduleId}
     *  adds. */
    @Transactional(readOnly = true)
    public List<EligibleFacultyCandidateDto> getEligibleFacultyForCohort(Long offeringId, Long cohortId, Long classScheduleId) {
        CourseOffering offering = courseOfferingRepository.findById(offeringId)
            .orElseThrow(() -> new ResourceNotFoundException("Course offering not found with id: " + offeringId));
        Subject subject = offering.getSubject();
        Long currentCohortFacultyId = currentCohortFacultyId(offering, cohortId);
        Set<Long> grandfatherIds = currentCohortFacultyId != null ? Set.of(currentCohortFacultyId) : Set.of();
        List<Faculty> pool = eligiblePoolGrandfathering(subject, grandfatherIds);

        double cohortHours = termHoursForOfferingInCohort(offering, cohortId, offering.getTermInstance().getId(), null).totalHours();
        TermDemandAggregation demand = computeTermDemand(offering.getTermInstance().getId());
        ClassSchedule targetCell = resolveTargetCell(classScheduleId);
        List<EligibleFacultyCandidateDto> candidates = new ArrayList<>();
        for (Faculty faculty : pool) {
            boolean alreadyHoldsCohort = faculty.getId().equals(currentCohortFacultyId);
            candidates.add(candidateDto(subject, faculty, demand, alreadyHoldsCohort, cohortHours, targetCell));
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
        int suggestedMinDailySessions = 0;
        if (overCapacity) {
            suggestedMinDailyHours = Math.ceil(projectedTotal / demand.workingDaysInTerm());
            suggestedMinDailySessions = minDailySessionsFor(suggestedMinDailyHours);
            if (offering.getSubject() != null) {
                OverageContributor asContributor = new OverageContributor(offeringId, offering.getSubject().getName(),
                    null, null, sectionHours, cohortSectionId, null, null, null, "THEORY");
                spreadLoad = buildSpreadLoadSuggestions(List.of(asContributor),
                    demand.demandByFaculty(), candidateFacultyId, demand.workingDaysInTerm(), demand.weeksInTerm());
            }
        }

        return new FacultyCapacityCheckResult(overCapacity, currentDemand, sectionHours, projectedTotal,
            capacity != null ? capacity.termCapacityHours() : 0, capacity != null ? capacity.dailyCapForDisplay() : 0,
            capacity != null ? capacity.tier() : "NONE", demand.workingDaysInTerm(), suggestedMinDailyHours,
            suggestedMinDailySessions, spreadLoad);
    }

    /** Converts an hours/day target into the session count an admin can actually type into the
     *  Raise Cap field (Faculty's {@code plannedDailySessionsOverride}) -- rounds up so the result
     *  genuinely clears the target rather than landing just short of it (see {@link
     *  FacultyCapacityCheckResult#suggestedMinDailySessions}'s own javadoc for why this exists).
     *  Public (not just this class's own two capacity-check callers) so {@link
     *  CourseOfferingSectionFacultyService#withExtraCommittedHours} can recompute a session count
     *  for a capacity result it adjusts after the fact, rather than duplicating this conversion. */
    public int minDailySessionsFor(double minDailyHours) {
        double avgPeriodHours = averagePeriodDurationHours(periodRepository.findByIsActiveTrueOrderByPeriodOrderAsc());
        return avgPeriodHours > 0 ? (int) Math.ceil(minDailyHours / avgPeriodHours) : 0;
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
        return candidateDto(subject, faculty, demand, alreadyHoldsSlot, slotHours, null);
    }

    /** {@code targetCell} non-null annotates each candidate with whether staffing THAT session
     *  would actually be refused — see {@link EligibleFacultyCandidateDto#slotBlockedReason}. */
    private EligibleFacultyCandidateDto candidateDto(Subject subject, Faculty faculty, TermDemandAggregation demand,
            boolean alreadyHoldsSlot, double slotHours, ClassSchedule targetCell) {
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
            alreadyHoldsSlot, currentDemand, capacityHours, tier, remaining, overCapacity,
            slotBlockedReason(faculty, targetCell));
    }

    /** Runs the save path's own daily/weekly/continuous cap check against one specific session, so
     *  the picker can say up front what the save would say. Null when no session was named, when it
     *  has no period to measure, or when nothing would be violated. */
    private String slotBlockedReason(Faculty faculty, ClassSchedule targetCell) {
        if (targetCell == null || targetCell.getPeriod() == null) {
            return null;
        }
        List<ConstraintViolation> violations = timetableStaffingService.checkWithinWorkloadCaps(
            faculty, targetCell, targetCell.getDayOfWeek(),
            targetCell.getPeriod().getStartTime(), targetCell.getPeriod().getEndTime());
        return violations.isEmpty() ? null : violations.get(0).message();
    }

    /** Uncapped candidates ({@code capacityTier == "NONE"}) sort first -- no configured limit reads
     *  as "most free" -- then the rest by descending remaining hours. */
    private static List<EligibleFacultyCandidateDto> sortMostFreeFirst(List<EligibleFacultyCandidateDto> candidates) {
        return candidates.stream()
            .sorted((a, b) -> {
                // Anyone the save would actually refuse for this session sinks below everyone it
                // wouldn't, whatever their term figure says. Without this the head of the list --
                // the value a picker naturally defaults to -- is chosen purely on term capacity and
                // can be someone who cannot take this slot at all. Null (no session named, or no
                // violation) sorts as assignable, so the offering-level pickers are unaffected.
                boolean aBlocked = a.slotBlockedReason() != null;
                boolean bBlocked = b.slotBlockedReason() != null;
                if (aBlocked != bBlocked) {
                    return aBlocked ? 1 : -1;
                }
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
        // Curriculum demand (the other side of every comparison this feeds) is inherently hours --
        // that's how a curriculum specifies Lab/Theory/Clinical requirements, before any block-size
        // scheduling decision exists yet to derive a session count from. The configured cap is now
        // sessions, so it's bridged into an hours-equivalent via each active Period's own real
        // duration -- the exact "periods carry a real minute figure, so sessions and hours are never
        // in conflict" design (2026-09-24), not a guessed/fabricated conversion factor.
        double avgPeriodHours = averagePeriodDurationHours(periodRepository.findByIsActiveTrueOrderByPeriodOrderAsc());
        Integer dailyOverrideOrDesignation = FacultyWorkloadCapacityService.resolveEffectiveDailyCapacity(faculty);
        if (dailyOverrideOrDesignation != null) {
            String tier = faculty.getPlannedDailySessionsOverride() != null ? "FACULTY_OVERRIDE" : "DESIGNATION_DEFAULT";
            double dailyHours = dailyOverrideOrDesignation * avgPeriodHours;
            return new CapacityResolution(dailyHours * workingDaysInTerm, dailyHours, tier);
        }
        Optional<Integer> globalDaily = timetableStaffingService.resolveDailyCap(faculty);
        if (globalDaily.isPresent()) {
            double dailyHours = globalDaily.get() * avgPeriodHours;
            return new CapacityResolution(dailyHours * workingDaysInTerm, dailyHours, "SYSTEM_CONFIGURATION");
        }

        Integer weeklyOverrideOrDesignation = FacultyWorkloadCapacityService.resolveEffectiveCapacity(faculty);
        if (weeklyOverrideOrDesignation != null) {
            String tier = faculty.getPlannedWeeklySessionsOverride() != null ? "FACULTY_OVERRIDE" : "DESIGNATION_DEFAULT";
            double termCapacity = weeklyOverrideOrDesignation * avgPeriodHours * weeksInTerm;
            return new CapacityResolution(termCapacity, termCapacity / workingDaysInTerm, tier);
        }
        Optional<Integer> globalWeekly = timetableStaffingService.resolveWeeklyCap(faculty);
        if (globalWeekly.isPresent()) {
            double termCapacity = globalWeekly.get() * avgPeriodHours * weeksInTerm;
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

    /** Backs the Skeleton Builder's pre-run confirmation for an All-Cohorts Global Auto-Schedule
     *  run: does this term already have ANY active DRAFT session placed, for any cohort? A
     *  single-cohort run answers the same question client-side from the grid already on screen
     *  (see {@code hasNoCells} on the frontend), but "All cohorts" mode never loads a grid, so there
     *  is nothing local to check against — this is the cheap, term-wide equivalent, checked BEFORE
     *  the existing prerequisite checklist even opens, so the admin sees the "this will overwrite
     *  what's already there" warning up front rather than buried among capacity/faculty checks. */
    @Transactional(readOnly = true)
    public boolean hasExistingDraftContent(Long termInstanceId) {
        return classScheduleRepository.existsByTermInstanceIdAndStatusAndIsActiveTrue(
            termInstanceId, ClassScheduleStatus.DRAFT);
    }

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
                faculty.getPlannedDailySessionsOverride(), configured,
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
     *  below it. {@code cohortId} null runs every cohort enrolled in the term, minus whichever ones
     *  already have their own timetable approved/{@code PUBLISHED} on Draft Review (checked per
     *  cohort — see {@link #isCohortTimetablePublished}); non-null scopes the run to just that
     *  cohort's shortfall, hard-blocked entirely once that cohort's own timetable is published. */
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

        // Once a cohort's own timetable is approved/PUBLISHED on Draft Review, that's the line past
        // which only manual period/staff edits (swap staff, swap sessions) are allowed for THAT
        // cohort -- never a full automated re-run, at any cost. Room allocation being committed in
        // Capacity Planner is a prerequisite for placement, not a "stop touching it" signal, so it
        // plays no part in this gate. An explicit single-cohort request against its own published
        // timetable is a hard block (the frontend also disables the action before this is ever
        // reached, but this defensive re-check can't be bypassed via a direct API call, same
        // philosophy as the capacity/venue prechecks above). An "All Cohorts" run instead reports
        // each already-published cohort back by name as skipped and proceeds with the rest -- OC-260
        // made Approve itself cohort-scoped (see TimetableGenerationService#approve's cohortIds
        // param), so one cohort in a term being published no longer implies every cohort sharing that
        // termInstanceId is. Checking per cohort here (instead of a single termInstanceId-wide
        // existsByTermInstanceIdAndStatus) is what makes that true -- see
        // isCohortTimetablePublished's own doc.
        List<SkippedPublishedCohort> skippedPublishedCohorts = new ArrayList<>();
        if (cohortId != null) {
            if (isCohortTimetablePublished(termInstanceId, cohortId)) {
                Cohort thisCohort = cohortRepository.findById(cohortId).orElse(null);
                throw new TimetableConstraintViolationException(List.of(new ConstraintViolation(
                    "GLOBAL_AUTO_SCHEDULE_TERM_PUBLISHED",
                    (thisCohort != null ? thisCohort.getDisplayName() : "This cohort")
                        + " — this cohort's timetable is already approved; only manual period/staff edits are allowed now")));
            }
        } else {
            Set<Long> publishedCohortIds = cohortIds.stream()
                .filter(id -> isCohortTimetablePublished(termInstanceId, id))
                .collect(Collectors.toSet());
            for (Long id : publishedCohortIds) {
                Cohort thisCohort = cohortRepository.findById(id).orElse(null);
                skippedPublishedCohorts.add(new SkippedPublishedCohort(id,
                    thisCohort != null ? thisCohort.getDisplayName() : ("Cohort " + id)));
            }
            cohortIds = cohortIds.stream().filter(id -> !publishedCohortIds.contains(id)).collect(Collectors.toSet());
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
        // Which cohorts each elective group actually draws students from. A group's one shared slot
        // has to be free of Clinical Shift duty for EVERY one of them, and duty windows are
        // cohort-scoped, so Phase 3 can't do that check without knowing who they are. Collected
        // here rather than re-derived later because this loop is the only place the two are in
        // scope together.
        Map<Long, Set<Long>> electiveGroupCohorts = new LinkedHashMap<>();

        List<ClinicalResidualItem> clinicalResiduals = new ArrayList<>();
        Set<Long> clinicalResidualOfferingIds = new HashSet<>();
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
            Set<Long> flaggedUnselectedGroups = new HashSet<>();
            for (SkeletonSubjectResponse subject : skeleton.subjects()) {
                CourseOffering offering = courseOfferingRepository.findById(subject.courseOfferingId()).orElse(null);
                if (offering == null) {
                    continue;
                }
                // Management-selected elective (OC-227): the chosen option is an ordinary Theory
                // subject for the whole cohort -- placed below like any other row, in the section's
                // own classroom -- and every other option in the group is simply not scheduled. No
                // shared slot, no room per option (Elective II's 9 options once needed 9 rooms at
                // once in a college with 5, for 60 students who were all in one option).
                boolean commonElective = TimetableSkeletonService.isCommonCohortElective(offering);
                if (commonElective && !isSelectedElectiveOption(offering)) {
                    if (subject.electiveGroupId() != null && flaggedUnselectedGroups.add(subject.electiveGroupId())
                            && !electiveGroupHasSelection(offering)) {
                        unplacedForCohort.add(new AutoPlaceUnplacedItem(
                            subject.electiveGroupName() != null ? subject.electiveGroupName() : "Elective group " + subject.electiveGroupId(),
                            ClassSessionType.THEORY, null,
                            "this elective group is management-selected but no option has been assigned to the cohort yet "
                                + "— bulk-assign the chosen elective on Elective Assignment, then run automation again", null, false, false));
                    }
                    continue;
                }
                if (!commonElective && timetableSkeletonService.isElectiveOffering(offering)) {
                    if (subject.electiveGroupId() != null) {
                        electiveGroupCohorts
                            .computeIfAbsent(subject.electiveGroupId(), k -> new LinkedHashSet<>())
                            .add(id);
                    }
                    continue;
                }
                for (SkeletonSubjectBudget budget : subject.budgets()) {
                    // Checked BEFORE the shortfall test (OC-227): TimetableSkeletonService#batchScopedBudgets
                    // already zeroes `required` for exactly this shape -- a small shift-credited
                    // leftover one weekly row would overshoot -- so gating it on shortfall > 0 made the
                    // closer unreachable for real data (Adult Health Nursing I's 12h never reached the
                    // report, and neither did any duty-length fit).
                    if (budget.sessionType() == ClassSessionType.CLINICAL) {
                        ClinicalResidualItem residual = clinicalResidualFor(subject.subjectName(), budget, offering, cohort, periods);
                        if (residual != null) {
                            if (clinicalResidualOfferingIds.add(offering.getId())) {
                                clinicalResiduals.add(residual); // one per offering, not one per batch row
                            }
                            continue; // the weekly grid can only overshoot this — don't ask it to try
                        }
                    }
                    // Planned against the term's total hours (2026-09-15): what's owed is the session
                    // occurrences the curriculum hours still need, whatever working days the placed
                    // sessions sit on -- see SkeletonSubjectBudget#remainingTermRuns.
                    int remainingRuns = budget.remainingTermRuns();
                    if (remainingRuns <= 0) {
                        continue;
                    }
                    Long facultyId = resolveBudgetFacultyId(offering, budget, id);
                    if (facultyId == null && commonElective) {
                        // Elective faculty is assigned cohort-wide (no section row), so a section's
                        // budget row finds nothing section-level -- the cohort-wide row is the answer.
                        facultyId = currentCohortFacultyId(offering, id);
                    }
                    if (facultyId == null) {
                        unplacedForCohort.add(new AutoPlaceUnplacedItem(subject.subjectName(), budget.sessionType(),
                            occupantLabel(budget), "no faculty assigned on its Course Offering", subject.courseOfferingId(), false, false));
                        continue;
                    }
                    List<EligibleFacultyCandidateDto> fallbackCandidates = rankedFallbackCandidates(offering, facultyId, termDemand);
                    List<Long> candidateFacultyIds = new ArrayList<>();
                    candidateFacultyIds.add(facultyId);
                    candidateFacultyIds.addAll(fallbackCandidates.stream().map(EligibleFacultyCandidateDto::facultyId).toList());
                    ShortfallRow row = new ShortfallRow(subject.subjectName(), offering, budget, facultyId,
                        candidateFacultyIds, fallbackCandidates.stream()
                            .collect(Collectors.toMap(EligibleFacultyCandidateDto::facultyId, c -> c, (a, b) -> a)),
                        remainingRuns, CurriculumHoursCalculator.resolveBlockSize(offering.getSubject(), budget.sessionType()));
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
                dayLoad, new ArrayList<>(), siblingDaysByOfferingAndType, new LinkedHashMap<>());
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
            // Rules 3/4/5 (user's hierarchy) -- runs over the SAME, now Phase-B-reduced queue, right
            // after it for the identical reason (claims rows before the main sort/consume loop below
            // can independently re-place them). See #attemptSingleOfferingBatchRotation's javadoc.
            rotationGroupsCreated += attemptSingleOfferingBatchRotation(context, term, periods, globalLabClinicalQueue, termDemand);
        }

        // Sibling batches (same cohort, same offering, same LAB/CLINICAL session type -- a section
        // split across parallel batches sharing one scarce venue) are grouped and interleaved one
        // session-placement at a time via #placeShortfallRowGroupInterleaved instead of each row
        // being fully drained before the next starts -- see that method's javadoc for why this turns
        // the old "first batch grabs every early day" clustering into a day-by-day alternation.
        // Every other row (the ordinary, unsplit case) keeps its original single-row #placeShortfallRow
        // path, unchanged, so this only affects offerings that actually split into sibling batches.
        globalLabClinicalQueue.sort(Comparator.comparingInt((TaggedShortfallRow t) -> -t.row().shortfall()));
        Set<TaggedShortfallRow> consumedShortfallRows = Collections.newSetFromMap(new IdentityHashMap<>());
        for (TaggedShortfallRow tagged : globalLabClinicalQueue) {
            if (consumedShortfallRows.contains(tagged)) {
                continue;
            }
            List<TaggedShortfallRow> siblingGroup = siblingBatchGroup(tagged, globalLabClinicalQueue, consumedShortfallRows);
            consumedShortfallRows.addAll(siblingGroup);
            if (siblingGroup.size() > 1) {
                placeShortfallRowGroupInterleaved(siblingGroup, term, periods, contextsById, periodDurationHours,
                    venueGaps, facultySubstitutionEvents, termDemand);
            } else {
                CohortRunContext context = contextsById.get(tagged.cohortId());
                placeShortfallRow(tagged.cohortId(), tagged.row(), term, periods, context, periodDurationHours, venueGaps, facultySubstitutionEvents, termDemand);
            }
        }

        // Phase 1.5: a cohort section split into batches for LAB/CLINICAL (one lab/venue can't hold
        // everyone at once) leaves every sibling batch NOT currently occupying that lab completely
        // unscheduled at the exact slot where one batch has its turn — Theory can't cover for them
        // (it's whole-section, never split), so those periods were dead time. Must run here, before
        // Phase 2 (THEORY): it needs the exact (day, period) each LAB/CLINICAL cell just landed on,
        // known only right after Phase 1 places it. Cannot take a slot Phase 2 would have legitimately
        // needed — that slot is already hard-blocked for THEORY at this cohort/section regardless.
        for (CohortRunContext context : contexts) {
            fillIdleBatchGaps(context.cohortId(), context.skeleton(), term, periods, context.dayLoad(),
                saturdayIsWorkingDay(term), context.placedThisCohortRun(), context.unplacedForCohort(), termDemand);
        }

        // Phase 2: THEORY, per cohort -- never contends for a cross-cohort resource (each active
        // CohortSection has its own exclusive committed classroom), so cohort order here is
        // irrelevant the way it's load-bearing in Phase 1.
        for (CohortRunContext context : contexts) {
            for (ShortfallRow row : context.theoryRows()) {
                int stillOwedRuns = placeShortfallRow(context.cohortId(), row, term, periods, context, periodDurationHours, venueGaps, facultySubstitutionEvents, termDemand);
                if (stillOwedRuns > 0) {
                    // Feeds fillSelfStudyGaps below (same run, later phase): a row that's still short
                    // of its own curriculum requirement after every normal/backtrack/double-session
                    // attempt gets first claim on any genuinely free period found there, ahead of
                    // padding an already-met subject with a bonus "extra" session -- user's call,
                    // 2026-09-17 (flips the prior 2026-09-10 "fill them equally" default): every
                    // offering's real required hours come first, THEN whatever's left over splits
                    // equally among Library/Self-Study/Sports and already-met subjects.
                    context.theoryStillOwedRuns().merge(
                        theoryRowKey(row.offering().getId(), row.budget().cohortSectionId()), stillOwedRuns, Integer::sum);
                }
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
        for (Map.Entry<Long, Set<Long>> entry : electiveGroupCohorts.entrySet()) {
            Long electiveGroupId = entry.getKey();
            int placed = placeAndStaffElectiveGroup(termInstanceId, electiveGroupId, term, periods, electiveUnplaced,
                entry.getValue());
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
        Map<Long, LibraryGapFillOutcome> libraryOutcomes = new LinkedHashMap<>();
        for (CohortRunContext context : libraryFillOrder) {
            LibraryGapFillOutcome libraryOutcome = fillLibraryGaps(context.cohortId(), term, periods,
                context.dayLoad(), context.unplacedForCohort(), context.skeleton().cells(), context.placedThisCohortRun(),
                saturdayIsWorkingDay(term), context.theoryStillOwedRuns());
            context.placedThisCohortRun().addAll(libraryOutcome.filled());
            libraryOutcomes.put(context.cohortId(), libraryOutcome);
        }
        // Sports right after Library, in the same most-constrained-first order: every cohort
        // competes for the same Sports venue and the same few PE faculty, exactly as for the Library
        // room. Both quotas are placed before the leftover extra-hours filler below claims the rest.
        Map<Long, SportsGapFillOutcome> sportsOutcomes = new LinkedHashMap<>();
        for (CohortRunContext context : libraryFillOrder) {
            SportsGapFillOutcome sportsOutcome = fillSportsGaps(context.cohortId(), term, periods, context.dayLoad(),
                context.skeleton().cells(), termDemand, saturdayIsWorkingDay(term), context.theoryStillOwedRuns());
            context.placedThisCohortRun().addAll(sportsOutcome.filled());
            sportsOutcomes.put(context.cohortId(), sportsOutcome);
        }
        // Library is one 2-period session a week; a second one is a bonus, only where curriculum,
        // Library and Sports still left the week plenty of free periods (user's call, 2026-09-15:
        // "if there are a lot of free periods, we can give another library session"). Same
        // most-constrained-first order, and before the extra-hours filler claims the rest.
        //
        // 2026-09-18 fix: this bonus session used to run unconditionally here, BEFORE
        // fillSelfStudyGaps below ever gets a look at these same free periods -- so on a cohort that
        // still had real Theory shortfall (context.theoryStillOwedRuns() > 0 for some row), a bonus
        // Library session could claim a (day, period) slot that fillSelfStudyGaps's own
        // shortfall-first tier would otherwise have handed to that short subject. Library's own
        // required quota (fillLibraryGaps, just above) and Sports' required quota are real curriculum
        // content and rightly compete for slots ahead of anything -- but this SECOND, non-required
        // Library session is pure filler exactly like the already-met subjects' bonus sessions in
        // fillSelfStudyGaps, and must yield to a genuine Theory shortfall the exact same way those do.
        // Real seed data confirmed this: a BSc Nursing cohort with 475h of term-wide spare capacity
        // still reported 38.3h of Theory unplaced while showing +23.3h of bonus Theory hours handed to
        // already-met subjects -- some of that gap traced to this bonus Library session claiming slots
        // ahead of the shortfall-aware pass, not to a genuine faculty/room ceiling.
        for (CohortRunContext context : libraryFillOrder) {
            boolean cohortStillOwesTheory = context.theoryStillOwedRuns().values().stream().anyMatch(owed -> owed > 0);
            if (cohortStillOwesTheory) {
                continue;
            }
            context.placedThisCohortRun().addAll(fillExtraLibrarySession(context.cohortId(), term, periods,
                context.dayLoad(), context.skeleton().cells(), context.placedThisCohortRun(), saturdayIsWorkingDay(term)));
        }

        // Genuine, capped Self-Study (user's rule: Library -> Sports -> Self-Study(capped) -> only
        // THEN the uncapped bonus-Theory filler below). Same most-constrained-cohort-first order as
        // Library/Sports, same theoryStillOwedRuns gate -- see #fillGenuineSelfStudyGaps.
        for (CohortRunContext context : libraryFillOrder) {
            fillGenuineSelfStudyGaps(context.cohortId(), term, periods, context.dayLoad(), context.skeleton(),
                context.skeleton().cells(), context.placedThisCohortRun(), saturdayIsWorkingDay(term),
                context.theoryStillOwedRuns(), termDemand);
        }

        for (CohortRunContext context : contexts) {
            // Self-Study fills whatever Library didn't claim -- both stay greedy filler, LAST of all
            // four phases (see the class javadoc's "Placement order" section for why), just no longer
            // sharing Library's own cross-cohort ordering concern (Self-Study never contends for a
            // shared resource the way the one Library room does).
            SelfStudyGapFillOutcome gapFillOutcome = fillSelfStudyGaps(context.cohortId(), context.skeleton(), term,
                periods, context.dayLoad(), context.unplacedForCohort(), termDemand, saturdayIsWorkingDay(term),
                context.theoryStillOwedRuns());
            context.placedThisCohortRun().addAll(gapFillOutcome.filled());
            totalUnfillableSelfStudyPeriods += gapFillOutcome.unfillablePeriods();

            int placedForCohort = context.placedThisCohortRun().size();
            boolean usedSaturdayForCohort = context.placedThisCohortRun().stream().anyMatch(p -> p.dayOfWeek() == DayOfWeek.SATURDAY);
            totalPlaced += placedForCohort;
            totalStaffed += placedForCohort;
            summaries.add(new CohortPlacementSummary(context.cohortId(), context.cohort().getDisplayName(),
                placedForCohort, placedForCohort,
                reconcileUnplacedAgainstFinalPlacements(context.unplacedForCohort(), context.skeleton().subjects(),
                    context.placedThisCohortRun(), term),
                usedSaturdayForCohort,
                java.util.stream.Stream.concat(libraryInfoNotes(libraryOutcomes.get(context.cohortId())).stream(),
                    sportsInfoNotes(sportsOutcomes.get(context.cohortId())).stream()).toList()));
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

        // Term-wide, not scoped to just the cohorts this run touched: a pinned cell this run left
        // standing (see purgeDraftCellsForRebuild's javadoc) is never re-placed or re-validated by
        // the placement passes above, so a conflict it now has with something this run DID place --
        // or with another cohort's own pre-existing cell -- would otherwise go completely unnoticed
        // until someone happened to open Conflict Inspector separately. Reuses the exact same
        // detection Conflict Inspector already runs (TimetableConflictInspectorService#scanTerm) --
        // flag-only for now, never auto-resolved: a pinned cell was pinned on purpose, so silently
        // moving or removing it here would be a far worse surprise than leaving a visible conflict
        // for a human to actually decide how to resolve.
        List<TimetableConflictRow> postRunConflicts = timetableConflictInspectorService.scanTerm(termInstanceId).rows();

        return new GlobalAutoScheduleResult(totalPlaced, totalStaffed, summaries, electiveUnplaced, staleDraftsCleared,
            purge.pinnedPreserved(), capacityCausedGapHours, recommendedAdditionalFacultyCount, venueCapacityGaps, skippedPublishedCohorts,
            rotationGroupsCreated, pairingSkipReasons, buildFacultySubstitutionTips(facultySubstitutionEvents),
            clinicalResiduals, postRunConflicts);
    }

    /** Subject labels {@link #fillSelfStudyGaps} uses for its own period-level notes ("N period(s)
     *  left empty"). Those items carry a courseOfferingId only so the report can deep-link
     *  somewhere — they describe empty periods, not one subject's weekly quota — so {@link
     *  #reconcileUnplacedAgainstFinalPlacements} must never treat them as budget shortfalls. */
    private static final String SELF_STUDY_ITEM_LABEL = "Self-Study/Co-curricular";
    private static final String GAP_FILL_ITEM_LABEL = "Gap-fill";

    /**
     * The run's unplaced log, trimmed to what the FINISHED grid still genuinely owes. {@code
     * unplacedForCohort} is append-only: every failure is logged the moment it happens and nothing
     * ever takes an entry back out — so a session {@link #attemptBacktrack} bumped and reported as
     * "displaced", then covered a phase later by a fresh placement or by {@link #fillSelfStudyGaps}'
     * extra sessions, still showed as unplaced. A real 2026-2027 ODD run (2026-09-11) reported
     * dozens of Theory sessions unplaced across four cohorts while every one of those subjects sat at
     * or above its weekly quota in the grid — the report, not the grid, was wrong.
     *
     * <p>Only subject-budget items are reconciled: ones with a courseOfferingId whose subject,
     * session type and occupant match one of this cohort's budget rows. Each such budget keeps at
     * most as many items as it is still short by — its pre-run (pinned) sessions plus everything
     * this run placed against it, each counted for the runs its day really has (a multi-period
     * block is one session; a first-Saturday-only session runs 6 times, not 26). What's still owed
     * is expressed in whole weekly sessions, the unit each logged item stands for. Library,
     * idle-batch, gap-fill notes and anything that can't be matched to a budget are kept
     * untouched: over-reporting is the safe direction, hiding a genuine gap is not. Package-private
     * so it can be verified directly.
     */
    static List<AutoPlaceUnplacedItem> reconcileUnplacedAgainstFinalPlacements(List<AutoPlaceUnplacedItem> logged,
                                                                              List<SkeletonSubjectResponse> subjects,
                                                                              List<Placement> placedThisRun,
                                                                              TermInstance term) {
        Map<String, Integer> remainingByItemKey = new LinkedHashMap<>();
        for (SkeletonSubjectResponse subject : subjects) {
            for (SkeletonSubjectBudget budget : subject.budgets()) {
                int deliveredThisRun = placedThisRun.stream()
                    .filter(p -> Objects.equals(p.courseOfferingId(), subject.courseOfferingId())
                        && p.sessionType() == budget.sessionType()
                        && Objects.equals(p.batchId(), budget.batchId())
                        && Objects.equals(p.cohortSectionId(), budget.cohortSectionId()))
                    .mapToInt(p -> runsFor(p.dayOfWeek(), term, budget))
                    .sum();
                int remainingRuns = Math.max(0, budget.remainingTermRuns() - deliveredThisRun);
                int remaining = (int) Math.ceil(remainingRuns / (double) weekRuns(budget));
                // Summed, not overwritten: two budget rows can share one item key (e.g. two
                // placeholder batches with no name), and an item for either must survive while
                // either is still short.
                remainingByItemKey.merge(unplacedItemKey(subject.courseOfferingId(), subject.subjectName(),
                    budget.sessionType(), occupantLabel(budget)), remaining, Integer::sum);
            }
        }

        List<AutoPlaceUnplacedItem> reconciled = new ArrayList<>();
        for (AutoPlaceUnplacedItem item : logged) {
            boolean budgetItem = item.courseOfferingId() != null
                && !SELF_STUDY_ITEM_LABEL.equals(item.subjectName())
                && !GAP_FILL_ITEM_LABEL.equals(item.subjectName());
            String key = unplacedItemKey(item.courseOfferingId(), item.subjectName(), item.sessionType(), item.occupantLabel());
            Integer remaining = budgetItem ? remainingByItemKey.get(key) : null;
            if (remaining == null) {
                reconciled.add(item);
            } else if (remaining > 0) {
                reconciled.add(item);
                remainingByItemKey.put(key, remaining - 1);
            }
        }
        return reconciled;
    }

    private static String unplacedItemKey(Long courseOfferingId, String subjectName, ClassSessionType sessionType,
                                          String occupantLabel) {
        return courseOfferingId + "|" + subjectName + "|" + sessionType + "|" + occupantLabel;
    }

    /** A term's chosen working Saturdays are regular working days (user's call, 2026-09-15). Once an
     *  admin opts into any Saturday pattern — 1st, 2nd, any nth, or all of them — every placement
     *  pass treats Saturday exactly like Monday-Friday, least-loaded-first, so sessions spread
     *  evenly across all six days and a chosen Saturday is never left empty. With no pattern chosen
     *  (or a pattern no Saturday in the term matches), Saturday isn't a working day and nothing goes
     *  there. Hours are planned against the term's total (26 × 5 × 8 weekday periods plus 6 × 8 on
     *  first Saturdays, say): a Saturday session counts for the runs it really has ({@link
     *  #runsFor}), so a subject with one keeps getting placed until its curriculum hours are met. */
    static boolean saturdayIsWorkingDay(TermInstance term) {
        return !term.getWorkingSaturdayWeeks().isEmpty() && WorkingSaturdayCalculator.workingSaturdayCount(term) > 0;
    }

    /** Runs a session on {@code day} adds to {@code budget} across the term: every week on
     *  Monday-Friday, only the chosen working Saturdays on Saturday (see {@link
     *  WorkingSaturdayCalculator#runsInTerm}), measured in the budget's own weeks. Never below 1, so
     *  every placement makes progress. */
    private static int runsFor(DayOfWeek day, TermInstance term, SkeletonSubjectBudget budget) {
        int weeks = weekRuns(budget);
        return day == DayOfWeek.SATURDAY ? Math.max(1, WorkingSaturdayCalculator.runsInTerm(day, term, weeks)) : weeks;
    }

    /** Runs one weekly session on a weekday adds to {@code budget} — its term's weeks. */
    private static int weekRuns(SkeletonSubjectBudget budget) {
        return Math.max(1, budget.weeksInTerm());
    }

    /** Monday-Friday, plus Saturday when {@link #saturdayIsWorkingDay}. */
    private static List<DayOfWeek> workingDays(TermInstance term) {
        return Arrays.stream(DayOfWeek.values())
            .filter(d -> d != DayOfWeek.SATURDAY || saturdayIsWorkingDay(term))
            .toList();
    }

    /** The neutral "Library shrank to fit" note (OC-227: Library yields to curriculum, so this is
     *  information, not an unplaced warning). Empty when Library got its full quota or no Library
     *  classroom exists at all (that case is already a real warning). */
    private static List<String> libraryInfoNotes(LibraryGapFillOutcome outcome) {
        if (outcome == null || outcome.unfillableSessions() <= 0 || outcome.targetSessions() <= 0) {
            return List.of();
        }
        int placed = outcome.targetSessions() - outcome.unfillableSessions();
        return List.of("Library reduced to " + placed + " of " + outcome.targetSessions() + " weekly session(s) — "
            + "once curriculum was placed, no other day had enough consecutive free periods with a free Library room");
    }

    /** Sports' counterpart of {@link #libraryInfoNotes}: always a neutral note, never an unplaced
     *  warning -- Sports yields to curriculum exactly like Library, and a missing Sports room or PE
     *  faculty is a setup gap the note names so the admin knows what to add. */
    private static List<String> sportsInfoNotes(SportsGapFillOutcome outcome) {
        if (outcome == null || outcome.unfillableSessions() <= 0 || outcome.targetSessions() <= 0) {
            return List.of();
        }
        if (outcome.setupGap() != null) {
            return List.of("Sports not placed — " + outcome.setupGap());
        }
        int placed = outcome.targetSessions() - outcome.unfillableSessions();
        return List.of("Sports reduced to " + placed + " of " + outcome.targetSessions() + " weekly session(s) — "
            + "once curriculum and Library were placed, no day had a free block with a free Sports room and a free PE faculty");
    }

    /** For a management-selected elective group, the option the cohort's students are bulk-assigned
     *  to on Elective Assignment IS management's selection (OC-227) — no separate field. */
    private boolean isSelectedElectiveOption(CourseOffering offering) {
        return courseRegistrationRepository.countByCourseOfferingIdAndStatus(offering.getId(), RegistrationStatus.REGISTERED) > 0;
    }

    private boolean electiveGroupHasSelection(CourseOffering member) {
        Long groupId = member.getCurriculumSemesterCourse().getElectiveGroup().getId();
        return courseOfferingRepository
            .findByTermInstanceIdAndCurriculumSemesterCourse_ElectiveGroupId(member.getTermInstance().getId(), groupId)
            .stream().anyMatch(this::isSelectedElectiveOption);
    }

    /**
     * Stage B residual closer. Returns a {@link ClinicalResidualItem} when this CLINICAL budget row
     * is a leftover the weekly grid can only OVERSHOOT, and null in every ordinary case (which is
     * most of them) so the row goes on to be placed exactly as before.
     *
     * <p>The shape it catches: a shift-configured offering whose duty roster already delivers
     * nearly all of its curriculum Clinical hours. A duty group runs one occurrence per week, so
     * three duty days over a 26-week term give 78 occurrences while a 480h subject at a 6h shift
     * needs 80 — a 12h residual. {@code creditClinicalShiftHours} correctly hands that 12h to the
     * grid, and {@code sessionsPerWeek} then rounds it up to one weekly session, because one per
     * week is the smallest cadence a grid row can express. That row delivers ~3.33h &times; 26
     * weeks &asymp; 86.7h against a 12h need: roughly 75 hours of clinical placement nobody asked
     * for, permanently occupying a slot some other subject needs. Placing it is worse than not
     * placing it, and leaving the row to fail placement is worse still — it reports as an ordinary
     * "couldn't find a slot", which sends the admin hunting for capacity that would not help.
     *
     * <p>So the run declines the row and says what actually closes it: N extra duty days on a
     * date-bounded {@code ClinicalShiftGroup}. That is a real, existing mechanism, not a proposal
     * for new modelling — see {@link ClinicalResidualItem} for why it is proposed rather than
     * created here.
     *
     * <p>Guarded narrowly on purpose. It only fires for CLINICAL, only for an offering that
     * actually has an active shift group (without one there is no duty roster and the grid IS the
     * delivery mechanism, however coarse), and only when one weekly session would genuinely
     * overshoot the residual — a subject legitimately short by several sessions a week still goes
     * to the grid untouched.
     */
    private ClinicalResidualItem clinicalResidualFor(String subjectName, SkeletonSubjectBudget budget,
                                                       CourseOffering offering, Cohort cohort, List<Period> periods) {
        if (budget.sessionType() != ClassSessionType.CLINICAL || budget.totalHours() <= 0) {
            return null;
        }
        Integer durationMinutes = offering.getClinicalShiftDurationMinutes();
        if (durationMinutes == null || durationMinutes <= 0) {
            return null;
        }
        ShiftRoster roster = shiftRosterFor(offering);
        if (roster.starts().isEmpty()) {
            return null;
        }

        // What ONE weekly grid row would actually deliver over the whole term, in clock hours --
        // the same session-length arithmetic CurriculumHoursCalculator uses, not an assumed hour.
        int blockSize = CurriculumHoursCalculator.resolveBlockSize(offering.getSubject(), ClassSessionType.CLINICAL);
        double slotMinutes = CurriculumHoursCalculator.averageDurationMinutes(periods.stream()
            .map(p -> (int) java.time.Duration.between(p.getStartTime(), p.getEndTime()).toMinutes())
            .toList());
        double oneWeeklyRowDeliversHours = (slotMinutes * blockSize / 60.0) * budget.weeksInTerm();
        // What the roster actually leaves owed, credited exactly as TimetableSkeletonService does
        // (#effectiveWeeksFor) -- NOT budget.totalHours(), which is the raw curriculum figure kept for
        // display (480h, not the 12h genuinely still owed).
        double residualHours = clinicalShiftResidualHours(offering, durationMinutes, roster);
        if (residualHours <= 0.001 || oneWeeklyRowDeliversHours <= residualHours) {
            return null; // the grid can deliver this without overshooting -- place it normally
        }

        double hoursPerDutyDay = durationMinutes / 60.0;
        int extraDutyDays = (int) Math.ceil(residualHours / hoursPerDutyDay);
        String remedy = "Add a Clinical Shift group bounded to " + extraDutyDays + " week(s) "
            + "(Effective From/To on the group) to deliver " + extraDutyDays + " more duty day(s) of "
            + trimHours(hoursPerDutyDay) + "h. A weekly grid row can't express this: the smallest one "
            + "would deliver about " + trimHours(oneWeeklyRowDeliversHours) + "h over the term against "
            + trimHours(residualHours) + "h owed.";
        DutyFit fit = dutyFitFor(offering, periods);
        return new ClinicalResidualItem(offering.getId(), subjectName,
            cohort != null ? cohort.getDisplayName() : null,
            residualHours, hoursPerDutyDay, extraDutyDays, remedy,
            durationMinutes, fit != null ? fit.suggestedMinutes() : null, fit != null && fit.costsNoPeriods());
    }

    /** The shortest duty length (rounded up to 5 minutes) at which {@code offering}'s existing active
     *  duty roster delivers every curriculum Clinical hour on its own, and whether switching to it
     *  costs any timetable period — the alternative to adding extra duty days (OC-227). */
    record DutyFit(int currentMinutes, int suggestedMinutes, boolean costsNoPeriods) {}

    private DutyFit dutyFitFor(CourseOffering offering, List<Period> periods) {
        Integer current = offering.getClinicalShiftDurationMinutes();
        CurriculumSemesterCourse csc = offering.getCurriculumSemesterCourse();
        if (current == null || current <= 0 || csc == null || csc.getClinicalHours() == null || csc.getClinicalHours() <= 0) {
            return null;
        }
        ShiftRoster roster = shiftRosterFor(offering);
        return computeDutyFit(csc.getClinicalHours(), current, offering.getClinicalTravelBufferMinutes(),
            roster.starts(), roster.weeks(), periods);
    }

    /** An offering's active duty roster: each active Clinical Shift group's start time and how many
     *  weeks it runs — the whole term, or a date-bounded group's own window — exactly the week count
     *  {@code TimetableSkeletonService#effectiveWeeksFor} credits before its hours cap. */
    private record ShiftRoster(List<java.time.LocalTime> starts, List<Integer> weeks) {}

    private ShiftRoster shiftRosterFor(CourseOffering offering) {
        int weeksInTerm = CurriculumHoursCalculator.weeksInTerm(offering.getTermInstance());
        List<com.cms.dto.ClinicalShiftGroupDto> active = clinicalShiftGroupService.getGroupsForOffering(offering.getId()).stream()
            .filter(g -> Boolean.TRUE.equals(g.isActive()))
            .toList();
        List<Integer> weeks = active.stream()
            .map(g -> g.effectiveStartDate() == null || g.effectiveEndDate() == null ? weeksInTerm
                : (int) Math.max(1, Math.ceil((java.time.temporal.ChronoUnit.DAYS.between(
                    g.effectiveStartDate(), g.effectiveEndDate()) + 1) / 7.0)))
            .toList();
        return new ShiftRoster(active.stream().map(com.cms.dto.ClinicalShiftGroupDto::clinicalStartTime).toList(), weeks);
    }

    /** Curriculum Clinical hours the duty roster leaves owed: raw hours minus each group's duty
     *  length × its weeks, each group capped at the weeks the hours actually need — the same math
     *  as {@code TimetableSkeletonService#toClinicalShiftHours}/{@code #effectiveWeeksFor}, so the
     *  report and the hours cards can never disagree. Never negative. */
    private static double clinicalShiftResidualHours(CourseOffering offering, int durationMinutes, ShiftRoster roster) {
        CurriculumSemesterCourse csc = offering.getCurriculumSemesterCourse();
        int rawHours = csc != null && csc.getClinicalHours() != null ? csc.getClinicalHours() : 0;
        double hoursPerOccurrence = durationMinutes / 60.0;
        int weeksNeeded = CurriculumHoursCalculator.weeksNeededFor(rawHours, hoursPerOccurrence);
        double credited = roster.weeks().stream()
            .mapToDouble(w -> hoursPerOccurrence * (weeksNeeded > 0 ? Math.min(w, weeksNeeded) : w))
            .sum();
        return Math.max(0, rawHours - credited);
    }

    /** Pure arithmetic behind {@link #dutyFitFor}. A duty group runs once a week, so a roster
     *  delivers {@code minutes × Σ groupWeeks}; the fit is the smallest 5-minute multiple reaching
     *  {@code clinicalHours}. "Costs no periods" means that for every group, no period that is free
     *  today (outside [busDepart, busReturn), half-open exactly like {@link ClinicalShiftWindow#overlaps})
     *  becomes blocked by the later bus return — e.g. 6h → 6h10m at 07:00 with a 60-minute buffer
     *  moves bus return from 14:00 to 14:10, which is exactly when Period 6 starts. Null when the
     *  roster already covers the hours (nothing to fit). Package-private for direct verification. */
    static DutyFit computeDutyFit(int clinicalHours, int currentMinutes, Integer bufferMinutes,
                                  List<java.time.LocalTime> groupStarts, List<Integer> groupWeeks, List<Period> periods) {
        int totalWeeks = groupWeeks.stream().mapToInt(Integer::intValue).sum();
        if (totalWeeks <= 0 || groupStarts.isEmpty()) {
            return null;
        }
        int suggested = (int) (Math.ceil(clinicalHours * 60.0 / totalWeeks / 5.0) * 5);
        if (suggested <= currentMinutes) {
            return null;
        }
        int buffer = bufferMinutes != null ? bufferMinutes : 0;
        boolean costsNoPeriods = true;
        for (java.time.LocalTime start : groupStarts) {
            if (start == null) {
                continue;
            }
            java.time.LocalTime depart = start.minusMinutes(buffer);
            java.time.LocalTime oldReturn = start.plusMinutes((long) currentMinutes + buffer);
            java.time.LocalTime newReturn = start.plusMinutes((long) suggested + buffer);
            if (newReturn.isBefore(start)) {
                costsNoPeriods = false; // wrapped past midnight -- never call that free
                continue;
            }
            for (Period p : periods) {
                boolean blockedToday = p.getStartTime().isBefore(oldReturn) && depart.isBefore(p.getEndTime());
                boolean blockedAfter = p.getStartTime().isBefore(newReturn) && depart.isBefore(p.getEndTime());
                if (blockedAfter && !blockedToday) {
                    costsNoPeriods = false;
                }
            }
        }
        return new DutyFit(currentMinutes, suggested, costsNoPeriods);
    }

    /** The run report's "Apply duty length" (OC-227). The minutes are recomputed here from the
     *  offering's own curriculum hours and active roster — never taken from the request — and saved
     *  through the Course Offering's own clinical-shift-config path, so its existing validation (a
     *  shift is never shorter than the subject's Clinical Session Length) still applies. */
    @Transactional
    public CourseOfferingDto applyClinicalDutyFit(Long courseOfferingId) {
        CourseOffering offering = courseOfferingRepository.findById(courseOfferingId)
            .orElseThrow(() -> new ResourceNotFoundException("Course offering not found with id: " + courseOfferingId));
        DutyFit fit = dutyFitFor(offering, periodRepository.findByIsActiveTrueOrderByPeriodOrderAsc());
        if (fit == null) {
            throw new IllegalStateException("This offering's duty roster already delivers its curriculum Clinical hours "
                + "-- there is no duty-length change to apply");
        }
        return courseOfferingService.updateClinicalShiftConfig(courseOfferingId,
            new com.cms.dto.ClinicalShiftConfigUpdateRequest(fit.suggestedMinutes(), offering.getClinicalTravelBufferMinutes()));
    }

    private static String trimHours(double hours) {
        return hours == Math.rint(hours) ? String.valueOf((long) hours) : String.format("%.1f", hours);
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

    /** Hard-deletes EVERY DRAFT cell for the cohorts in scope, so each run re-packs the week from
     *  an empty grid instead of adding on top of whatever previous runs left behind. A PUBLISHED
     *  cell is never touched -- {@link #doRunGlobalAutoSchedule}'s own publish gate already
     *  refuses to run against a published term at all, so in practice everything reachable here is
     *  DRAFT.
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
        classScheduleCleanupService.purgeOccurrencesForCells(idsToDeactivate);
        classScheduleCleanupService.purgeRotationRowsForCells(idsToDeactivate);
        List<ClassSchedule> toDeactivate = classScheduleRepository.findAllById(idsToDeactivate);
        Set<Long> touchedBatchIds = new HashSet<>();
        for (ClassSchedule cs : toDeactivate) {
            AutoScheduleRunCache.current().ifPresent(cache -> cache.recordRemoval(cs));
            if (cs.getBatch() != null) {
                touchedBatchIds.add(cs.getBatch().getId());
            }
        }
        classScheduleRepository.deleteAllInBatch(toDeactivate);
        // A batch CohortRoomAllocation#revert correctly kept alive because it still had a real
        // DRAFT cell riding on it can lose that last reason to exist right here, the moment this
        // rebuild deletes that same cell -- revert's own "delete if zero real history" check
        // only ever runs once, at revert time. See BatchService#deleteOrphanedInactiveBatches's own
        // doc comment for the real incident (6 stale "Lab/Clinical - Section 1 - Batch N" rows) this
        // closes the gap on.
        batchService.deleteOrphanedInactiveBatches(touchedBatchIds);
        return new PurgeOutcome(toDeactivate.size(), pinnedPreserved);
    }

    /** Both halves of what the rebuild did to the existing DRAFT grid: how many cells it cleared,
     *  and how many it deliberately left standing because they were pinned. */
    private record PurgeOutcome(int cleared, int pinnedPreserved) {}

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
                // The pairing is one weekly session per row. On a day that runs less often than every
                // week (a partial working-Saturday pattern), each row keeps the hours that day didn't
                // deliver and is placed like any other row for the rest.
                for (TaggedShortfallRow paired : List.of(a.row0(), a.row1(), b.row0(), b.row1())) {
                    int leftover = paired.row().remainingRuns()
                        - runsFor(outcome.placementA().dayOfWeek(), term, paired.row().budget());
                    if (leftover > 0) {
                        queue.add(new TaggedShortfallRow(paired.cohortId(), paired.row().withRemainingRuns(leftover)));
                    }
                }
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
     *  Every working day (Saturday included on a term with chosen working Saturdays, see {@link
     *  #saturdayIsWorkingDay}), least-loaded-day-first — mirrors {@link #tryPlaceAndStaff}'s own
     *  ordering.
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
        List<DayOfWeek> candidateDays = workingDays(term).stream()
            .sorted(Comparator.comparingInt(d -> context.dayLoad().getOrDefault(d, 0)))
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
                    // placedA is already staffed at this point (its own staffCell try block above
                    // succeeded, or we'd already have `continue`d out) -- ordinary #removeCell refuses
                    // to remove a staffed cell (LifecycleConflictException, SKELETON_CELL_NOT_REMOVABLE
                    // -- "Only an unstaffed draft skeleton cell can be removed here"), so tearing it
                    // down here needs the same escape hatch attemptBacktrack uses.
                    timetableSkeletonService.forceRemoveCell(placedA.id());
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
                    // placedB itself is still unstaffed (its own staffCell above just failed), but
                    // placedA is staffed -- same forceRemoveCell reasoning as the branch above.
                    timetableSkeletonService.removeCell(placedB.id());
                    timetableSkeletonService.forceRemoveCell(placedA.id());
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
                    // Both cells are staffed by this point -- forceRemoveCell, not removeCell (see
                    // the two branches above).
                    timetableSkeletonService.forceRemoveCell(placedB.id());
                    timetableSkeletonService.forceRemoveCell(placedA.id());
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

    /** Rules 3/4/5 (user's hierarchy) — the single-offering counterpart of {@link
     *  #attemptCrossOfferingPairing} (Phase B, cross-OFFERING pairing — rule 2, already implemented,
     *  unchanged by this method). Runs immediately after it, per cohort, over the SAME (already
     *  Phase-B-reduced) {@code queue}: every remaining LAB shortfall row whose offering's exactly-2
     *  active batches already share ONE lab (Phase B's own "pairable" gate — see that method's
     *  identical filter, reused verbatim here) but found no cross-offering partner to pair with (so
     *  it's still sitting in the queue) is this method's target — the screenshot's N-AHN-I-215 case:
     *  one offering, one lab, two batches, nothing else to pair with.
     *
     * <p>Rather than leave the "off-duty" batch idle at that slot (the OLD behavior — Phase 1.5's
     *  generic {@link #fillIdleBatchGaps} covering it fresh, with zero memory of which batch got
     *  Library last time, the exact gap the user reported), this places the off-duty batch into
     *  Library — or Self-Study if no Library room is free, same fallback order the user confirmed —
     *  at the SAME slot, and wraps both cells in a {@link RotationGroup} that swaps the two batches
     *  week to week. Rules 4 (alternation) and 5 (equal term-end totals) fall out of {@link
     *  RotationGroupService}'s existing week-parity math for free, exactly like Phase B's own
     *  rotation already does — no changes needed there.
     *
     * <p>When neither a Library room nor a Self-Study fallback is available at ANY candidate
     *  day/block, this leaves the row untouched in {@code queue} (no regression — Phase 1's
     *  ordinary placement and Phase 1.5's per-run idle-batch fallback still cover it exactly as
     *  before) and records one advisory recommendation (not a warning — a capacity-planning
     *  suggestion, not a placement failure) that a second lab be provisioned via Capacity Auto-Plan.
     *  Returns how many {@link RotationGroup}s this cohort's pass created. */
    private int attemptSingleOfferingBatchRotation(CohortRunContext context, TermInstance term, List<Period> periods,
                                                     List<TaggedShortfallRow> queue, TermDemandAggregation termDemand) {
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

        int created = 0;
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
                continue; // not this shape -- either not sharing one lab, or Phase B already paired it
            }
            int blockSize = rows.get(0).row().blockSize();
            if (rows.get(1).row().blockSize() != blockSize) {
                continue;
            }

            RotationPairingOutcome outcome = tryRotateSingleOfferingBatches(cohortId, offering,
                rows.get(0).row().facultyId(), batch0, batch1, blockSize, term, periods, context, termDemand);
            if (outcome == null) {
                context.unplacedForCohort().add(new AutoPlaceUnplacedItem("Lab capacity", ClassSessionType.LAB, null,
                    offering.getSubject().getName() + ": only 1 lab provisioned for " + activeBatches.size()
                        + " batches, and no free Library room or eligible Self-Study faculty was found to cover "
                        + "the other batch at any candidate slot — consider provisioning a second lab via "
                        + "Capacity Auto-Plan", offering.getId(), false, true));
                continue;
            }
            context.placedThisCohortRun().add(outcome.placementA());
            context.placedThisCohortRun().add(outcome.placementB());
            TaggedShortfallRow row0 = rows.get(0);
            TaggedShortfallRow row1 = rows.get(1);
            queue.removeIf(t -> t == row0 || t == row1);
            // Same partial-week leftover handling as Phase B (#attemptCrossOfferingPairing) -- a day
            // that runs less often than every week (a partial working-Saturday pattern) leaves each
            // row owing the hours that day didn't deliver.
            for (TaggedShortfallRow paired : List.of(row0, row1)) {
                int leftover = paired.row().remainingRuns()
                    - runsFor(outcome.placementA().dayOfWeek(), term, paired.row().budget());
                if (leftover > 0) {
                    queue.add(new TaggedShortfallRow(paired.cohortId(), paired.row().withRemainingRuns(leftover)));
                }
            }
            created++;
        }
        return created;
    }

    /** Day/block search for {@link #attemptSingleOfferingBatchRotation}, mirroring {@link
     *  #tryPairOfferings}'s exact search/teardown-on-failure discipline: places {@code batch0} in
     *  the shared Lab, then tries Library for {@code batch1} at the SAME slot (falling back to
     *  Self-Study if no Library room is free — {@link #saveIdleBatchLibraryCell}/{@link
     *  #saveIdleBatchSelfStudyCell}, the exact same fallback the per-run idle-batch path already
     *  uses), and on success wraps both cells in a 2-slot/2-member {@link RotationGroup} that swaps
     *  {@code batch0}/{@code batch1} between the Lab and the Library/Self-Study slot week to week.
     *  Verifies {@code batch1} has no OTHER cell at the candidate slot first — unlike {@link
     *  #tryPairOfferings}'s two real offerings (each independently conflict-checked by {@link
     *  TimetableSkeletonService#placeCell}), {@link #saveIdleBatchLibraryCell}/{@link
     *  #saveIdleBatchSelfStudyCell} deliberately bypass {@code placeCell}'s own conflict checks (see
     *  their own javadoc), so this method must check {@code batch1}'s freeness itself before calling
     *  either. Returns {@code null} if no day/block in the whole week works for both halves. */
    private RotationPairingOutcome tryRotateSingleOfferingBatches(Long cohortId, CourseOffering offering, Long facultyId,
            Batch batch0, Batch batch1, int blockSize, TermInstance term, List<Period> periods,
            CohortRunContext context, TermDemandAggregation termDemand) {
        CohortSection section = batch0.getCohortSection();
        Subject librarySubject = subjectRepository.findByCode(LIBRARY_SUBJECT_CODE).orElse(null);
        List<Classroom> libraryClassrooms = librarySubject == null ? List.of()
            : classroomRepository.findByIsActiveTrueAndRoom_PurposeCategory_CodeOrderByNameAsc(RoomPurposeCategoryCode.LIBRARY);
        // Self-Study's row (which offering/budget, if any) doesn't vary by day/block -- only whether
        // ITS STAFFING succeeds does, so this is resolved once here rather than inside the loop below.
        // If BOTH fallbacks are structurally absent for this section (no Library classroom configured
        // AND no Self-Study curriculum offering at all), there is nothing any day/block could ever
        // find -- bail immediately rather than exhaustively placeCell+staffCell+removeCell-cycling
        // through every candidate for no reason (real waste in production, and what made
        // pairingSkipsWhenBatchCountsMismatch_realIncidentFixtureNumbers's "always succeeds" Lab stub
        // balloon this into dozens of pointless placements in the test that caught it).
        boolean selfStudyRowExists = resolveSelfStudyRowForFallback(cohortId, section, context.skeleton(), termDemand) != null;
        if (libraryClassrooms.isEmpty() && !selfStudyRowExists) {
            return null;
        }
        int batch1Headcount = realBatchHeadcount(batch1);
        List<ClassSchedule> batch1ExistingCells = classScheduleRepository.findByBatchIdInAndIsActiveTrue(List.of(batch1.getId()));

        List<DayOfWeek> candidateDays = workingDays(term).stream()
            .sorted(Comparator.comparingInt(d -> context.dayLoad().getOrDefault(d, 0)))
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
                Set<Long> blockPeriodIds = block.stream().map(Period::getId).collect(Collectors.toSet());
                boolean batch1Busy = batch1ExistingCells.stream().anyMatch(cs -> cs.getDayOfWeek() == day
                    && cs.getPeriod() != null && blockPeriodIds.contains(cs.getPeriod().getId()));
                if (batch1Busy) {
                    continue;
                }

                Period primary = block.get(0);
                List<Long> spanPeriodIds = block.size() > 1
                    ? block.subList(1, block.size()).stream().map(Period::getId).toList() : null;

                SkeletonCellResponse labCell;
                try {
                    labCell = timetableSkeletonService.placeCell(new SkeletonCellPlacementRequest(
                        offering.getId(), ClassSessionType.LAB, day, primary.getId(), batch0.getId(), cohortId,
                        section != null ? section.getId() : null, spanPeriodIds));
                } catch (TimetableConstraintViolationException | LifecycleConflictException | IllegalArgumentException ex) {
                    continue;
                }
                try {
                    timetableStaffingService.staffCell(labCell.id(), new StaffingAssignmentRequest(facultyId, null));
                } catch (TimetableConstraintViolationException | LifecycleConflictException | IllegalArgumentException ex) {
                    timetableSkeletonService.removeCell(labCell.id());
                    continue;
                }

                Classroom libRoom = librarySubject == null ? null
                    : firstFreeLibraryClassroomForCapacity(libraryClassrooms, term.getId(), day, block, batch1Headcount);
                Placement partner = libRoom != null
                    ? saveIdleBatchLibraryCell(librarySubject, term, day, block, section, libRoom, batch1)
                    : saveIdleBatchSelfStudyCell(cohortId, context.skeleton(), term, day, block, section, batch1, termDemand);
                if (partner == null) {
                    // labCell is already staffed (the staffCell try block above succeeded) --
                    // ordinary #removeCell refuses a staffed cell, same as tryPairOfferings above.
                    timetableSkeletonService.forceRemoveCell(labCell.id());
                    continue;
                }

                LocalDate anchor = firstOccurrenceOnOrAfter(term.getStartDate(), day);
                RotationGroupCreateRequest request = new RotationGroupCreateRequest(
                    term.getId(),
                    offering.getSubject().getName() + " Lab/Library rotation",
                    anchor,
                    List.of(new RotationGroupCreateRequest.RotationSlotInput(labCell.id(), 0),
                            new RotationGroupCreateRequest.RotationSlotInput(partner.cellId(), 1)),
                    List.of(
                        new RotationGroupCreateRequest.RotationMemberInput(0, "Group 1", List.of(
                            new RotationGroupCreateRequest.RotationAssignmentInput(labCell.id(), batch0.getId()),
                            new RotationGroupCreateRequest.RotationAssignmentInput(partner.cellId(), batch1.getId()))),
                        new RotationGroupCreateRequest.RotationMemberInput(1, "Group 2", List.of(
                            new RotationGroupCreateRequest.RotationAssignmentInput(labCell.id(), batch1.getId()),
                            new RotationGroupCreateRequest.RotationAssignmentInput(partner.cellId(), batch0.getId())))));

                try {
                    rotationGroupService.create(request, "system:global-auto-schedule");
                } catch (RuntimeException ex) {
                    // labCell is always staffed by this point. partner may or may not be (a Library
                    // cell is never staffed; a Self-Study cell is, via saveIdleBatchSelfStudyCell's
                    // own tryStaffWithFallback) -- forceRemoveCell works either way, so use it
                    // unconditionally rather than branching on which fallback partner turned out to be.
                    timetableSkeletonService.forceRemoveCell(partner.cellId());
                    timetableSkeletonService.forceRemoveCell(labCell.id());
                    continue;
                }

                List<Long> periodIds = block.stream().map(Period::getId).toList();
                context.dayLoad().merge(day, blockSize, Integer::sum);
                Placement placementA = new Placement(labCell.id(), offering.getId(), ClassSessionType.LAB, null,
                    section != null ? section.getId() : null, facultyId, offering.getSubject().getName(), "Rotation",
                    day, periodIds);
                Placement placementB = new Placement(partner.cellId(), partner.courseOfferingId(), partner.sessionType(),
                    null, section != null ? section.getId() : null, partner.facultyId(), partner.subjectName(),
                    "Rotation", day, periodIds);
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

    /** True once THIS cohort's own timetable has been approved on Draft Review. {@code
     *  TimetableGenerationService#approve} takes an explicit {@code cohortIds} list (OC-258/OC-260)
     *  and flips only the selected cohorts' {@code ClassSchedule} rows to {@code PUBLISHED} — so two
     *  cohorts sharing the same {@code termInstanceId} (a shared physical term, e.g. every year-group
     *  of BSc Nursing running concurrently in the same academic-year/term-type) can legitimately be
     *  in different states: one already approved, another still Pending/DRAFT. Checking
     *  {@code classScheduleRepository.existsByTermInstanceIdAndStatus(termInstanceId, PUBLISHED)}
     *  alone (the old implementation) answers "has ANY cohort in this term instance been approved",
     *  not "has this one" — that mistakenly treated every other cohort in the term as published too
     *  and silently skipped them from Global Auto-Schedule the moment a single cohort was approved.
     *  Reuses {@link TimetableSkeletonService#getCohortActiveClassSchedules(Long, Long,
     *  com.cms.model.enums.ClassScheduleStatus)}, the same per-cohort row resolution Approve/Revert/
     *  Discard already rely on, so this can never disagree with what Approve itself just published.
     *  This is the line past which only manual period/staff edits (swap staff, swap sessions) are
     *  allowed for that cohort, never a full automated re-run. Committing a Cohort Room Allocation in
     *  Capacity Planner does NOT trip this — that only unlocks placement (see {@code
     *  TimetableSkeletonService#resolveActiveSections}), it isn't itself a "done, stop touching this"
     *  signal. */
    private boolean isCohortTimetablePublished(Long termInstanceId, Long cohortId) {
        return !timetableSkeletonService
            .getCohortActiveClassSchedules(termInstanceId, cohortId, ClassScheduleStatus.PUBLISHED)
            .isEmpty();
    }

    private static String occupantLabel(SkeletonSubjectBudget budget) {
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
                                 int remainingRuns, int blockSize) {
        /** Whole weekly sessions still owed: {@code remainingRuns} in weekday terms, rounded up.
         *  Used for ordering (bigger need first) and Phase B's one-session-a-week shape. */
        int shortfall() {
            return (int) Math.ceil(remainingRuns / (double) weekRuns(budget));
        }

        ShortfallRow withRemainingRuns(int runs) {
            return new ShortfallRow(subjectName, offering, budget, facultyId, candidateFacultyIds, fallbackCandidatesById,
                runs, blockSize);
        }
    }

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
                                     Map<String, Set<DayOfWeek>> siblingDaysByOfferingAndType,
                                     Map<String, Integer> theoryStillOwedRuns) {}

    /** Key for {@link CohortRunContext#theoryStillOwedRuns()} -- one row per (offering, section),
     *  matching how {@link SkeletonSubjectBudget} scopes a THEORY row. */
    private static String theoryRowKey(Long offeringId, Long cohortSectionId) {
        return offeringId + ":" + cohortSectionId;
    }

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
     *  interchangeable slots than a THEORY lecture) are attempted before THEORY. Within the same
     *  session type, a mandatory row (CORE/FOUNDATIONAL/ELECTIVE) always goes before an advisory
     *  {@code CO_CURRICULAR} row (e.g. Self-Study) — a hard partition, not a heuristic — since an
     *  advisory line must never starve a real curriculum requirement out of a tight week. Within
     *  each of those tiers, a row with a bigger remaining shortfall (more sessions still needing a
     *  home) goes before one with a smaller one. The shortfall ordering is a cheap heuristic, not an
     *  actual per-row feasible-slot count — a real constraint-count scan would be more precise but
     *  isn't worth the complexity for a first cut; {@link #attemptBacktrack} is what actually
     *  recovers from a wrong ordering guess, not this comparator alone. */
    private static final Comparator<ShortfallRow> SHORTFALL_ROW_ORDER = Comparator
        .comparing((ShortfallRow r) -> r.budget().sessionType() == ClassSessionType.THEORY ? 1 : 0)
        .thenComparing((ShortfallRow r) -> isAdvisoryRow(r) ? 1 : 0)
        .thenComparing((ShortfallRow r) -> -r.shortfall());

    /** True for a curriculum-backed row explicitly typed {@code CO_CURRICULAR} (advisory, e.g.
     *  Self-Study) — false for anything mandatory (CORE/FOUNDATIONAL/ELECTIVE) and false for a
     *  row with no {@link CurriculumSemesterCourse} at all (e.g. a system-owned Library/Sports
     *  filler offering), which is unaffected by this partition. */
    private static boolean isAdvisoryRow(ShortfallRow row) {
        CourseOffering offering = row.offering();
        CurriculumSemesterCourse csc = offering != null ? offering.getCurriculumSemesterCourse() : null;
        return csc != null && csc.getSubjectType() == SubjectType.CO_CURRICULAR;
    }

    /** Same check as {@link #isAdvisoryRow}, from a bare offering id — used by {@link
     *  #attemptBacktrack}, which only has the already-placed {@link Placement}'s id, not its
     *  original {@link ShortfallRow}. */
    private boolean isAdvisoryOfferingId(Long offeringId) {
        if (offeringId == null) {
            return false;
        }
        CourseOffering offering = courseOfferingRepository.findById(offeringId).orElse(null);
        CurriculumSemesterCourse csc = offering != null ? offering.getCurriculumSemesterCourse() : null;
        return csc != null && csc.getSubjectType() == SubjectType.CO_CURRICULAR;
    }

    /** Bonus-Theory fill priority ({@link #fillSelfStudyGaps}'s equal-extra-hours rotation, user's
     *  call): CORE/FOUNDATIONAL rows get first claim on leftover periods, ELECTIVE next, and
     *  CO_CURRICULAR last (shouldn't normally reach this pool at all -- {@link
     *  #resolveExtraHoursFillerRows} already excludes the real Self-Study/co-curricular subject by
     *  name -- this is just a safe default if one ever slips through unnamed). A row with no {@link
     *  CurriculumSemesterCourse} defaults to the same tier as CORE/FOUNDATIONAL, matching {@link
     *  CurriculumSemesterCourse#subjectType}'s own default. */
    private static int subjectTypePriorityRank(SelfStudyRow row) {
        CourseOffering offering = row.offering();
        CurriculumSemesterCourse csc = offering != null ? offering.getCurriculumSemesterCourse() : null;
        if (csc == null) {
            return 0;
        }
        return switch (csc.getSubjectType()) {
            case CORE, FOUNDATIONAL -> 0;
            case ELECTIVE -> 1;
            case CO_CURRICULAR -> 2;
        };
    }

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
    /** @return how many run-occurrences this row still genuinely owes once every attempt above has
     *  been exhausted (0 once fully met) -- for THEORY (blockSize 1) this is real run count; for
     *  LAB/CLINICAL it's periods, unused by any caller today since only THEORY rows feed {@link
     *  #fillSelfStudyGaps}'s shortfall-priority tier. */
    private int placeShortfallRow(Long cohortId, ShortfallRow row, TermInstance term, List<Period> periods,
                                    CohortRunContext context, double periodDurationHours,
                                    Map<String, VenueGapAccumulator> venueGaps,
                                    List<FacultySubstitutionEvent> facultySubstitutionEvents,
                                    TermDemandAggregation termDemand) {
        RowPlacementState state = newRowPlacementState(row, context);
        while (state.remainingRuns > 0) {
            placeOneSessionAttempt(cohortId, row, term, periods, context, state, periodDurationHours,
                venueGaps, facultySubstitutionEvents, termDemand);
        }
        finalizeRowPlacement(row, state, periodDurationHours, context);
        return state.unplacedPeriods;
    }

    /** Same job as {@link #placeShortfallRow}, but for 2+ sibling rows splitting one course offering
     *  across parallel batches that share a single scarce venue (Lab/Clinical) -- round-robins ONE
     *  session placement per row per pass, instead of the first row in queue order draining its
     *  entire week before the next sibling even gets a turn.
     *
     * <p>This is what turns the old emergent clustering (whichever batch's row got processed first
     *  grabbed every lightly-loaded day, e.g. Mon+Tue, pushing the other batch onto whatever was left,
     *  e.g. Wed+Thu) into a day-by-day alternation instead, with no new day-selection heuristic
     *  needed: after Batch A claims Monday, {@code context.dayLoad()} for Monday is now higher, so
     *  Batch B's very next turn (least-loaded day, excluding its own days) naturally prefers Tuesday;
     *  Batch A's following turn then avoids both Monday (its own) and Tuesday (just bumped by B), so
     *  it lands on Wednesday; and so on -- alternating by construction, purely from turn order.
     *  Matches ordinary human scheduling instinct (alternate the days, don't block-cluster them) --
     *  user preference, 2026-09-22.
     *
     * <p>Every other per-row mechanism (sibling-day same-venue alignment attempt, backtrack,
     *  same-day-twice fallback, shortfall reporting) is untouched -- only the turn order across rows
     *  changes; a singleton group behaves identically to the original {@link #placeShortfallRow}. */
    private void placeShortfallRowGroupInterleaved(List<TaggedShortfallRow> group, TermInstance term, List<Period> periods,
                                                     Map<Long, CohortRunContext> contextsById, double periodDurationHours,
                                                     Map<String, VenueGapAccumulator> venueGaps,
                                                     List<FacultySubstitutionEvent> facultySubstitutionEvents,
                                                     TermDemandAggregation termDemand) {
        List<RowPlacementState> states = new ArrayList<>();
        for (TaggedShortfallRow tagged : group) {
            states.add(newRowPlacementState(tagged.row(), contextsById.get(tagged.cohortId())));
        }
        while (states.stream().anyMatch(s -> s.remainingRuns > 0)) {
            for (int i = 0; i < group.size(); i++) {
                RowPlacementState state = states.get(i);
                if (state.remainingRuns <= 0) {
                    continue;
                }
                TaggedShortfallRow tagged = group.get(i);
                placeOneSessionAttempt(tagged.cohortId(), tagged.row(), term, periods, contextsById.get(tagged.cohortId()),
                    state, periodDurationHours, venueGaps, facultySubstitutionEvents, termDemand);
            }
        }
        for (int i = 0; i < group.size(); i++) {
            finalizeRowPlacement(group.get(i).row(), states.get(i), periodDurationHours, contextsById.get(group.get(i).cohortId()));
        }
    }

    /** Every not-yet-processed row in {@code queue} sharing {@code anchor}'s (cohort, offering,
     *  session type) -- the same identity {@link CohortRunContext#siblingDaysByOfferingAndType()}
     *  keys on -- so a batch-split LAB/CLINICAL offering's rows are handled together. Always includes
     *  {@code anchor} itself; size 1 for the ordinary unsplit case, which the caller routes to the
     *  original single-row {@link #placeShortfallRow} path unchanged. */
    private List<TaggedShortfallRow> siblingBatchGroup(TaggedShortfallRow anchor, List<TaggedShortfallRow> queue,
                                                          Set<TaggedShortfallRow> consumed) {
        List<TaggedShortfallRow> group = new ArrayList<>();
        for (TaggedShortfallRow t : queue) {
            if (consumed.contains(t)) {
                continue;
            }
            if (!t.cohortId().equals(anchor.cohortId())
                || !t.row().offering().getId().equals(anchor.row().offering().getId())
                || t.row().budget().sessionType() != anchor.row().budget().sessionType()) {
                continue;
            }
            group.add(t);
        }
        return group;
    }

    /** Mutable per-row progress carried across {@link #placeOneSessionAttempt} calls -- lets sibling
     *  rows be interleaved turn-by-turn (see {@link #placeShortfallRowGroupInterleaved}) instead of
     *  only ever letting one row's {@code while} loop finish before another starts. {@link
     *  #placeShortfallRow} drains the same state start-to-finish in one call, identically to how this
     *  logic behaved before it was extracted out of that method's body. */
    private static final class RowPlacementState {
        final Set<DayOfWeek> daysUsed;
        // Second-session-per-day fallback (see #daysOtherThanUsedOnce's javadoc): starts empty and
        // only ever gains a day once the one-session-per-day pass has genuinely exhausted every
        // candidate day for this exact row -- a day only ever lands in here after it's already in
        // daysUsed, so this is additive to, never a replacement for, the normal cap.
        final Set<DayOfWeek> daysUsedTwice = EnumSet.noneOf(DayOfWeek.class);
        // Sibling-batch alignment (LAB/CLINICAL only) -- see CohortRunContext#siblingDaysByOfferingAndType.
        // Null for THEORY, which has no per-batch splitting.
        final String siblingKey;
        // Real-world sequencing preference for a THEORY row whose subject also has LAB/CLINICAL --
        // see #newRowPlacementState's javadoc.
        final Set<DayOfWeek> preferredDays;
        // shortfall() is a count of SESSIONS still owed this week, NOT periods -- see
        // CurriculumHoursCalculator#sessionsPerWeek. Planned against the term's total hours
        // (2026-09-15): each placed session takes off the runs its day really has.
        int remainingRuns;
        int unplacedPeriods = 0;
        String lastFailureReason;

        RowPlacementState(Set<DayOfWeek> daysUsed, String siblingKey, Set<DayOfWeek> preferredDays, int remainingRuns) {
            this.daysUsed = daysUsed;
            this.siblingKey = siblingKey;
            this.preferredDays = preferredDays;
            this.remainingRuns = remainingRuns;
        }
    }

    /** Builds a fresh {@link RowPlacementState} for {@code row} -- the same one-time setup {@link
     *  #placeShortfallRow}'s body always ran inline before its {@code while} loop, now shared with
     *  {@link #placeShortfallRowGroupInterleaved}. The THEORY {@code preferredDays} computation
     *  prefers landing on the same day as, or before, that subject's own LAB/CLINICAL day(s) --
     *  students shouldn't walk into a practical before the lecture behind it has even been taught
     *  that week. Phase 1 (LAB/CLINICAL, every cohort) has always fully finished by the time Phase 2
     *  (THEORY) reaches this row, so siblingDaysByOfferingAndType's LAB/CLINICAL entries for this
     *  offering are already final -- safe to read once here rather than per candidate slot. */
    private RowPlacementState newRowPlacementState(ShortfallRow row, CohortRunContext context) {
        Set<DayOfWeek> daysUsed = existingDaysForBudgetRow(context.skeleton().cells(), row.offering().getId(), row.budget());
        String siblingKey = row.budget().sessionType() == ClassSessionType.THEORY ? null
            : offeringSessionTypeKey(row.offering().getId(), row.budget().sessionType());
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
                    if (d.ordinal() <= earliestLabClinicalRank) {
                        preferredDays.add(d);
                    }
                }
            }
        }
        return new RowPlacementState(daysUsed, siblingKey, preferredDays, row.remainingRuns());
    }

    /** Performs exactly ONE placement attempt (one session-occurrence) for {@code row}, mutating
     *  {@code state} in place -- the same per-iteration logic {@link #placeShortfallRow} always ran
     *  inline inside its {@code while} loop, lifted out so {@link #placeShortfallRowGroupInterleaved}
     *  can call it turn-by-turn across sibling rows. */
    private void placeOneSessionAttempt(Long cohortId, ShortfallRow row, TermInstance term, List<Period> periods,
                                          CohortRunContext context, RowPlacementState state, double periodDurationHours,
                                          Map<String, VenueGapAccumulator> venueGaps,
                                          List<FacultySubstitutionEvent> facultySubstitutionEvents,
                                          TermDemandAggregation termDemand) {
        int thisBlockSize = row.blockSize();
        PlacementAttempt attempt = null;
        if (state.siblingKey != null) {
            for (DayOfWeek siblingDay : context.siblingDaysByOfferingAndType().getOrDefault(state.siblingKey, Set.of())) {
                if (state.daysUsed.contains(siblingDay)) {
                    continue;
                }
                PlacementAttempt siblingAttempt = tryPlaceAndStaff(cohortId, row.offering(), row.budget(), row.candidateFacultyIds(),
                    term, periods, allDaysExcept(siblingDay), thisBlockSize, context.dayLoad(), state.preferredDays);
                if (siblingAttempt.dayPlaced() != null) {
                    attempt = siblingAttempt;
                    break;
                }
            }
        }
        if (attempt == null) {
            attempt = tryPlaceAndStaff(cohortId, row.offering(), row.budget(), row.candidateFacultyIds(), term,
                periods, state.daysUsed, thisBlockSize, context.dayLoad(), state.preferredDays);
        }
        if (attempt.dayPlaced() != null) {
            state.daysUsed.add(attempt.dayPlaced());
            context.dayLoad().merge(attempt.dayPlaced(), thisBlockSize, Integer::sum);
            context.placedThisCohortRun().add(new Placement(attempt.cellId(), row.offering().getId(), row.budget().sessionType(),
                row.budget().batchId(), row.budget().cohortSectionId(), attempt.facultyId(), row.subjectName(),
                occupantLabel(row.budget()), attempt.dayPlaced(), attempt.periodIds()));
            recordFacultySubstitutionIfAny(row, cohortId, attempt.facultyId(), facultySubstitutionEvents);
            if (state.siblingKey != null) {
                context.siblingDaysByOfferingAndType().computeIfAbsent(state.siblingKey, k -> new HashSet<>()).add(attempt.dayPlaced());
            }
            state.remainingRuns -= runsFor(attempt.dayPlaced(), term, row.budget()); // one SESSION, whatever its blockSize
            return;
        }
        DayOfWeek backtrackDay = attemptBacktrack(cohortId, row, term, periods, state.daysUsed, context.placedThisCohortRun(),
            context.unplacedForCohort(), thisBlockSize, context.dayLoad(), termDemand, facultySubstitutionEvents,
            context.theoryStillOwedRuns());
        if (backtrackDay != null) {
            state.remainingRuns -= runsFor(backtrackDay, term, row.budget());
            return;
        }
        // Every working day this row hasn't used has now failed, and so has a backtrack: a second
        // session on a day this row already uses once, back-to-back allowed, fully conflict-checked.
        // Saturday is one of those days whenever the term has chosen working Saturdays
        // (#saturdayIsWorkingDay) -- a regular day, not a fallback tried after the rest.
        PlacementAttempt doubleAttempt = tryPlaceAndStaff(cohortId, row.offering(), row.budget(), row.candidateFacultyIds(), term,
            periods, daysOtherThanUsedOnce(state.daysUsed, state.daysUsedTwice), thisBlockSize, context.dayLoad(), state.preferredDays);
        if (doubleAttempt.dayPlaced() != null) {
            state.daysUsedTwice.add(doubleAttempt.dayPlaced());
            context.dayLoad().merge(doubleAttempt.dayPlaced(), thisBlockSize, Integer::sum);
            context.placedThisCohortRun().add(new Placement(doubleAttempt.cellId(), row.offering().getId(), row.budget().sessionType(),
                row.budget().batchId(), row.budget().cohortSectionId(), doubleAttempt.facultyId(), row.subjectName(),
                occupantLabel(row.budget()), doubleAttempt.dayPlaced(), doubleAttempt.periodIds()));
            recordFacultySubstitutionIfAny(row, cohortId, doubleAttempt.facultyId(), facultySubstitutionEvents);
            state.remainingRuns -= runsFor(doubleAttempt.dayPlaced(), term, row.budget());
        } else {
            // Nothing fits: the row's whole remaining weekly cadence is given up at once below
            // (remainingRuns -= weekRuns(...), not just this one attempt), so the reported shortfall
            // must cover every run-occurrence being abandoned here -- not just the one block just
            // tried. Reporting only `thisBlockSize` (one occurrence, ~1 period) understated a full
            // remaining-term shortfall (e.g. 16 runs) as under 1 -- "0.8h still unplaced" when the
            // real gap was 13.3h. Capped at what's actually left so a final partial week isn't
            // over-reported past the row's true remaining requirement.
            int abandonedRuns = Math.min(state.remainingRuns, weekRuns(row.budget()));
            state.unplacedPeriods += abandonedRuns * thisBlockSize;
            // The fresh-day attempt names the real blocker (faculty/room/duty) unless every working
            // day was already used, in which case only the double attempt tried anything.
            state.lastFailureReason = everyWorkingDayUsed(state.daysUsed, term) ? doubleAttempt.failureReason() : attempt.failureReason();
            tallyVenueGap(row, thisBlockSize, periodDurationHours, venueGaps);
            state.remainingRuns -= weekRuns(row.budget());
        }
    }

    /** Reports at most ONE {@link AutoPlaceUnplacedItem} for this row, not one per failed chunk -- a
     *  row needing 6 more blocks that fails on all 6 used to add 6 near-identical lines (same
     *  subject/reason, differing only in the trailing "N of M combinations tried" count), which read
     *  as noise rather than a signal an admin could act on. The single line now says how much is
     *  still short and the LAST attempt's reason (the most-exhausted, most-informative one, since
     *  earlier attempts in the same row are strictly less constrained as daysUsed/dayLoad fill up). */
    private void finalizeRowPlacement(ShortfallRow row, RowPlacementState state, double periodDurationHours, CohortRunContext context) {
        if (state.unplacedPeriods > 0) {
            double unplacedHours = state.unplacedPeriods * periodDurationHours;
            context.unplacedForCohort().add(new AutoPlaceUnplacedItem(row.subjectName(), row.budget().sessionType(),
                occupantLabel(row.budget()), formatHours(unplacedHours) + " still unplaced — " + state.lastFailureReason,
                row.offering().getId(), true, false));
        }
    }

    /** Exclusion set for the same-day-double attempt: every day EXCEPT the days this row already
     *  uses exactly once — the only days a second session may go (a day it already doubled up on,
     *  and an unused day that already failed, are both out). Saturday is treated like any other
     *  day; on a term with no working Saturdays it can never be in {@code daysUsed} anyway. */
    private static Set<DayOfWeek> daysOtherThanUsedOnce(Set<DayOfWeek> daysUsed, Set<DayOfWeek> daysUsedTwice) {
        Set<DayOfWeek> excluded = EnumSet.allOf(DayOfWeek.class);
        for (DayOfWeek day : daysUsed) {
            if (!daysUsedTwice.contains(day)) {
                excluded.remove(day);
            }
        }
        return excluded;
    }

    private static boolean everyWorkingDayUsed(Set<DayOfWeek> daysUsed, TermInstance term) {
        return daysUsed.containsAll(workingDays(term));
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
     *  block-size support existed. Candidate days are tried least-loaded-first ({@code
     *  dayLoad}, a running count of periods already placed for this cohort on each day — see the
     *  caller), not in fixed Monday-first order: a static order let every independently-processed
     *  row grab Monday's early periods first and then move on once its own weekly quota was met, so
     *  several one-session-a-week subjects piled onto the same day's morning and nothing ever came
     *  back to fill that day's afternoon, while a later, harder-to-place LAB/CLINICAL block packed a
     *  different day solid — an uneven week, not a defensible one. Saturday joins that same ordering
     *  as a regular day on a term with chosen working Saturdays and is never a candidate otherwise
     *  (see {@link #saturdayIsWorkingDay}). Tallies every {@link ConstraintViolation}
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
        // Every working day least-loaded-first -- Saturday included as a regular day whenever the
        // term has chosen working Saturdays (#saturdayIsWorkingDay), so the week spreads evenly
        // across all of them rather than leaving a chosen Saturday empty. A term with no working
        // Saturdays never lists Saturday at all, so 8 doomed attempts there can't dilute the
        // reported failure fraction below.
        List<DayOfWeek> candidateDays = workingDays(term).stream()
            .sorted(Comparator.<DayOfWeek>comparingInt(d -> preferredDays.isEmpty() || preferredDays.contains(d) ? 0 : 1)
                .thenComparingInt(d -> dayLoad.getOrDefault(d, 0)))
            .toList();
        for (DayOfWeek day : candidateDays) {
            if (daysUsed.contains(day)) {
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

    /** Outcome of trying an ordered list of {@link SelfStudyRow}s against one (day, period) slot —
     *  {@code rowIndex} indexes into whichever candidate list the caller passed in. {@code
     *  freeButUnstaffable} and {@code lastBlockCode} matter only when {@link #filled} is false, and
     *  are always the LAST such signal seen across every row tried at this slot (mirrors the
     *  single-tier loop this was extracted from). */
    private record RowSlotAttempt(Long cellId, int rowIndex, Long facultyId, boolean freeButUnstaffable, String lastBlockCode) {
        boolean filled() {
            return cellId != null;
        }
    }

    /** Shared placement mechanics for {@link #fillSelfStudyGaps}'s two priority tiers (shortfall-first,
     *  then equal-extra among already-met rows) — tries each row in {@code attemptOrder} in turn,
     *  returning as soon as one is placed AND staffed. */
    private RowSlotAttempt attemptRowsForSlot(Long cohortId, List<SelfStudyRow> candidateRows, List<Integer> attemptOrder,
                                               DayOfWeek day, Period period) {
        boolean freeButUnstaffable = false;
        String lastBlockCode = null;
        for (int rowIndex : attemptOrder) {
            SelfStudyRow row = candidateRows.get(rowIndex);
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
                return new RowSlotAttempt(placed.id(), rowIndex, staffedBy, freeButUnstaffable, lastBlockCode);
            }
            timetableSkeletonService.removeCell(placed.id());
            freeButUnstaffable = true;
        }
        return new RowSlotAttempt(null, -1, null, freeButUnstaffable, lastBlockCode);
    }

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
     *  correctly skipped, not overwritten. Saturday is included when {@code saturdayOpen} — the term
     *  has chosen working Saturdays, which makes Saturday a regular day (see {@link
     *  #saturdayIsWorkingDay}), so a leftover empty period there is as unacceptable as a weekday one.
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
                                               List<AutoPlaceUnplacedItem> unplacedForCohort, TermDemandAggregation termDemand,
                                               boolean saturdayOpen, Map<String, Integer> theoryStillOwedRuns) {
        List<DayOfWeek> weekdays = saturdayOpen
            ? List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY,
                DayOfWeek.SATURDAY)
            : List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY);
        List<Placement> filled = new ArrayList<>();

        // Every non-elective Theory offering this cohort runs except Self-Study/Co-curricular, which
        // already got its curriculum hours and, like Library and Sports (placed just before this),
        // takes no extra (2026-09-15). Deliberately does NOT touch CurriculumSemesterCourse.theoryHours:
        // the recorded, regulator-facing requirement stays exactly what the curriculum specifies, and
        // this only adds ADDITIONAL budget-uncapped sessions beyond it (placeCell's
        // enforceBudgetCap=false below).
        List<SelfStudyRow> allRows = resolveExtraHoursFillerRows(cohortId, skeleton, termDemand);
        if (allRows.isEmpty()) {
            unplacedForCohort.add(new AutoPlaceUnplacedItem(SELF_STUDY_ITEM_LABEL, ClassSessionType.THEORY, null,
                "no curriculum Theory offering other than Self-Study exists for this cohort to take extra hours "
                    + "— every remaining free period stays empty until one is added", null, false, true));
            return new SelfStudyGapFillOutcome(filled, 0);
        }

        // User's call, 2026-09-17 (flips the prior 2026-09-10 "fill them equally" default): every
        // offering's real required hours come first -- a row Phase 2 (placeShortfallRow) still owes
        // runs on gets first claim on any genuinely free period found below, via theoryStillOwedRuns
        // (fed from that phase's own return value, not this method's stale pre-run `skeleton`
        // snapshot, which always shows 0 delivered for a freshly-rebuilt DRAFT and so can't tell a
        // just-closed row from one this run never touched). Only once every still-short row has
        // either closed its own gap or genuinely can't be staffed there does a period fall through to
        // the equal-extra-hours rotation among rows that are already met.
        List<SelfStudyRow> shortRows = new ArrayList<>();
        int[] shortRemaining = new int[allRows.size()];
        List<SelfStudyRow> metRows = new ArrayList<>();
        for (SelfStudyRow row : allRows) {
            int owed = theoryStillOwedRuns.getOrDefault(
                theoryRowKey(row.offering().getId(), row.budget().cohortSectionId()), 0);
            if (owed > 0) {
                shortRemaining[shortRows.size()] = owed;
                shortRows.add(row);
            } else {
                metRows.add(row);
            }
        }

        int unfillablePeriods = 0;
        int shortRotation = 0;
        int rotation = 0;
        // Extra sessions this pass has given each already-met row: the next free period goes to
        // whichever subject has had the fewest, so every subject ends with equal extra hours instead
        // of merely taking equal turns (a subject that couldn't take one slot no longer falls behind
        // for good). CORE/FOUNDATIONAL rows are tried before ELECTIVE ones for the same period
        // (see #subjectTypePriorityRank) -- this equal-extra-hours balancing only kicks in as the
        // tie-break WITHIN a priority tier, not across tiers, so an Elective subject only starts
        // taking extra hours once every Core/Foundational row has had an equal share first.
        int[] extraByRow = new int[metRows.size()];
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

                if (!shortRows.isEmpty()) {
                    int start = shortRotation;
                    List<Integer> shortOrder = java.util.stream.IntStream.range(0, shortRows.size()).boxed()
                        .filter(i -> shortRemaining[i] > 0)
                        .sorted(Comparator.comparingInt(i -> Math.floorMod(i - start, shortRows.size())))
                        .toList();
                    if (!shortOrder.isEmpty()) {
                        RowSlotAttempt result = attemptRowsForSlot(cohortId, shortRows, shortOrder, day, period);
                        if (result.filled()) {
                            dayLoad.merge(day, 1, Integer::sum);
                            SelfStudyRow row = shortRows.get(result.rowIndex());
                            filled.add(new Placement(result.cellId(), row.offering().getId(), ClassSessionType.THEORY,
                                row.budget().batchId(), row.budget().cohortSectionId(), result.facultyId(), row.subjectName(),
                                occupantLabel(row.budget()), day, List.of(period.getId())));
                            periodFilled = true;
                            shortRemaining[result.rowIndex()]--;
                            shortRotation = (result.rowIndex() + 1) % shortRows.size();
                        } else {
                            periodWasFreeButUnstaffable = result.freeButUnstaffable();
                            lastBlockCode = result.lastBlockCode();
                        }
                    }
                }

                if (!periodFilled && !metRows.isEmpty()) {
                    int start = rotation;
                    List<Integer> attemptOrder = java.util.stream.IntStream.range(0, metRows.size()).boxed()
                        .sorted(Comparator.comparingInt((Integer i) -> subjectTypePriorityRank(metRows.get(i)))
                            .thenComparingInt(i -> extraByRow[i])
                            .thenComparingInt(i -> Math.floorMod(i - start, metRows.size())))
                        .toList();
                    RowSlotAttempt result = attemptRowsForSlot(cohortId, metRows, attemptOrder, day, period);
                    if (result.filled()) {
                        dayLoad.merge(day, 1, Integer::sum);
                        SelfStudyRow row = metRows.get(result.rowIndex());
                        filled.add(new Placement(result.cellId(), row.offering().getId(), ClassSessionType.THEORY,
                            row.budget().batchId(), row.budget().cohortSectionId(), result.facultyId(), row.subjectName(),
                            occupantLabel(row.budget()), day, List.of(period.getId())));
                        periodFilled = true;
                        extraByRow[result.rowIndex()]++;
                        rotation = (result.rowIndex() + 1) % metRows.size();
                    } else {
                        periodWasFreeButUnstaffable = periodWasFreeButUnstaffable || result.freeButUnstaffable();
                        if (result.lastBlockCode() != null) {
                            lastBlockCode = result.lastBlockCode();
                        }
                    }
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
            unplacedForCohort.add(new AutoPlaceUnplacedItem(SELF_STUDY_ITEM_LABEL, ClassSessionType.THEORY, null,
                unfillablePeriods + " period(s) left genuinely empty — every eligible extra-hours"
                    + " faculty is unavailable, already teaching elsewhere, or at their capacity cap at that exact slot",
                allRows.get(0).offering().getId(), false, true));
        }
        for (Map.Entry<String, Integer> blocked : blockedPeriodsByReason.entrySet()) {
            unplacedForCohort.add(new AutoPlaceUnplacedItem(GAP_FILL_ITEM_LABEL, ClassSessionType.THEORY, null,
                blocked.getValue() + " period(s) left empty — " + friendlyFailureReason(blocked.getKey()),
                allRows.get(0).offering().getId(), false, true));
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

    /** Every real, non-elective Theory offering in this cohort's curriculum EXCEPT Self-Study/
     *  Co-curricular, one {@link SelfStudyRow} per (offering, section) budget row — the subjects
     *  {@link #fillSelfStudyGaps} shares leftover periods among as extra revision time. Self-Study
     *  gets exactly its curriculum hours like any subject and no extra (2026-09-15: "allocate
     *  required Library and Sports and Self-Study, and then spare among the other offerings
     *  equally"). */
    private List<SelfStudyRow> resolveExtraHoursFillerRows(Long cohortId, SkeletonBuilderResponse skeleton, TermDemandAggregation termDemand) {
        List<SelfStudyRow> rows = new ArrayList<>();
        for (SkeletonSubjectResponse subject : skeleton.subjects()) {
            if (isSelfStudySubject(subject.subjectName())) {
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
    private record LibraryGapFillOutcome(List<Placement> filled, int unfillableSessions, int targetSessions) {}

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
                                                   List<SkeletonCellResponse> existingCells, List<Placement> placedThisRun,
                                                   boolean saturdayOpen, Map<String, Integer> theoryStillOwedRuns) {
        // Library is filler (OC-227): it takes the working-day periods curriculum left free --
        // Saturday included as a regular day when the term has chosen working Saturdays
        // (saturdayOpen, see #saturdayIsWorkingDay). Whatever doesn't fit shrinks, reported as a
        // neutral note, not an unplaced warning.
        List<DayOfWeek> weekdays = saturdayOpen
            ? List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY, DayOfWeek.SATURDAY)
            : List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY);
        List<Placement> filled = new ArrayList<>();

        Subject librarySubject = subjectRepository.findByCode(LIBRARY_SUBJECT_CODE).orElse(null);
        List<Classroom> libraryClassrooms = classroomRepository
            .findByIsActiveTrueAndRoom_PurposeCategory_CodeOrderByNameAsc(RoomPurposeCategoryCode.LIBRARY);
        if (librarySubject == null || libraryClassrooms.isEmpty()) {
            unplacedForCohort.add(new AutoPlaceUnplacedItem("Library", ClassSessionType.LIBRARY, null,
                "no Library classroom is configured (a Classroom linked to a Room tagged with the Library "
                    + "Purpose Category) — every cohort's Library quota stays unplaced until one is added", null, false, true));
            return new LibraryGapFillOutcome(filled, 0, 0);
        }

        int sessionsPerWeek = resolveLibraryConfigInt(CONFIG_LIBRARY_SESSIONS_PER_WEEK, DEFAULT_LIBRARY_SESSIONS_PER_WEEK);
        int blockSize = resolveLibraryConfigInt(CONFIG_LIBRARY_BLOCK_SIZE_PERIODS, DEFAULT_LIBRARY_BLOCK_SIZE_PERIODS);
        List<List<Period>> candidateBlocks = contiguousPeriodBlocks(periods, blockSize);

        List<CohortSection> activeSections = timetableSkeletonService.resolveActiveSections(cohortId, term.getId());
        List<CohortSection> audiences = activeSections.isEmpty() ? Arrays.asList((CohortSection) null) : activeSections;

        // Library yields to curriculum (OC-227) -- including its OWN required quota, not just the
        // bonus second session (#fillExtraLibrarySession already gated this; this quota didn't,
        // which let it claim a period ahead of a still-short mandatory or CO_CURRICULAR row). Same
        // principle #fillSelfStudyGaps applies to its own filler: every offering's real required
        // hours come first, Library (and Sports, gated the same way) only takes what's genuinely
        // left over.
        if (theoryStillOwedRuns.values().stream().anyMatch(owed -> owed > 0)) {
            int target = sessionsPerWeek * audiences.size();
            return new LibraryGapFillOutcome(filled, target, target);
        }

        int unfillableSessions = 0;
        for (CohortSection section : audiences) {
            int placedForAudience = placeLibraryBlocks(cohortId, term, section, sessionsPerWeek, weekdays, candidateBlocks,
                librarySubject, libraryClassrooms, dayLoad, existingCells, placedThisRun, filled);
            if (placedForAudience < sessionsPerWeek) {
                unfillableSessions += sessionsPerWeek - placedForAudience;
            }
        }
        // No unplaced item for a shortfall here: Library yields to curriculum by design (OC-227), so
        // the caller reports it as a neutral note via #libraryInfoNotes. Only a missing Library
        // classroom -- a configuration gap, handled above -- is a warning.
        return new LibraryGapFillOutcome(filled, unfillableSessions, sessionsPerWeek * audiences.size());
    }

    /** The bonus second Library session (user's call, 2026-09-15): Library's required quota is one
     *  2-period session a week ({@link #fillLibraryGaps}). An audience that already holds that quota
     *  gets one more only while its cohort still has at least {@code
     *  timetable.library_extra_session_min_free_periods} free periods a week ({@link
     *  #weeklyFreePeriods}) after curriculum, Library and Sports, so the extra-hours filler after it
     *  still has plenty to share. A bonus that isn't placed is not a shortfall: no note, no warning. */
    private List<Placement> fillExtraLibrarySession(Long cohortId, TermInstance term, List<Period> periods,
                                                    Map<DayOfWeek, Integer> dayLoad, List<SkeletonCellResponse> existingCells,
                                                    List<Placement> placedThisRun, boolean saturdayOpen) {
        List<Placement> filled = new ArrayList<>();
        Subject librarySubject = subjectRepository.findByCode(LIBRARY_SUBJECT_CODE).orElse(null);
        List<Classroom> libraryClassrooms = classroomRepository
            .findByIsActiveTrueAndRoom_PurposeCategory_CodeOrderByNameAsc(RoomPurposeCategoryCode.LIBRARY);
        if (librarySubject == null || libraryClassrooms.isEmpty()) {
            // fillLibraryGaps has already warned about the missing Library classroom.
            return filled;
        }
        int baseSessions = resolveLibraryConfigInt(CONFIG_LIBRARY_SESSIONS_PER_WEEK, DEFAULT_LIBRARY_SESSIONS_PER_WEEK);
        int minFreePeriods = resolveLibraryConfigInt(CONFIG_LIBRARY_EXTRA_SESSION_MIN_FREE_PERIODS,
            DEFAULT_LIBRARY_EXTRA_SESSION_MIN_FREE_PERIODS);
        List<List<Period>> candidateBlocks = contiguousPeriodBlocks(periods,
            resolveLibraryConfigInt(CONFIG_LIBRARY_BLOCK_SIZE_PERIODS, DEFAULT_LIBRARY_BLOCK_SIZE_PERIODS));
        List<DayOfWeek> weekdays = saturdayOpen
            ? List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY, DayOfWeek.SATURDAY)
            : List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY);

        List<CohortSection> activeSections = timetableSkeletonService.resolveActiveSections(cohortId, term.getId());
        List<CohortSection> audiences = activeSections.isEmpty() ? Arrays.asList((CohortSection) null) : activeSections;
        for (CohortSection section : audiences) {
            Long sectionId = section != null ? section.getId() : null;
            // An audience still short of its required quota couldn't find a Library slot at all; the
            // bonus is only ever the session after the quota.
            if (libraryDaysUsed(sectionId, existingCells, placedThisRun).size() < baseSessions) {
                continue;
            }
            // Re-measured per audience: each earlier section's bonus block uses up cohort time.
            if (weeklyFreePeriods(cohortId, term, periods, weekdays) + CAPACITY_EPSILON < minFreePeriods) {
                continue;
            }
            placeLibraryBlocks(cohortId, term, section, baseSessions + 1, weekdays, candidateBlocks,
                librarySubject, libraryClassrooms, dayLoad, existingCells, placedThisRun, filled);
        }
        return filled;
    }

    /** Places Library blocks for one audience (a section, or null for the whole cohort) on distinct
     *  days, least-loaded day first, until it holds {@code target} of them, and returns how many it
     *  holds. Each block needs a genuinely free, unblocked, non-clinical-duty run of contiguous
     *  periods with a free Library classroom. Shared by {@link #fillLibraryGaps} (the weekly quota)
     *  and {@link #fillExtraLibrarySession} (the bonus session). */
    private int placeLibraryBlocks(Long cohortId, TermInstance term, CohortSection section, int target,
                                   List<DayOfWeek> weekdays, List<List<Period>> candidateBlocks, Subject librarySubject,
                                   List<Classroom> libraryClassrooms, Map<DayOfWeek, Integer> dayLoad,
                                   List<SkeletonCellResponse> existingCells, List<Placement> placedThisRun,
                                   List<Placement> filled) {
        Set<DayOfWeek> daysAlreadyUsed = libraryDaysUsed(section != null ? section.getId() : null, existingCells, placedThisRun);
        List<DayOfWeek> orderedDays = weekdays.stream()
            .sorted(Comparator.comparingInt(d -> dayLoad.getOrDefault(d, 0)))
            .toList();
        int placedForAudience = daysAlreadyUsed.size();
        for (DayOfWeek day : orderedDays) {
            if (placedForAudience >= target) {
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
        return placedForAudience;
    }

    /** Days this audience already has Library exposure of any kind: pre-run cells (a published or
     *  pinned one survives the rebuild) plus blocks this run placed before the caller. Counts a
     *  batch-scoped idle-batch fallback cell (see {@link #saveIdleBatchLibraryCell}, Phase 1.5) the
     *  same as a whole-section block — a batch already sent to Library while its sibling was in
     *  Lab/Clinical has genuinely had its Library time for that day, so this quota (targeting
     *  {@code timetable.library_sessions_per_week} PER CohortSection, not per batch) must not go on
     *  to add a second, redundant whole-cohort block on top of it. Pre-run cells never distinguished
     *  batch-scoped from whole-section here (no {@code batchId} filter below either); previously this
     *  method's own same-run half of the check applied that filter anyway, so a batch's idle-filler
     *  Library day this same run wasn't recognized as satisfying the quota and a second, whole-cohort
     *  session got added on top of it (fixed 2026-09-23, reported by user: cohort was ending up with
     *  Library filler on top of Library filler, well beyond the intended weekly quota). The run's own
     *  count matters too: a Phase 2 backtrack that bumps an idle-batch Library cell relocates it as a
     *  section-level block (#tryRePlaceBumpedLibrarySession, batchId null), and counting only pre-run
     *  cells made the pass report "reduced to 0 of 2" for a cohort that visibly had one (OC-227 local
     *  run). */
    private static Set<DayOfWeek> libraryDaysUsed(Long sectionId, List<SkeletonCellResponse> existingCells,
                                                  List<Placement> placedThisRun) {
        Set<DayOfWeek> days = existingCells.stream()
            .filter(c -> c.sessionType() == ClassSessionType.LIBRARY)
            .filter(c -> Objects.equals(c.cohortSectionId(), sectionId))
            .map(SkeletonCellResponse::dayOfWeek)
            .collect(java.util.stream.Collectors.toCollection(HashSet::new));
        placedThisRun.stream()
            .filter(p -> p.sessionType() == ClassSessionType.LIBRARY
                && Objects.equals(p.cohortSectionId(), sectionId))
            .map(Placement::dayOfWeek)
            .forEach(days::add);
        return days;
    }

    /** New capped "genuine Self-Study" tier (user's rule hierarchy: leftover periods go
     *  Library -> Sports -> Self-Study(capped, {@code timetable.self_study_sessions_per_week},
     *  default 1 session/week of a {@code timetable.self_study_block_size_periods}-period block) ->
     *  only THEN the uncapped bonus-Theory filler ({@link #fillSelfStudyGaps}). Distinct from that
     *  filler (which excludes the real Self-Study subject by name, see {@link #isSelfStudySubject})
     *  and from the per-slot idle-batch Self-Study fallback ({@link #saveIdleBatchSelfStudyCell},
     *  scoped to one idle batch at one exact slot): this places a real, WHOLE-SECTION Self-Study
     *  session using the same already-met-budget row {@link #resolveSelfStudyRowForFallback} finds,
     *  same least-loaded-day search and {@code theoryStillOwedRuns} curriculum-yields gate as {@link
     *  #fillLibraryGaps}/{@link #fillSportsGaps}. Mutates {@code placedThisRun}/{@code dayLoad}
     *  directly (no dedicated outcome record) -- same void, in-place style as {@link
     *  #fillIdleBatchGaps}, since this filler's placements need no separate run-report line: a
     *  session it can't place simply falls through to the always-uncapped filler right after it. */
    private void fillGenuineSelfStudyGaps(Long cohortId, TermInstance term, List<Period> periods,
            Map<DayOfWeek, Integer> dayLoad, SkeletonBuilderResponse skeleton, List<SkeletonCellResponse> existingCells,
            List<Placement> placedThisRun, boolean saturdayOpen, Map<String, Integer> theoryStillOwedRuns,
            TermDemandAggregation termDemand) {
        // Self-Study yields to curriculum exactly like Library/Sports (OC-227 principle) -- every
        // offering's real required hours come first.
        if (theoryStillOwedRuns.values().stream().anyMatch(owed -> owed > 0)) {
            return;
        }
        List<DayOfWeek> weekdays = saturdayOpen
            ? List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY, DayOfWeek.SATURDAY)
            : List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY);
        int sessionsPerWeek = resolveLibraryConfigInt(CONFIG_SELF_STUDY_SESSIONS_PER_WEEK, DEFAULT_SELF_STUDY_SESSIONS_PER_WEEK);
        int blockSize = resolveLibraryConfigInt(CONFIG_SELF_STUDY_BLOCK_SIZE_PERIODS, DEFAULT_SELF_STUDY_BLOCK_SIZE_PERIODS);
        List<List<Period>> candidateBlocks = contiguousPeriodBlocks(periods, blockSize);

        List<CohortSection> activeSections = timetableSkeletonService.resolveActiveSections(cohortId, term.getId());
        List<CohortSection> audiences = activeSections.isEmpty() ? Arrays.asList((CohortSection) null) : activeSections;

        for (CohortSection section : audiences) {
            SelfStudyRow selfStudyRow = resolveSelfStudyRowForFallback(cohortId, section, skeleton, termDemand);
            if (selfStudyRow == null) {
                continue; // no already-met Self-Study curriculum offering for this audience -- nothing to add
            }
            Long sectionId = section != null ? section.getId() : null;
            Set<DayOfWeek> daysAlreadyUsed = selfStudyDaysUsed(selfStudyRow.offering().getId(), sectionId, existingCells, placedThisRun);
            List<DayOfWeek> orderedDays = weekdays.stream()
                .sorted(Comparator.comparingInt(d -> dayLoad.getOrDefault(d, 0)))
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
                    List<Long> ids = timetableSkeletonService.saveGenuineSelfStudyBlockCells(
                        selfStudyRow.offering(), term, day, block, section);
                    Long staffedBy = tryStaffWithFallback(ids.get(0), selfStudyRow.candidateFacultyIds());
                    if (staffedBy == null) {
                        ids.forEach(timetableSkeletonService::removeCell);
                        continue;
                    }
                    dayLoad.merge(day, 1, Integer::sum);
                    placedThisRun.add(new Placement(ids.get(0), selfStudyRow.offering().getId(), ClassSessionType.THEORY,
                        null, sectionId, staffedBy, selfStudyRow.subjectName(),
                        section != null ? section.getSectionLabel() : "Whole cohort", day,
                        block.stream().map(Period::getId).toList()));
                    daysAlreadyUsed.add(day);
                    placedForAudience++;
                    break;
                }
            }
        }
    }

    /** Days this audience already has GENUINE Self-Study exposure (this specific offering), for the
     *  capped tier above -- mirrors {@link #libraryDaysUsed}'s exact reasoning: counts a batch-scoped
     *  idle-batch Self-Study fallback cell (see {@link #saveIdleBatchSelfStudyCell}) the same as a
     *  whole-section block, so a batch already sent to Self-Study while its sibling was in Lab/
     *  Clinical doesn't also get a second, redundant whole-section block piled on the same day. */
    private static Set<DayOfWeek> selfStudyDaysUsed(Long offeringId, Long sectionId,
            List<SkeletonCellResponse> existingCells, List<Placement> placedThisRun) {
        Set<DayOfWeek> days = existingCells.stream()
            .filter(c -> c.sessionType() == ClassSessionType.THEORY)
            .filter(c -> Objects.equals(c.courseOfferingId(), offeringId))
            .filter(c -> Objects.equals(c.cohortSectionId(), sectionId))
            .map(SkeletonCellResponse::dayOfWeek)
            .collect(java.util.stream.Collectors.toCollection(HashSet::new));
        placedThisRun.stream()
            .filter(p -> p.sessionType() == ClassSessionType.THEORY && Objects.equals(p.courseOfferingId(), offeringId)
                && Objects.equals(p.cohortSectionId(), sectionId))
            .map(Placement::dayOfWeek)
            .forEach(days::add);
        return days;
    }

    /** Periods this cohort still has free in a week, skipping blocked periods and clinical duty like
     *  placement does. Measured in weekday-equivalents per the term-total hours rule: a free Saturday
     *  period counts only for the share of weeks that Saturday really runs (6 of 26 on a
     *  1st-Saturday-only term), so a mostly-idle Saturday can't earn the bonus Library session. */
    private double weeklyFreePeriods(Long cohortId, TermInstance term, List<Period> periods, List<DayOfWeek> days) {
        int weeks = Math.max(1, CurriculumHoursCalculator.weeksInTerm(term));
        List<ClinicalShiftWindow> dutyWindows = clinicalShiftGroupService.resolveActiveWindowsForCohort(cohortId, term.getId());
        double free = 0;
        for (DayOfWeek day : days) {
            double weight = day == DayOfWeek.SATURDAY
                ? (double) WorkingSaturdayCalculator.runsInTerm(day, term, weeks) / weeks
                : 1.0;
            for (Period period : periods) {
                boolean unusable = blockedPeriodChecker.blockReason(day, period.getStartTime(), period.getEndTime(), term).isPresent()
                    || dutyWindows.stream().anyMatch(w -> w.dayOfWeek() == day && w.overlaps(period.getStartTime(), period.getEndTime()))
                    || !timetableSkeletonService.isSlotFreeForCohort(cohortId, term.getId(), day, period.getId());
                if (!unusable) {
                    free += weight;
                }
            }
        }
        return free;
    }

    /** {@code setupGap} names what's missing (no Sports subject/room, or no PE faculty on the Sports
     *  subject's eligible-faculty list) when nothing could even be attempted; null otherwise. */
    private record SportsGapFillOutcome(List<Placement> filled, int unfillableSessions, int targetSessions, String setupGap) {}

    /** Fixed-quota Sports pass, run right after {@link #fillLibraryGaps} for every cohort: places
     *  {@code timetable.sports_sessions_per_week} blocks of {@code timetable.sports_block_size_periods}
     *  contiguous periods per active CohortSection (or once for the whole cohort), each in a classroom
     *  whose Room is tagged SPORTS and staffed by a PE faculty.
     *
     *  <p>PE faculty are exactly the faculty ticked on the {@link #SPORTS_SUBJECT_CODE} subject's
     *  eligible-faculty list -- deliberately not {@link FacultyEligibility#eligibleFaculty}, which
     *  treats a subject with no speciality (Sports has none) as open to every faculty member.
     *
     *  <p>Games go at the end of the day so students don't return to class afterwards: the first
     *  pass only tries a block ending at the day's final period, the second any free block, latest
     *  first. Like Library, Sports yields to curriculum -- whatever can't fit is a neutral note
     *  ({@link #sportsInfoNotes}), never an unplaced warning. */
    private SportsGapFillOutcome fillSportsGaps(Long cohortId, TermInstance term, List<Period> periods,
                                                Map<DayOfWeek, Integer> dayLoad, List<SkeletonCellResponse> existingCells,
                                                TermDemandAggregation termDemand, boolean saturdayOpen,
                                                Map<String, Integer> theoryStillOwedRuns) {
        List<Placement> filled = new ArrayList<>();
        int sessionsPerWeek = resolveLibraryConfigInt(CONFIG_SPORTS_SESSIONS_PER_WEEK, DEFAULT_SPORTS_SESSIONS_PER_WEEK);
        List<CohortSection> activeSections = timetableSkeletonService.resolveActiveSections(cohortId, term.getId());
        List<CohortSection> audiences = activeSections.isEmpty() ? Arrays.asList((CohortSection) null) : activeSections;
        int targetSessions = sessionsPerWeek * audiences.size();

        // Sports yields to curriculum exactly like Library's required quota, just above -- a cohort
        // with any real Theory shortfall left (mandatory or CO_CURRICULAR) keeps every remaining
        // free period until that's closed.
        if (theoryStillOwedRuns.values().stream().anyMatch(owed -> owed > 0)) {
            return new SportsGapFillOutcome(filled, targetSessions, targetSessions, null);
        }

        Subject sportsSubject = subjectRepository.findByCode(SPORTS_SUBJECT_CODE).orElse(null);
        List<Classroom> sportsVenues = classroomRepository
            .findByIsActiveTrueAndRoom_PurposeCategory_CodeOrderByNameAsc(RoomPurposeCategoryCode.SPORTS);
        if (sportsSubject == null || sportsVenues.isEmpty()) {
            return new SportsGapFillOutcome(filled, targetSessions, targetSessions,
                "no room is tagged Sports & Recreation (a Classroom linked to a Room with that Purpose Category)");
        }
        List<Long> peFacultyIds = rankedPeFacultyIds(sportsSubject, termDemand);
        if (peFacultyIds.isEmpty()) {
            return new SportsGapFillOutcome(filled, targetSessions, targetSessions,
                "no active PE faculty with spare capacity is on the Sports subject's eligible-faculty list");
        }

        List<DayOfWeek> weekdays = saturdayOpen
            ? List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY, DayOfWeek.SATURDAY)
            : List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY);
        int blockSize = resolveLibraryConfigInt(CONFIG_SPORTS_BLOCK_SIZE_PERIODS, DEFAULT_SPORTS_BLOCK_SIZE_PERIODS);
        List<List<Period>> latestFirst = contiguousPeriodBlocks(periods, blockSize).stream()
            .sorted(Comparator.comparing((List<Period> b) -> b.get(b.size() - 1).getEndTime()).reversed())
            .toList();
        LocalTime dayEnd = periods.isEmpty() ? null : periods.get(periods.size() - 1).getEndTime();

        int unfillableSessions = 0;
        for (CohortSection section : audiences) {
            Long sectionId = section != null ? section.getId() : null;
            Set<DayOfWeek> daysUsed = existingCells.stream()
                .filter(c -> c.sessionType() == ClassSessionType.SPORTS && Objects.equals(c.cohortSectionId(), sectionId))
                .map(SkeletonCellResponse::dayOfWeek)
                .collect(Collectors.toCollection(HashSet::new));
            int placedForAudience = daysUsed.size();
            List<DayOfWeek> orderedDays = weekdays.stream()
                .sorted(Comparator.comparingInt(d -> dayLoad.getOrDefault(d, 0)))
                .toList();
            for (int pass = 0; pass < 2 && placedForAudience < sessionsPerWeek; pass++) {
                boolean lastPeriodsOnly = pass == 0;
                for (DayOfWeek day : orderedDays) {
                    if (placedForAudience >= sessionsPerWeek) {
                        break;
                    }
                    if (daysUsed.contains(day)) {
                        continue;
                    }
                    for (List<Period> block : latestFirst) {
                        if (lastPeriodsOnly && !block.get(block.size() - 1).getEndTime().equals(dayEnd)) {
                            continue;
                        }
                        Placement placed = tryPlaceSportsBlock(cohortId, term, day, block, section, sportsSubject,
                            sportsVenues, peFacultyIds);
                        if (placed != null) {
                            filled.add(placed);
                            dayLoad.merge(day, 1, Integer::sum);
                            daysUsed.add(day);
                            placedForAudience++;
                            break;
                        }
                    }
                }
            }
            unfillableSessions += Math.max(0, sessionsPerWeek - placedForAudience);
        }
        return new SportsGapFillOutcome(filled, unfillableSessions, targetSessions, null);
    }

    /** One Sports block at one slot, or null if the slot, the Sports room or every PE faculty is
     *  unavailable there. The block is saved first and then staffed through {@code staffCell}, so
     *  the PE faculty gets the same conflict and workload-cap checks as any taught session; an
     *  unstaffable block is removed again before trying the next slot. */
    private Placement tryPlaceSportsBlock(Long cohortId, TermInstance term, DayOfWeek day, List<Period> block,
                                          CohortSection section, Subject sportsSubject, List<Classroom> sportsVenues,
                                          List<Long> peFacultyIds) {
        boolean blocked = block.stream().anyMatch(p ->
            blockedPeriodChecker.blockReason(day, p.getStartTime(), p.getEndTime(), term).isPresent());
        if (blocked || overlapsClinicalShift(cohortId, term, day, block)) {
            return null;
        }
        boolean slotFree = block.stream().allMatch(p ->
            timetableSkeletonService.isSlotFreeForCohort(cohortId, term.getId(), day, p.getId()));
        if (!slotFree) {
            return null;
        }
        Classroom venue = firstFreeClassroom(ClassSessionType.SPORTS, sportsVenues, term.getId(), day, block);
        if (venue == null) {
            return null;
        }
        List<ClassSchedule> saved = timetableSkeletonService.saveSportsBlockCells(sportsSubject, term, day, block, section, venue);
        Long staffedBy = tryStaffWithFallback(saved.get(0).getId(), peFacultyIds);
        if (staffedBy == null) {
            timetableSkeletonService.removeCell(saved.get(0).getId()); // unstaffed draft: removes the whole block
            return null;
        }
        return new Placement(saved.get(0).getId(), null, ClassSessionType.SPORTS, null,
            section != null ? section.getId() : null, staffedBy, "Sports",
            section != null ? section.getSectionLabel() : "Whole cohort",
            day, block.stream().map(Period::getId).toList());
    }

    /** Active faculty on the Sports subject's own eligible-faculty list with spare term capacity,
     *  ranked the same least-remaining-first way as {@link #rankedFallbackCandidates}. */
    private List<Long> rankedPeFacultyIds(Subject sportsSubject, TermDemandAggregation termDemand) {
        return facultyRepository.findByStatus(FacultyStatus.ACTIVE).stream()
            .filter(f -> FacultyEligibility.viaEligibleList(sportsSubject, f))
            .map(f -> candidateDto(sportsSubject, f, termDemand, false, 0))
            .filter(c -> !c.overCapacity())
            .sorted(Comparator.comparingDouble(c -> "NONE".equals(c.capacityTier()) ? Double.MAX_VALUE : c.remainingHours()))
            .map(EligibleFacultyCandidateDto::facultyId)
            .toList();
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
        return firstFreeClassroom(ClassSessionType.LIBRARY, classrooms, termInstanceId, day, block);
    }

    private Classroom firstFreeClassroom(ClassSessionType sessionType, List<Classroom> classrooms, Long termInstanceId,
                                         DayOfWeek day, List<Period> block) {
        LocalTime start = block.get(0).getStartTime();
        LocalTime end = block.get(block.size() - 1).getEndTime();
        for (Classroom classroom : classrooms) {
            boolean free = timetableStaffingService.checkRoomFree(sessionType, classroom.getId(),
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
    record IdleFillResult(List<Placement> filled, int unfillableCount) {}

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
            List<Period> periods, Map<DayOfWeek, Integer> dayLoad, boolean saturdayOpen,
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
                p.dayOfWeek(), p.periodIds(), librarySubject, libraryClassrooms, cohortId, skeleton, termDemand,
                periods, dayLoad, saturdayOpen);
            placedThisCohortRun.addAll(result.filled());
            unfillable += result.unfillableCount();
        }
        if (unfillable > 0) {
            unplacedForCohort.add(new AutoPlaceUnplacedItem("Idle batch fallback", ClassSessionType.LIBRARY, null,
                unfillable + " idle batch instance(s) could not be given a Library/Self-Study fallback -- no free "
                    + "Library classroom and no eligible Self-Study faculty available for that slot", null, false, true));
        }
    }

    /** Confirmed fallback algorithm: if a single free Library classroom's capacity covers the SUM
     *  of every idle batch's real headcount, all idle batches go there together (sharing one room
     *  concurrently — each still gets its own {@link ClassSchedule} row). Otherwise, give the
     *  largest idle batch a Library seat (if any free room fits it alone) and put the rest into
     *  unsupervised classroom Self-Study, staffed the same way {@link #fillSelfStudyGaps} staffs
     *  its own rows (a THEORY row needs a real faculty to ever be publishable — see {@link
     *  #saveIdleBatchSelfStudyCell}). Every batch is placed as a whole unit — never split.
     *  Package-private (not private) so this regression can be verified directly. */
    IdleFillResult placeIdleBatchFallback(List<Batch> idleBatches, CohortSection section, TermInstance term,
            DayOfWeek day, List<Long> periodIds, Subject librarySubject, List<Classroom> libraryClassrooms,
            Long cohortId, SkeletonBuilderResponse skeleton, TermDemandAggregation termDemand,
            List<Period> periods, Map<DayOfWeek, Integer> dayLoad, boolean saturdayOpen) {
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
            // The Library room isn't free for this batch RIGHT NOW (either no room fit the whole
            // group, or this specific batch lost out to a bigger sibling for the one room that did
            // fit at this exact slot) -- before settling for Self-Study, see whether this batch
            // (which may be genuinely free on a day its sibling isn't) can still get its Library
            // fallback somewhere else in the week, the same exhaustive way the baseline weekly quota
            // would search for it. Only Self-Study's own slot stays pinned to `day`/`block`: it needs
            // no dedicated room, so there's no reason to move it off the moment the sibling is
            // actually occupying the shared Lab/Clinical venue.
            Placement elsewhereInWeek = placeIdleBatchLibraryElsewhere(b, section, cohortId, term, periods, dayLoad,
                saturdayOpen, day, librarySubject, libraryClassrooms);
            if (elsewhereInWeek != null) {
                filled.add(elsewhereInWeek);
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

    /** Idle-batch fallback's own week-wide search: when the exact triggering slot has no free Library
     *  room for {@code batch}, scans the REST of the week the same way {@link #placeLibraryBlocks}
     *  does for the baseline weekly quota -- least-loaded day first, every contiguous period block,
     *  respecting {@code blockedPeriodChecker}/clinical-shift/room-capacity -- instead of giving up
     *  the instant the one triggering slot fails. Unlike {@link
     *  TimetableSkeletonService#isSlotFreeForCohort} (built for the ordinary, unsplit placement,
     *  where any sibling batch's LAB/CLINICAL row correctly occupies the whole cohort/offering), this
     *  checks freeness for {@code batch} SPECIFICALLY (see {@link #isSlotFreeForBatch}): a split
     *  batch is idle whenever IT personally has nothing on, regardless of what its sibling is doing
     *  elsewhere in the week -- reusing the cohort-wide check here would wrongly treat every day the
     *  sibling has its own Lab/Clinical turn as unavailable to this batch too.
     *
     *  <p>Landing on a different day than the triggering slot is safe against the section's weekly
     *  Library quota: {@link #libraryDaysUsed} already counts a batch-scoped idle-fallback cell,
     *  wherever it lands, as satisfying that day for the whole section, and the live {@link
     *  TimetableStaffingService#checkRoomFree} check inside {@link #firstFreeLibraryClassroomForCapacity}
     *  means any later pass (this cohort's own baseline {@link #fillLibraryGaps}, another cohort's
     *  idle-batch fallback, anything) will correctly see the room as taken from here on.
     *
     *  @param excludeDay the slot already tried by the caller at the triggering block -- skipped here
     *      to avoid repeating a check just performed with the same answer.
     *  @return the placed cell, or null if genuinely no free Library slot exists anywhere else in the
     *      week for this batch either -- only then does the caller fall through to Self-Study. */
    private Placement placeIdleBatchLibraryElsewhere(Batch batch, CohortSection section, Long cohortId, TermInstance term,
            List<Period> periods, Map<DayOfWeek, Integer> dayLoad, boolean saturdayOpen, DayOfWeek excludeDay,
            Subject librarySubject, List<Classroom> libraryClassrooms) {
        if (librarySubject == null || libraryClassrooms.isEmpty()) {
            return null;
        }
        List<DayOfWeek> weekdays = saturdayOpen
            ? List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY, DayOfWeek.SATURDAY)
            : List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY);
        List<DayOfWeek> orderedDays = weekdays.stream()
            .filter(d -> d != excludeDay)
            .sorted(Comparator.comparingInt(d -> dayLoad.getOrDefault(d, 0)))
            .toList();
        List<List<Period>> candidateBlocks = contiguousPeriodBlocks(periods,
            resolveLibraryConfigInt(CONFIG_LIBRARY_BLOCK_SIZE_PERIODS, DEFAULT_LIBRARY_BLOCK_SIZE_PERIODS));

        List<ClassSchedule> batchOwnCells = classScheduleRepository.findByBatchIdInAndIsActiveTrue(List.of(batch.getId()));
        List<ClassSchedule> sectionWideCells = section != null
            ? classScheduleRepository.findByCohortSectionIdInAndIsActiveTrue(List.of(section.getId()))
            : List.of();
        int headcount = realBatchHeadcount(batch);

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
                boolean batchFree = block.stream().allMatch(p ->
                    isSlotFreeForBatch(batchOwnCells, sectionWideCells, day, p.getId()));
                if (!batchFree) {
                    continue;
                }
                Classroom room = firstFreeLibraryClassroomForCapacity(libraryClassrooms, term.getId(), day, block, headcount);
                if (room == null) {
                    continue;
                }
                return saveIdleBatchLibraryCell(librarySubject, term, day, block, section, room, batch);
            }
        }
        return null;
    }

    /** Whether {@code batch} itself — not the whole cohort/offering — has any active session at this
     *  day/period: its own batch-scoped cells (LAB/CLINICAL, where a sibling's row never counts
     *  against it — see {@code batchOwnCells}' own fetch, filtered to this one batch's id) plus its
     *  CohortSection's whole-section cells (THEORY/LIBRARY/SPORTS, which bind every batch in the
     *  section regardless of the Lab/Clinical split, since {@code class_schedules.cohort_section_id}
     *  is only ever set on those session types — see {@code TimetableSkeletonService#scopeKeyForCell}).
     *  Used instead of {@link TimetableSkeletonService#isSlotFreeForCohort} by {@link
     *  #placeIdleBatchLibraryElsewhere}, which needs per-batch freeness, not whole-cohort freeness. */
    private static boolean isSlotFreeForBatch(List<ClassSchedule> batchOwnCells, List<ClassSchedule> sectionWideCells,
                                              DayOfWeek day, Long periodId) {
        boolean batchBusy = batchOwnCells.stream().anyMatch(cs ->
            cs.getDayOfWeek() == day && cs.getPeriod() != null && cs.getPeriod().getId().equals(periodId));
        if (batchBusy) {
            return false;
        }
        return sectionWideCells.stream().noneMatch(cs ->
            cs.getDayOfWeek() == day && cs.getPeriod() != null && cs.getPeriod().getId().equals(periodId));
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
                if (!budget.isMet()) {
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
     *  disappearing from the report. Returns the day {@code row} landed on, or null when the
     *  backtrack didn't place it — the caller credits that day's real runs to the row. */
    private DayOfWeek attemptBacktrack(Long cohortId, ShortfallRow row, TermInstance term, List<Period> periods,
                                      Set<DayOfWeek> daysUsed, List<Placement> placedThisCohortRun,
                                      List<AutoPlaceUnplacedItem> unplacedForCohort, int blockSize,
                                      Map<DayOfWeek, Integer> dayLoad, TermDemandAggregation termDemand,
                                      List<FacultySubstitutionEvent> facultySubstitutionEvents,
                                      Map<String, Integer> theoryStillOwedRuns) {
        for (int idx = placedThisCohortRun.size() - 1; idx >= 0; idx--) {
            Placement bumped = placedThisCohortRun.get(idx);
            if (bumped.sameRowAs(row)) {
                continue;
            }
            // Mandatory always beats advisory (see SHORTFALL_ROW_ORDER) -- an advisory row (e.g.
            // Self-Study) must never evict an already-placed mandatory session just to find itself
            // a home. A mandatory row may still bump anything, advisory included.
            if (isAdvisoryRow(row) && !isAdvisoryOfferingId(bumped.courseOfferingId())) {
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
                restoreBumpedOrReportUnplaced(bumped, cohortId, placedThisCohortRun, unplacedForCohort, dayLoad, term, periods,
                    termDemand, facultySubstitutionEvents, theoryStillOwedRuns);
                return null;
            }
            dayLoad.merge(retry.dayPlaced(), blockSize, Integer::sum);
            daysUsed.add(retry.dayPlaced());
            placedThisCohortRun.add(new Placement(retry.cellId(), row.offering().getId(), row.budget().sessionType(),
                row.budget().batchId(), row.budget().cohortSectionId(), retry.facultyId(), row.subjectName(),
                occupantLabel(row.budget()), retry.dayPlaced(), retry.periodIds()));
            restoreBumpedOrReportUnplaced(bumped, cohortId, placedThisCohortRun, unplacedForCohort, dayLoad, term, periods,
                termDemand, facultySubstitutionEvents, theoryStillOwedRuns);
            // Whether or not the restore worked, `row` is now placed and the total count never
            // dropped below what it was before this attempt (net zero at worst, a genuine swap).
            return retry.dayPlaced();
        }
        return null;
    }

    /** Shared guard around every {@code forceRemoveCell} call in this class's backtrack path
     *  ({@link #attemptBacktrack}) — it bumps a {@link Placement} this run recorded earlier and
     *  expects its cell to still be real, but production has shown
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

    /** Shared tail of both {@link #attemptBacktrack} outcomes: try to put {@code bumped} back
     *  exactly where it was (already removed from
     *  {@code placedThisCohortRun} by the caller); if that exact slot is gone, fall back to a full
     *  day/period search — the same one a freshly-failing row gets via {@link #tryPlaceAndStaff} —
     *  before giving up. Restoring to the exact original slot only was too narrow: a bump can lose a
     *  session even when a different day/period is genuinely free for it, which is exactly what a
     *  real cohort run surfaced (several Theory rows reported unplaced with a free Monday-Friday
     *  slot still available elsewhere in the week). Only once both the exact restore and the full
     *  search fail is it recorded as a fresh unplaced item, instead of letting it silently vanish
     *  from the report.
     *
     * <p>2026-09-18 fix: the day/period retry below used to try ONLY {@code bumped}'s own original
     *  bound faculty ({@code List.of(bumped.facultyId())}), never a fallback -- deliberately, per the
     *  comment this replaced ("restores a displaced row back to its own original faculty, never a
     *  substitute"). In practice this meant a session {@code attemptBacktrack} legitimately freed a
     *  genuinely open (day, period) for -- just one where {@code bumped}'s own faculty happens to
     *  already be teaching something else -- was reported unplaced outright, even though the exact
     *  same {@link #rankedFallbackCandidates} pool a fresh {@link ShortfallRow} gets to use was sitting
     *  right there unused. A real 2026-2027 ODD run surfaced exactly this: two Theory rows reported
     *  "displaced during a backtrack attempt... could not be placed... at any other free day/period"
     *  while the cohort's own report showed hundreds of term-wide spare hours elsewhere. Now tries the
     *  same ranked fallback pool as a fresh row would, and records a {@link FacultySubstitutionEvent}
     *  (surfaced to the admin as a confirmable tip, same as any other fallback placement) whenever the
     *  winning faculty isn't {@code bumped}'s own original one.
     *
     * <p>2026-09-18 second fix: even with the fallback pool above, a bumped session can still
     *  genuinely have nowhere left to go (a real Monday-Friday room ceiling, not a faculty gap) --
     *  and when {@code bumped} is THEORY, that permanent loss used to vanish from {@code
     *  theoryStillOwedRuns} entirely. That map is only ever written from {@code placeShortfallRow}'s
     *  own return value for the row CURRENTLY being placed; a session this same phase already placed
     *  successfully earlier, then lost here to a LATER row's backtrack, was never that later row's
     *  own {@code ShortfallRow}, so nothing ever recorded the gap it just reopened. The Library bonus
     *  gate and {@code fillSelfStudyGaps}' shortfall-first tier both read {@code theoryStillOwedRuns}
     *  to decide whether this cohort still has a real Theory gap -- reading it as "0" here let a
     *  cohort's bonus second Library session go out immediately after this same run had just
     *  re-opened a genuine 20h/week Theory hole, which is exactly what a real 2026-2027 ODD BSc
     *  Nursing run did, unchanged across three separate re-runs, because the gate could never see
     *  what this method itself only found out about after the gate had already been read. */
    // Package-private (not private) so the no-longer-relocates-a-bumped-Library-session regression
    // can be verified directly, matching #tryRePlaceBumpedLibrarySession's own visibility.
    void restoreBumpedOrReportUnplaced(Placement bumped, Long cohortId, List<Placement> placedThisCohortRun,
                                        List<AutoPlaceUnplacedItem> unplacedForCohort, Map<DayOfWeek, Integer> dayLoad,
                                        TermInstance term, List<Period> periods, TermDemandAggregation termDemand,
                                        List<FacultySubstitutionEvent> facultySubstitutionEvents,
                                        Map<String, Integer> theoryStillOwedRuns) {
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
            // bumped's own faculty first, then the same ranked fallback pool a fresh ShortfallRow gets
            // (see this method's javadoc for why a fallback-blind retry used to strand real sessions).
            List<EligibleFacultyCandidateDto> fallbackCandidates = rankedFallbackCandidates(offering, bumped.facultyId(), termDemand);
            List<Long> restoreCandidateFacultyIds = new ArrayList<>();
            restoreCandidateFacultyIds.add(bumped.facultyId());
            restoreCandidateFacultyIds.addAll(fallbackCandidates.stream().map(EligibleFacultyCandidateDto::facultyId).toList());
            Map<Long, EligibleFacultyCandidateDto> fallbackCandidatesById = fallbackCandidates.stream()
                .collect(Collectors.toMap(EligibleFacultyCandidateDto::facultyId, c -> c, (a, b) -> a));
            // Same order as placeShortfallRow: a fresh working day, then a same-day double on a day
            // this row already uses once.
            Set<DayOfWeek> daysUsedTwice = placedThisCohortRun.stream()
                .filter(p -> sameRow(p, bumped))
                .collect(Collectors.groupingBy(Placement::dayOfWeek, Collectors.counting()))
                .entrySet().stream()
                .filter(e -> e.getValue() >= 2)
                .map(Map.Entry::getKey)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(DayOfWeek.class)));
            // Never onto a day that runs less often than the one it was bumped from -- a weekday
            // session moved to a first-Saturday-only Saturday runs 6 times instead of 26, quietly
            // costing its subject hours it already had.
            int weeks = CurriculumHoursCalculator.weeksInTerm(term);
            int originalRuns = WorkingSaturdayCalculator.runsInTerm(bumped.dayOfWeek(), term, weeks);
            Set<DayOfWeek> fewerRunDays = Arrays.stream(DayOfWeek.values())
                .filter(d -> WorkingSaturdayCalculator.runsInTerm(d, term, weeks) < originalRuns)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(DayOfWeek.class)));
            Set<DayOfWeek> freshExcluded = EnumSet.noneOf(DayOfWeek.class);
            freshExcluded.addAll(fewerRunDays);
            freshExcluded.addAll(daysUsed);
            PlacementAttempt retry = tryPlaceAndStaff(cohortId, offering, minimalBudget, restoreCandidateFacultyIds,
                term, periods, freshExcluded, bumped.periodIds().size(), dayLoad, Set.of());
            if (retry.dayPlaced() == null) {
                Set<DayOfWeek> doubleExcluded = daysOtherThanUsedOnce(daysUsed, daysUsedTwice);
                doubleExcluded.addAll(fewerRunDays);
                retry = tryPlaceAndStaff(cohortId, offering, minimalBudget, restoreCandidateFacultyIds,
                    term, periods, doubleExcluded, bumped.periodIds().size(), dayLoad, Set.of());
            }
            if (retry.dayPlaced() != null) {
                dayLoad.merge(retry.dayPlaced(), bumped.periodIds().size(), Integer::sum);
                placedThisCohortRun.add(new Placement(retry.cellId(), bumped.courseOfferingId(), bumped.sessionType(),
                    bumped.batchId(), bumped.cohortSectionId(), retry.facultyId(), bumped.subjectName(),
                    bumped.occupantLabel(), retry.dayPlaced(), retry.periodIds()));
                if (!Objects.equals(retry.facultyId(), bumped.facultyId())) {
                    EligibleFacultyCandidateDto snapshot = fallbackCandidatesById.get(retry.facultyId());
                    facultySubstitutionEvents.add(new FacultySubstitutionEvent(bumped.courseOfferingId(), bumped.subjectName(),
                        bumped.facultyId(), retry.facultyId(), snapshot != null ? snapshot.remainingHours() : Double.NaN,
                        snapshot != null ? snapshot.capacityTier() : "NONE", cohortId, bumped.cohortSectionId(), bumped.batchId()));
                }
                return;
            }
        }
        // 2026-09-21: deliberately no longer relocating a bumped LIBRARY session (that path -- see
        // #tryRePlaceBumpedLibrarySession, kept for its own direct test coverage but no longer called
        // here -- searched for ANY other free Monday-Friday day/room, with no way to know whether a
        // not-yet-processed lower-priority row like Self-Study would need that exact slot, since
        // Self-Study is deliberately placed LAST in Phase 2's SHORTFALL_ROW_ORDER and its own
        // shortfall isn't known yet when an earlier mandatory row's backtrack bumps an idle-batch
        // Library filler mid-Phase-2). A bumped Library session (always advisory/idle-batch filler,
        // never real curriculum) is now simply left lost and reported as a neutral note below --
        // consistent with the same "Library/Sports/Self-Study never claim a period a real requirement
        // might need" principle already enforced in #fillLibraryGaps and #fillSportsGaps. Real
        // incident: this exact relocation created a whole-section Thursday Library block ahead of a
        // cohort's still-unplaced Self-Study/Co-curricular V, invisible to every later gate because it
        // ran mid-Phase-2, long before Phase 4 (Library) even starts.
        unplacedForCohort.add(new AutoPlaceUnplacedItem(bumped.subjectName(), bumped.sessionType(), bumped.occupantLabel(),
            "displaced during a backtrack attempt and could not be placed at its original slot or any other free day/period",
            bumped.courseOfferingId(), true, bumped.sessionType() == ClassSessionType.LIBRARY));
        // A permanently-lost THEORY session reopens a real gap in this row's own weekly requirement --
        // theoryStillOwedRuns must reflect it too, or a downstream phase that already read "0 owed"
        // for this exact row earlier in Phase 2 (the Library bonus gate, fillSelfStudyGaps' shortfall
        // tier) has no way to find out this run just lost a session it thought was safely delivered.
        // +1, not periodIds().size(): blockSize is always 1 for THEORY, so one lost session is one
        // lost period/run in the same unit fillSelfStudyGaps' own shortRemaining counter already uses.
        if (bumped.sessionType() == ClassSessionType.THEORY) {
            theoryStillOwedRuns.merge(theoryRowKey(bumped.courseOfferingId(), bumped.cohortSectionId()), 1, Integer::sum);
        }
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
        // Same working days as #fillLibraryGaps: Saturday is a regular day on a term with chosen
        // working Saturdays (#saturdayIsWorkingDay).
        List<DayOfWeek> orderedDays = workingDays(term).stream()
            .filter(d -> !daysUsed.contains(d))
            .sorted(Comparator.comparingInt(d -> dayLoad.getOrDefault(d, 0)))
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
     *  half-placed-but-unstaffed cell is never left behind. Thin wrapper over {@link #tryRestoreAt}. */
    private Optional<Placement> tryRestoreExact(Placement placement, Long cohortId) {
        return tryRestoreAt(placement, placement.dayOfWeek(), placement.periodIds(), cohortId);
    }

    /** Places {@code placement}'s offering/batch/section at {@code day}/{@code periodIds} (which
     *  need not be its original slot) and re-staffs it
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

    /** Same-row identity for two {@link Placement}s (as opposed to {@link Placement#sameRowAs},
     *  which compares against a {@link ShortfallRow} instead) — used to find a row's own other
     *  placements this run so a day-search retry doesn't land it twice on the same day. */
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
                                            List<AutoPlaceUnplacedItem> unplacedSink, Set<Long> audienceCohortIds) {
        List<CourseOffering> allMembers = courseOfferingRepository
            .findByTermInstanceIdAndCurriculumSemesterCourse_ElectiveGroupId(termInstanceId, electiveGroupId);
        if (allMembers.isEmpty()) {
            return 0;
        }
        // Student-choice group (management-selected groups never reach this pass -- see Phase 0).
        // Once students have registered their choices, an option nobody chose has no audience and
        // must not claim a room or a faculty slot (OC-227); before any choice exists, every option
        // still runs so students can see what's on offer.
        List<CourseOffering> chosenMembers = allMembers.stream().filter(this::isSelectedElectiveOption).toList();
        List<CourseOffering> members = chosenMembers.isEmpty() ? allMembers : chosenMembers;
        for (CourseOffering member : members) {
            if (Boolean.TRUE.equals(member.getIsActive()) && resolveElectiveMemberFacultyId(member) == null
                    && safe(member.getCurriculumSemesterCourse() != null ? member.getCurriculumSemesterCourse().getTheoryHours() : null) > 0) {
                unplacedSink.add(new AutoPlaceUnplacedItem(member.getSubject().getName(), ClassSessionType.THEORY, null,
                    "no faculty assigned on its Course Offering (elective)", member.getId(), false, false));
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
            List<Classroom> rooms = period == null ? List.of()
                : freeClassrooms(activeClassrooms, registeredStrength, day, period, term, unplacedTheoryMembers.size());
            if (period == null || !placeAndStaffElectiveMembers(unplacedTheoryMembers, day, period, rooms, term)) {
                String detail = period != null && rooms.size() < unplacedTheoryMembers.size()
                    ? " — " + unplacedTheoryMembers.size() + " member(s) still need a room of their own at that slot "
                        + "and only " + rooms.size() + " suitable room(s) are free then"
                    : " — one or more new members can't join that exact slot";
                unplacedSink.add(new AutoPlaceUnplacedItem("Elective group " + electiveGroupId, ClassSessionType.THEORY, null,
                    "already scheduled for " + day + (period != null ? ", " + period.getName() : "") + detail, null, false, false));
                return 0;
            }
            return unplacedTheoryMembers.size();
        }

        int dutyBlockedSlots = 0;
        int roomShortSlots = 0;
        int bestRoomsFound = 0;
        for (DayOfWeek day : DayOfWeek.values()) {
            for (Period period : periods) {
                if (blockedPeriodChecker.blockReason(day, period.getStartTime(), period.getEndTime(), term).isPresent()) {
                    continue;
                }
                // Institutional blocks alone are not enough. A group's single shared slot must also
                // be free of Clinical Shift duty for EVERY cohort drawing students from it -- those
                // windows are cohort-scoped, so blockedPeriodChecker (which is deliberately
                // institution-wide) cannot see them. Skipping this put real elective sessions
                // inside a duty window where the students were off-campus, and because the grid
                // collapses duty periods into one banner those sessions then rendered nowhere at
                // all. The admin-driven placeElectiveGroup had always checked this; only the
                // automated path had not.
                if (clinicalShiftChecker.blocksAnyCohort(audienceCohortIds, day, period, term)) {
                    dutyBlockedSlots++;
                    continue;
                }
                // One room per member, not one room for the group -- see freeClassrooms().
                List<Classroom> rooms = freeClassrooms(activeClassrooms, registeredStrength, day, period, term,
                    unplacedTheoryMembers.size());
                if (rooms.size() < unplacedTheoryMembers.size()) {
                    bestRoomsFound = Math.max(bestRoomsFound, rooms.size());
                    roomShortSlots++;
                    continue;
                }
                if (placeAndStaffElectiveMembers(unplacedTheoryMembers, day, period, rooms, term)) {
                    return unplacedTheoryMembers.size();
                }
            }
        }
        // Name the actual constraint. A bare "no slot found" sends the admin hunting for staffing
        // capacity when the real answer is often structural: the group's students are off-campus
        // then (clinical duty), or the institution simply does not own enough rooms to run this
        // many options at one shared slot -- which no amount of rescheduling can fix, and which the
        // admin can only act on (split the group, retire an option, add a room) if told.
        String reason = "no day/period found where every member's bound faculty and a suitable room are all free";
        if (roomShortSlots > 0) {
            reason = "this group runs " + unplacedTheoryMembers.size() + " options at one shared slot, so it needs "
                + unplacedTheoryMembers.size() + " suitable rooms free at the same time — the best any day/period "
                + "offered was " + bestRoomsFound;
        }
        if (dutyBlockedSlots > 0) {
            reason += " (" + dutyBlockedSlots + " slot(s) were also ruled out because a participating cohort"
                + " is away on Clinical Shift duty then)";
        }
        // A room ceiling is not a lack of days: another working day adds slots, not rooms.
        unplacedSink.add(new AutoPlaceUnplacedItem("Elective group " + electiveGroupId, ClassSessionType.THEORY, null,
            reason, null, roomShortSlots == 0, false));
        return 0;
    }

    /** Attempts every member at the given slot/room, undoing everything on the first failure so a
     *  partially-placed group is never left behind for the caller's next candidate slot to build on. */
    private boolean placeAndStaffElectiveMembers(List<CourseOffering> members, DayOfWeek day, Period period,
                                                  List<Classroom> classrooms, TermInstance term) {
        if (classrooms.size() < members.size()) {
            return false; // caller reports the shortfall; never cram two options into one room
        }
        List<Long> placedCellIds = new ArrayList<>();
        for (int i = 0; i < members.size(); i++) {
            CourseOffering member = members.get(i);
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
                // Its OWN room -- see freeClassrooms() for why one room per group was wrong.
                timetableStaffingService.staffCell(placed.id(),
                    new StaffingAssignmentRequest(resolveElectiveMemberFacultyId(member), classrooms.get(i).getId()));
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

    /** Up to {@code wanted} DISTINCT free classrooms at one (day, period), each big enough for
     *  {@code requiredStrength}. Returns fewer than {@code wanted} when that many aren't free —
     *  callers decide whether a partial answer is usable.
     *
     *  <p>Exists because an elective group needs one room PER member, not one room for the group.
     *  Every option in a group runs simultaneously at the group's single shared slot (that is what
     *  makes it a group), but they are different subjects taught by different faculty, so they
     *  cannot share a room. The elective pass used to resolve one classroom and hand the same one
     *  to every member: local dev had all 9 ELEC-II options booked into Library Hall at Monday
     *  Period 4 and all 5 ELEC-III options into the same Library Hall at Period 2 — nine subjects,
     *  nine faculty, one 30-seat room, at once. Nothing rejected it because the per-room conflict
     *  check runs against cells already persisted, and each member was placed and staffed inside
     *  the same pass before the next member looked. */
    private List<Classroom> freeClassrooms(List<Classroom> candidates, int requiredStrength, DayOfWeek day,
                                            Period period, TermInstance term, int wanted) {
        List<Classroom> free = new ArrayList<>();
        for (Classroom classroom : candidates) {
            if (free.size() >= wanted) {
                break;
            }
            if (classroom.getCapacity() != null && classroom.getCapacity() < requiredStrength) {
                continue;
            }
            Optional<ConstraintViolation> conflict = timetableStaffingService.checkRoomFree(
                ClassSessionType.THEORY, classroom.getId(), classroom.getRoom(), term.getId(), null,
                day, period.getStartTime(), period.getEndTime());
            if (conflict.isEmpty()) {
                free.add(classroom);
            }
        }
        return free;
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
