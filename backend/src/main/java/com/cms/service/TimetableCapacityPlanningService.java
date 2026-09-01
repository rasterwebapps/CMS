package com.cms.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.CapacityPlanResponse;
import com.cms.dto.CohortAutoPlanSummaryResponse;
import com.cms.dto.LabClinicalVenueCapacityResult;
import com.cms.dto.RoomInventoryRowResponse;
import com.cms.dto.SuggestedBatchResponse;
import com.cms.dto.SuggestedSectionResponse;
import com.cms.dto.TermCapacityOverviewResponse;
import com.cms.dto.VenueOptionResponse;
import com.cms.dto.VenueOverCapacity;
import com.cms.dto.VenueRebalanceBlocked;
import com.cms.dto.VenueRebalanceMove;
import com.cms.dto.VenueRebalancePreview;
import com.cms.dto.VenueRebalanceResult;
import com.cms.dto.VenueTightCapacity;
import com.cms.dto.VenueUtilizationResponse;
import com.cms.exception.LifecycleConflictException;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Batch;
import com.cms.model.BlockedPeriod;
import com.cms.model.CalendarEvent;
import com.cms.model.ClassSchedule;
import com.cms.model.Classroom;
import com.cms.model.ClinicalVenue;
import com.cms.model.Cohort;
import com.cms.model.CohortRoomAllocation;
import com.cms.model.CourseOffering;
import com.cms.model.CurriculumSemesterCourse;
import com.cms.model.Lab;
import com.cms.model.Period;
import com.cms.model.StudentTermEnrollment;
import com.cms.model.TermInstance;
import com.cms.model.enums.BlockType;
import com.cms.model.enums.ClassScheduleStatus;
import com.cms.model.enums.ClassSessionType;
import com.cms.model.enums.CohortRoomAllocationStatus;
import com.cms.model.enums.EnrollmentStatus;
import com.cms.model.enums.LabStatus;
import com.cms.model.enums.PlanningBasis;
import com.cms.repository.BatchRepository;
import com.cms.repository.BlockedPeriodRepository;
import com.cms.repository.CalendarEventRepository;
import com.cms.repository.ClassScheduleRepository;
import com.cms.repository.ClassroomRepository;
import com.cms.repository.ClinicalVenueRepository;
import com.cms.repository.CohortRepository;
import com.cms.repository.CohortRoomAllocationRepository;
import com.cms.repository.CohortSectionRepository;
import com.cms.repository.CourseOfferingRepository;
import com.cms.repository.LabRepository;
import com.cms.repository.PeriodRepository;
import com.cms.repository.StudentTermEnrollmentRepository;
import com.cms.repository.TermInstanceRepository;

/**
 * Answers "how many classrooms/lab-batches do I need, and what's already free" for a Cohort in a
 * TermInstance, ahead of building a Skeleton Builder / Staffing pass — the visibility Staffing's
 * per-cell {@code requireRoomFree}/{@code requireCapacityFit} checks don't give the admin until
 * they're already mid-assignment. Cohort strength is admin-selected (picked explicitly via
 * {@code cohortId}) rather than auto-resolved from a CourseOffering, because CourseOffering has
 * no reliable FK to Cohort before that term's offerings/registrations even exist yet.
 */
@Service
@Transactional(readOnly = true)
public class TimetableCapacityPlanningService {

    /** Weekly slot denominator for Lab/Clinical venue utilization (classroomUtilization/
     *  utilization below). Deliberately 5, not the 6 real teaching days {@code DayOfWeek} supports
     *  (Mon-Sat) -- the routine weekly plan targets Monday-Friday, with Saturday reserved as real
     *  but occasional overflow capacity for whatever doesn't fit. A venue with genuine Saturday
     *  bookings can therefore read over 100% utilized -- an intentional, honest signal that it's
     *  already leaning on overflow, not a bug to clamp away. */
    private static final int WORKING_DAYS_PER_WEEK = 5;

    /** Shared with {@link TimetableGlobalAutoScheduleService}'s faculty over/tight-capacity split
     *  so a Lab/Clinical venue and a faculty member are both flagged "tight" at the same real
     *  utilization threshold — one literal, not two independently-typed copies. */
    public static final double TIGHT_CAPACITY_THRESHOLD = 0.95;

    private final CohortRepository cohortRepository;
    private final CohortSectionRepository cohortSectionRepository;
    private final TermInstanceRepository termInstanceRepository;
    private final StudentTermEnrollmentRepository studentTermEnrollmentRepository;
    private final ClassroomRepository classroomRepository;
    private final LabRepository labRepository;
    private final ClinicalVenueRepository clinicalVenueRepository;
    private final PeriodRepository periodRepository;
    private final ClassScheduleRepository classScheduleRepository;
    private final CalendarEventRepository calendarEventRepository;
    private final CourseOfferingRepository courseOfferingRepository;
    private final BlockedPeriodRepository blockedPeriodRepository;
    private final CohortRoomAllocationRepository cohortRoomAllocationRepository;
    private final BatchRepository batchRepository;
    private final TimetableBlockedPeriodChecker blockedPeriodChecker;
    private final AuditLogService auditLogService;

    public TimetableCapacityPlanningService(CohortRepository cohortRepository,
                                             CohortSectionRepository cohortSectionRepository,
                                             TermInstanceRepository termInstanceRepository,
                                             StudentTermEnrollmentRepository studentTermEnrollmentRepository,
                                             ClassroomRepository classroomRepository,
                                             LabRepository labRepository,
                                             ClinicalVenueRepository clinicalVenueRepository,
                                             PeriodRepository periodRepository,
                                             ClassScheduleRepository classScheduleRepository,
                                             CalendarEventRepository calendarEventRepository,
                                             CourseOfferingRepository courseOfferingRepository,
                                             BlockedPeriodRepository blockedPeriodRepository,
                                             CohortRoomAllocationRepository cohortRoomAllocationRepository,
                                             BatchRepository batchRepository,
                                             TimetableBlockedPeriodChecker blockedPeriodChecker,
                                             AuditLogService auditLogService) {
        this.cohortRepository = cohortRepository;
        this.cohortSectionRepository = cohortSectionRepository;
        this.termInstanceRepository = termInstanceRepository;
        this.studentTermEnrollmentRepository = studentTermEnrollmentRepository;
        this.classroomRepository = classroomRepository;
        this.labRepository = labRepository;
        this.clinicalVenueRepository = clinicalVenueRepository;
        this.periodRepository = periodRepository;
        this.classScheduleRepository = classScheduleRepository;
        this.calendarEventRepository = calendarEventRepository;
        this.courseOfferingRepository = courseOfferingRepository;
        this.blockedPeriodRepository = blockedPeriodRepository;
        this.cohortRoomAllocationRepository = cohortRoomAllocationRepository;
        this.batchRepository = batchRepository;
        this.blockedPeriodChecker = blockedPeriodChecker;
        this.auditLogService = auditLogService;
    }

    public CapacityPlanResponse getPlan(Long termInstanceId, Long cohortId, PlanningBasis planningBasisParam) {
        return getPlan(termInstanceId, cohortId, planningBasisParam, Set.of());
    }

