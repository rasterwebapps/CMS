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
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.CohortPlacementSummary;
import com.cms.dto.ConstraintViolation;
import com.cms.dto.CourseOfferingDto;
import com.cms.dto.FacultyCapacityCheckResult;
import com.cms.dto.FacultyOverCapacity;
import com.cms.dto.GlobalAutoScheduleResult;
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
 * with a single action covering every cohort in a term at once, treating {@link
 * CourseOffering#getFacultyId()} as authoritative (unlike {@link TimetableStaffingAutoAssignService},
 * which picks freely from the eligible department pool). Two calls, always in this order:
 * {@link #precheckCapacity} (read-only — every faculty's real total term-hour demand across every
 * offering they're bound to, across every cohort, against their real term capacity) must come back
 * clean before {@link #runGlobalAutoSchedule} (write, fully atomic) is ever attempted — the
 * frontend enforces this by never calling the write endpoint otherwise, and this class re-checks it
 * defensively so a bad precheck can never be bypassed even via a direct API call.
 *
 * <p>Deliberately scoped down from the per-cohort tool in two ways, both accepted trade-offs of the
 * "any single problem aborts the whole run" design (see {@link #runGlobalAutoSchedule}), not
 * oversights: (1) no bounded-backtrack displacement — {@link TimetableSkeletonAutoPlaceService}'s
 * heuristic only ever displaces a *placement*, never an already-staffed cell (there is no "unstaff"
 * capability to cheaply undo), and since here every placement is immediately staffed as one unit,
 * a failed backtrack attempt would still mean total failure anyway, so the extra complexity isn't
 * worth it for a run that aborts entirely on any remaining problem regardless; (2) elective groups
 * are only auto-scheduled for their one shared slot (mirroring the existing "Place Elective Block"
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
                topContributors, demand.secondaryFacultyByOffering(), demand.demandByFaculty(), facultyId, demand.workingDaysInTerm(), demand.weeksInTerm());

            overCapacity.add(new FacultyOverCapacity(facultyId, faculty.getFullName(), capacity.dailyCapForDisplay(), capacity.tier(),
                demand.workingDaysInTerm(), capacity.termCapacityHours(), totalDemand, shortfall, suggestedMinDailyHours,
                topContributors, raiseCap, spreadLoad));
        }
        overCapacity.sort(Comparator.comparing(FacultyOverCapacity::facultyName, String.CASE_INSENSITIVE_ORDER));
        return new GlobalCapacityPrecheckResult(overCapacity);
    }

    /** Live, single-(faculty, offering) counterpart to {@link #precheckCapacity} -- used by Course
     *  Offerings to check, before save, whether assigning {@code candidateFacultyId} to {@code
     *  offeringId} would push their real term-wide load over capacity. Reuses the exact same
     *  aggregation {@link #precheckCapacity} runs (via {@link #computeTermDemand}) so the two can
     *  never disagree. {@code demand.demandByFaculty()} already excludes {@code offeringId}'s own
     *  contribution for a candidate who isn't currently bound to it (the aggregation keys off each
     *  offering's *current* DB faculty, not the candidate under consideration) -- so "projected
     *  total" is simply the candidate's current demand plus this one offering's own contribution,
     *  no separate exclusion logic needed even when re-checking the offering's already-assigned
     *  faculty (whose current demand already includes it exactly once). */
    @Transactional(readOnly = true)
    public FacultyCapacityCheckResult checkFacultyCapacityForOffering(Long termInstanceId, Long offeringId, Long candidateFacultyId) {
        TermDemandAggregation demand = computeTermDemand(termInstanceId);
        Faculty candidate = facultyRepository.findById(candidateFacultyId)
            .orElseThrow(() -> new ResourceNotFoundException("Faculty not found with id: " + candidateFacultyId));

        double currentDemand = demand.demandByFaculty().getOrDefault(candidateFacultyId, 0.0);
        boolean alreadyAssignedHere = courseOfferingRepository.findById(offeringId)
            .map(o -> candidateFacultyId.equals(o.getFacultyId())).orElse(false);
        double offeringHours = demand.demandByOffering().getOrDefault(offeringId, 0.0);
        double projectedTotal = alreadyAssignedHere ? currentDemand : currentDemand + offeringHours;

        CapacityResolution capacity = resolveEffectiveTermCapacity(candidate, demand.workingDaysInTerm(), demand.weeksInTerm());
        boolean overCapacity = capacity != null && projectedTotal > capacity.termCapacityHours() + CAPACITY_EPSILON;

        List<SpreadLoadSuggestion> spreadLoad = List.of();
        double suggestedMinDailyHours = 0;
        if (overCapacity) {
            suggestedMinDailyHours = Math.ceil(projectedTotal / demand.workingDaysInTerm());
            CourseOffering offering = courseOfferingRepository.findById(offeringId).orElse(null);
            if (offering != null) {
                OverageContributor asContributor = new OverageContributor(offeringId, offering.getSubject() != null ? offering.getSubject().getName() : "",
                    null, null, offeringHours);
                spreadLoad = buildSpreadLoadSuggestions(List.of(asContributor), demand.secondaryFacultyByOffering(),
                    demand.demandByFaculty(), candidateFacultyId, demand.workingDaysInTerm(), demand.weeksInTerm());
            }
        }

        return new FacultyCapacityCheckResult(overCapacity, currentDemand, offeringHours, projectedTotal,
            capacity != null ? capacity.termCapacityHours() : 0, capacity != null ? capacity.dailyCapForDisplay() : 0,
            capacity != null ? capacity.tier() : "NONE", demand.workingDaysInTerm(), suggestedMinDailyHours, spreadLoad);
    }

    private record TermDemandAggregation(int workingDaysInTerm, int weeksInTerm, Map<Long, Double> demandByFaculty,
                                          Map<Long, List<OverageContributor>> contributorsByFaculty,
                                          Map<Long, Long> secondaryFacultyByOffering, Map<Long, Double> demandByOffering) {}

    /** The shared per-term aggregation both {@link #precheckCapacity} and {@link
     *  #checkFacultyCapacityForOffering} run off, so the two can never compute a faculty's demand
     *  differently. Loops every cohort active in the term, then every offering that cohort has,
     *  summing each offering+cohort pair's contribution both per bound faculty and per offering
     *  (an offering shared across cohorts on the same curriculum version contributes once per
     *  cohort to each map, correctly -- see class javadoc / {@link #precheckCapacity}'s original
     *  double-counting note). */
    private TermDemandAggregation computeTermDemand(Long termInstanceId) {
        TermInstance term = requireTermInstance(termInstanceId);
        int workingDaysInTerm = timetableCapacityPlanningService.countWorkingDays(
            term, timetableCapacityPlanningService.nonTeachingDates(term));
        int weeksInTerm = CurriculumHoursCalculator.weeksInTerm(term);

        Map<Long, Double> demandByFaculty = new LinkedHashMap<>();
        Map<Long, List<OverageContributor>> contributorsByFaculty = new LinkedHashMap<>();
        Map<Long, Long> secondaryFacultyByOffering = new LinkedHashMap<>();
        Map<Long, Double> demandByOffering = new LinkedHashMap<>();

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
                OfferingHoursSplit split = termHoursForOfferingInCohort(offering, cohortId, termInstanceId, offeringDto.facultyId());
                if (split.totalHours() <= 0) {
                    continue;
                }
                demandByOffering.merge(offering.getId(), split.totalHours(), Double::sum);
                if (offeringDto.secondaryFacultyId() != null) {
                    secondaryFacultyByOffering.putIfAbsent(offering.getId(), offeringDto.secondaryFacultyId());
                }
                for (Map.Entry<Long, Double> entry : split.hoursByFacultyId().entrySet()) {
                    Long facultyId = entry.getKey();
                    double contribution = entry.getValue();
                    demandByFaculty.merge(facultyId, contribution, Double::sum);
                    contributorsByFaculty.computeIfAbsent(facultyId, k -> new ArrayList<>())
                        .add(new OverageContributor(offering.getId(), offeringDto.subjectName(), cohortId, cohort.getDisplayName(), contribution));
                }
            }
        }
        return new TermDemandAggregation(workingDaysInTerm, weeksInTerm, demandByFaculty, contributorsByFaculty, secondaryFacultyByOffering, demandByOffering);
    }

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
     *  mechanism existed. */
    private OfferingHoursSplit termHoursForOfferingInCohort(CourseOffering offering, Long cohortId, Long termInstanceId, Long primaryFacultyId) {
        CurriculumSemesterCourse csc = offering.getCurriculumSemesterCourse();
        int theoryHours = safe(csc.getTheoryHours());
        int labClinicalHours = safe(csc.getLabHours()) + safe(csc.getClinicalHours());

        List<CohortSection> activeSections = timetableSkeletonService.resolveActiveSections(cohortId, termInstanceId);
        List<Batch> activeBatches = batchRepository.findByCourseOfferingId(offering.getId()).stream()
            .filter(b -> Boolean.TRUE.equals(b.getIsActive()))
            .toList();

        Map<Long, Double> hoursByFacultyId = new LinkedHashMap<>();

        double theoryTotal;
        if (activeSections.isEmpty()) {
            theoryTotal = theoryHours;
            if (theoryTotal > 0 && primaryFacultyId != null) {
                hoursByFacultyId.merge(primaryFacultyId, theoryTotal, Double::sum);
            }
        } else {
            theoryTotal = theoryHours * (double) activeSections.size();
            if (theoryHours > 0) {
                Map<Long, Long> sectionFacultyIdBySectionId = courseOfferingSectionFacultyRepository
                    .findByCourseOfferingId(offering.getId()).stream()
                    .collect(Collectors.toMap(sf -> sf.getCohortSection().getId(), sf -> sf.getFaculty().getId()));
                for (CohortSection section : activeSections) {
                    Long facultyForSection = sectionFacultyIdBySectionId.getOrDefault(section.getId(), primaryFacultyId);
                    if (facultyForSection != null) {
                        hoursByFacultyId.merge(facultyForSection, (double) theoryHours, Double::sum);
                    }
                }
            }
        }

        double labClinicalTotal;
        if (activeBatches.isEmpty()) {
            labClinicalTotal = labClinicalHours;
            if (labClinicalTotal > 0 && primaryFacultyId != null) {
                hoursByFacultyId.merge(primaryFacultyId, labClinicalTotal, Double::sum);
            }
        } else {
            labClinicalTotal = labClinicalHours * (double) activeBatches.size();
            for (Batch batch : activeBatches) {
                Long facultyForBatch = batch.getCoordinatorFaculty() != null
                    ? batch.getCoordinatorFaculty().getId() : primaryFacultyId;
                if (labClinicalHours > 0 && facultyForBatch != null) {
                    hoursByFacultyId.merge(facultyForBatch, (double) labClinicalHours, Double::sum);
                }
            }
        }

        return new OfferingHoursSplit(theoryTotal + labClinicalTotal, hoursByFacultyId);
    }

    private record OfferingHoursSplit(double totalHours, Map<Long, Double> hoursByFacultyId) {}

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

    /** Advisory-only, nothing applied automatically -- checks the offering's own {@code
     *  secondaryFacultyId} first (already a college-vetted co-instructor candidate for that exact
     *  offering), then the wider candidate pool, for each of the faculty's top contributing
     *  offerings, picking the first candidate whose own existing demand plus this offering's hours
     *  still fits their own capacity. Candidate pool is same-speciality when the subject has one,
     *  same as {@link ClassScheduleService#requireEligibleFaculty} -- but mirrors that method's own
     *  "no speciality on the subject means no eligibility constraint, anyone can teach it" rule
     *  rather than skipping the subject entirely, since a real subject having no speciality tag is
     *  the common case in this data set (unfilled master data), not the exception. */
    private List<SpreadLoadSuggestion> buildSpreadLoadSuggestions(List<OverageContributor> topContributors,
            Map<Long, Long> secondaryFacultyByOffering, Map<Long, Double> demandByFaculty,
            Long overCapacityFacultyId, int workingDaysInTerm, int weeksInTerm) {
        List<SpreadLoadSuggestion> suggestions = new ArrayList<>();
        for (OverageContributor contributor : topContributors) {
            CourseOffering offering = courseOfferingRepository.findById(contributor.courseOfferingId()).orElse(null);
            if (offering == null || offering.getSubject() == null) {
                continue;
            }
            var speciality = offering.getSubject().getSpeciality();

            Long secondaryId = secondaryFacultyByOffering.get(contributor.courseOfferingId());
            Faculty secondary = secondaryId != null && !secondaryId.equals(overCapacityFacultyId)
                ? facultyRepository.findById(secondaryId).orElse(null) : null;
            SpreadLoadSuggestion secondarySuggestion = secondary == null ? null
                : spreadLoadSuggestionIfSpare(secondary, true, contributor, demandByFaculty, workingDaysInTerm, weeksInTerm);
            if (secondarySuggestion != null) {
                suggestions.add(secondarySuggestion);
                continue;
            }

            List<Faculty> pool = speciality != null
                ? facultyRepository.findBySpecialityIdAndStatus(speciality.getId(), FacultyStatus.ACTIVE)
                : facultyRepository.findByStatus(FacultyStatus.ACTIVE);
            for (Faculty candidate : pool) {
                if (candidate.getId().equals(overCapacityFacultyId)) {
                    continue;
                }
                SpreadLoadSuggestion suggestion = spreadLoadSuggestionIfSpare(candidate, false, contributor, demandByFaculty, workingDaysInTerm, weeksInTerm);
                if (suggestion != null) {
                    suggestions.add(suggestion);
                    break;
                }
            }
        }
        return suggestions;
    }

    private SpreadLoadSuggestion spreadLoadSuggestionIfSpare(Faculty candidate, boolean isSecondary, OverageContributor contributor,
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
        return new SpreadLoadSuggestion(candidate.getId(), candidate.getFullName(), isSecondary, spare,
            contributor.courseOfferingId(), contributor.subjectName());
    }

    // ─────────────────────────────────────────────────────────────────────
    // Placement + staffing run
    // ─────────────────────────────────────────────────────────────────────

    /** Fully atomic -- any single unplaceable session (capacity, blocked period, faculty
     *  double-booked, room conflict, ...) throws and rolls back everything via Spring's default
     *  rollback-on-{@link RuntimeException} behavior, so nothing is left half-placed for any
     *  cohort. Re-runs {@link #precheckCapacity} defensively so a bad precheck can never be
     *  bypassed even via a direct API call. */
    @Transactional
    public GlobalAutoScheduleResult runGlobalAutoSchedule(Long termInstanceId) {
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
        Set<Long> cohortIds = enumerateCohortIds(termInstanceId);

        int totalPlaced = 0;
        int totalStaffed = 0;
        List<CohortPlacementSummary> summaries = new ArrayList<>();
        Set<Long> electiveGroupIdsSeen = new LinkedHashSet<>();

        for (Long cohortId : cohortIds) {
            Cohort cohort = cohortRepository.findById(cohortId)
                .orElseThrow(() -> new ResourceNotFoundException("Cohort not found with id: " + cohortId));
            SkeletonBuilderResponse skeleton = timetableSkeletonService.getCohortSkeleton(termInstanceId, cohortId);
            int placedForCohort = 0;

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
                Long facultyId = offering.getFacultyId();
                if (facultyId == null) {
                    throw new TimetableConstraintViolationException(List.of(new ConstraintViolation(
                        "GLOBAL_AUTO_SCHEDULE_NO_FACULTY_BOUND",
                        subject.subjectName() + " (" + cohort.getDisplayName()
                            + ") has no faculty assigned on its Course Offering — assign one before running the global scheduler")));
                }

                for (SkeletonSubjectBudget budget : subject.budgets()) {
                    int shortfall = budget.requiredSessionsPerWeek() - budget.placedSessionsPerWeek();
                    if (shortfall <= 0) {
                        continue;
                    }
                    Set<DayOfWeek> daysUsed = existingDaysForBudgetRow(skeleton.cells(), subject.courseOfferingId(), budget);
                    for (int i = 0; i < shortfall; i++) {
                        DayOfWeek placedOn = tryPlaceAndStaff(cohortId, offering, budget, facultyId, term, periods, daysUsed);
                        if (placedOn == null) {
                            throw new TimetableConstraintViolationException(List.of(new ConstraintViolation(
                                "GLOBAL_AUTO_SCHEDULE_UNPLACEABLE",
                                subject.subjectName() + " (" + budget.sessionType() + ") for " + cohort.getDisplayName()
                                    + occupantSuffix(budget) + ": no day/period found where both placement and "
                                    + offering.getFacultyId() + "'s staffing succeed")));
                        }
                        daysUsed.add(placedOn);
                        placedForCohort++;
                    }
                }
            }
            totalPlaced += placedForCohort;
            totalStaffed += placedForCohort;
            summaries.add(new CohortPlacementSummary(cohortId, cohort.getDisplayName(), placedForCohort, placedForCohort));
        }

        // Electives: only each group's one shared slot is automated (see class javadoc) -- counts
        // fold into the totals but aren't attributed to any single cohort summary row, since a
        // group can span students from more than one cohort.
        for (Long electiveGroupId : electiveGroupIdsSeen) {
            int placed = placeAndStaffElectiveGroup(termInstanceId, electiveGroupId, term, periods);
            totalPlaced += placed;
            totalStaffed += placed;
        }

        return new GlobalAutoScheduleResult(totalPlaced, totalStaffed, summaries);
    }

    private String occupantSuffix(SkeletonSubjectBudget budget) {
        if (budget.cohortSectionLabel() != null) {
            return "/" + budget.cohortSectionLabel();
        }
        if (budget.batchName() != null) {
            return "/" + budget.batchName();
        }
        return "";
    }

    /** Scans every free day/period until one is found where placement AND staffing the offering's
     *  bound faculty both succeed, undoing the placement and trying the next candidate on a
     *  staffing failure (that faculty is busy at that slot, not a shared resource another row could
     *  be nudged out of — see class javadoc for why this doesn't attempt backtracking). Returns the
     *  day it landed on, or null if every combination was exhausted. */
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
     *  unplaced by this pass, same as any other structural prerequisite gap. */
    private int placeAndStaffElectiveGroup(Long termInstanceId, Long electiveGroupId, TermInstance term, List<Period> periods) {
        List<CourseOffering> members = courseOfferingRepository
            .findByTermInstanceIdAndCurriculumSemesterCourse_ElectiveGroupId(termInstanceId, electiveGroupId);
        if (members.isEmpty()) {
            return 0;
        }
        for (CourseOffering member : members) {
            if (Boolean.TRUE.equals(member.getIsActive()) && member.getFacultyId() == null
                    && safe(member.getCurriculumSemesterCourse() != null ? member.getCurriculumSemesterCourse().getTheoryHours() : null) > 0) {
                throw new TimetableConstraintViolationException(List.of(new ConstraintViolation(
                    "GLOBAL_AUTO_SCHEDULE_NO_FACULTY_BOUND",
                    member.getSubject().getName() + " (elective) has no faculty assigned on its Course Offering")));
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
                throw new TimetableConstraintViolationException(List.of(new ConstraintViolation(
                    "GLOBAL_AUTO_SCHEDULE_UNPLACEABLE",
                    "Elective group " + electiveGroupId + " is already scheduled for " + day
                        + (period != null ? ", " + period.getName() : "") + " — one or more new members can't join that exact slot")));
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
        throw new TimetableConstraintViolationException(List.of(new ConstraintViolation(
            "GLOBAL_AUTO_SCHEDULE_UNPLACEABLE",
            "Elective group " + electiveGroupId + ": no day/period found where every member's bound faculty and a suitable room are all free")));
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
                timetableStaffingService.staffCell(placed.id(), new StaffingAssignmentRequest(member.getFacultyId(), classroom.getId()));
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