    /**
     * @param provisionallyClaimedClassroomIds classrooms already handed to an earlier, still-uncommitted
     *      cohort's suggestion within the same {@link #getTermOverview} pass -- excluded from this
     *      cohort's candidate pool exactly like a genuinely committed {@code CohortSection}'s room, so two
     *      Not Planned cohorts reviewed side-by-side on the bulk screen never get suggested the same
     *      physical room. Always empty for the single-cohort Capacity Planner screen (the public
     *      3-arg overload above), which has no sibling cohorts to reserve against.
     */
    private CapacityPlanResponse getPlan(Long termInstanceId, Long cohortId, PlanningBasis planningBasisParam,
                                          Set<Long> provisionallyClaimedClassroomIds) {
        TermInstance term = termInstanceRepository.findById(termInstanceId)
            .orElseThrow(() -> new ResourceNotFoundException("Term instance not found with id: " + termInstanceId));
        Cohort cohort = cohortRepository.findByIdWithCourse(cohortId)
            .orElseThrow(() -> new ResourceNotFoundException("Cohort not found with id: " + cohortId));

        long enrolledStrength = studentTermEnrollmentRepository.countByTermInstanceIdAndCohortIdAndStatus(
            termInstanceId, cohortId, EnrollmentStatus.ENROLLED);
        Integer sanctionedStrength = cohort.getSanctionedIntake();

        PlanningBasis planningBasis = planningBasisParam != null ? planningBasisParam : PlanningBasis.ENROLLED;
        // Read-only view: fall back to enrolled headcount if SANCTIONED was requested but this
        // cohort has no seat data configured, rather than failing the whole plan just to show it
        // — the commit path enforces this strictly instead (CohortRoomAllocationService).
        long strength = (planningBasis == PlanningBasis.SANCTIONED && sanctionedStrength != null)
            ? sanctionedStrength : enrolledStrength;

        // A cohort's enrolled students all share one semester in a given term (whole-cohort
        // promotion) -- used to scope the "Create Suggested Batches" subject picker down to this
        // cohort's actual offerings instead of every offering in the shared TermInstance (which
        // packs every concurrent year's subjects together).
        Integer semesterNumber = studentTermEnrollmentRepository
            .findFirstByTermInstanceIdAndCohortIdAndStatus(termInstanceId, cohortId, EnrollmentStatus.ENROLLED)
            .map(StudentTermEnrollment::getSemesterNumber)
            .orElse(null);

        List<Classroom> activeClassrooms = classroomRepository.findByIsActiveTrueOrderByNameAsc();
        List<Lab> activeLabs = labRepository.findAll().stream()
            .filter(l -> l.getStatus() == LabStatus.ACTIVE || l.getStatus() == LabStatus.AVAILABLE)
            .toList();
        List<ClinicalVenue> activeClinicalVenues = clinicalVenueRepository.findByIsActiveTrueOrderByNameAsc();

        // A classroom flagged allowsConcurrentSharing (large lecture/drawing hall, see
        // SpecialClassRequestService) is never a candidate for exclusive Theory-section locking --
        // committing it here would defeat the whole point of flagging it shareable. Still shown in
        // Venue Utilization below (activeClassrooms, unfiltered) since that's just occupancy
        // information, not a candidacy list.
        List<Classroom> exclusiveClassrooms = activeClassrooms.stream()
            .filter(c -> !Boolean.TRUE.equals(c.getAllowsConcurrentSharing()))
            .toList();

        List<VenueOptionResponse> fittingClassrooms = exclusiveClassrooms.stream()
            .filter(c -> c.getCapacity() != null && c.getCapacity() >= strength)
            .map(c -> new VenueOptionResponse(c.getId(), c.getName(), c.getCapacity()))
            .toList();
        boolean theoryFits = !fittingClassrooms.isEmpty();
        String theoryShortfall = theoryFits ? null
            : "No single classroom seats " + strength + " students — split this cohort's Theory into sections below.";

        // Every classroom with a genuine active claim this term, split two ways:
        //  - claimedByOtherCohortLabel: claims from OTHER cohorts only -- drives the sectioning
        //    candidate pool below and the Venue Utilization card's "Committed — <cohort>" red tag,
        //    so a room's unavailability is self-explanatory instead of silently vanishing from the
        //    picker with no on-screen reason. This plan's own cohort's committed rooms are excluded
        //    here and instead read via isClassroomHighlighted() on the frontend (blue "active"
        //    tag), since a room a cohort holds itself is not "unavailable" the way a room genuinely
        //    claimed by someone else is.
        //  - claimedByAnyCohortId: EVERY active claim, including this plan's own cohort -- feeds
        //    classroomUtilization()'s occupied/percent gate, which must stay true for the cohort's
        //    own claimed rooms too. Splitting these was required after a prior fix: excluding the
        //    own cohort from a single shared map fixed the red "claimed by <self>" mislabel but
        //    also zeroed this cohort's own Room 101/102-style utilization percentages, since both
        //    concerns used to read the same map.
        List<com.cms.model.CohortSection> activeSectionsThisTerm = cohortSectionRepository.findByTermInstanceIdAndIsActiveTrue(termInstanceId);
        Map<Long, String> claimedByOtherCohortLabel = activeSectionsThisTerm.stream()
            .filter(s -> !s.getCohortRoomAllocation().getCohort().getId().equals(cohortId))
            .collect(Collectors.toMap(s -> s.getClassroom().getId(), s -> s.getCohortRoomAllocation().getCohort().getDisplayName(),
                (a, b) -> a));
        Set<Long> claimedByAnyCohortId = activeSectionsThisTerm.stream()
            .map(s -> s.getClassroom().getId())
            .collect(Collectors.toSet());
        Set<Long> claimedClassroomIds = claimedByOtherCohortLabel.keySet();

        // Candidate pool for Theory sectioning: every active classroom not already claimed by
        // another cohort's active section this term, sorted biggest-first so the frontend's
        // greedy auto-split fills the fewest possible sections by default.
        List<VenueOptionResponse> classroomsForSectioning = exclusiveClassrooms.stream()
            .filter(c -> !claimedClassroomIds.contains(c.getId()) && !provisionallyClaimedClassroomIds.contains(c.getId()))
            .sorted((a, b) -> Integer.compare(
                b.getCapacity() != null ? b.getCapacity() : 0, a.getCapacity() != null ? a.getCapacity() : 0))
            .map(c -> new VenueOptionResponse(c.getId(), c.getName(), c.getCapacity()))
            .toList();

        List<VenueOptionResponse> fittingLabs = activeLabs.stream()
            .map(l -> new VenueOptionResponse(l.getId(), l.getName(), l.getCapacity()))
            .toList();
        List<VenueOptionResponse> fittingClinicalVenues = activeClinicalVenues.stream()
            .map(v -> new VenueOptionResponse(v.getId(), v.getName(), v.getCapacity()))
            .toList();

        List<Period> activePeriods = periodRepository.findByIsActiveTrueOrderByPeriodOrderAsc();
        double periodDurationMinutes = com.cms.service.CurriculumHoursCalculator.averageDurationMinutes(
            activePeriods.stream().map(Period::getDurationMinutes).toList());

        Set<LocalDate> nonTeachingDates = nonTeachingDates(term);
        int workingDaysInTerm = countWorkingDays(term, nonTeachingDates);
        double totalWorkingPeriodHours = workingDaysInTerm * activePeriods.size() * periodDurationMinutes / 60.0;
        double blockedHours = blockedHoursInTerm(term, nonTeachingDates);
        List<CourseOffering> nonElectiveOfferings = semesterNumber == null
            ? List.of() : nonElectiveOfferings(termInstanceId, semesterNumber);
        int curriculumHoursRequired = curriculumHoursRequired(nonElectiveOfferings);
        double bufferHours = totalWorkingPeriodHours - blockedHours - curriculumHoursRequired;

        List<SuggestedSectionResponse> suggestedSections = suggestSections(classroomsForSectioning, strength);
        int weeksInTermForBatches = CurriculumHoursCalculator.weeksInTerm(term);
        int weeklyAvailableForBatches = weeklyAvailablePeriods(term, activePeriods);
        LabClinicalSuggestion labClinicalSuggestion =
            suggestLabClinicalBatches(nonElectiveOfferings, suggestedSections, fittingLabs, fittingClinicalVenues,
                weeksInTermForBatches, periodDurationMinutes, weeklyAvailableForBatches);
        List<SuggestedBatchResponse> suggestedLabClinicalBatches = labClinicalSuggestion.batches();
        boolean labClinicalMappingSufficient = labClinicalSuggestion.mappingIssues().isEmpty();
        String labClinicalMappingIssuesMessage = labClinicalMappingSufficient
            ? null : String.join("; ", labClinicalSuggestion.mappingIssues());

        int totalSlots = WORKING_DAYS_PER_WEEK * activePeriods.size();
        // isActive filter matters here: findByTermInstanceIdAndStatus filters workflow status
        // (PUBLISHED/DRAFT) only, not soft-delete -- a superseded/removed row (e.g. from a
        // Skeleton Builder revert-and-regenerate cycle) can still carry PUBLISHED/DRAFT status
        // while isActive=false, and without this filter it stayed counted as "occupied" here
        // forever, inflating this venue-utilization tile independently of everything actually
        // still on the calendar (found 2026-08-31: Computer Lab showing 80/40, 200% from 80
        // real-but-inactive stale rows for two subjects, with zero currently-active rows).
        List<ClassSchedule> termSchedule = List.copyOf(
            java.util.stream.Stream.concat(
                classScheduleRepository.findByTermInstanceIdAndStatus(termInstanceId, ClassScheduleStatus.PUBLISHED).stream(),
                classScheduleRepository.findByTermInstanceIdAndStatus(termInstanceId, ClassScheduleStatus.DRAFT).stream()
            ).filter(cs -> Boolean.TRUE.equals(cs.getIsActive())).toList());

        return new CapacityPlanResponse(
            cohortId,
            cohort.getDisplayName(),
            termInstanceId,
            term.getAcademicYear().getName() + " " + term.getTermType(),
            semesterNumber,
            strength,
            enrolledStrength,
            sanctionedStrength,
            workingDaysInTerm,
            totalWorkingPeriodHours,
            blockedHours,
            curriculumHoursRequired,
            bufferHours,
            theoryFits,
            theoryShortfall,
            fittingClassrooms,
            classroomsForSectioning,
            fittingLabs,
            fittingClinicalVenues,
            classroomUtilization(activeClassrooms, termSchedule, totalSlots, claimedByAnyCohortId, claimedByOtherCohortLabel),
            utilization(activeLabs, Lab::getId, Lab::getName, Lab::getCapacity,
                termSchedule, ClassSessionType.LAB, cs -> cs.getLab() != null ? cs.getLab().getId() : null, totalSlots, Map.of()),
            utilization(activeClinicalVenues, ClinicalVenue::getId, ClinicalVenue::getName, ClinicalVenue::getCapacity,
                termSchedule, ClassSessionType.CLINICAL, cs -> cs.getClinicalVenue() != null ? cs.getClinicalVenue().getId() : null, totalSlots,
                Map.of()),
            suggestedSections,
            suggestedLabClinicalBatches,
            labClinicalMappingSufficient,
            labClinicalMappingIssuesMessage
        );
    }

    /** One cohort's {@link #getPlan} result plus its real committed-allocation status/counts --
     *  extracted from {@link #getTermOverview}'s own cohort loop so {@link
     *  #computeLabClinicalVenueCapacity} can reuse the exact same per-cohort planning pass (same
     *  provisional-room-claim ordering, same suggestion algorithm) instead of re-running it a
     *  second time within the same overview request. */
    private record CohortPlan(CapacityPlanResponse plan, boolean committed, int committedSectionsCount, int committedBatchesCount) {}

    /** Plans every Cohort with an ENROLLED student in {@code termInstanceId}, in the same
     *  deterministic display order {@link #getTermOverview}'s {@code cohortRows} sorts by,
     *  reserving each still-uncommitted cohort's suggested classrooms against the next cohort in
     *  the pass via {@code provisionallyClaimedClassroomIds} -- see the field-level comment
     *  formerly here, now inlined below, for why that reservation exists. */
    private List<CohortPlan> planEveryCohort(Long termInstanceId, PlanningBasis planningBasis) {
        Set<Long> cohortIds = studentTermEnrollmentRepository
            .findDistinctCohortIdsByTermInstanceId(termInstanceId, EnrollmentStatus.ENROLLED);

        List<Cohort> cohortsInPlanOrder = cohortRepository.findAllById(cohortIds).stream()
            .sorted(Comparator.comparing(Cohort::getDisplayName))
            .toList();

        // Rooms already handed to an earlier, still-uncommitted cohort's suggestion in THIS pass --
        // without this, two Not Planned cohorts reviewed side-by-side would each independently pick
        // the same "best fit" room, since neither has a real CohortSection claim yet to exclude it
        // for the other. Committed cohorts never add to this (their rooms are already excluded via
        // the real committed-claim path inside getPlan), and never consume from it either.
        Set<Long> provisionallyClaimedClassroomIds = new HashSet<>();

        List<CohortPlan> cohortPlans = new ArrayList<>();
        for (Cohort cohortRow : cohortsInPlanOrder) {
            Long cohortId = cohortRow.getId();
            Optional<CohortRoomAllocation> committedAllocation = cohortRoomAllocationRepository
                .findByCohortIdAndTermInstanceIdAndStatus(cohortId, termInstanceId, CohortRoomAllocationStatus.COMMITTED);
            boolean committed = committedAllocation.isPresent();
            int committedSectionsCount = committedAllocation
                .map(a -> cohortSectionRepository.findByCohortRoomAllocationIdAndIsActiveTrue(a.getId()).size())
                .orElse(0);
            int committedBatchesCount = committedAllocation
                .map(a -> batchRepository.findByCohortRoomAllocationIdAndIsActiveTrue(a.getId()).size())
                .orElse(0);
            CapacityPlanResponse plan = getPlan(termInstanceId, cohortId, planningBasis, provisionallyClaimedClassroomIds);
            if (!committed) {
                for (SuggestedSectionResponse section : plan.suggestedSections()) {
                    provisionallyClaimedClassroomIds.add(section.classroomId());
                }
            }
            cohortPlans.add(new CohortPlan(plan, committed, committedSectionsCount, committedBatchesCount));
        }
        return cohortPlans;
    }

    /** Term-wide overview for the Capacity Auto-Plan bulk screen -- every Cohort with an ENROLLED
     *  student in this term, each cohort's committed-allocation status, its suggested
     *  sections/batches (reusing {@link #getPlan} via {@link #planEveryCohort}, so there is exactly
     *  one implementation of the suggestion algorithm), a strict Theory sufficiency check, a
     *  Lab/Clinical weekly-capacity feasibility check ({@link #computeLabClinicalVenueCapacity}),
     *  and a whole-term room inventory. Committed cohorts are never re-planned -- their suggestions
     *  are surfaced empty since the bulk screen never acts on them. */
    public TermCapacityOverviewResponse getTermOverview(Long termInstanceId, PlanningBasis planningBasis) {
        TermInstance term = termInstanceRepository.findById(termInstanceId)
            .orElseThrow(() -> new ResourceNotFoundException("Term instance not found with id: " + termInstanceId));

        List<CohortPlan> cohortPlans = planEveryCohort(termInstanceId, planningBasis);

        List<CohortAutoPlanSummaryResponse> cohortRows = new ArrayList<>();
        for (CohortPlan cohortPlan : cohortPlans) {
            CapacityPlanResponse plan = cohortPlan.plan();
            boolean committed = cohortPlan.committed();
            cohortRows.add(new CohortAutoPlanSummaryResponse(
                plan.cohortId(),
                plan.cohortLabel(),
                plan.semesterNumber(),
                plan.cohortStrength(),
                committed,
                plan.theoryFits(),
                plan.theoryShortfallMessage(),
                committed ? List.of() : plan.suggestedSections(),
                committed ? List.of() : plan.suggestedLabClinicalBatches(),
                committed || plan.labClinicalMappingSufficient(),
                committed ? null : plan.labClinicalMappingIssuesMessage(),
                cohortPlan.committedSectionsCount(),
                cohortPlan.committedBatchesCount()
            ));
        }
        cohortRows.sort(Comparator.comparing(CohortAutoPlanSummaryResponse::cohortLabel));

        // Theory sufficiency: a genuine bin-packing feasibility check, not a naive capacity sum.
        // Summing every free classroom's capacity and comparing it against total demand looks fine
        // on paper (e.g. four 60-cap rooms = 240 "covers" 200 students) but is WRONG the moment one
        // cohort doesn't fit a single room: a 100-student cohort with only 60/80-cap rooms available
        // needs at least two DISTINCT rooms of its own (e.g. 60+40) -- it can't draw "leftover"
        // capacity out of a room another cohort is sitting in. The cohortRows loop above already ran
        // the real greedy-fill (suggestSections) per cohort, sequentially reserving rooms via
        // provisionallyClaimedClassroomIds exactly as committing them in this order would -- so
        // whether every not-yet-planned cohort's suggestion actually covers its full strength IS the
        // true feasibility answer, for free, no separate simulation needed.
        List<Classroom> activeClassrooms = classroomRepository.findByIsActiveTrueOrderByNameAsc();
        List<com.cms.model.CohortSection> activeSectionsThisTerm = cohortSectionRepository.findByTermInstanceIdAndIsActiveTrue(termInstanceId);
        Map<Long, String> claimedByCohortLabel = activeSectionsThisTerm.stream()
            .collect(Collectors.toMap(s -> s.getClassroom().getId(), s -> s.getCohortRoomAllocation().getCohort().getDisplayName(), (a, b) -> a));

        int totalFreeClassroomCapacity = activeClassrooms.stream()
            .filter(c -> !claimedByCohortLabel.containsKey(c.getId()))
            .mapToInt(c -> c.getCapacity() != null ? c.getCapacity() : 0)
            .sum();
        int totalNotPlannedStrength = cohortRows.stream()
            .filter(r -> !r.hasCommittedAllocation())
            .mapToInt(r -> (int) r.cohortStrength())
            .sum();

        // Each under-covered cohort's own shortfall (strength - seated) is the exact number of
        // additional seats needed, wherever they come from -- a small bump (e.g. 60->80 on one
        // already-used room) may still leave the cohort short if it doesn't close the actual gap;
        // naming the real number tells the admin precisely how far a fix needs to go, rather than a
        // vague "increase capacity" that could turn out to still not be enough.
        List<String> underCoveredCohorts = new ArrayList<>();
        for (CohortAutoPlanSummaryResponse row : cohortRows) {
            if (row.hasCommittedAllocation()) continue;
            long seated = row.suggestedSections().stream().mapToInt(SuggestedSectionResponse::plannedSize).sum();
            long shortfall = row.cohortStrength() - seated;
            if (shortfall > 0) {
                underCoveredCohorts.add(row.cohortLabel() + " (" + seated + " of " + row.cohortStrength() + " seated, "
                    + shortfall + " short)");
            }
        }
        boolean theorySufficient = underCoveredCohorts.isEmpty();
        String theorySufficiencyMessage = theorySufficient ? null
            : "Not enough distinct classrooms to seat every not-yet-planned cohort: " + String.join("; ", underCoveredCohorts)
                + ". For each, increase total capacity by at least the seats it's short -- either enlarge an existing"
                + " classroom enough to cover the remainder, or add a new classroom seating at least that many"
                + " (a small capacity bump may still not be enough).";

        // Term-wide Lab/Clinical mapping sufficiency -- same "block every commit until resolved"
        // treatment as the Theory check above, per explicit product decision: a subject anywhere in
        // the term suggesting into an unrelated venue (or nothing at all) is a data-correctness
        // problem, not a per-cohort inconvenience.
        List<String> mappingIssueLines = new ArrayList<>();
        for (CohortAutoPlanSummaryResponse row : cohortRows) {
            if (row.hasCommittedAllocation() || row.labClinicalMappingSufficient()) continue;
            mappingIssueLines.add(row.cohortLabel() + ": " + row.labClinicalMappingIssuesMessage());
        }
        boolean labClinicalMappingSufficient = mappingIssueLines.isEmpty();
        String labClinicalMappingIssuesMessage = labClinicalMappingSufficient
            ? null : String.join("; ", mappingIssueLines);

        // Room inventory: every active room of all three types. Classrooms stay full-or-empty
        // (claimedByCohortLabel only, matching Theory's exclusive-per-term lock) -- no percentage,
        // since a fractional "62% occupied" reading has no meaning for a room one cohort either
        // holds for the whole term or doesn't. Lab/Clinical venues instead carry real weekly
        // period-slot occupancy (they're genuinely shared across different day/period slots), reusing
        // the exact same #utilization computation Capacity Planner's own Venue Utilization panel
        // already uses -- single source of truth, not a second implementation.
        List<Period> activePeriods = periodRepository.findByIsActiveTrueOrderByPeriodOrderAsc();
        int totalSlots = activePeriods.size() * WORKING_DAYS_PER_WEEK;
        // isActive filter matters here: findByTermInstanceIdAndStatus filters workflow status
        // (PUBLISHED/DRAFT) only, not soft-delete -- a superseded/removed row (e.g. from a
        // Skeleton Builder revert-and-regenerate cycle) can still carry PUBLISHED/DRAFT status
        // while isActive=false, and without this filter it stayed counted as "occupied" here
        // forever, inflating this venue-utilization tile independently of everything actually
        // still on the calendar (found 2026-08-31: Computer Lab showing 80/40, 200% from 80
        // real-but-inactive stale rows for two subjects, with zero currently-active rows).
        List<ClassSchedule> termSchedule = List.copyOf(
            java.util.stream.Stream.concat(
                classScheduleRepository.findByTermInstanceIdAndStatus(termInstanceId, ClassScheduleStatus.PUBLISHED).stream(),
                classScheduleRepository.findByTermInstanceIdAndStatus(termInstanceId, ClassScheduleStatus.DRAFT).stream()
            ).filter(cs -> Boolean.TRUE.equals(cs.getIsActive())).toList());

        List<RoomInventoryRowResponse> roomInventory = new ArrayList<>();
        for (Classroom c : activeClassrooms) {
            roomInventory.add(new RoomInventoryRowResponse(c.getId(), c.getName(), "CLASSROOM", c.getCapacity(),
                claimedByCohortLabel.get(c.getId()), 0L, 0, 0.0));
        }
        List<Lab> activeLabs = labRepository.findAll().stream()
            .filter(l -> l.getStatus() == LabStatus.ACTIVE || l.getStatus() == LabStatus.AVAILABLE)
            .toList();
        Map<Long, VenueUtilizationResponse> labUtilizationById = utilization(activeLabs, Lab::getId, Lab::getName, Lab::getCapacity,
            termSchedule, ClassSessionType.LAB, cs -> cs.getLab() != null ? cs.getLab().getId() : null, totalSlots, Map.of())
            .stream().collect(Collectors.toMap(VenueUtilizationResponse::id, u -> u));
        for (Lab l : activeLabs) {
            VenueUtilizationResponse u = labUtilizationById.get(l.getId());
            roomInventory.add(new RoomInventoryRowResponse(l.getId(), l.getName(), "LAB", l.getCapacity(),
                null, u != null ? u.occupiedSlots() : 0L, u != null ? u.totalSlots() : 0, u != null ? u.utilizationPercent() : 0.0));
        }
        List<ClinicalVenue> activeClinicalVenues = clinicalVenueRepository.findByIsActiveTrueOrderByNameAsc();
        Map<Long, VenueUtilizationResponse> clinicalUtilizationById = utilization(activeClinicalVenues, ClinicalVenue::getId,
            ClinicalVenue::getName, ClinicalVenue::getCapacity, termSchedule, ClassSessionType.CLINICAL,
            cs -> cs.getClinicalVenue() != null ? cs.getClinicalVenue().getId() : null, totalSlots, Map.of())
            .stream().collect(Collectors.toMap(VenueUtilizationResponse::id, u -> u));
        for (ClinicalVenue v : activeClinicalVenues) {
            VenueUtilizationResponse u = clinicalUtilizationById.get(v.getId());
            roomInventory.add(new RoomInventoryRowResponse(v.getId(), v.getName(), "CLINICAL", v.getCapacity(),
                null, u != null ? u.occupiedSlots() : 0L, u != null ? u.totalSlots() : 0, u != null ? u.utilizationPercent() : 0.0));
        }

        LabClinicalVenueCapacityResult venueCapacity = computeLabClinicalVenueCapacity(term, activePeriods, cohortPlans);
        boolean labClinicalVenueCapacitySufficient = venueCapacity.overCapacityVenues().isEmpty();
        String labClinicalVenueCapacityIssuesMessage = labClinicalVenueCapacitySufficient ? null
            : formatVenueCapacityIssuesMessage(venueCapacity.overCapacityVenues());
        boolean labClinicalVenueCapacityTight = !venueCapacity.tightCapacityVenues().isEmpty();
        String labClinicalVenueCapacityTightMessage = !labClinicalVenueCapacityTight ? null
            : formatVenueCapacityTightMessage(venueCapacity.tightCapacityVenues());

        return new TermCapacityOverviewResponse(termInstanceId, theorySufficient, totalFreeClassroomCapacity,
            totalNotPlannedStrength, theorySufficiencyMessage, cohortRows, roomInventory,
            labClinicalMappingSufficient, labClinicalMappingIssuesMessage,
            labClinicalVenueCapacitySufficient, labClinicalVenueCapacityIssuesMessage,
            labClinicalVenueCapacityTight, labClinicalVenueCapacityTightMessage,
            venueCapacity.overCapacityVenues(), venueCapacity.tightCapacityVenues());
    }

    /** Run-scoped tally for one Lab or Clinical venue's total weekly demand across every cohort
     *  sharing it this term -- mirrors {@code TimetableGlobalAutoScheduleService.VenueGapAccumulator}'s
     *  shape/key scheme ({@code sessionType + ":" + venueId}), but accumulates a demand TOTAL
     *  up front (before any placement is attempted) rather than a post-hoc failure tally. */
    private static final class VenueDemandAccumulator {
        final Long venueId;
        final String venueType;
        final String venueName;
        final Integer capacity;
        int weeklyDemandPeriods;
        /** id -> name, insertion-ordered -- keyed by id (not name) so {@code affectedSubjectIds} can
         *  drive the frontend's auto-link-on-create-venue flow (see {@code
         *  VenueOverCapacity#affectedSubjectIds}). */
        final Map<Long, String> subjects = new LinkedHashMap<>();

        VenueDemandAccumulator(Long venueId, String venueType, String venueName, Integer capacity) {
            this.venueId = venueId;
            this.venueType = venueType;
            this.venueName = venueName;
            this.capacity = capacity;
        }
    }

    /** Public entry point -- resolves the term and its active periods, plans every cohort fresh
     *  (see {@link #planEveryCohort}), then delegates to the shared core below. Called
     *  independently by {@code TimetableGlobalAutoScheduleService} (Run Automation's prerequisite
     *  checklist and its own defensive re-check) -- {@link #getTermOverview} calls the private
     *  overload directly instead, since it already has {@code term}/{@code activePeriods}/{@code
     *  cohortPlans} on hand and re-running {@link #planEveryCohort} a second time in the same
     *  request would double its cost for no reason. */
    public LabClinicalVenueCapacityResult computeLabClinicalVenueCapacity(Long termInstanceId, PlanningBasis planningBasis) {
        TermInstance term = termInstanceRepository.findById(termInstanceId)
            .orElseThrow(() -> new ResourceNotFoundException("Term instance not found with id: " + termInstanceId));
        List<Period> activePeriods = periodRepository.findByIsActiveTrueOrderByPeriodOrderAsc();
        List<CohortPlan> cohortPlans = planEveryCohort(termInstanceId, planningBasis);
        return computeLabClinicalVenueCapacity(term, activePeriods, cohortPlans);
    }

    /** Real feasibility core: for every Lab/Clinical venue referenced by either a genuinely
     *  committed {@link Batch} row (real allocation, already fixed) or a not-yet-committed
     *  cohort's {@link #suggestLabClinicalBatches} suggestion (from {@code cohortPlans}), sums
     *  each contributing batch's own full weekly demand ({@code sessionsPerWeek * blockSize}
     *  periods -- every batch is its own separately-scheduled session, per {@link
     *  #splitIntoSequentialBatches}'s javadoc, so each one demands its own full weekly quota, not
     *  a shared fraction of one) and classifies the venue as over/tight/fine against {@link
     *  #weeklyAvailablePeriods(TermInstance, List)}. This is a necessary-condition aggregate, not a
     *  true day/period collision simulation -- see {@link LabClinicalVenueCapacityResult}'s javadoc. */
    private LabClinicalVenueCapacityResult computeLabClinicalVenueCapacity(TermInstance term, List<Period> activePeriods,
                                                                             List<CohortPlan> cohortPlans) {
        int weeksInTerm = CurriculumHoursCalculator.weeksInTerm(term);
        double periodDurationMinutes = CurriculumHoursCalculator.averageDurationMinutes(
            activePeriods.stream().map(Period::getDurationMinutes).toList());
        int weeklyAvailable = weeklyAvailablePeriods(term, activePeriods);

        Map<String, VenueDemandAccumulator> demandByVenue = buildVenueDemand(term, cohortPlans, weeksInTerm, periodDurationMinutes);

        List<VenueOverCapacity> overCapacityVenues = new ArrayList<>();
        List<VenueTightCapacity> tightCapacityVenues = new ArrayList<>();
        for (VenueDemandAccumulator acc : demandByVenue.values()) {
            List<String> subjectNames = new ArrayList<>(acc.subjects.values());
            List<Long> subjectIds = new ArrayList<>(acc.subjects.keySet());
            if (acc.weeklyDemandPeriods > weeklyAvailable) {
                overCapacityVenues.add(new VenueOverCapacity(acc.venueId, acc.venueType, acc.venueName, acc.capacity,
                    weeklyAvailable, acc.weeklyDemandPeriods, acc.weeklyDemandPeriods - weeklyAvailable, subjectNames,
                    subjectIds));
            } else if (weeklyAvailable > 0 && acc.weeklyDemandPeriods >= weeklyAvailable * TIGHT_CAPACITY_THRESHOLD) {
                double utilizationPercent = (acc.weeklyDemandPeriods * 100.0) / weeklyAvailable;
                tightCapacityVenues.add(new VenueTightCapacity(acc.venueId, acc.venueType, acc.venueName, acc.capacity,
                    weeklyAvailable, acc.weeklyDemandPeriods, utilizationPercent, subjectNames, subjectIds));
            }
        }
        overCapacityVenues.sort(Comparator.comparing(VenueOverCapacity::venueName, String.CASE_INSENSITIVE_ORDER));
        tightCapacityVenues.sort(Comparator.comparing(VenueTightCapacity::venueName, String.CASE_INSENSITIVE_ORDER));
        return new LabClinicalVenueCapacityResult(overCapacityVenues, tightCapacityVenues);
    }

    /** Builds the same {@code sessionType:venueId -> VenueDemandAccumulator} map {@link
     *  #computeLabClinicalVenueCapacity} classifies into over/tight/fine — extracted so {@link
     *  #previewRebalance} can look up ANY venue's current real demand (including one under the
     *  tight-capacity threshold, which never appears in that method's over/tight lists), not just
     *  the ones already flagged. */
    private Map<String, VenueDemandAccumulator> buildVenueDemand(TermInstance term, List<CohortPlan> cohortPlans,
                                                                   int weeksInTerm, double periodDurationMinutes) {
        Map<String, VenueDemandAccumulator> demandByVenue = new LinkedHashMap<>();

        // Committed cohorts: real Batch rows already carry a fixed venue -- these are facts, not
        // suggestions, so they're read directly rather than re-derived from getPlan.
        for (Batch batch : batchRepository.findByTermInstanceIdAndIsActiveTrue(term.getId())) {
            CourseOffering offering = batch.getCourseOffering();
            if (offering == null) continue;
            ClassSessionType sessionType;
            Long venueId;
            String venueName;
            Integer capacity;
            if (batch.getLab() != null) {
                sessionType = ClassSessionType.LAB;
                venueId = batch.getLab().getId();
                venueName = batch.getLab().getName();
                capacity = batch.getLab().getCapacity();
            } else if (batch.getClinicalVenue() != null) {
                sessionType = ClassSessionType.CLINICAL;
                venueId = batch.getClinicalVenue().getId();
                venueName = batch.getClinicalVenue().getName();
                capacity = batch.getClinicalVenue().getCapacity();
            } else {
                continue;
            }
            accumulateVenueDemand(demandByVenue, sessionType, venueId, venueName, capacity,
                offering, weeksInTerm, periodDurationMinutes);
        }

        // Not-yet-committed cohorts: the same suggested-batch algorithm Capacity Auto-Plan itself
        // already shows on screen -- reused as-is, not re-derived.
        for (CohortPlan cohortPlan : cohortPlans) {
            if (cohortPlan.committed()) continue;
            for (SuggestedBatchResponse row : cohortPlan.plan().suggestedLabClinicalBatches()) {
                if (row.venueId() == null) continue;
                CourseOffering offering = courseOfferingRepository.findById(row.courseOfferingId()).orElse(null);
                if (offering == null) continue;
                accumulateVenueDemand(demandByVenue, row.sessionType(), row.venueId(), row.venueName(), row.venueCapacity(),
                    offering, weeksInTerm, periodDurationMinutes);
            }
        }
        return demandByVenue;
    }

    private void accumulateVenueDemand(Map<String, VenueDemandAccumulator> demandByVenue, ClassSessionType sessionType,
                                        Long venueId, String venueName, Integer capacity, CourseOffering offering,
                                        int weeksInTerm, double periodDurationMinutes) {
        int demand = weeklyDemandPeriodsForOffering(offering, sessionType, weeksInTerm, periodDurationMinutes);
        if (demand <= 0) return;
        String key = sessionType + ":" + venueId;
        VenueDemandAccumulator acc = demandByVenue.computeIfAbsent(key,
            k -> new VenueDemandAccumulator(venueId, sessionType.name(), venueName, capacity));
        acc.weeklyDemandPeriods += demand;
        if (offering.getSubject() != null) {
            acc.subjects.put(offering.getSubject().getId(), offering.getSubject().getName());
        }
    }

    /** This offering's own weekly demand periods for one session type -- {@code sessionsPerWeek *
     *  blockSize}, the same figures {@code TimetableGlobalAutoScheduleService} places against, so
     *  this feasibility check can never disagree with what a real run would actually attempt. */
    private int weeklyDemandPeriodsForOffering(CourseOffering offering, ClassSessionType sessionType,
                                                int weeksInTerm, double periodDurationMinutes) {
        CurriculumSemesterCourse csc = offering.getCurriculumSemesterCourse();
        if (csc == null) return 0;
        Integer hours = sessionType == ClassSessionType.LAB ? csc.getLabHours() : csc.getClinicalHours();
        int totalHours = hours != null ? hours : 0;
        int blockSize = CurriculumHoursCalculator.resolveBlockSize(offering.getSubject(), sessionType);
        int sessionsPerWeek = CurriculumHoursCalculator.sessionsPerWeek(totalHours, weeksInTerm, periodDurationMinutes, blockSize);
        return sessionsPerWeek * blockSize;
    }

    /** "Rebalance now" preview — the minimum number of already-committed batches sharing {@code
     *  venueId} that need to move to relieve it, each paired with the best-fitting alternate venue
     *  its subject is already eligible for (never an unrelated one — same DESIGNATED-ONLY rule
     *  {@link #suggestBatchesForSessionType} follows). Offered — and accepted here — for a venue at
     *  either the "over" or "tight" reading {@link #computeLabClinicalVenueCapacity} itself would
     *  report ({@link #TIGHT_CAPACITY_THRESHOLD} shared with that method, so this can never accept a
     *  venue that method would call "fine"): a genuinely over-capacity venue must get back under its
     *  own weekly window, while a merely tight one already fits on raw totals but the scheduler never
     *  splits one subject across two venues on its own (see the precheck copy shown alongside this
     *  panel), so moving a batch here is the only real way to turn a newly-added second venue into
     *  actual slack. Always recomputes fresh (never trusts a caller-supplied {@code
     *  affectedSubjectIds}) so this can never act on stale data. Read-only — nothing is applied until
     *  {@link #applyRebalance}. */
    public VenueRebalancePreview previewRebalance(Long termInstanceId, ClassSessionType sessionType, Long venueId,
                                                    PlanningBasis planningBasis) {
        TermInstance term = termInstanceRepository.findById(termInstanceId)
            .orElseThrow(() -> new ResourceNotFoundException("Term instance not found with id: " + termInstanceId));
        List<Period> activePeriods = periodRepository.findByIsActiveTrueOrderByPeriodOrderAsc();
        List<CohortPlan> cohortPlans = planEveryCohort(termInstanceId, planningBasis);
        int weeksInTerm = CurriculumHoursCalculator.weeksInTerm(term);
        double periodDurationMinutes = CurriculumHoursCalculator.averageDurationMinutes(
            activePeriods.stream().map(Period::getDurationMinutes).toList());
        int weeklyAvailable = weeklyAvailablePeriods(term, activePeriods);
        Map<String, VenueDemandAccumulator> demandByVenue = buildVenueDemand(term, cohortPlans, weeksInTerm, periodDurationMinutes);

        String sourceKey = sessionType + ":" + venueId;
        VenueDemandAccumulator sourceAcc = demandByVenue.get(sourceKey);
        int weeklyTightThreshold = (int) Math.ceil(weeklyAvailable * TIGHT_CAPACITY_THRESHOLD);
        if (sourceAcc == null || sourceAcc.weeklyDemandPeriods < weeklyTightThreshold) {
            throw new LifecycleConflictException(
                "This venue is not currently over or at its weekly capacity — there is nothing to rebalance.",
                "VENUE_NOT_OVER_CAPACITY", "ClinicalVenue", venueId, null);
        }
        Set<Long> affectedSubjectIds = sourceAcc.subjects.keySet();

        // Committed batches actually sitting on the source venue for the affected subjects,
        // largest-demand-first so moving the fewest of them clears the shortfall.
        List<Batch> candidates = batchRepository.findByTermInstanceIdAndIsActiveTrue(termInstanceId).stream()
            .filter(b -> b.getCourseOffering() != null && b.getCourseOffering().getSubject() != null
                && affectedSubjectIds.contains(b.getCourseOffering().getSubject().getId()))
            .filter(b -> venueId.equals(sessionType == ClassSessionType.LAB
                ? (b.getLab() != null ? b.getLab().getId() : null)
                : (b.getClinicalVenue() != null ? b.getClinicalVenue().getId() : null)))
            .sorted(Comparator.comparingInt((Batch b) ->
                    weeklyDemandPeriodsForOffering(b.getCourseOffering(), sessionType, weeksInTerm, periodDurationMinutes))
                .reversed())
            .toList();

        // Over capacity: must get back under the hard weekly window. Merely tight (fits, but with
        // no real slack): move enough to land strictly under the same tight threshold that flagged
        // it, so the freed room is genuine, not a zero-move no-op.
        int shortfall = sourceAcc.weeklyDemandPeriods > weeklyAvailable
            ? sourceAcc.weeklyDemandPeriods - weeklyAvailable
            : sourceAcc.weeklyDemandPeriods - weeklyTightThreshold + 1;
        List<Batch> toMove = new ArrayList<>();
        int freed = 0;
        for (Batch b : candidates) {
            if (freed >= shortfall) break;
            toMove.add(b);
            freed += weeklyDemandPeriodsForOffering(b.getCourseOffering(), sessionType, weeksInTerm, periodDurationMinutes);
        }

        // Running copy so picking a destination for one batch counts against the next pick too --
        // two batches from this pass must never both get sent to the same venue past ITS window.
        Map<String, Integer> runningDemand = new HashMap<>();
        List<VenueRebalanceMove> willMove = new ArrayList<>();
        List<VenueRebalanceBlocked> notMovable = new ArrayList<>();
        for (Batch b : toMove) {
            var subject = b.getCourseOffering().getSubject();
            int batchDemand = weeklyDemandPeriodsForOffering(b.getCourseOffering(), sessionType, weeksInTerm, periodDurationMinutes);
            List<VenueOptionResponse> eligible = eligibleActiveVenues(subject, sessionType).stream()
                .filter(v -> !v.id().equals(venueId))
                .toList();
            VenueOptionResponse best = null;
            int bestSpare = Integer.MIN_VALUE;
            for (VenueOptionResponse candidate : eligible) {
                String key = sessionType + ":" + candidate.id();
                int currentDemand = runningDemand.getOrDefault(key,
                    demandByVenue.containsKey(key) ? demandByVenue.get(key).weeklyDemandPeriods : 0);
                int spare = weeklyAvailable - currentDemand;
                if (spare > bestSpare) {
                    bestSpare = spare;
                    best = candidate;
                }
            }
            if (best == null) {
                notMovable.add(new VenueRebalanceBlocked(b.getId(), b.getName(), subject.getName(),
                    subject.getName() + " has no other eligible " + (sessionType == ClassSessionType.LAB ? "Lab" : "Clinical venue")
                        + " designated — add one before this batch can be moved."));
                continue;
            }
            String destKey = sessionType + ":" + best.id();
            runningDemand.put(destKey, runningDemand.getOrDefault(destKey,
                demandByVenue.containsKey(destKey) ? demandByVenue.get(destKey).weeklyDemandPeriods : 0) + batchDemand);

            int sessionsRiding = classScheduleRepository.findByBatchIdInAndIsActiveTrue(List.of(b.getId())).size();
            willMove.add(new VenueRebalanceMove(b.getId(), b.getName(), subject.getName(),
                b.getCohortRoomAllocation() != null && b.getCohortRoomAllocation().getCohort() != null
                    ? b.getCohortRoomAllocation().getCohort().getDisplayName() : null,
                b.getCohortSection() != null ? b.getCohortSection().getSectionLabel() : null,
                b.getCapacity(), venueId, sourceAcc.venueName, best.id(), best.name(), sessionsRiding));
        }

        return new VenueRebalancePreview(venueId, sourceAcc.venueName, sessionType, weeklyAvailable,
            sourceAcc.weeklyDemandPeriods, willMove, notMovable);
    }

    /** Every active Lab (or Clinical Venue) this subject is eligible for — designated-only, no
     *  fallback, mirroring {@link #eligibleAndActive}'s own rule, but self-contained (queries fresh
     *  rather than requiring a pre-built {@code activeVenues} list) since {@link #previewRebalance}
     *  has no per-cohort {@link #getPlan} context to reuse one from. */
    private List<VenueOptionResponse> eligibleActiveVenues(com.cms.model.Subject subject, ClassSessionType sessionType) {
        if (sessionType == ClassSessionType.LAB) {
            Set<Long> eligibleIds = subject.getEligibleLabs().stream().map(Lab::getId).collect(Collectors.toSet());
            return labRepository.findAll().stream()
                .filter(l -> l.getStatus() == LabStatus.ACTIVE || l.getStatus() == LabStatus.AVAILABLE)
                .filter(l -> eligibleIds.contains(l.getId()))
                .map(l -> new VenueOptionResponse(l.getId(), l.getName(), l.getCapacity()))
                .toList();
        }
        Set<Long> eligibleIds = subject.getEligibleClinicalVenues().stream().map(ClinicalVenue::getId).collect(Collectors.toSet());
        return clinicalVenueRepository.findByIsActiveTrueOrderByNameAsc().stream()
            .filter(v -> eligibleIds.contains(v.getId()))
            .map(v -> new VenueOptionResponse(v.getId(), v.getName(), v.getCapacity()))
            .toList();
    }

    /** Applies a previously-previewed Rebalance — every {@code batchIds} entry is re-validated
     *  fresh (still active, still on {@code venueId}, still resolvable to a real destination)
     *  rather than trusted from the preview, since time may have passed between the two calls (see
     *  {@link VenueRebalanceApplyRequest}'s javadoc). Guards against an already-PUBLISHED riding
     *  session exactly like {@link CohortRoomAllocationService#revert} — real students/faculty
     *  already rely on a published session, so it must be explicitly unpublished first, never
     *  silently moved. */
    @Transactional
    public VenueRebalanceResult applyRebalance(Long termInstanceId, ClassSessionType sessionType, Long venueId,
                                                 List<Long> batchIds, String actor) {
        VenueRebalancePreview freshPreview = previewRebalance(termInstanceId, sessionType, venueId, PlanningBasis.SANCTIONED);
        Map<Long, VenueRebalanceMove> byBatchId = freshPreview.willMove().stream()
            .collect(Collectors.toMap(VenueRebalanceMove::batchId, m -> m));

        List<VenueRebalanceMove> moved = new ArrayList<>();
        int sessionsCleared = 0;
        for (Long batchId : batchIds) {
            VenueRebalanceMove move = byBatchId.get(batchId);
            if (move == null) {
                throw new LifecycleConflictException(
                    "This batch is no longer eligible to move — it may have already been rebalanced, or the venue's "
                        + "capacity situation has changed. Re-open the preview and try again.",
                    "VENUE_REBALANCE_STALE_BATCH", "Batch", batchId, null);
            }
            Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found with id: " + batchId));

            List<ClassSchedule> ridingCells = classScheduleRepository.findByBatchIdInAndIsActiveTrue(List.of(batchId));
            boolean hasPublished = ridingCells.stream().anyMatch(cs -> cs.getStatus() == ClassScheduleStatus.PUBLISHED);
            if (hasPublished) {
                throw new LifecycleConflictException(
                    "Batch '" + batch.getName() + "' has already-published timetable sessions riding on it — "
                        + "unpublish or remove those sessions before rebalancing its venue.",
                    "VENUE_REBALANCE_HAS_PUBLISHED_SESSIONS", "Batch", batchId, null);
            }

            for (ClassSchedule cell : ridingCells) {
                cell.setIsActive(false);
                classScheduleRepository.save(cell);
            }
            if (sessionType == ClassSessionType.LAB) {
                batch.setLab(labRepository.getReferenceById(move.toVenueId()));
            } else {
                batch.setClinicalVenue(clinicalVenueRepository.getReferenceById(move.toVenueId()));
            }
            batchRepository.save(batch);

            auditLogService.record(actor, "TIMETABLE_VENUE_REBALANCED", "Batch", batchId.toString(),
                "Moved from " + move.fromVenueName() + " to " + move.toVenueName() + " — "
                    + ridingCells.size() + " session(s) cleared for re-placement");
            sessionsCleared += ridingCells.size();
            moved.add(move);
        }
        return new VenueRebalanceResult(moved.size(), sessionsCleared, moved);
    }

    /** Real weekly (day, period) window for scheduling -- Monday-Saturday, each pair counted only
     *  when {@link TimetableBlockedPeriodChecker#blockReason} finds it free. Saturday's own
     *  working-Saturday gating (blocked entirely with no pattern configured, otherwise counted as
     *  one full weekly day regardless of how sparse the actual pattern is -- matching the real
     *  placement grid's own day-of-week-only granularity) is handled inside that checker, not
     *  duplicated here. Deliberately NOT {@link #WORKING_DAYS_PER_WEEK} -- that constant is a
     *  fixed-5 Theory-only denominator that would silently ignore an active working-Saturday
     *  pattern. */
    private int weeklyAvailablePeriods(TermInstance term, List<Period> activePeriods) {
        int count = 0;
        for (com.cms.model.enums.DayOfWeek day : com.cms.model.enums.DayOfWeek.values()) {
            for (Period period : activePeriods) {
                if (blockedPeriodChecker.blockReason(day, period.getStartTime(), period.getEndTime(), term).isEmpty()) {
                    count++;
                }
            }
        }
        return count;
    }

    private String formatVenueCapacityIssuesMessage(List<VenueOverCapacity> overCapacityVenues) {
        return overCapacityVenues.stream()
            .map(v -> v.venueName() + " needs " + v.weeklyDemandPeriods() + " periods/week but only has "
                + v.weeklyAvailablePeriods() + " available (" + v.shortfallPeriods() + " short) -- "
                + String.join(", ", v.affectedSubjectNames()))
            .collect(Collectors.joining("; "));
    }

    private String formatVenueCapacityTightMessage(List<VenueTightCapacity> tightCapacityVenues) {
        return tightCapacityVenues.stream()
            .map(v -> v.venueName() + " is at " + Math.round(v.utilizationPercent()) + "% of its weekly capacity ("
                + v.weeklyDemandPeriods() + " of " + v.weeklyAvailablePeriods() + " periods) -- "
                + String.join(", ", v.affectedSubjectNames()))
            .collect(Collectors.joining("; "));
    }

    /** Fewest-rooms EQUAL split for Theory sectioning (now the single source of truth, reused by
     *  both {@link #getPlan} and the bulk {@link #getTermOverview}) -- deliberately NOT "fill each
     *  room to its own capacity before moving to the next" (that produces lopsided sections, e.g.
     *  80+20 for a 100-strong cohort just because the first room happens to seat 80). Splits the
     *  cohort as evenly as possible across the fewest rooms whose capacity can each hold that equal
     *  share; see {@link #equalSplitSizes} for the algorithm, shared with Lab/Clinical batch
     *  splitting below. Package-private (not private) so it's directly unit-testable, same
     *  convention as {@link #nonTeachingDates} / {@link #countWorkingDays} above. */
    List<SuggestedSectionResponse> suggestSections(List<VenueOptionResponse> classroomsForSectioning, long strength) {
        List<Long> sizes = equalSplitSizes(strength, classroomsForSectioning);
        List<SuggestedSectionResponse> sections = new ArrayList<>();
        for (int i = 0; i < sizes.size(); i++) {
            VenueOptionResponse classroom = classroomsForSectioning.get(i);
            sections.add(new SuggestedSectionResponse("Section " + (i + 1), classroom.id(), classroom.name(),
                classroom.capacity(), sizes.get(i).intValue()));
        }
        return sections;
    }

    /** Splits {@code strength} as evenly as possible -- differing by at most 1 seat, never a
     *  fractional split (e.g. 95 across 2 rooms is 48+47, never 47.5 each) -- across the fewest
     *  rooms from {@code roomsSortedDesc} (already sorted biggest-first by the caller) whose
     *  capacity can each actually hold their equal share. Tries N = 1, 2, 3... (the N biggest rooms,
     *  i.e. the first N entries) and stops at the first N where the SMALLEST of those N rooms (the
     *  Nth, since sorted descending) is still big enough for {@code ceil(strength / N)} -- that's
     *  the binding constraint, since every room in an N-way equal split must hold the largest share.
     *  Falls back to greedy fill-to-capacity across every room in the pool only when no N (even
     *  using every available room) can support an equal split -- a genuine shortage; the returned
     *  sizes then legitimately sum to less than {@code strength} rather than silently overfilling a
     *  room past its own capacity, which is exactly what the term-wide sufficiency check in {@link
     *  #getTermOverview} is designed to catch and report as under-coverage. */
    private List<Long> equalSplitSizes(long strength, List<VenueOptionResponse> roomsSortedDesc) {
        if (strength <= 0 || roomsSortedDesc.isEmpty()) return List.of();

        int maxN = roomsSortedDesc.size();
        int chosenN = -1;
        for (int n = 1; n <= maxN; n++) {
            int equalShare = ceilDiv(strength, n);
            Integer nthRoomCapacity = roomsSortedDesc.get(n - 1).capacity();
            if (nthRoomCapacity != null && nthRoomCapacity >= equalShare) {
                chosenN = n;
                break;
            }
        }

        if (chosenN > 0) {
            long base = strength / chosenN;
            long remainder = strength % chosenN;
            List<Long> sizes = new ArrayList<>();
            for (int i = 0; i < chosenN; i++) {
                sizes.add(base + (i < remainder ? 1 : 0));
            }
            return sizes;
        }

        List<Long> sizes = new ArrayList<>();
        long remaining = strength;
        for (VenueOptionResponse room : roomsSortedDesc) {
            if (remaining <= 0) break;
            int capacity = room.capacity() != null ? room.capacity() : 0;
            if (capacity <= 0) continue;
            long size = Math.min(remaining, capacity);
            sizes.add(size);
            remaining -= size;
        }
        return sizes;
    }

    private static int ceilDiv(long numerator, int divisor) {
        return (int) ((numerator + divisor - 1) / divisor);
    }

    /** Batches placed + any mapping issues found while placing them -- see
     *  {@link #suggestLabClinicalBatches}. A non-empty {@code mappingIssues} means at least one
     *  offering's Lab/Clinical suggestion is either missing (no designated venue configured/exists)
     *  or under-covering (designated venues too small) -- the caller surfaces this as a hard-block
     *  alert, never silently swallowed. */
    record LabClinicalSuggestion(List<SuggestedBatchResponse> batches, List<String> mappingIssues) {}

    /** Fewest-rooms suggestion for Lab/Clinical batches, per non-elective offering with Lab/Clinical
     *  hours this term. For each suggested Theory section, prefers ONE shared venue across every
     *  section if a single venue is big enough for the largest section -- the 1-room-per-subject
     *  common case, mirroring how a human using the manual draft-builder would pick a block-wide
     *  venue. Only a section too big for every available venue falls back to a greedy pack (largest
     *  remaining venue first) into "Batch 1"/"Batch 2"... rows summing exactly to that section's
     *  headcount, the automated equivalent of the manual Create Batches / Add Batch flow. Venue
     *  selection is DESIGNATED-ONLY against the offering's Subject.eligibleLabs/
     *  eligibleClinicalVenues (see #suggestBatchesForSessionType) -- never an unrelated venue. A
     *  designated venue too small for the section is handled by reusing it across sequential batches
     *  (see #splitIntoSequentialBatches), not by under-covering -- only a subject with NO designated
     *  venue at all produces no batches plus a recorded issue in the returned {@link
     *  LabClinicalSuggestion#mappingIssues()}. Package-private for direct unit testing, same
     *  convention as {@link #suggestSections}. */
    LabClinicalSuggestion suggestLabClinicalBatches(List<CourseOffering> offerings,
                                                                     List<SuggestedSectionResponse> sections,
                                                                     List<VenueOptionResponse> fittingLabs,
                                                                     List<VenueOptionResponse> fittingClinicalVenues) {
        if (sections.isEmpty()) return new LabClinicalSuggestion(List.of(), List.of());
        List<SuggestedBatchResponse> batches = new ArrayList<>();
        List<String> issues = new ArrayList<>();
        // weeklyDemandPeriods=0 below is a deliberate no-op sentinel (see the throughput filter's
        // "weeklyDemandPeriods <= 0" guard in suggestBatchesForSessionType), NOT a stand-in
        // computed via weeklyDemandPeriodsForOffering -- this overload's callers (existing tests,
        // any caller that hasn't been updated to pass real weeksInTerm/periodDurationMinutes) have
        // no real period-duration figure to compute a genuine demand from, and feeding a fake
        // periodDurationMinutes of e.g. 0.0 into that formula would divide by zero. Keeps this
        // overload's behavior exactly seating-only, as before.
        Map<String, Integer> remainingWeeklyPeriodsByVenueKey = new HashMap<>();
        for (CourseOffering offering : offerings) {
            CurriculumSemesterCourse csc = offering.getCurriculumSemesterCourse();
            if (csc == null) continue;
            String subjectName = offering.getSubject().getName();
            if (csc.getLabHours() != null && csc.getLabHours() > 0) {
                List<VenueOptionResponse> eligibleLabs = eligibleAndActive(offering, fittingLabs, true);
                recordMappingIssue(subjectName, "Lab", eligibleLabs, !fittingLabs.isEmpty(), issues);
                batches.addAll(suggestBatchesForSessionType(offering.getId(), subjectName, ClassSessionType.LAB,
                    sections, eligibleLabs, 0, 0, remainingWeeklyPeriodsByVenueKey));
            }
            if (csc.getClinicalHours() != null && csc.getClinicalHours() > 0) {
                List<VenueOptionResponse> eligibleVenues = eligibleAndActive(offering, fittingClinicalVenues, false);
                recordMappingIssue(subjectName, "Clinical Venue", eligibleVenues, !fittingClinicalVenues.isEmpty(), issues);
                batches.addAll(suggestBatchesForSessionType(offering.getId(), subjectName, ClassSessionType.CLINICAL,
                    sections, eligibleVenues, 0, 0, remainingWeeklyPeriodsByVenueKey));
            }
        }
        return new LabClinicalSuggestion(batches, issues);
    }

    /** Same as the 4-arg overload, but throughput-aware: {@code weeksInTerm}/{@code
     *  periodDurationMinutes} let each offering's own weekly Lab/Clinical period demand be computed
     *  (see {@link #weeklyDemandPeriodsForOffering}), and {@code weeklyAvailablePeriods} (same
     *  institution-wide figure {@link #weeklyAvailablePeriods(TermInstance, List)} computes) caps
     *  how much of that demand any ONE venue can absorb before this method prefers spreading the
     *  rest across the subject's other eligible venues instead. Fixes a real gap: the "one shared
     *  venue" seating optimization used to check ONLY whether a venue could physically SEAT the
     *  cohort, never whether it had enough weekly time-slots left to actually HOST that many
     *  sessions -- so a subject (or several sections/subjects of the same cohort) could pile their
     *  entire weekly demand onto a single big-enough-to-seat-everyone venue even when that
     *  venue's real weekly window was already spoken for, and adding a second eligible venue
     *  silently did nothing because this method never got that far. A shared, running {@code
     *  remainingWeeklyPeriodsByVenueKey} tracker (keyed {@code sessionType + ":" + venueId}, same
     *  scheme {@link VenueDemandAccumulator} uses) is threaded across every offering/section this
     *  call handles, so later subjects correctly see less room left on a venue earlier ones already
     *  committed to -- scoped to this one planning call (one cohort's own offerings), not across
     *  other cohorts or already-committed Batch rows, which is out of what a single {@link
     *  #getPlan} call can see. */
    LabClinicalSuggestion suggestLabClinicalBatches(List<CourseOffering> offerings,
                                                                     List<SuggestedSectionResponse> sections,
                                                                     List<VenueOptionResponse> fittingLabs,
                                                                     List<VenueOptionResponse> fittingClinicalVenues,
                                                                     int weeksInTerm,
                                                                     double periodDurationMinutes,
                                                                     int weeklyAvailablePeriods) {
        if (sections.isEmpty()) return new LabClinicalSuggestion(List.of(), List.of());
        List<SuggestedBatchResponse> batches = new ArrayList<>();
        List<String> issues = new ArrayList<>();
        Map<String, Integer> remainingWeeklyPeriodsByVenueKey = new HashMap<>();
        for (CourseOffering offering : offerings) {
            CurriculumSemesterCourse csc = offering.getCurriculumSemesterCourse();
            if (csc == null) continue;
            String subjectName = offering.getSubject().getName();
            if (csc.getLabHours() != null && csc.getLabHours() > 0) {
                List<VenueOptionResponse> eligibleLabs = eligibleAndActive(offering, fittingLabs, true);
                recordMappingIssue(subjectName, "Lab", eligibleLabs, !fittingLabs.isEmpty(), issues);
                int demand = weeklyDemandPeriodsForOffering(offering, ClassSessionType.LAB, weeksInTerm, periodDurationMinutes);
                batches.addAll(suggestBatchesForSessionType(offering.getId(), subjectName, ClassSessionType.LAB,
                    sections, eligibleLabs, demand, weeklyAvailablePeriods, remainingWeeklyPeriodsByVenueKey));
            }
            if (csc.getClinicalHours() != null && csc.getClinicalHours() > 0) {
                List<VenueOptionResponse> eligibleVenues = eligibleAndActive(offering, fittingClinicalVenues, false);
                recordMappingIssue(subjectName, "Clinical Venue", eligibleVenues, !fittingClinicalVenues.isEmpty(), issues);
                int demand = weeklyDemandPeriodsForOffering(offering, ClassSessionType.CLINICAL, weeksInTerm, periodDurationMinutes);
                batches.addAll(suggestBatchesForSessionType(offering.getId(), subjectName, ClassSessionType.CLINICAL,
                    sections, eligibleVenues, demand, weeklyAvailablePeriods, remainingWeeklyPeriodsByVenueKey));
            }
        }
        return new LabClinicalSuggestion(batches, issues);
    }

    /** Records a human-readable issue for one subject/session-type combo whose designated venue
     *  mapping is missing entirely -- worded differently depending on whether any active venue of
     *  this type even exists at all, so the admin knows whether to configure a mapping or create a
     *  venue first. A configured-but-small mapping is NOT an issue (see
     *  {@link #splitIntoSequentialBatches} -- it's resolved via sequential batches, never a real
     *  capacity shortfall the way Theory sectioning is). A no-op when at least one designated venue
     *  is configured. */
    private void recordMappingIssue(String subjectName, String kind, List<VenueOptionResponse> eligibleVenues,
                                     boolean anyActiveVenueExists, List<String> issues) {
        if (eligibleVenues.isEmpty()) {
            issues.add(anyActiveVenueExists
                ? "'" + subjectName + "' has no designated " + kind + " configured"
                : "'" + subjectName + "' needs a " + kind + " but none exist yet -- create one");
        }
    }

    /** Intersects the offering's Subject's eligible Labs (or Clinical Venues, per {@code isLab})
     *  against the already-active-filtered {@code activeVenues} list -- an eligible venue that's
     *  since gone inactive is correctly excluded, reusing the active-filtering {@link #getPlan}
     *  already did rather than re-deriving it from the raw entity relationship. */
    private List<VenueOptionResponse> eligibleAndActive(CourseOffering offering, List<VenueOptionResponse> activeVenues, boolean isLab) {
        Set<Long> eligibleIds = isLab
            ? offering.getSubject().getEligibleLabs().stream().map(Lab::getId).collect(Collectors.toSet())
            : offering.getSubject().getEligibleClinicalVenues().stream().map(ClinicalVenue::getId).collect(Collectors.toSet());
        if (eligibleIds.isEmpty()) return List.of();
        return activeVenues.stream().filter(v -> eligibleIds.contains(v.id())).toList();
    }

    /** Designated-only, no fallback: places every batch using ONLY the subject's own eligible
     *  venues -- never an unrelated one, even if the eligible set is too small to cover demand (that
     *  under-coverage is exactly what {@link #suggestLabClinicalBatches}'s mapping-issue check is
     *  for). Returns {@code List.of()} when {@code eligibleVenues} is empty; the caller is
     *  responsible for recording why (no mapping configured vs. no active venue of this type exists
     *  at all). Every returned row still carries {@code eligibleVenueIds} so manual pickers can sort/
     *  highlight the subject's preference without a second lookup. */
    private List<SuggestedBatchResponse> suggestBatchesForSessionType(Long courseOfferingId, String subjectName,
                                                                        ClassSessionType sessionType,
                                                                        List<SuggestedSectionResponse> sections,
                                                                        List<VenueOptionResponse> eligibleVenues,
                                                                        int weeklyDemandPeriods,
                                                                        int weeklyAvailablePeriods,
                                                                        Map<String, Integer> remainingWeeklyPeriodsByVenueKey) {
        if (eligibleVenues.isEmpty()) return List.of();
        List<Long> eligibleVenueIds = eligibleVenues.stream().map(VenueOptionResponse::id).toList();

        // Prefer ONE shared venue across EVERY section (the common case -- same venue for the
        // whole subject) when a single eligible venue both seats the largest section AND has
        // enough remaining weekly throughput left to host every section's own full weekly quota
        // (each section is its own separately-scheduled batch, so sharing one venue across N
        // sections costs that venue N full quotas, not one shared fraction -- see
        // VenueDemandAccumulator's javadoc).
        int largestSection = sections.stream().mapToInt(SuggestedSectionResponse::plannedSize).max().orElse(0);
        long totalDemandIfShared = (long) weeklyDemandPeriods * sections.size();
        VenueOptionResponse sharedVenue = eligibleVenues.stream()
            .filter(v -> v.capacity() != null && v.capacity() >= largestSection)
            .filter(v -> weeklyDemandPeriods <= 0
                || remainingThroughput(remainingWeeklyPeriodsByVenueKey, sessionType, v.id(), weeklyAvailablePeriods)
                    >= totalDemandIfShared)
            .min(Comparator.comparingInt(v -> v.capacity() != null ? v.capacity() : 0))
            .orElse(null);

        List<SuggestedBatchResponse> rows = new ArrayList<>();
        if (sharedVenue != null) {
            for (SuggestedSectionResponse section : sections) {
                rows.add(new SuggestedBatchResponse(courseOfferingId, subjectName, sessionType,
                    sharedVenue.id(), sharedVenue.name(), sharedVenue.capacity(), section.sectionLabel(), null,
                    section.plannedSize(), eligibleVenueIds));
                consumeThroughput(remainingWeeklyPeriodsByVenueKey, sessionType, sharedVenue.id(),
                    weeklyAvailablePeriods, weeklyDemandPeriods);
            }
            return rows;
        }

        // No single venue can host every section together (by seats, or now by throughput) --
        // assign each section its own venue, re-narrowing to throughput-available venues FRESH
        // for each section so a later one correctly sees less room left on a venue an earlier one
        // in this same loop already claimed. Falls back to the full eligible set only when EVERY
        // eligible venue is already throughput-saturated, so batches still get placed somewhere
        // (never under-covers) -- that case then correctly surfaces as a genuine over-capacity
        // venue in computeLabClinicalVenueCapacity rather than being silently hidden.
        for (SuggestedSectionResponse section : sections) {
            List<VenueOptionResponse> throughputAvailable = weeklyDemandPeriods <= 0 ? eligibleVenues
                : eligibleVenues.stream()
                    .filter(v -> remainingThroughput(remainingWeeklyPeriodsByVenueKey, sessionType, v.id(), weeklyAvailablePeriods)
                        >= weeklyDemandPeriods)
                    .toList();
            List<VenueOptionResponse> venues = throughputAvailable.isEmpty() ? eligibleVenues : throughputAvailable;

            VenueOptionResponse soleFit = venues.stream()
                .filter(v -> v.capacity() != null && v.capacity() >= section.plannedSize())
                .min(Comparator.comparingInt(v -> v.capacity() != null ? v.capacity() : 0))
                .orElse(null);
            if (soleFit != null) {
                rows.add(new SuggestedBatchResponse(courseOfferingId, subjectName, sessionType,
                    soleFit.id(), soleFit.name(), soleFit.capacity(), section.sectionLabel(), null,
                    section.plannedSize(), eligibleVenueIds));
                consumeThroughput(remainingWeeklyPeriodsByVenueKey, sessionType, soleFit.id(),
                    weeklyAvailablePeriods, weeklyDemandPeriods);
                continue;
            }

            // Genuinely too big for one venue's seats even within this section's own
            // throughput-available pool -- split it (see #splitIntoSequentialBatches):
            // fewest-DISTINCT-venues equal split first, and if even that can't cover it, reuse
            // the single largest designated venue across as many additional turns as needed.
            // Never a fractional split.
            List<VenueOptionResponse> byCapacityDesc = venues.stream()
                .sorted(Comparator.comparingInt((VenueOptionResponse v) -> v.capacity() != null ? v.capacity() : 0).reversed())
                .toList();
            List<BatchSlot> slots = splitIntoSequentialBatches(section.plannedSize(), byCapacityDesc);
            for (int i = 0; i < slots.size(); i++) {
                BatchSlot slot = slots.get(i);
                rows.add(new SuggestedBatchResponse(courseOfferingId, subjectName, sessionType,
                    slot.venue().id(), slot.venue().name(), slot.venue().capacity(), section.sectionLabel(),
                    "Batch " + (i + 1), slot.size(), eligibleVenueIds));
                consumeThroughput(remainingWeeklyPeriodsByVenueKey, sessionType, slot.venue().id(),
                    weeklyAvailablePeriods, weeklyDemandPeriods);
            }
        }
        return rows;
    }

    private int remainingThroughput(Map<String, Integer> remainingByKey, ClassSessionType sessionType, Long venueId,
                                     int weeklyAvailablePeriods) {
        return remainingByKey.getOrDefault(sessionType + ":" + venueId, weeklyAvailablePeriods);
    }

    private void consumeThroughput(Map<String, Integer> remainingByKey, ClassSessionType sessionType, Long venueId,
                                    int weeklyAvailablePeriods, int demand) {
        String key = sessionType + ":" + venueId;
        remainingByKey.put(key, remainingByKey.getOrDefault(key, weeklyAvailablePeriods) - demand);
    }

    /** One Lab/Clinical batch's venue + headcount -- see {@link #splitIntoSequentialBatches}. */
    private record BatchSlot(VenueOptionResponse venue, int size) {}

    /** Splits a too-big-for-one-venue section into sequential Lab/Clinical batches. Unlike Theory
     *  sectioning ({@link #equalSplitSizes}, whose whole cohort attends at the SAME moment and
     *  therefore genuinely needs distinct simultaneous rooms), two batches of the same subject are
     *  two SEPARATE scheduled sessions -- Skeleton Builder decides the actual day/period later, so
     *  the same designated venue can be reused turn after turn. Tries the same fewest-distinct-venues
     *  equal split as Theory first (spreads load across multiple designated venues when more than one
     *  exists); only when that genuinely can't cover the section (even using every distinct venue
     *  once) does it fall through to reusing the single largest designated venue across as many
     *  additional equal-sized turns as needed. This always fully covers as long as at least one
     *  designated venue has capacity > 0 -- Lab/Clinical therefore has no capacity-shortfall failure
     *  mode the way Theory does; only "no designated venue configured at all" remains a real gap (see
     *  {@link #recordMappingIssue}). */
    private List<BatchSlot> splitIntoSequentialBatches(long strength, List<VenueOptionResponse> venuesSortedDesc) {
        if (strength <= 0 || venuesSortedDesc.isEmpty()) return List.of();

        List<Long> distinctVenueSizes = equalSplitSizes(strength, venuesSortedDesc);
        long covered = distinctVenueSizes.stream().mapToLong(Long::longValue).sum();
        if (covered >= strength) {
            List<BatchSlot> slots = new ArrayList<>();
            for (int i = 0; i < distinctVenueSizes.size(); i++) {
                slots.add(new BatchSlot(venuesSortedDesc.get(i), distinctVenueSizes.get(i).intValue()));
            }
            return slots;
        }

        VenueOptionResponse largest = venuesSortedDesc.get(0);
        int largestCapacity = largest.capacity() != null ? largest.capacity() : 0;
        if (largestCapacity <= 0) return List.of();
        int turns = ceilDiv(strength, largestCapacity);
        long base = strength / turns;
        long remainder = strength % turns;
        List<BatchSlot> slots = new ArrayList<>();
        for (int i = 0; i < turns; i++) {
            slots.add(new BatchSlot(largest, (int) (base + (i < remainder ? 1 : 0))));
        }
        return slots;
    }

    /** Every date in this term covered by a HOLIDAY or EXAM {@link CalendarEvent} -- shared by
     *  {@link #countWorkingDays} and {@link #blockedHoursInTerm} so both agree on which days are
     *  already fully excluded (avoids double-subtracting a blocked period on a day that's already
     *  a holiday). */
    Set<LocalDate> nonTeachingDates(TermInstance term) {
        List<CalendarEvent> nonTeachingEvents = calendarEventRepository.findNonTeachingDaysOverlapping(
            term.getAcademicYear().getId(), term.getStartDate(), term.getEndDate());

        Set<LocalDate> dates = new HashSet<>();
        for (CalendarEvent event : nonTeachingEvents) {
            LocalDate d = event.getStartDate().isBefore(term.getStartDate()) ? term.getStartDate() : event.getStartDate();
            LocalDate end = event.getEndDate().isAfter(term.getEndDate()) ? term.getEndDate() : event.getEndDate();
            while (!d.isAfter(end)) {
                dates.add(d);
                d = d.plusDays(1);
            }
        }
        return dates;
    }

    /** Actual working days across this term's real date range -- Sundays, any HOLIDAY/EXAM day, and
     *  any Saturday not opted into the term's {@code workingSaturdayWeeks} pattern don't count. The
     *  Saturday rule mirrors {@link WorkingSaturdayCalculator#isNonWorkingSaturday} exactly --
     *  {@link TimetableBlockedPeriodChecker} already refuses to place anything on a non-opted-in
     *  Saturday, so counting it here as if it were real available capacity would silently overstate
     *  every hours-based total downstream (buffer-hours here, and {@code
     *  TimetableGlobalAutoScheduleService#computeTermDemand}'s term capacity hours) by every
     *  Saturday in the term, and made toggling the working-Saturday pattern look like it had no
     *  effect on capacity at all when in fact the baseline was simply wrong regardless of the
     *  pattern. Distinct from the fixed weekly {@code WORKING_DAYS_PER_WEEK} constant used by venue
     *  utilization below (a per-week denominator); this is a term-total count used for the
     *  buffer-hours calculation. */
    int countWorkingDays(TermInstance term, Set<LocalDate> nonTeachingDates) {
        int workingDays = 0;
        for (LocalDate d = term.getStartDate(); !d.isAfter(term.getEndDate()); d = d.plusDays(1)) {
            if (d.getDayOfWeek() != DayOfWeek.SUNDAY && !nonTeachingDates.contains(d)
                    && !WorkingSaturdayCalculator.isNonWorkingSaturday(d, term)) {
                workingDays++;
            }
        }
        return workingDays;
    }

    /** Hours lost to ONE_OFF and RECURRING {@link BlockedPeriod} rows within this term -- both
     *  types count here (unlike Skeleton Builder placement, which only hard-blocks RECURRING).
     *  Skips days already excluded as a full holiday/exam day to avoid double-subtracting. */
    private double blockedHoursInTerm(TermInstance term, Set<LocalDate> nonTeachingDates) {
        List<BlockedPeriod> applicable = blockedPeriodRepository.findApplicableInRange(term.getStartDate(), term.getEndDate());
        if (applicable.isEmpty()) {
            return 0.0;
        }
        double totalMinutes = 0;
        for (LocalDate d = term.getStartDate(); !d.isAfter(term.getEndDate()); d = d.plusDays(1)) {
            if (d.getDayOfWeek() == DayOfWeek.SUNDAY || nonTeachingDates.contains(d)) {
                continue;
            }
            com.cms.model.enums.DayOfWeek appDayOfWeek = com.cms.model.enums.DayOfWeek.valueOf(d.getDayOfWeek().name());
            for (BlockedPeriod block : applicable) {
                boolean matches = block.getBlockType() == BlockType.ONE_OFF
                    ? d.equals(block.getSpecificDate())
                    : appDayOfWeek == block.getDayOfWeek()
                        && !d.isBefore(block.getRangeStartDate()) && !d.isAfter(block.getRangeEndDate());
                if (matches) {
                    totalMinutes += block.getPeriod().getDurationMinutes();
                }
            }
        }
        return totalMinutes / 60.0;
    }

    /** Every non-elective offering at this cohort's semester in this term with a real curriculum
     *  link -- shared basis for both {@link #curriculumHoursRequired} and {@link
     *  #suggestLabClinicalBatches}, so both agree on exactly which offerings count. Electives are
     *  excluded since a student takes only one per elective group, not every one on offer. */
    private List<CourseOffering> nonElectiveOfferings(Long termInstanceId, Integer semesterNumber) {
        return courseOfferingRepository.findByTermInstanceIdAndSemesterNumber(termInstanceId, semesterNumber).stream()
            .filter(o -> o.getCurriculumSemesterCourse() != null
                && !Boolean.TRUE.equals(o.getCurriculumSemesterCourse().getIsElective()))
            .toList();
    }

    /** Sum of theory+lab+clinical curriculum hours across the given offerings -- the "demand" side
     *  of the buffer calculation. */
    private int curriculumHoursRequired(List<CourseOffering> offerings) {
        return offerings.stream()
            .map(CourseOffering::getCurriculumSemesterCourse)
            .mapToInt(csc -> nullToZero(csc.getTheoryHours()) + nullToZero(csc.getLabHours()) + nullToZero(csc.getClinicalHours()))
            .sum();
    }

    private static int nullToZero(Integer value) {
        return value != null ? value : 0;
    }

    /** Classroom utilization is deliberately NOT raw cross-cohort {@link ClassSchedule} occupancy
     *  the way Lab/Clinical utilization is -- a Theory classroom is exclusively one cohort's home
     *  room per term (enforced by {@code ux_cohort_section_classroom_per_term}), so once that
     *  claim is reverted the room is genuinely free again even though its old DRAFT sessions
     *  usually linger in Skeleton Builder (Theory {@code ClassSchedule} rows aren't cascaded from
     *  {@code CohortRoomAllocation}/{@code CohortSection} the way Lab/Clinical batches are, so
     *  reverting never cleans them up). Showing that leftover schedule debris as "occupied" next
     *  to a room the admin is actively free to claim right now is actively misleading -- so a
     *  classroom with no active claim always reports 0%, full stop, regardless of what stale rows
     *  sit underneath it. Only a classroom someone currently, actively claims shows a real number. */
    private List<VenueUtilizationResponse> classroomUtilization(List<Classroom> classrooms, List<ClassSchedule> termSchedule,
                                                                  int totalSlots, Set<Long> claimedByAnyCohortId,
                                                                  Map<Long, String> claimedByOtherCohortLabel) {
        Map<Long, Long> occupiedByClassroomId = termSchedule.stream()
            .filter(cs -> cs.getSessionType() == ClassSessionType.THEORY && cs.getClassroom() != null)
            .collect(Collectors.groupingBy(cs -> cs.getClassroom().getId(), Collectors.counting()));

        return classrooms.stream()
            .map(c -> {
                boolean claimed = claimedByAnyCohortId.contains(c.getId());
                long occupied = claimed ? occupiedByClassroomId.getOrDefault(c.getId(), 0L) : 0L;
                double percent = !claimed || totalSlots == 0 ? 0.0 : (occupied * 100.0) / totalSlots;
                return new VenueUtilizationResponse(c.getId(), c.getName(), c.getCapacity(), occupied, totalSlots, percent,
                    claimedByOtherCohortLabel.get(c.getId()));
            })
            .toList();
    }

    /** Occupied-vs-total-slot utilization for Lab/Clinical venues, computed from the term's
     *  already globally-scoped schedule (a TermInstance is shared institution-wide across every
     *  cohort/program, so this is genuinely cross-cohort occupancy) -- unlike classrooms, a
     *  Lab/Clinical venue has no per-term exclusivity lock, so multiple cohorts legitimately share
     *  one venue across different day/period slots and the raw count is the correct signal. */
    private <T> List<VenueUtilizationResponse> utilization(List<T> venues, Function<T, Long> idFn, Function<T, String> nameFn,
                                                             Function<T, Integer> capacityFn, List<ClassSchedule> termSchedule,
                                                             ClassSessionType type, Function<ClassSchedule, Long> venueIdFn,
                                                             int totalSlots, Map<Long, String> claimedByCohortLabel) {
        Map<Long, Long> occupiedByVenueId = termSchedule.stream()
            .filter(cs -> cs.getSessionType() == type && venueIdFn.apply(cs) != null)
            .collect(Collectors.groupingBy(venueIdFn, Collectors.counting()));

        return venues.stream()
            .map(v -> {
                Long id = idFn.apply(v);
                long occupied = occupiedByVenueId.getOrDefault(id, 0L);
                double percent = totalSlots == 0 ? 0.0 : (occupied * 100.0) / totalSlots;
                return new VenueUtilizationResponse(id, nameFn.apply(v), capacityFn.apply(v), occupied, totalSlots, percent,
                    claimedByCohortLabel.get(id));
            })
            .toList();
    }
}
