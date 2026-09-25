package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.dto.AutoPlaceUnplacedItem;
import com.cms.dto.CourseOfferingDto;
import com.cms.dto.CourseOfferingFacultySummaryDto;
import com.cms.dto.CohortPlacementSummary;
import com.cms.dto.CourseOfferingSectionFacultyResponse;
import com.cms.dto.SectionFacultyAssignment;
import com.cms.dto.EligibleFacultyCandidateDto;
import com.cms.dto.ClinicalShiftPeriodAvailabilityResult;
import com.cms.dto.LabClinicalVenueCapacityResult;
import com.cms.dto.VenueOverCapacity;
import com.cms.dto.FacultyCapacityCheckResult;
import com.cms.dto.FacultyOverCapacity;
import com.cms.dto.FacultyTightCapacity;
import com.cms.dto.FacultyWorkloadDetail;
import com.cms.dto.GlobalAutoSchedulePrerequisites;
import com.cms.dto.GlobalCapacityPrecheckResult;
import com.cms.dto.RotationGroupCreateRequest;
import com.cms.dto.RotationGroupResponse;
import com.cms.dto.SkeletonBuilderResponse;
import com.cms.dto.SkeletonCellPlacementRequest;
import com.cms.dto.SkeletonCellResponse;
import com.cms.dto.SkeletonSubjectBudget;
import com.cms.dto.SkeletonSubjectResponse;
import com.cms.dto.SkippedPublishedCohort;
import com.cms.dto.SystemConfigurationResponse;
import com.cms.dto.StaffingAssignmentRequest;
import com.cms.dto.ConstraintViolation;
import com.cms.dto.TimetableConflictRow;
import com.cms.dto.UnstaffedCellResponse;
import com.cms.exception.TimetableConstraintViolationException;
import com.cms.model.AcademicYear;
import com.cms.model.Batch;
import com.cms.model.Cohort;
import com.cms.model.ClassSchedule;
import com.cms.model.Classroom;
import com.cms.model.CohortSection;
import com.cms.model.CourseOffering;
import com.cms.model.CourseOfferingSectionFaculty;
import com.cms.model.CurriculumSemesterCourse;
import com.cms.model.DesignationMaster;
import com.cms.model.Faculty;
import com.cms.model.Lab;
import com.cms.model.Period;
import com.cms.model.Speciality;
import com.cms.model.Subject;
import com.cms.model.TermInstance;
import com.cms.model.enums.ClassScheduleStatus;
import com.cms.model.enums.ClassSessionType;
import com.cms.model.enums.DayOfWeek;
import com.cms.model.enums.EnrollmentStatus;
import com.cms.model.enums.FacultyStatus;
import com.cms.model.enums.OfferingAssignmentStatus;
import com.cms.model.enums.SubjectType;
import com.cms.model.enums.TermInstanceStatus;
import com.cms.model.enums.TermType;
import com.cms.model.enums.WeekOfMonth;
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

@ExtendWith(MockitoExtension.class)
class TimetableGlobalAutoScheduleServiceTest {

    @Mock private TimetableSkeletonService timetableSkeletonService;
    @Mock private TimetableStaffingService timetableStaffingService;
    @Mock private TimetableCapacityPlanningService timetableCapacityPlanningService;
    @Mock private CourseOfferingService courseOfferingService;
    @Mock private CourseOfferingRepository courseOfferingRepository;
    @Mock private ClassScheduleRepository classScheduleRepository;
    @Mock private StudentTermEnrollmentRepository studentTermEnrollmentRepository;
    @Mock private CohortRepository cohortRepository;
    @Mock private BatchRepository batchRepository;
    @Mock private CourseOfferingSectionFacultyRepository courseOfferingSectionFacultyRepository;
    @Mock private FacultyRepository facultyRepository;
    @Mock private TermInstanceRepository termInstanceRepository;
    @Mock private PeriodRepository periodRepository;
    @Mock private TimetableBlockedPeriodChecker blockedPeriodChecker;
    @Mock private ClassroomRepository classroomRepository;
    @Mock private CourseRegistrationRepository courseRegistrationRepository;
    @Mock private SubjectRepository subjectRepository;
    @Mock private SystemConfigurationService systemConfigurationService;
    @Mock private ClinicalShiftGroupService clinicalShiftGroupService;
    @Mock private TimetableClinicalShiftChecker clinicalShiftChecker;
    @Mock private CourseOfferingSectionFacultyService courseOfferingSectionFacultyService;
    @Mock private RotationGroupService rotationGroupService;
    @Mock private TimetableConflictInspectorService timetableConflictInspectorService;
    @Mock private BatchService batchService;
    @Mock private ClassScheduleCleanupService classScheduleCleanupService;

    /** {@code fillSelfStudyGaps}'s final fallback message -- reached only when a fixture configures
     *  neither a genuine Self-Study/Co-curricular offering NOR any other real Theory offering for
     *  the cohort to use as extra-hours gap-fill (see {@code resolveExtraHoursFillerRows}). Most
     *  fixtures in this suite DO configure a Theory offering (even without Self-Study), so they now
     *  silently use it as extra-hours filler instead of hitting this message at all. */
    private static final String NO_SELF_STUDY_OR_THEORY_OFFERING_REASON =
        "no curriculum Theory offering other than Self-Study exists for this cohort to take extra hours "
            + "— every remaining free period stays empty until one is added";

    /** None of these fixtures configure a Library classroom either, so {@code fillLibraryGaps} now
     *  correctly reports this once per cohort per run too — added ahead of the Self-Study reason in
     *  every {@code unplaced} list, since Library runs first (see the class's placement-order note). */
    private static final String NO_LIBRARY_CLASSROOM_REASON =
        "no Library classroom is configured (a Classroom linked to a Room tagged with the Library "
            + "Purpose Category) — every cohort's Library quota stays unplaced until one is added";

    private TimetableGlobalAutoScheduleService service;
    private TermInstance termInstance;
    private Period period1;
    private final java.util.concurrent.atomic.AtomicLong idSequence = new java.util.concurrent.atomic.AtomicLong();

    @BeforeEach
    void setUp() {
        service = new TimetableGlobalAutoScheduleService(timetableSkeletonService, timetableStaffingService, clinicalShiftChecker,
            timetableCapacityPlanningService, courseOfferingService, courseOfferingRepository, classScheduleRepository,
            studentTermEnrollmentRepository, cohortRepository, batchRepository,
            courseOfferingSectionFacultyRepository, facultyRepository, termInstanceRepository, periodRepository,
            blockedPeriodChecker, classroomRepository, courseRegistrationRepository, subjectRepository, systemConfigurationService,
            clinicalShiftGroupService, rotationGroupService, timetableConflictInspectorService, batchService,
            classScheduleCleanupService);
        service.setCourseOfferingSectionFacultyService(courseOfferingSectionFacultyService);
        lenient().when(courseOfferingSectionFacultyRepository.findByCourseOfferingId(anyLong())).thenReturn(List.of());
        // Every successful run now ends with a term-wide post-run conflict scan (flag-only) --
        // stub it to an empty result by default so the hundreds of existing assertions in this
        // suite (none of which are about conflict scanning) don't all need updating individually.
        lenient().when(timetableConflictInspectorService.scanTerm(anyLong())).thenReturn(
            new com.cms.dto.ConflictScanResponse(null, null, null, 0, 0, 0, java.util.Map.of(), List.of()));
        // No fixture in this suite approves/publishes any cohort's timetable, so every cohort defaults
        // to "draft" (not published) unless a specific test overrides this stub — Mockito already
        // returns an empty list by default for the unstubbed getCohortActiveClassSchedules(...,
        // PUBLISHED) call the per-cohort publish gate now uses.
        // No fixture configures a Library classroom (Classroom linked to a Room tagged Library
        // Purpose Category), so fillLibraryGaps correctly reports this once per cohort per run —
        // see NO_LIBRARY_CLASSROOM_REASON below.
        lenient().when(subjectRepository.findByCode("SYSTEM-LIBRARY")).thenReturn(Optional.empty());
        lenient().when(classroomRepository.findByIsActiveTrueAndRoom_PurposeCategory_CodeOrderByNameAsc(any()))
            .thenReturn(List.of());
        // None of these fixtures configure a genuine Self-Study/Co-curricular offering either, so
        // fillSelfStudyGaps' extra-hours-filler fallback (resolveExtraHoursFillerRows) now reaches
        // every cohort's own real Theory offerings via the budget-uncapped placeCell(request, false)
        // overload -- unstubbed, that returns null and NPEs on placed.id() the instant any test's
        // skeleton fixture has at least one real subject (nearly all of them do). Default this
        // overload to "nothing else fits either" (a thrown constraint violation, exactly what the
        // code's own catch already handles as "try the next row/period") so unrelated tests keep
        // their original unplaced-message assertions; a test that actually wants to exercise the
        // extra-hours fallback overrides this stub itself.
        lenient().when(timetableSkeletonService.placeCell(any(SkeletonCellPlacementRequest.class), eq(false)))
            .thenThrow(new TimetableConstraintViolationException(List.of(
                new com.cms.dto.ConstraintViolation("SKELETON_CELL_ALREADY_PLACED", "no fixture stub for this slot"))));

        AcademicYear ay = new AcademicYear("2025-2026", LocalDate.of(2025, 6, 1), LocalDate.of(2026, 5, 31), false);
        ay.setId(1L);
        termInstance = new TermInstance(ay, TermType.ODD, LocalDate.of(2025, 6, 1), LocalDate.of(2025, 11, 30), TermInstanceStatus.OPEN);
        termInstance.setId(10L);
        lenient().when(termInstanceRepository.findById(10L)).thenReturn(Optional.of(termInstance));

        period1 = new Period("1st Period", LocalTime.of(9, 0), LocalTime.of(9, 50), 1);
        period1.setId(1L);
        lenient().when(periodRepository.findByIsActiveTrueOrderByPeriodOrderAsc()).thenReturn(List.of(period1));

        // 100 real working days, mirroring the user's own worked example.
        lenient().when(timetableCapacityPlanningService.nonTeachingDates(termInstance)).thenReturn(Set.of());
        lenient().when(timetableCapacityPlanningService.countWorkingDays(eq(termInstance), any())).thenReturn(100);

        // Default: no Lab/Clinical venue is over/tight capacity -- individual tests override this
        // to exercise checkPrerequisites'/doRunGlobalAutoSchedule's own handling of a real gap.
        lenient().when(timetableCapacityPlanningService.computeLabClinicalVenueCapacity(anyLong(), any()))
            .thenReturn(new LabClinicalVenueCapacityResult(List.of(), List.of()));

        // Default: no cohort/day has a Clinical Shift period shortfall -- individual tests override
        // this to exercise checkPrerequisites' own handling of a real gap.
        lenient().when(timetableCapacityPlanningService.computeClinicalShiftPeriodAvailability(anyLong()))
            .thenReturn(new ClinicalShiftPeriodAvailabilityResult(List.of(), List.of()));

        // Default: every offering is fully staffed per the real Assign Faculty/Publish-gate rollup --
        // individual checkPrerequisites tests override this to exercise a real NONE/PARTIAL gap.
        lenient().when(courseOfferingSectionFacultyService.getAssignmentSummaryForTermInstance(anyLong()))
            .thenReturn(List.of());
    }

    private Faculty facultyWithDailyCap(Long id, String name, Integer plannedDailySessionsOverride) {
        Faculty faculty = new Faculty();
        faculty.setId(id);
        faculty.setFirstName(name);
        faculty.setLastName("Staff");
        faculty.setStatus(FacultyStatus.ACTIVE);
        faculty.setPlannedDailySessionsOverride(plannedDailySessionsOverride);
        when(facultyRepository.findById(id)).thenReturn(Optional.of(faculty));
        return faculty;
    }

    /** A session cap is now bridged into an hours-equivalent via the real average Period duration
     *  (see resolveEffectiveTermCapacity) -- an exact 1-hour period keeps a precheck test's existing
     *  "N sessions -> N hours" expected values valid. Deliberately NOT folded into {@link
     *  #facultyWithDailyCap} itself: that helper is shared by tests (e.g. runSingleSubjectWithOccupancy)
     *  that stub periodRepository with their own real, varying-duration period fixture just before
     *  calling it -- an unconditional stub in the shared helper silently clobbered theirs (Mockito is
     *  last-stub-wins), halving their effective daily period count. Call this only from a test that
     *  actually asserts an exact hours figure. */
    private void usePreciseOneHourPeriodFixture() {
        Period oneHourPeriod = new Period("1st Period", LocalTime.of(9, 0), LocalTime.of(10, 0), 1);
        oneHourPeriod.setId(1L);
        when(periodRepository.findByIsActiveTrueOrderByPeriodOrderAsc()).thenReturn(List.of(oneHourPeriod));
    }

    private Cohort cohort(Long id, String name) {
        Cohort cohort = new Cohort();
        cohort.setId(id);
        when(cohortRepository.findById(id)).thenReturn(Optional.of(cohort));
        return cohort;
    }

    private CourseOfferingDto offeringDto(Long id, String subjectName) {
        return new CourseOfferingDto(id, 10L, null, null, null, id, subjectName, subjectName.substring(0, 4).toUpperCase(),
            null, null, List.of(), 1, true, null, false, null, null, null, null, null, null, null, null, null, null, null, null, List.of());
    }

    private CourseOffering offeringEntity(Long id, int theoryHours, int labHours, int clinicalHours) {
        CurriculumSemesterCourse csc = new CurriculumSemesterCourse();
        csc.setTheoryHours(theoryHours);
        csc.setLabHours(labHours);
        csc.setClinicalHours(clinicalHours);
        csc.setIsElective(false);
        CourseOffering offering = new CourseOffering();
        offering.setId(id);
        offering.setCurriculumSemesterCourse(csc);
        offering.setTermInstance(termInstance);
        when(courseOfferingRepository.findById(id)).thenReturn(Optional.of(offering));
        lenient().when(timetableSkeletonService.isElectiveOffering(offering)).thenReturn(false);
        return offering;
    }

    /** Same as {@link #offeringEntity(Long, int, int, int)} but with an explicit {@link
     *  SubjectType} — used to exercise the CO_CURRICULAR-is-always-placed-last partition in
     *  {@code SHORTFALL_ROW_ORDER}, distinct from every other offeringEntity fixture which stays
     *  on the entity's default CORE. */
    private CourseOffering offeringEntity(Long id, int theoryHours, int labHours, int clinicalHours, SubjectType subjectType) {
        CourseOffering offering = offeringEntity(id, theoryHours, labHours, clinicalHours);
        offering.getCurriculumSemesterCourse().setSubjectType(subjectType);
        return offering;
    }

    /** Stubs this offering's whole-cohort (no section split) CourseOfferingSectionFaculty row --
     *  the generalized replacement for the old scalar CourseOffering.facultyId, used as every
     *  unsectioned budget row's resolved faculty and as computeTermDemand's per-cohort attribution. */
    private void assignWholeCohort(Long offeringId, Long cohortId, Long facultyId) {
        Faculty faculty = new Faculty();
        faculty.setId(facultyId);
        CourseOfferingSectionFaculty row = new CourseOfferingSectionFaculty();
        row.setFaculty(faculty);
        // lenient(): other offerings checked in the same test (deliberately left unassigned) call
        // this same method with different arguments -- without lenient, Mockito's strict stubbing
        // flags that as a likely mistake rather than falling through to the empty-Optional default.
        lenient().when(courseOfferingSectionFacultyRepository.findByCourseOfferingIdAndCohortIdAndCohortSectionIdIsNull(offeringId, cohortId))
            .thenReturn(Optional.of(row));
    }

    private Lab lab(Long id) {
        Lab lab = new Lab();
        lab.setId(id);
        return lab;
    }

    private Subject labSubject(String name) {
        Subject subject = new Subject();
        subject.setName(name);
        return subject;
    }

    /** One active LAB batch, resolvable both by id (for {@code resolveBudgetFacultyId}'s coordinator
     *  lookup) and via {@code batchRepository.findByCourseOfferingId} — a real fixture for Phase B's
     *  cross-offering pairing tests below. */
    private Batch labBatch(Long id, Lab labEntity, CohortSection section, Long coordinatorFacultyId) {
        Batch batch = new Batch();
        batch.setId(id);
        batch.setIsActive(true);
        batch.setLab(labEntity);
        batch.setCohortSection(section);
        Faculty coordinator = new Faculty();
        coordinator.setId(coordinatorFacultyId);
        batch.setCoordinatorFaculty(coordinator);
        lenient().when(batchRepository.findById(id)).thenReturn(Optional.of(batch));
        return batch;
    }

    // ── Capacity precheck ──────────────────────────────────────────────

    @Test
    void precheckFlagsFacultyOverDailyCapacity_matchingTheUsersWorkedExample() {
        // Offering A (90h) in a 2-section cohort 1 -> 180h real demand; offering B (90h) in cohort 2;
        // offering C (90h) in cohort 3 -> 360h total, all bound to the same faculty XYZ.
        when(studentTermEnrollmentRepository.findDistinctCohortIdsByTermInstanceId(10L, EnrollmentStatus.ENROLLED))
            .thenReturn(new HashSet<>(List.of(1L, 2L, 3L)));
        cohort(1L, "Cohort 1");
        cohort(2L, "Cohort 2");
        cohort(3L, "Cohort 3");

        Faculty xyz = facultyWithDailyCap(500L, "XYZ", 3); // 3h/day x 100 days = 300h capacity
        usePreciseOneHourPeriodFixture();
        when(courseOfferingService.getOfferingsByTermInstanceAndCohort(10L, 1L)).thenReturn(List.of(offeringDto(100L, "Offering A")));
        when(courseOfferingService.getOfferingsByTermInstanceAndCohort(10L, 2L)).thenReturn(List.of(offeringDto(200L, "Offering B")));
        when(courseOfferingService.getOfferingsByTermInstanceAndCohort(10L, 3L)).thenReturn(List.of(offeringDto(300L, "Offering C")));
        assignWholeCohort(100L, 1L, 500L);
        assignWholeCohort(200L, 2L, 500L);
        assignWholeCohort(300L, 3L, 500L);

        CourseOffering offeringA = offeringEntity(100L, 90, 0, 0);
        offeringEntity(200L, 90, 0, 0);
        offeringEntity(300L, 90, 0, 0);

        CohortSection sectionA = new CohortSection();
        CohortSection sectionB = new CohortSection();
        when(timetableSkeletonService.resolveActiveSections(1L, 10L)).thenReturn(List.of(sectionA, sectionB));
        when(timetableSkeletonService.resolveActiveSections(2L, 10L)).thenReturn(List.of());
        when(timetableSkeletonService.resolveActiveSections(3L, 10L)).thenReturn(List.of());
        when(batchRepository.findByCourseOfferingId(anyLong())).thenReturn(List.of());

        GlobalCapacityPrecheckResult result = service.precheckCapacity(10L);

        assertThat(result.overCapacityFaculty()).hasSize(1);
        FacultyOverCapacity over = result.overCapacityFaculty().get(0);
        assertThat(over.facultyId()).isEqualTo(500L);
        assertThat(over.totalTermDemandHours()).isEqualTo(360.0); // 90*2 + 90 + 90
        assertThat(over.workingDaysInTerm()).isEqualTo(100);
        assertThat(over.termCapacityHours()).isEqualTo(300.0); // 3h/day * 100 days
        assertThat(over.shortfallHours()).isEqualTo(60.0);
        assertThat(over.suggestedMinDailyHours()).isEqualTo(4.0); // ceil(360/100)
        assertThat(over.raiseCap().suggestedMinDailyHours()).isEqualTo(4.0);
    }

    @Test
    void precheckDoesNotFlagFacultyWhoFitsWithinCapacity() {
        // Same 360h demand, but a 6h/day cap x 100 days = 600h capacity easily covers it.
        when(studentTermEnrollmentRepository.findDistinctCohortIdsByTermInstanceId(10L, EnrollmentStatus.ENROLLED))
            .thenReturn(new HashSet<>(List.of(1L)));
        cohort(1L, "Cohort 1");
        facultyWithDailyCap(500L, "XYZ", 6);
        when(courseOfferingService.getOfferingsByTermInstanceAndCohort(10L, 1L)).thenReturn(List.of(offeringDto(100L, "Offering A")));
        assignWholeCohort(100L, 1L, 500L);
        offeringEntity(100L, 90, 0, 0);
        when(timetableSkeletonService.resolveActiveSections(1L, 10L)).thenReturn(List.of());
        when(batchRepository.findByCourseOfferingId(anyLong())).thenReturn(List.of());

        GlobalCapacityPrecheckResult result = service.precheckCapacity(10L);

        assertThat(result.overCapacityFaculty()).isEmpty();
        assertThat(result.tightCapacityFaculty()).isEmpty();
    }

    @Test
    void precheckFlagsFacultyAtTightCapacityAsWarningNotBlock() {
        // 385h demand against a 4h/day x 100 days = 400h capacity -- 96.25% utilization, over the
        // 95% tight threshold but not actually over capacity, so it must land in
        // tightCapacityFaculty (a non-blocking warning) and NOT in overCapacityFaculty.
        when(studentTermEnrollmentRepository.findDistinctCohortIdsByTermInstanceId(10L, EnrollmentStatus.ENROLLED))
            .thenReturn(new HashSet<>(List.of(1L)));
        cohort(1L, "Cohort 1");
        facultyWithDailyCap(500L, "XYZ", 4);
        usePreciseOneHourPeriodFixture();
        when(courseOfferingService.getOfferingsByTermInstanceAndCohort(10L, 1L)).thenReturn(List.of(offeringDto(100L, "Offering A")));
        assignWholeCohort(100L, 1L, 500L);
        offeringEntity(100L, 385, 0, 0);
        when(timetableSkeletonService.resolveActiveSections(1L, 10L)).thenReturn(List.of());
        when(batchRepository.findByCourseOfferingId(anyLong())).thenReturn(List.of());

        GlobalCapacityPrecheckResult result = service.precheckCapacity(10L);

        assertThat(result.overCapacityFaculty()).isEmpty();
        assertThat(result.tightCapacityFaculty()).hasSize(1);
        FacultyTightCapacity tight = result.tightCapacityFaculty().get(0);
        assertThat(tight.facultyId()).isEqualTo(500L);
        assertThat(tight.totalTermDemandHours()).isEqualTo(385.0);
        assertThat(tight.termCapacityHours()).isEqualTo(400.0);
        assertThat(tight.utilizationPercent()).isEqualTo(96.25);
    }

    @Test
    void precheckCreditsEachBatchsOwnCoordinatorForLabClinicalHours_notThePrimaryFaculty() {
        // Offering: 0 theory, 150 lab+clinical hours, primary faculty XYZ (500) with a 1h/day cap
        // (100h capacity). Two active batches, each coordinated by a DIFFERENT faculty (600, 700),
        // also 1h/day each. Each batch's full 150h must land on ITS OWN coordinator (150h > 100h
        // capacity -> both flagged, independently, for exactly 150h each) -- not divided between
        // them, not summed onto XYZ, who has no batch of their own and no theory hours here, so
        // they must end up with zero demand from this offering and never be flagged despite having
        // the same tiny cap as the coordinators.
        when(studentTermEnrollmentRepository.findDistinctCohortIdsByTermInstanceId(10L, EnrollmentStatus.ENROLLED))
            .thenReturn(new HashSet<>(List.of(1L)));
        cohort(1L, "Cohort 1");
        // XYZ (500) is the offering's primary but ends up with zero credited demand from this
        // offering -- no theory hours and no batch of their own -- so their capacity record is
        // never even looked up; not stubbed here on purpose.
        Faculty coordA = facultyWithDailyCap(600L, "Coord A", 1);
        Faculty coordB = facultyWithDailyCap(700L, "Coord B", 1);
        when(courseOfferingService.getOfferingsByTermInstanceAndCohort(10L, 1L)).thenReturn(List.of(offeringDto(100L, "Offering A")));
        offeringEntity(100L, 0, 90, 60);
        when(timetableSkeletonService.resolveActiveSections(1L, 10L)).thenReturn(List.of());

        Batch batchA = new Batch();
        batchA.setIsActive(true);
        batchA.setCoordinatorFaculty(coordA);
        Batch batchB = new Batch();
        batchB.setIsActive(true);
        batchB.setCoordinatorFaculty(coordB);
        when(batchRepository.findByCourseOfferingId(100L)).thenReturn(List.of(batchA, batchB));

        GlobalCapacityPrecheckResult result = service.precheckCapacity(10L);

        assertThat(result.overCapacityFaculty()).extracting(FacultyOverCapacity::facultyId)
            .containsExactlyInAnyOrder(600L, 700L);
        assertThat(result.overCapacityFaculty()).allSatisfy(over ->
            assertThat(over.totalTermDemandHours()).isEqualTo(150.0));
    }

    @Test
    void precheckChargesEachTypedBatchOnlyItsOwnLabOrClinicalHours_notTheCombinedTotal() {
        // Real-data regression: offering with 40 lab + 480 clinical hours (520 combined), primary
        // faculty XYZ (500, no cap configured -- never looked up). One LAB-typed batch (linked to a
        // Lab venue) and one CLINICAL-typed batch (linked to a ClinicalVenue), both coordinated by
        // the SAME faculty (600, 1h/day = 100h capacity). Before the fix, each batch was wrongly
        // charged the full 520h combined total (1040h total, wildly over); after the fix, the Lab
        // batch owes only 40h and the Clinical batch owes only 60h -- 100h total, exactly at cap,
        // never flagged.
        when(studentTermEnrollmentRepository.findDistinctCohortIdsByTermInstanceId(10L, EnrollmentStatus.ENROLLED))
            .thenReturn(new HashSet<>(List.of(1L)));
        cohort(1L, "Cohort 1");
        Faculty coord = facultyWithDailyCap(600L, "Coordinator", 1); // 100h capacity
        usePreciseOneHourPeriodFixture();
        when(courseOfferingService.getOfferingsByTermInstanceAndCohort(10L, 1L)).thenReturn(List.of(offeringDto(100L, "Offering A")));
        offeringEntity(100L, 0, 40, 60);
        when(timetableSkeletonService.resolveActiveSections(1L, 10L)).thenReturn(List.of());

        com.cms.model.Lab lab = new com.cms.model.Lab();
        Batch labBatch = new Batch();
        labBatch.setIsActive(true);
        labBatch.setCoordinatorFaculty(coord);
        labBatch.setLab(lab);

        com.cms.model.ClinicalVenue venue = new com.cms.model.ClinicalVenue();
        Batch clinicalBatch = new Batch();
        clinicalBatch.setIsActive(true);
        clinicalBatch.setCoordinatorFaculty(coord);
        clinicalBatch.setClinicalVenue(venue);

        when(batchRepository.findByCourseOfferingId(100L)).thenReturn(List.of(labBatch, clinicalBatch));

        GlobalCapacityPrecheckResult result = service.precheckCapacity(10L);

        assertThat(result.overCapacityFaculty()).isEmpty();
    }

    @Test
    void precheckCreditsOnlyEachSectionsOwnFacultyOverrideForTheoryHours_leavesUnoverriddenSectionsUncredited() {
        // Offering: 100 theory hours, 0 lab/clinical. Cohort has two active sections; Section B has
        // an override to faculty 700 (1h/day cap, 100h capacity). Section A has NO override -- with
        // no offering-wide primary to fall back to anymore, its 100h simply isn't credited to
        // anyone (mirrors placement: an unoverridden split section is genuinely unassigned, not
        // silently defaulted to somebody). 700 ends up with exactly 100h (Section B only) and fits
        // within their own cap.
        when(studentTermEnrollmentRepository.findDistinctCohortIdsByTermInstanceId(10L, EnrollmentStatus.ENROLLED))
            .thenReturn(new HashSet<>(List.of(1L)));
        cohort(1L, "Cohort 1");
        Faculty sectionBFaculty = facultyWithDailyCap(700L, "Section B Faculty", 1);
        usePreciseOneHourPeriodFixture();
        when(courseOfferingService.getOfferingsByTermInstanceAndCohort(10L, 1L)).thenReturn(List.of(offeringDto(100L, "Offering A")));
        offeringEntity(100L, 100, 0, 0);

        CohortSection sectionA = new CohortSection();
        sectionA.setId(1L);
        CohortSection sectionB = new CohortSection();
        sectionB.setId(2L);
        when(timetableSkeletonService.resolveActiveSections(1L, 10L)).thenReturn(List.of(sectionA, sectionB));

        CourseOfferingSectionFaculty override = new CourseOfferingSectionFaculty();
        override.setCohortSection(sectionB);
        override.setFaculty(sectionBFaculty);
        when(courseOfferingSectionFacultyRepository.findByCourseOfferingId(100L)).thenReturn(List.of(override));

        GlobalCapacityPrecheckResult result = service.precheckCapacity(10L);

        assertThat(result.overCapacityFaculty()).isEmpty();
    }

    // ── Placement + staffing run ───────────────────────────────────────

    @Test
    void runAbortsWithoutPlacingAnything_whenPrecheckFails() {
        when(studentTermEnrollmentRepository.findDistinctCohortIdsByTermInstanceId(10L, EnrollmentStatus.ENROLLED))
            .thenReturn(new HashSet<>(List.of(1L)));
        cohort(1L, "Cohort 1");
        facultyWithDailyCap(500L, "XYZ", 1); // 1h/day x 100 = 100h, well under 90h*... wait keep simple: force over-cap
        when(courseOfferingService.getOfferingsByTermInstanceAndCohort(10L, 1L)).thenReturn(List.of(offeringDto(100L, "Offering A")));
        assignWholeCohort(100L, 1L, 500L);
        offeringEntity(100L, 200, 0, 0); // 200h demand vs 100h capacity -> over
        when(timetableSkeletonService.resolveActiveSections(1L, 10L)).thenReturn(List.of());
        when(batchRepository.findByCourseOfferingId(anyLong())).thenReturn(List.of());

        assertThatThrownBy(() -> service.runGlobalAutoSchedule(10L, null))
            .isInstanceOf(TimetableConstraintViolationException.class);

        verify(timetableSkeletonService, never()).placeCell(any());
        verify(timetableStaffingService, never()).staffCell(anyLong(), any());
    }

    @Test
    void runAbortsWithoutPlacingAnything_whenVenueCapacityPrecheckFails() {
        when(studentTermEnrollmentRepository.findDistinctCohortIdsByTermInstanceId(10L, EnrollmentStatus.ENROLLED))
            .thenReturn(new HashSet<>(List.of(1L)));
        cohort(1L, "Cohort 1");
        facultyWithDailyCap(500L, "XYZ", 6); // plenty of capacity, faculty precheck passes
        when(courseOfferingService.getOfferingsByTermInstanceAndCohort(10L, 1L)).thenReturn(List.of(offeringDto(100L, "Offering A")));
        assignWholeCohort(100L, 1L, 500L);
        offeringEntity(100L, 10, 0, 0);
        when(timetableSkeletonService.resolveActiveSections(1L, 10L)).thenReturn(List.of());
        when(batchRepository.findByCourseOfferingId(anyLong())).thenReturn(List.of());

        VenueOverCapacity overCapacity = new VenueOverCapacity(50L, "LAB", "Anatomy Lab", 30, 12, 13, 1, List.of("Anatomy"), List.of(100L));
        when(timetableCapacityPlanningService.computeLabClinicalVenueCapacity(eq(10L), any()))
            .thenReturn(new LabClinicalVenueCapacityResult(List.of(overCapacity), List.of()));

        assertThatThrownBy(() -> service.runGlobalAutoSchedule(10L, null))
            .isInstanceOf(TimetableConstraintViolationException.class);

        verify(timetableSkeletonService, never()).placeCell(any());
        verify(timetableStaffingService, never()).staffCell(anyLong(), any());
    }

    @Test
    void runPlacesAndStaffsUsingTheOfferingsBoundFaculty_notAFreePool() {
        when(studentTermEnrollmentRepository.findDistinctCohortIdsByTermInstanceId(10L, EnrollmentStatus.ENROLLED))
            .thenReturn(new HashSet<>(List.of(1L)));
        Cohort cohort = cohort(1L, "Cohort 1");
        facultyWithDailyCap(500L, "XYZ", 6); // plenty of capacity, precheck passes
        when(courseOfferingService.getOfferingsByTermInstanceAndCohort(10L, 1L)).thenReturn(List.of(offeringDto(100L, "Offering A")));
        assignWholeCohort(100L, 1L, 500L);
        CourseOffering offering = offeringEntity(100L, 10, 0, 0);
        when(timetableSkeletonService.resolveActiveSections(1L, 10L)).thenReturn(List.of());
        when(batchRepository.findByCourseOfferingId(anyLong())).thenReturn(List.of());

        SkeletonSubjectBudget budget = new SkeletonSubjectBudget(ClassSessionType.THEORY, null, null, null, null, 10, 10, 1, 0);
        SkeletonSubjectResponse subject = new SkeletonSubjectResponse(100L, "Offering A", "OFFE", List.of(budget), null, null);
        SkeletonBuilderResponse skeleton = new SkeletonBuilderResponse(1L, "Cohort 1", "Term", List.of(subject), List.of(), List.of(), List.of(), 25, 0L, List.of(), false, List.of());
        when(timetableSkeletonService.getCohortSkeleton(10L, 1L)).thenReturn(skeleton);

        SkeletonCellResponse placed = new SkeletonCellResponse(900L, ClassSessionType.THEORY, DayOfWeek.MONDAY, 1L, "1st Period",
            LocalTime.of(9, 0), LocalTime.of(9, 50), null, null, null, null, false, null, null, List.of(),
            100L, "Offering A", "OFFE", null, null, null, false, false);
        when(timetableSkeletonService.placeCell(any(SkeletonCellPlacementRequest.class))).thenReturn(placed);
        when(timetableStaffingService.staffCell(eq(900L), any(StaffingAssignmentRequest.class)))
            .thenReturn(new UnstaffedCellResponse(900L, 100L, "Offering A", "OFFE", null, null,
                ClassSessionType.THEORY, DayOfWeek.MONDAY, 1L, "1st Period", LocalTime.of(9, 0), LocalTime.of(9, 50),
                null, null, null, null, null, false, List.of(), null, null));

        var result = service.runGlobalAutoSchedule(10L, null);

        assertThat(result.totalPlaced()).isEqualTo(1);
        assertThat(result.totalStaffed()).isEqualTo(1);
        assertThat(result.cohortSummaries()).hasSize(1);
        assertThat(result.cohortSummaries().get(0).unplaced()).extracting(AutoPlaceUnplacedItem::reason)
            .containsExactly(NO_LIBRARY_CLASSROOM_REASON);
        assertThat(result.cohortSummaries().get(0).usedSaturday()).isFalse();
        assertThat(result.electiveUnplaced()).isEmpty();
        verify(timetableStaffingService).staffCell(900L, new StaffingAssignmentRequest(500L, null));
    }

    @Test
    void runSurfacesPostRunConflictsFromTermWideScan_flagOnlyNeverAutoResolved() {
        when(studentTermEnrollmentRepository.findDistinctCohortIdsByTermInstanceId(10L, EnrollmentStatus.ENROLLED))
            .thenReturn(new HashSet<>(List.of(1L)));
        cohort(1L, "Cohort 1");
        facultyWithDailyCap(500L, "XYZ", 6);
        when(courseOfferingService.getOfferingsByTermInstanceAndCohort(10L, 1L)).thenReturn(List.of(offeringDto(100L, "Offering A")));
        assignWholeCohort(100L, 1L, 500L);
        offeringEntity(100L, 10, 0, 0);
        when(timetableSkeletonService.resolveActiveSections(1L, 10L)).thenReturn(List.of());
        when(batchRepository.findByCourseOfferingId(anyLong())).thenReturn(List.of());

        SkeletonSubjectBudget budget = new SkeletonSubjectBudget(ClassSessionType.THEORY, null, null, null, null, 10, 10, 1, 0);
        SkeletonSubjectResponse subject = new SkeletonSubjectResponse(100L, "Offering A", "OFFE", List.of(budget), null, null);
        SkeletonBuilderResponse skeleton = new SkeletonBuilderResponse(1L, "Cohort 1", "Term", List.of(subject), List.of(), List.of(), List.of(), 25, 0L, List.of(), false, List.of());
        when(timetableSkeletonService.getCohortSkeleton(10L, 1L)).thenReturn(skeleton);

        SkeletonCellResponse placed = new SkeletonCellResponse(900L, ClassSessionType.THEORY, DayOfWeek.MONDAY, 1L, "1st Period",
            LocalTime.of(9, 0), LocalTime.of(9, 50), null, null, null, null, false, null, null, List.of(),
            100L, "Offering A", "OFFE", null, null, null, false, false);
        when(timetableSkeletonService.placeCell(any(SkeletonCellPlacementRequest.class))).thenReturn(placed);
        when(timetableStaffingService.staffCell(eq(900L), any(StaffingAssignmentRequest.class)))
            .thenReturn(new UnstaffedCellResponse(900L, 100L, "Offering A", "OFFE", null, null,
                ClassSessionType.THEORY, DayOfWeek.MONDAY, 1L, "1st Period", LocalTime.of(9, 0), LocalTime.of(9, 50),
                null, null, null, null, null, false, List.of(), null, null));

        // A pinned cell this run left standing (never re-placed) happens to double-book a room with
        // something else already in the term -- exactly the case a rebuild cannot fix on its own.
        TimetableConflictRow conflictRow = new TimetableConflictRow(
            777L, "Pathophysiology", "PATH101", ClassSessionType.THEORY, DayOfWeek.TUESDAY, "3rd Period",
            LocalTime.of(11, 0), LocalTime.of(11, 50), "Dr. Faculty", "Room 204", "Cohort 2",
            com.cms.model.enums.ClassScheduleStatus.DRAFT,
            List.of(new ConstraintViolation("CONFLICT_ROOM_DOUBLE_BOOKED", "Room 204 is already booked for this day/period")));
        when(timetableConflictInspectorService.scanTerm(10L)).thenReturn(
            new com.cms.dto.ConflictScanResponse(10L, "Term", java.time.Instant.now(), 5, 1, 1,
                java.util.Map.of("CONFLICT_ROOM_DOUBLE_BOOKED", 1), List.of(conflictRow)));

        var result = service.runGlobalAutoSchedule(10L, null);

        // The run still placed/staffed normally -- the conflict scan is purely additive reporting,
        // never a reason to withhold or alter what this run itself placed.
        assertThat(result.totalPlaced()).isEqualTo(1);
        assertThat(result.postRunConflicts()).hasSize(1);
        assertThat(result.postRunConflicts().get(0).classScheduleId()).isEqualTo(777L);
        assertThat(result.postRunConflicts().get(0).violations()).extracting(ConstraintViolation::code)
            .containsExactly("CONFLICT_ROOM_DOUBLE_BOOKED");
        // Flag-only: the scan is never asked to resolve or remove anything -- scanTerm has no
        // side-effecting counterpart this run could even call.
        verify(timetableConflictInspectorService).scanTerm(10L);
    }

    /** Regression for the real-world "Adult Health Nursing I" incident: 140 curriculum theory
     *  hours over a 26-week term round up (via CurriculumHoursCalculator's ceil-per-week math) to
     *  7 required weekly THEORY sessions -- one more than there are candidate days (Mon-Sat, with
     *  Saturday configured here) under the one-session-per-day rule. Before the
     *  second-session-per-day fallback in placeShortfallRow, the 7th session reported as
     *  permanently "unplaced" on every single run, forever, with no way to ever close it. Now it
     *  falls back to a second session on an already-used day once every day has genuinely been
     *  tried once -- still real placeCell/staffCell calls, just not excluded purely for the day
     *  already carrying one session.
     *
     *  <p>Hours are planned against the term's total (2026-09-15): 7 weekly sessions x 26 weeks is
     *  182 runs. One a day Monday-Saturday delivers 5 x 26 + 6 (first Saturdays only) = 136, so two
     *  weekday doubles close it -- 8 placements, not 7, because the Saturday session doesn't run
     *  every week. */
    @Test
    void runFallsBackToASecondSessionPerDay_whenWeeklyRequirementExceedsAvailableDays() {
        termInstance.setWorkingSaturdayWeeks(java.util.Set.of(com.cms.model.enums.WeekOfMonth.FIRST));
        when(studentTermEnrollmentRepository.findDistinctCohortIdsByTermInstanceId(10L, EnrollmentStatus.ENROLLED))
            .thenReturn(new HashSet<>(List.of(1L)));
        cohort(1L, "Cohort 1");
        facultyWithDailyCap(500L, "XYZ", 8);
        when(courseOfferingService.getOfferingsByTermInstanceAndCohort(10L, 1L)).thenReturn(List.of(offeringDto(100L, "Offering A")));
        assignWholeCohort(100L, 1L, 500L);
        offeringEntity(100L, 140, 0, 0);
        when(timetableSkeletonService.resolveActiveSections(1L, 10L)).thenReturn(List.of());
        when(batchRepository.findByCourseOfferingId(anyLong())).thenReturn(List.of());

        // totalHours=140/weeksInTerm=26 (the real curriculum numbers) round up to
        // requiredSessionsPerWeek=7; placedSessionsPerWeek=0 -- a 7-session shortfall in a 6-day week.
        SkeletonSubjectBudget budget = new SkeletonSubjectBudget(ClassSessionType.THEORY, null, null, null, null, 140, 26, 7, 0);
        SkeletonSubjectResponse subject = new SkeletonSubjectResponse(100L, "Offering A", "OFFE", List.of(budget), null, null);
        SkeletonBuilderResponse skeleton = new SkeletonBuilderResponse(1L, "Cohort 1", "Term", List.of(subject), List.of(), List.of(), List.of(), 25, 0L, List.of(), false, List.of());
        when(timetableSkeletonService.getCohortSkeleton(10L, 1L)).thenReturn(skeleton);

        SkeletonCellResponse placed = new SkeletonCellResponse(900L, ClassSessionType.THEORY, DayOfWeek.MONDAY, 1L, "1st Period",
            LocalTime.of(9, 0), LocalTime.of(9, 50), null, null, null, null, false, null, null, List.of(),
            100L, "Offering A", "OFFE", null, null, null, false, false);
        when(timetableSkeletonService.placeCell(any(SkeletonCellPlacementRequest.class))).thenReturn(placed);
        when(timetableStaffingService.staffCell(eq(900L), any(StaffingAssignmentRequest.class)))
            .thenReturn(new UnstaffedCellResponse(900L, 100L, "Offering A", "OFFE", null, null,
                ClassSessionType.THEORY, DayOfWeek.MONDAY, 1L, "1st Period", LocalTime.of(9, 0), LocalTime.of(9, 50),
                null, null, null, null, null, false, List.of(), null, null));

        var result = service.runGlobalAutoSchedule(10L, null);

        assertThat(result.totalPlaced()).isEqualTo(8);
        assertThat(result.totalStaffed()).isEqualTo(8);
        assertThat(result.cohortSummaries().get(0).unplaced()).extracting(AutoPlaceUnplacedItem::reason)
            .containsExactly(NO_LIBRARY_CLASSROOM_REASON);
        verify(timetableSkeletonService, org.mockito.Mockito.times(8)).placeCell(any(SkeletonCellPlacementRequest.class));
    }

    /** Real incident fixture: "Forensic Nursing and Indian Laws" (CORE, 1 session/week owed) and
     *  "Self-Study/Co-curricular V" (CO_CURRICULAR, 2 sessions/week owed) compete for the cohort's
     *  one remaining free slot. Self-Study has the BIGGER shortfall, so the old descending-
     *  shortfall-only comparator tried it first and let it claim the slot, starving the real
     *  curriculum requirement (Forensic Nursing) entirely — exactly what was reported. {@code
     *  SHORTFALL_ROW_ORDER}'s mandatory-vs-advisory partition must place Forensic Nursing first
     *  regardless of the shortfall gap. */
    @Test
    void runPlacesMandatoryCoreSubjectBeforeAdvisoryCoCurricularSubject_evenWithASmallerShortfall() {
        when(studentTermEnrollmentRepository.findDistinctCohortIdsByTermInstanceId(10L, EnrollmentStatus.ENROLLED))
            .thenReturn(new HashSet<>(List.of(1L)));
        cohort(1L, "Cohort 1");
        facultyWithDailyCap(500L, "XYZ", 8);
        when(courseOfferingService.getOfferingsByTermInstanceAndCohort(10L, 1L))
            .thenReturn(List.of(offeringDto(100L, "Forensic Nursing and Indian Laws"), offeringDto(200L, "Self-Study/Co-curricular V")));
        assignWholeCohort(100L, 1L, 500L);
        assignWholeCohort(200L, 1L, 500L);
        offeringEntity(100L, 20, 0, 0);
        offeringEntity(200L, 40, 0, 0, SubjectType.CO_CURRICULAR);
        when(timetableSkeletonService.resolveActiveSections(1L, 10L)).thenReturn(List.of());
        when(batchRepository.findByCourseOfferingId(anyLong())).thenReturn(List.of());

        SkeletonSubjectBudget forensicBudget = new SkeletonSubjectBudget(ClassSessionType.THEORY, null, null, null, null, 20, 26, 1, 0);
        SkeletonSubjectBudget selfStudyBudget = new SkeletonSubjectBudget(ClassSessionType.THEORY, null, null, null, null, 40, 26, 2, 0);
        SkeletonSubjectResponse forensicSubject = new SkeletonSubjectResponse(100L, "Forensic Nursing and Indian Laws", "FORE", List.of(forensicBudget), null, null);
        SkeletonSubjectResponse selfStudySubject = new SkeletonSubjectResponse(200L, "Self-Study/Co-curricular V", "SELF", List.of(selfStudyBudget), null, null);
        SkeletonBuilderResponse skeleton = new SkeletonBuilderResponse(1L, "Cohort 1", "Term",
            List.of(forensicSubject, selfStudySubject), List.of(), List.of(), List.of(), 25, 0L, List.of(), false, List.of());
        when(timetableSkeletonService.getCohortSkeleton(10L, 1L)).thenReturn(skeleton);

        // Only one placement can ever succeed -- simulates the cohort's week being fully saturated
        // except for this single free slot, exactly like the reported real-world scenario.
        java.util.concurrent.atomic.AtomicInteger placeCalls = new java.util.concurrent.atomic.AtomicInteger(0);
        java.util.concurrent.atomic.AtomicLong placedOfferingId = new java.util.concurrent.atomic.AtomicLong(-1);
        when(timetableSkeletonService.placeCell(any(SkeletonCellPlacementRequest.class))).thenAnswer(invocation -> {
            SkeletonCellPlacementRequest request = invocation.getArgument(0);
            if (placeCalls.getAndIncrement() == 0) {
                placedOfferingId.set(request.courseOfferingId());
                return new SkeletonCellResponse(900L, request.sessionType(), request.dayOfWeek(), request.periodId(), "1st Period",
                    LocalTime.of(9, 0), LocalTime.of(9, 50), null, null, null, null, false, null, null, List.of(),
                    request.courseOfferingId(), "Offering", "OFF", null, null, null, false, false);
            }
            throw new TimetableConstraintViolationException(List.of(
                new com.cms.dto.ConstraintViolation("SKELETON_CELL_ALREADY_PLACED", "only one free slot in this fixture")));
        });
        lenient().when(timetableStaffingService.staffCell(eq(900L), any(StaffingAssignmentRequest.class)))
            .thenReturn(new UnstaffedCellResponse(900L, 100L, "Offering", "OFF", null, null,
                ClassSessionType.THEORY, DayOfWeek.MONDAY, 1L, "1st Period", LocalTime.of(9, 0), LocalTime.of(9, 50),
                null, null, null, null, null, false, List.of(), null, null));

        var result = service.runGlobalAutoSchedule(10L, null);

        assertThat(result.totalPlaced()).isEqualTo(1);
        assertThat(placedOfferingId.get()).isEqualTo(100L);
    }

    // ── Phase B — cross-offering LAB pairing ──────────────────────────────

    private java.util.concurrent.atomic.AtomicLong stubPlaceCellAlwaysSucceeds() {
        java.util.concurrent.atomic.AtomicLong nextCellId = new java.util.concurrent.atomic.AtomicLong(900L);
        when(timetableSkeletonService.placeCell(any(SkeletonCellPlacementRequest.class))).thenAnswer(invocation -> {
            SkeletonCellPlacementRequest request = invocation.getArgument(0);
            return new SkeletonCellResponse(nextCellId.incrementAndGet(), request.sessionType(), request.dayOfWeek(),
                request.periodId(), "Period", LocalTime.of(9, 0), LocalTime.of(9, 50), request.batchId(), null, null, null,
                false, null, null, List.of(), request.courseOfferingId(), "Offering", "OFF", null, null, null, false, false);
        });
        when(timetableStaffingService.staffCell(anyLong(), any(StaffingAssignmentRequest.class)))
            .thenAnswer(invocation -> new UnstaffedCellResponse(invocation.getArgument(0), 100L, "Subject", "SUBJ", null, null,
                ClassSessionType.LAB, DayOfWeek.MONDAY, 1L, "Period", LocalTime.of(9, 0), LocalTime.of(9, 50),
                null, null, null, null, null, false, List.of(), null, null));
        return nextCellId;
    }

    /** Real fixture numbers from this session's own investigation: Child Health Nursing (offering
     *  73) split into 4 active batches on one Lab; Educational Technology (offering 76) split into
     *  only 2, on a different Lab, for the exact same cohort section. V1's pairing gate deliberately
     *  requires BOTH offerings to have exactly 2 — see class javadoc / this session's specialist
     *  round choosing "skip" over guessing an asymmetric N/M design. Both offerings must therefore
     *  fall straight through to independent per-batch placement, unpaired. */
    @Test
    void pairingSkipsWhenBatchCountsMismatch_realIncidentFixtureNumbers() {
        when(studentTermEnrollmentRepository.findDistinctCohortIdsByTermInstanceId(10L, EnrollmentStatus.ENROLLED))
            .thenReturn(new HashSet<>(List.of(1L)));
        cohort(1L, "Cohort 1");
        when(courseOfferingService.getOfferingsByTermInstanceAndCohort(10L, 1L))
            .thenReturn(List.of(offeringDto(73L, "Child Health Nursing"), offeringDto(76L, "Educational Technology")));
        when(timetableSkeletonService.resolveActiveSections(1L, 10L)).thenReturn(List.of());

        CohortSection section = new CohortSection();
        section.setId(52L);

        CourseOffering childHealth = offeringEntity(73L, 0, 30, 0);
        childHealth.setSubject(labSubject("Child Health Nursing"));
        CourseOffering edTech = offeringEntity(76L, 0, 30, 0);
        edTech.setSubject(labSubject("Educational Technology"));

        Lab labA = lab(1L);
        Lab labB = lab(2L);
        Batch a0 = labBatch(3001L, labA, section, 500L);
        Batch a1 = labBatch(3002L, labA, section, 500L);
        Batch a2 = labBatch(3003L, labA, section, 500L);
        Batch a3 = labBatch(3004L, labA, section, 500L);
        Batch b0 = labBatch(3005L, labB, section, 600L);
        Batch b1 = labBatch(3006L, labB, section, 600L);
        when(batchRepository.findByCourseOfferingId(73L)).thenReturn(List.of(a0, a1, a2, a3));
        when(batchRepository.findByCourseOfferingId(76L)).thenReturn(List.of(b0, b1));

        List<SkeletonSubjectBudget> chnBudgets = List.of(a0, a1, a2, a3).stream()
            .map(b -> new SkeletonSubjectBudget(ClassSessionType.LAB, b.getId(), null, 52L, null, 30, 10, 1, 0))
            .toList();
        List<SkeletonSubjectBudget> edtBudgets = List.of(b0, b1).stream()
            .map(b -> new SkeletonSubjectBudget(ClassSessionType.LAB, b.getId(), null, 52L, null, 30, 10, 1, 0))
            .toList();
        SkeletonSubjectResponse chnSubject = new SkeletonSubjectResponse(73L, "Child Health Nursing", "CHN", chnBudgets, null, null);
        SkeletonSubjectResponse edtSubject = new SkeletonSubjectResponse(76L, "Educational Technology", "EDT", edtBudgets, null, null);
        SkeletonBuilderResponse skeleton = new SkeletonBuilderResponse(1L, "Cohort 1", "Term",
            List.of(chnSubject, edtSubject), List.of(), List.of(), List.of(), 25, 0L, List.of(), false, List.of());
        when(timetableSkeletonService.getCohortSkeleton(10L, 1L)).thenReturn(skeleton);

        lenient().when(periodRepository.findById(1L)).thenReturn(Optional.of(period1));
        stubPlaceCellAlwaysSucceeds();

        var result = service.runGlobalAutoSchedule(10L, null);

        verify(rotationGroupService, never()).create(any(), any());
        assertThat(result.rotationGroupsCreated()).isZero();
        verify(timetableSkeletonService, times(6)).placeCell(any(SkeletonCellPlacementRequest.class));
    }

    /** The matched shape V1 actually supports: two offerings sharing one cohort section, each split
     *  into exactly 2 active batches on its own (different) Lab, each needing exactly one LAB
     *  session/week. Confirms the real behavior end to end: exactly one RotationGroup is created,
     *  its 2 slots/2 members pair the batches ordinally (batch0-with-batch0, batch1-with-batch1 --
     *  see class javadoc for why that's treated as safe), and Phase 1's own independent loop is
     *  never ALSO given these 4 rows to place on top of the rotation (only the 2 real rotation cells
     *  get placed, not 4). */
    @Test
    void pairingCreatesOneRotationGroupWhenBothOfferingsHaveExactlyTwoMatchingBatches() {
        when(studentTermEnrollmentRepository.findDistinctCohortIdsByTermInstanceId(10L, EnrollmentStatus.ENROLLED))
            .thenReturn(new HashSet<>(List.of(1L)));
        cohort(1L, "Cohort 1");
        when(courseOfferingService.getOfferingsByTermInstanceAndCohort(10L, 1L))
            .thenReturn(List.of(offeringDto(73L, "Child Health Nursing"), offeringDto(76L, "Educational Technology")));
        when(timetableSkeletonService.resolveActiveSections(1L, 10L)).thenReturn(List.of());

        CohortSection section = new CohortSection();
        section.setId(52L);

        CourseOffering offeringA = offeringEntity(73L, 0, 30, 0);
        offeringA.setSubject(labSubject("Child Health Nursing"));
        CourseOffering offeringB = offeringEntity(76L, 0, 30, 0);
        offeringB.setSubject(labSubject("Educational Technology"));

        Lab labA = lab(1L);
        Lab labB = lab(2L);
        Batch a0 = labBatch(3001L, labA, section, 500L);
        Batch a1 = labBatch(3002L, labA, section, 500L);
        Batch b0 = labBatch(3005L, labB, section, 600L);
        Batch b1 = labBatch(3006L, labB, section, 600L);
        when(batchRepository.findByCourseOfferingId(73L)).thenReturn(List.of(a0, a1));
        when(batchRepository.findByCourseOfferingId(76L)).thenReturn(List.of(b0, b1));

        List<SkeletonSubjectBudget> chnBudgets = List.of(a0, a1).stream()
            .map(b -> new SkeletonSubjectBudget(ClassSessionType.LAB, b.getId(), null, 52L, null, 30, 10, 1, 0))
            .toList();
        List<SkeletonSubjectBudget> edtBudgets = List.of(b0, b1).stream()
            .map(b -> new SkeletonSubjectBudget(ClassSessionType.LAB, b.getId(), null, 52L, null, 30, 10, 1, 0))
            .toList();
        SkeletonSubjectResponse chnSubject = new SkeletonSubjectResponse(73L, "Child Health Nursing", "CHN", chnBudgets, null, null);
        SkeletonSubjectResponse edtSubject = new SkeletonSubjectResponse(76L, "Educational Technology", "EDT", edtBudgets, null, null);
        SkeletonBuilderResponse skeleton = new SkeletonBuilderResponse(1L, "Cohort 1", "Term",
            List.of(chnSubject, edtSubject), List.of(), List.of(), List.of(), 25, 0L, List.of(), false, List.of());
        when(timetableSkeletonService.getCohortSkeleton(10L, 1L)).thenReturn(skeleton);

        stubPlaceCellAlwaysSucceeds();
        when(rotationGroupService.create(any(RotationGroupCreateRequest.class), anyString()))
            .thenReturn(new RotationGroupResponse(1L, 10L, "label", 2, LocalDate.of(2025, 6, 2), List.of(), List.of(), List.of()));

        var result = service.runGlobalAutoSchedule(10L, null);

        ArgumentCaptor<RotationGroupCreateRequest> requestCaptor = ArgumentCaptor.forClass(RotationGroupCreateRequest.class);
        verify(rotationGroupService, times(1)).create(requestCaptor.capture(), eq("system:global-auto-schedule"));
        RotationGroupCreateRequest captured = requestCaptor.getValue();
        assertThat(captured.slots()).hasSize(2);
        assertThat(captured.members()).hasSize(2);
        assertThat(captured.members().get(0).assignments())
            .extracting(RotationGroupCreateRequest.RotationAssignmentInput::batchId)
            .containsExactlyInAnyOrder(a0.getId(), b0.getId());
        assertThat(captured.members().get(1).assignments())
            .extracting(RotationGroupCreateRequest.RotationAssignmentInput::batchId)
            .containsExactlyInAnyOrder(a1.getId(), b1.getId());

        // Only the 2 rotation cells are placed for these offerings -- Phase 1's own independent loop
        // must never ALSO place a 3rd/4th LAB session on top of the rotation.
        verify(timetableSkeletonService, times(2)).placeCell(any(SkeletonCellPlacementRequest.class));
        assertThat(result.rotationGroupsCreated()).isEqualTo(1);
    }

    // ── Single-offering batch rotation (rules 3/4/5, user's hierarchy) ────

    /** The screenshot's own case: ONE offering, ONE lab, two batches -- no second offering exists to
     *  pair with (Phase B above never even considers it: its own inner loop needs a SECOND
     *  {@code PairableOfferingGroup}). Confirms the new fallback picks this up instead of leaving the
     *  "off-duty" batch with nothing: batch0 gets the real Lab cell, batch1 gets a Library cell at
     *  the exact same slot, and both are wrapped in a rotation that swaps them (proven by asserting
     *  each rotation slot gets a DIFFERENT batch in Group 1 vs Group 2, not just "both batches appear
     *  somewhere"). */
    @Test
    void singleOfferingWithOneSharedLabAndTwoBatches_rotatesLabAgainstLibrary() {
        when(studentTermEnrollmentRepository.findDistinctCohortIdsByTermInstanceId(10L, EnrollmentStatus.ENROLLED))
            .thenReturn(new HashSet<>(List.of(1L)));
        cohort(1L, "Cohort 1");
        when(courseOfferingService.getOfferingsByTermInstanceAndCohort(10L, 1L))
            .thenReturn(List.of(offeringDto(73L, "Nursing Skills Lab")));
        when(timetableSkeletonService.resolveActiveSections(1L, 10L)).thenReturn(List.of());

        CohortSection section = new CohortSection();
        section.setId(52L);

        CourseOffering offering = offeringEntity(73L, 0, 30, 0);
        offering.setSubject(labSubject("Nursing Skills Lab"));

        Lab sharedLab = lab(1L);
        Batch batch0 = labBatch(3001L, sharedLab, section, 500L);
        Batch batch1 = labBatch(3002L, sharedLab, section, 500L);
        when(batchRepository.findByCourseOfferingId(73L)).thenReturn(List.of(batch0, batch1));
        when(batchRepository.countStudents(anyLong())).thenReturn(0L);
        when(classScheduleRepository.findByBatchIdInAndIsActiveTrue(List.of(batch1.getId()))).thenReturn(List.of());

        List<SkeletonSubjectBudget> budgets = List.of(batch0, batch1).stream()
            .map(b -> new SkeletonSubjectBudget(ClassSessionType.LAB, b.getId(), null, 52L, null, 30, 10, 1, 0))
            .toList();
        SkeletonSubjectResponse subject = new SkeletonSubjectResponse(73L, "Nursing Skills Lab", "NSKL", budgets, null, null);
        SkeletonBuilderResponse skeleton = new SkeletonBuilderResponse(1L, "Cohort 1", "Term",
            List.of(subject), List.of(), List.of(), List.of(), 25, 0L, List.of(), false, List.of());
        when(timetableSkeletonService.getCohortSkeleton(10L, 1L)).thenReturn(skeleton);

        stubPlaceCellAlwaysSucceeds();

        Subject librarySubject = new Subject();
        librarySubject.setId(999L);
        librarySubject.setCode("SYSTEM-LIBRARY");
        when(subjectRepository.findByCode("SYSTEM-LIBRARY")).thenReturn(Optional.of(librarySubject));
        Classroom libraryRoom = new Classroom("Library Hall", null, null, 60);
        libraryRoom.setId(50L);
        when(classroomRepository.findByIsActiveTrueAndRoom_PurposeCategory_CodeOrderByNameAsc(any()))
            .thenReturn(List.of(libraryRoom));
        when(timetableStaffingService.checkRoomFree(any(), any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(Optional.empty());
        when(timetableSkeletonService.saveIdleBatchLibraryCells(any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(List.of(777L));
        when(rotationGroupService.create(any(RotationGroupCreateRequest.class), anyString()))
            .thenReturn(new RotationGroupResponse(1L, 10L, "label", 2, LocalDate.of(2025, 6, 2), List.of(), List.of(), List.of()));

        var result = service.runGlobalAutoSchedule(10L, null);

        ArgumentCaptor<RotationGroupCreateRequest> requestCaptor = ArgumentCaptor.forClass(RotationGroupCreateRequest.class);
        verify(rotationGroupService, times(1)).create(requestCaptor.capture(), eq("system:global-auto-schedule"));
        RotationGroupCreateRequest captured = requestCaptor.getValue();
        assertThat(captured.slots()).hasSize(2);
        assertThat(captured.members()).hasSize(2);

        Long labSlotId = captured.slots().get(0).classScheduleId();
        Long librarySlotId = captured.slots().get(1).classScheduleId();
        var group1 = captured.members().get(0).assignments();
        var group2 = captured.members().get(1).assignments();
        Long group1LabBatch = group1.stream().filter(a -> a.classScheduleId().equals(labSlotId)).findFirst().orElseThrow().batchId();
        Long group2LabBatch = group2.stream().filter(a -> a.classScheduleId().equals(labSlotId)).findFirst().orElseThrow().batchId();
        Long group1LibraryBatch = group1.stream().filter(a -> a.classScheduleId().equals(librarySlotId)).findFirst().orElseThrow().batchId();
        Long group2LibraryBatch = group2.stream().filter(a -> a.classScheduleId().equals(librarySlotId)).findFirst().orElseThrow().batchId();

        // The two groups must swap which batch is in the Lab vs. Library slot -- that's the whole
        // point (rule 4). Whichever batch has the Lab in Group 1 must be the one in the Library in
        // Group 2, and vice versa.
        assertThat(group1LabBatch).isNotEqualTo(group2LabBatch);
        assertThat(group1LabBatch).isEqualTo(group2LibraryBatch);
        assertThat(group1LibraryBatch).isEqualTo(group2LabBatch);
        assertThat(Set.of(group1LabBatch, group2LabBatch)).containsExactlyInAnyOrder(batch0.getId(), batch1.getId());

        verify(timetableSkeletonService, times(1)).saveIdleBatchLibraryCells(any(), any(), any(), any(), any(), any(), any());
        verify(timetableSkeletonService, times(1)).placeCell(any(SkeletonCellPlacementRequest.class));
        assertThat(result.rotationGroupsCreated()).isEqualTo(1);
        assertThat(result.cohortSummaries()).extracting(CohortPlacementSummary::unplaced)
            .allSatisfy(items -> assertThat(items).noneMatch(i -> "Lab capacity".equals(i.subjectName())));
    }

    /** When neither a Library room nor a Self-Study fallback exists at ANY candidate slot, the row
     *  must NOT be silently dropped -- it stays in the queue exactly as before this feature existed
     *  (falls through to Phase 1's ordinary placement), and one advisory recommendation is recorded
     *  instead of a rotation. No regression: both batches still end up with SOME real Lab cell (via
     *  the pre-existing independent-placement path), just not rotated. */
    @Test
    void singleOfferingWithOneSharedLab_fallsThroughWithAdvisoryWhenNoLibraryOrSelfStudyAvailable() {
        when(studentTermEnrollmentRepository.findDistinctCohortIdsByTermInstanceId(10L, EnrollmentStatus.ENROLLED))
            .thenReturn(new HashSet<>(List.of(1L)));
        cohort(1L, "Cohort 1");
        when(courseOfferingService.getOfferingsByTermInstanceAndCohort(10L, 1L))
            .thenReturn(List.of(offeringDto(73L, "Nursing Skills Lab")));
        when(timetableSkeletonService.resolveActiveSections(1L, 10L)).thenReturn(List.of());

        CohortSection section = new CohortSection();
        section.setId(52L);

        CourseOffering offering = offeringEntity(73L, 0, 30, 0);
        offering.setSubject(labSubject("Nursing Skills Lab"));

        Lab sharedLab = lab(1L);
        Batch batch0 = labBatch(3001L, sharedLab, section, 500L);
        Batch batch1 = labBatch(3002L, sharedLab, section, 500L);
        when(batchRepository.findByCourseOfferingId(73L)).thenReturn(List.of(batch0, batch1));
        when(batchRepository.countStudents(anyLong())).thenReturn(0L);
        when(classScheduleRepository.findByBatchIdInAndIsActiveTrue(List.of(batch1.getId()))).thenReturn(List.of());

        List<SkeletonSubjectBudget> budgets = List.of(batch0, batch1).stream()
            .map(b -> new SkeletonSubjectBudget(ClassSessionType.LAB, b.getId(), null, 52L, null, 30, 10, 1, 0))
            .toList();
        SkeletonSubjectResponse subject = new SkeletonSubjectResponse(73L, "Nursing Skills Lab", "NSKL", budgets, null, null);
        SkeletonBuilderResponse skeleton = new SkeletonBuilderResponse(1L, "Cohort 1", "Term",
            List.of(subject), List.of(), List.of(), List.of(), 25, 0L, List.of(), false, List.of());
        when(timetableSkeletonService.getCohortSkeleton(10L, 1L)).thenReturn(skeleton);

        stubPlaceCellAlwaysSucceeds();

        // No Library classroom configured at all, and no Self-Study curriculum offering in the
        // skeleton either (resolveSelfStudyRowForFallback finds nothing) -- both fallbacks refuse.
        when(subjectRepository.findByCode("SYSTEM-LIBRARY")).thenReturn(Optional.empty());
        when(classroomRepository.findByIsActiveTrueAndRoom_PurposeCategory_CodeOrderByNameAsc(any())).thenReturn(List.of());
        // Both rows fall through to ordinary placement, which Phase 1.5's fillIdleBatchGaps then
        // examines (same reason pairingSkipsWhenBatchCountsMismatch_realIncidentFixtureNumbers above
        // needs this same stub) -- it reconstructs each placed cell's Period from periodRepository.
        lenient().when(periodRepository.findById(1L)).thenReturn(Optional.of(period1));

        var result = service.runGlobalAutoSchedule(10L, null);

        verify(rotationGroupService, never()).create(any(), any());
        assertThat(result.rotationGroupsCreated()).isZero();
        // Both rows fell through to the ordinary, independent per-batch placement, exactly as if
        // this feature didn't exist -- both batches still get a real LAB cell each (no regression).
        verify(timetableSkeletonService, atLeastOnce()).placeCell(any(SkeletonCellPlacementRequest.class));
        assertThat(result.cohortSummaries()).extracting(CohortPlacementSummary::unplaced)
            .anySatisfy(items -> assertThat(items).anyMatch(i -> "Lab capacity".equals(i.subjectName())
                && i.reason().contains("only 1 lab provisioned")));
    }

    @Test
    void runToleratesAPreExistingLibraryCellInTheSkeletonSnapshot_libraryCellsHaveNoCourseOffering() {
        // Regression: a LIBRARY cell has no CourseOffering at all (TimetableSkeletonService's
        // toCellResponse), so courseOfferingId() is legitimately null on it -- existingDaysForBudgetRow
        // used to call cell.courseOfferingId().equals(...) unguarded, which NPE'd the instant any
        // Library cell already sat in the skeleton snapshot passed to a THEORY/LAB/CLINICAL row's
        // shortfall placement.
        when(studentTermEnrollmentRepository.findDistinctCohortIdsByTermInstanceId(10L, EnrollmentStatus.ENROLLED))
            .thenReturn(new HashSet<>(List.of(1L)));
        cohort(1L, "Cohort 1");
        facultyWithDailyCap(500L, "XYZ", 6);
        when(courseOfferingService.getOfferingsByTermInstanceAndCohort(10L, 1L)).thenReturn(List.of(offeringDto(100L, "Offering A")));
        assignWholeCohort(100L, 1L, 500L);
        offeringEntity(100L, 10, 0, 0);
        when(timetableSkeletonService.resolveActiveSections(1L, 10L)).thenReturn(List.of());
        when(batchRepository.findByCourseOfferingId(anyLong())).thenReturn(List.of());

        SkeletonSubjectBudget budget = new SkeletonSubjectBudget(ClassSessionType.THEORY, null, null, null, null, 10, 10, 1, 0);
        SkeletonSubjectResponse subject = new SkeletonSubjectResponse(100L, "Offering A", "OFFE", List.of(budget), null, null);
        SkeletonCellResponse preExistingLibraryCell = new SkeletonCellResponse(800L, ClassSessionType.LIBRARY,
            DayOfWeek.TUESDAY, 2L, "2nd Period", LocalTime.of(9, 50), LocalTime.of(10, 40), null, null, null, null,
            true, null, null, List.of(), null, null, null, null, null, null, false, false);
        SkeletonBuilderResponse skeleton = new SkeletonBuilderResponse(1L, "Cohort 1", "Term", List.of(subject),
            List.of(preExistingLibraryCell), List.of(), List.of(), 25, 0L, List.of(), false, List.of());
        when(timetableSkeletonService.getCohortSkeleton(10L, 1L)).thenReturn(skeleton);

        SkeletonCellResponse placed = new SkeletonCellResponse(900L, ClassSessionType.THEORY, DayOfWeek.MONDAY, 1L, "1st Period",
            LocalTime.of(9, 0), LocalTime.of(9, 50), null, null, null, null, false, null, null, List.of(),
            100L, "Offering A", "OFFE", null, null, null, false, false);
        when(timetableSkeletonService.placeCell(any(SkeletonCellPlacementRequest.class))).thenReturn(placed);
        when(timetableStaffingService.staffCell(eq(900L), any(StaffingAssignmentRequest.class)))
            .thenReturn(new UnstaffedCellResponse(900L, 100L, "Offering A", "OFFE", null, null,
                ClassSessionType.THEORY, DayOfWeek.MONDAY, 1L, "1st Period", LocalTime.of(9, 0), LocalTime.of(9, 50),
                null, null, null, null, null, false, List.of(), null, null));

        var result = service.runGlobalAutoSchedule(10L, null);

        assertThat(result.totalPlaced()).isEqualTo(1);
        assertThat(result.totalStaffed()).isEqualTo(1);
    }

    private SkeletonCellResponse libraryCell(long id, DayOfWeek day) {
        return new SkeletonCellResponse(id, ClassSessionType.LIBRARY, day, 1L, "1st Period",
            LocalTime.of(9, 0), LocalTime.of(9, 50), null, null, null, null,
            true, com.cms.model.enums.ClassScheduleStatus.PUBLISHED, null, List.of(), null, null, null, null, null, null, false, false);
    }

    /** Regression for the real-world "Library over-placed on 3 days when the quota is 2/week"
     *  incident: {@code fillLibraryGaps} used to start counting from zero every run, never checking
     *  what was already placed -- a cohort that already had Monday's session would still get a
     *  fresh attempt at the FULL sessionsPerWeek quota, spill past Monday (blocked by
     *  isSlotFreeForCohort) onto a 3rd day, and never stop. This asserts only the genuinely-
     *  remaining 1 session (of a 2/week quota) gets placed, and never on the day that's already
     *  accounted for. The pre-existing cells here are PUBLISHED, since a DRAFT one wouldn't survive
     *  {@code purgeDraftCellsForRebuild} to be counted at all -- the surviving case this guards is a
     *  published session, or one this same run placed earlier for another audience. */
    @Test
    void fillLibraryGapsCountsExistingPlacements_beforeAddingMore() {
        when(studentTermEnrollmentRepository.findDistinctCohortIdsByTermInstanceId(10L, EnrollmentStatus.ENROLLED))
            .thenReturn(new HashSet<>(List.of(1L)));
        cohort(1L, "Cohort 1");
        when(courseOfferingService.getOfferingsByTermInstanceAndCohort(10L, 1L)).thenReturn(List.of());
        when(timetableSkeletonService.resolveActiveSections(1L, 10L)).thenReturn(List.of());

        Subject librarySubject = new Subject();
        librarySubject.setId(999L);
        librarySubject.setCode("SYSTEM-LIBRARY");
        when(subjectRepository.findByCode("SYSTEM-LIBRARY")).thenReturn(Optional.of(librarySubject));
        Classroom libraryRoom = new Classroom("Library Hall", null, null, 200);
        libraryRoom.setId(50L);
        when(classroomRepository.findByIsActiveTrueAndRoom_PurposeCategory_CodeOrderByNameAsc(any()))
            .thenReturn(List.of(libraryRoom));
        when(timetableSkeletonService.isSlotFreeForCohort(eq(1L), eq(10L), any(), any())).thenReturn(true);

        Period p2 = new Period("2nd Period", LocalTime.of(9, 50), LocalTime.of(10, 40), 2);
        p2.setId(2L);
        when(periodRepository.findByIsActiveTrueOrderByPeriodOrderAsc()).thenReturn(List.of(period1, p2));

        when(classScheduleRepository.save(any(ClassSchedule.class))).thenAnswer(inv -> {
            ClassSchedule cs = inv.getArgument(0);
            cs.setId(800L + idSequence.getAndIncrement());
            return cs;
        });
        // TimetableSkeletonService#saveLibraryBlockCells now does what this method's own
        // classScheduleRepository.save loop used to do directly (see that method's REQUIRES_NEW
        // javadoc) -- mirror the real implementation here so it still routes through the stub above,
        // keeping every save-count/day assertion below unchanged.
        when(timetableSkeletonService.saveLibraryBlockCells(any(), any(), any(), any(), any(), any()))
            .thenAnswer(inv -> {
                DayOfWeek day = inv.getArgument(2);
                @SuppressWarnings("unchecked")
                List<Period> block = (List<Period>) inv.getArgument(3);
                List<ClassSchedule> saved = new ArrayList<>();
                for (Period period : block) {
                    ClassSchedule cs = new ClassSchedule();
                    cs.setSessionType(ClassSessionType.LIBRARY);
                    cs.setDayOfWeek(day);
                    cs.setPeriod(period);
                    saved.add(classScheduleRepository.save(cs));
                }
                return saved;
            });

        // A 2/week quota so one session genuinely remains to place, and the bonus-session threshold
        // out of reach so only the quota is exercised here.
        libraryConfig("timetable.library_sessions_per_week", "2");
        libraryConfig("timetable.library_extra_session_min_free_periods", "99");

        // Pre-existing: Monday already has a full 2-period Library block for the whole cohort
        // (cohortSectionId null), PUBLISHED so the rebuild purge correctly leaves it standing.
        SkeletonCellResponse existingMon1 = libraryCell(700L, DayOfWeek.MONDAY);
        SkeletonCellResponse existingMon2 = libraryCell(701L, DayOfWeek.MONDAY);
        SkeletonBuilderResponse skeleton = new SkeletonBuilderResponse(1L, "Cohort 1", "Term", List.of(),
            List.of(existingMon1, existingMon2), List.of(), List.of(), 25, 0L, List.of(), false, List.of());
        when(timetableSkeletonService.getCohortSkeleton(10L, 1L)).thenReturn(skeleton);

        service.runGlobalAutoSchedule(10L, null);

        // Quota is 2/week; 1 already exists (Monday) -> exactly ONE more block (2 rows, since block
        // size defaults to 2 periods) gets placed, and never on Monday again.
        ArgumentCaptor<ClassSchedule> captor = ArgumentCaptor.forClass(ClassSchedule.class);
        verify(classScheduleRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).extracting(ClassSchedule::getDayOfWeek)
            .doesNotContain(DayOfWeek.MONDAY);
    }

    /** Lenient: the run also reads other Library/Sports settings, which keep their defaults. */
    private void libraryConfig(String key, String value) {
        lenient().when(systemConfigurationService.findByKey(key))
            .thenReturn(Optional.of(new SystemConfigurationResponse(null, key, value, null, null, null, null, null, null)));
    }

    private record LibraryRun(List<DayOfWeek> blockDays, List<String> infoNotes) {}

    /** One cohort with no curriculum and an empty two-period week (2 periods x 5 weekdays = 10 free
     *  periods), one Library room and the default quota of one Library session. Runs automation and
     *  returns the day of every Library block placed, plus the cohort's neutral notes. */
    private LibraryRun runLibraryOnEmptyWeek() {
        when(studentTermEnrollmentRepository.findDistinctCohortIdsByTermInstanceId(10L, EnrollmentStatus.ENROLLED))
            .thenReturn(new HashSet<>(List.of(1L)));
        cohort(1L, "Cohort 1");
        when(courseOfferingService.getOfferingsByTermInstanceAndCohort(10L, 1L)).thenReturn(List.of());
        when(timetableSkeletonService.resolveActiveSections(1L, 10L)).thenReturn(List.of());
        when(timetableSkeletonService.getCohortSkeleton(10L, 1L)).thenReturn(new SkeletonBuilderResponse(1L, "Cohort 1",
            "Term", List.of(), List.of(), List.of(), List.of(), 25, 0L, List.of(), false, List.of()));

        Subject librarySubject = new Subject();
        librarySubject.setId(999L);
        librarySubject.setCode("SYSTEM-LIBRARY");
        when(subjectRepository.findByCode("SYSTEM-LIBRARY")).thenReturn(Optional.of(librarySubject));
        Classroom libraryRoom = new Classroom("Library Hall", null, null, 200);
        libraryRoom.setId(50L);
        when(classroomRepository.findByIsActiveTrueAndRoom_PurposeCategory_CodeOrderByNameAsc(any()))
            .thenReturn(List.of(libraryRoom));
        when(timetableSkeletonService.isSlotFreeForCohort(eq(1L), eq(10L), any(), any())).thenReturn(true);
        Period p2 = new Period("2nd Period", LocalTime.of(9, 50), LocalTime.of(10, 40), 2);
        p2.setId(2L);
        when(periodRepository.findByIsActiveTrueOrderByPeriodOrderAsc()).thenReturn(List.of(period1, p2));

        List<DayOfWeek> blockDays = new ArrayList<>();
        when(timetableSkeletonService.saveLibraryBlockCells(any(), any(), any(), any(), any(), any()))
            .thenAnswer(inv -> {
                DayOfWeek day = inv.getArgument(2);
                blockDays.add(day);
                @SuppressWarnings("unchecked")
                List<Period> block = (List<Period>) inv.getArgument(3);
                List<ClassSchedule> saved = new ArrayList<>();
                for (Period period : block) {
                    ClassSchedule cs = new ClassSchedule();
                    cs.setSessionType(ClassSessionType.LIBRARY);
                    cs.setDayOfWeek(day);
                    cs.setPeriod(period);
                    cs.setId(950L + idSequence.getAndIncrement());
                    saved.add(cs);
                }
                return saved;
            });

        var result = service.runGlobalAutoSchedule(10L, null);
        return new LibraryRun(blockDays, result.cohortSummaries().get(0).infoNotes());
    }

    /** Library is one 2-period session a week (2026-09-15); a week with plenty of free periods (10,
     *  at or above the default 8) earns a bonus second session, on a different day. */
    @Test
    void librarySecondSessionIsABonus_whenTheWeekHasPlentyOfFreePeriods() {
        LibraryRun run = runLibraryOnEmptyWeek();

        assertThat(run.blockDays()).hasSize(2).doesNotHaveDuplicates();
        assertThat(run.infoNotes()).noneMatch(n -> n.startsWith("Library"));
    }

    /** Below the threshold the section keeps just its one required session, and the missing bonus is
     *  not reported as a shortfall. */
    @Test
    void librarySecondSessionIsSkippedQuietly_whenFreePeriodsAreBelowTheThreshold() {
        libraryConfig("timetable.library_extra_session_min_free_periods", "11");

        LibraryRun run = runLibraryOnEmptyWeek();

        assertThat(run.blockDays()).hasSize(1);
        assertThat(run.infoNotes()).noneMatch(n -> n.startsWith("Library"));
    }

    /** Free Saturday periods count for the share of weeks Saturday really runs: every Saturday
     *  working adds 2 full periods (10 + 2 = 12, at or above 11)... */
    @Test
    void librarySecondSessionCountsAnEveryWeekSaturdayInFull() {
        libraryConfig("timetable.library_extra_session_min_free_periods", "11");
        termInstance.setWorkingSaturdayWeeks(java.util.EnumSet.allOf(WeekOfMonth.class));

        assertThat(runLibraryOnEmptyWeek().blockDays()).hasSize(2);
    }

    /** ...while a 1st-Saturday-only term's Saturday runs about 6 of 26 weeks, adding only about half
     *  a period (10 + 2 x 6/26, below 11), so it can't earn the bonus on its own. */
    @Test
    void librarySecondSessionCountsAFirstSaturdayOnlyByItsRealRuns() {
        libraryConfig("timetable.library_extra_session_min_free_periods", "11");
        termInstance.setWorkingSaturdayWeeks(Set.of(WeekOfMonth.FIRST));

        assertThat(runLibraryOnEmptyWeek().blockDays()).hasSize(1);
    }

    /** Regression for 2026-09-18 (bonus session) extended 2026-09-21 to Library's OWN required
     *  quota, not just the bonus: both used to run without ever checking {@code theoryStillOwedRuns},
     *  so a cohort with a genuine, unresolvable Theory shortfall could still have its free periods
     *  consumed by Library -- exactly the real-world "38.3h Theory unassigned, +23.3h Theory extra,
     *  475h of term-wide spare capacity" complaint this closes, and the follow-up report that Library
     *  and Sports (both non-curriculum, advisory/co-curricular filler, same as Self-Study) were still
     *  claiming slots ahead of an unmet curriculum requirement even after the bonus-only gate.
     *  Same free-period shape as {@link #librarySecondSessionIsABonus_whenTheWeekHasPlentyOfFreePeriods}
     *  (10 free periods, above the default 8-period bonus threshold) but with one Theory offering
     *  that can never be placed anywhere, so the cohort has a real shortfall Library must yield to
     *  entirely, even though the free-period threshold alone would still allow it. */
    @Test
    void libraryIsWithheldEntirely_whenTheCohortStillHasAGenuineTheoryShortfall() {
        when(studentTermEnrollmentRepository.findDistinctCohortIdsByTermInstanceId(10L, EnrollmentStatus.ENROLLED))
            .thenReturn(new HashSet<>(List.of(1L)));
        cohort(1L, "Cohort 1");
        facultyWithDailyCap(500L, "XYZ", 6);
        when(courseOfferingService.getOfferingsByTermInstanceAndCohort(10L, 1L)).thenReturn(List.of(offeringDto(100L, "Offering A")));
        assignWholeCohort(100L, 1L, 500L);
        offeringEntity(100L, 10, 0, 0);
        when(timetableSkeletonService.resolveActiveSections(1L, 10L)).thenReturn(List.of());
        when(batchRepository.findByCourseOfferingId(anyLong())).thenReturn(List.of());

        SkeletonSubjectBudget theoryBudget = new SkeletonSubjectBudget(ClassSessionType.THEORY, null, null, null, null, 10, 10, 1, 0);
        SkeletonSubjectResponse theorySubject = new SkeletonSubjectResponse(100L, "Offering A", "OFFE", List.of(theoryBudget), null, null);

        Subject librarySubject = new Subject();
        librarySubject.setId(999L);
        librarySubject.setCode("SYSTEM-LIBRARY");
        when(subjectRepository.findByCode("SYSTEM-LIBRARY")).thenReturn(Optional.of(librarySubject));
        Classroom libraryRoom = new Classroom("Library Hall", null, null, 200);
        libraryRoom.setId(50L);
        when(classroomRepository.findByIsActiveTrueAndRoom_PurposeCategory_CodeOrderByNameAsc(any()))
            .thenReturn(List.of(libraryRoom));
        // lenient(): the theoryStillOwedRuns gate now returns before Library ever checks slot
        // freedom or saves a cell -- these stubs stay only to document what WOULD have been asked,
        // matching this test's pre-gate sibling that still exercises the placement path.
        lenient().when(timetableSkeletonService.isSlotFreeForCohort(eq(1L), eq(10L), any(), any())).thenReturn(true);

        Period p2 = new Period("2nd Period", LocalTime.of(9, 50), LocalTime.of(10, 40), 2);
        p2.setId(2L);
        when(periodRepository.findByIsActiveTrueOrderByPeriodOrderAsc()).thenReturn(List.of(period1, p2));

        SkeletonBuilderResponse skeleton = new SkeletonBuilderResponse(1L, "Cohort 1", "Term", List.of(theorySubject),
            List.of(), List.of(), List.of(), 25, 0L, List.of(), false, List.of());
        when(timetableSkeletonService.getCohortSkeleton(10L, 1L)).thenReturn(skeleton);

        // Theory can never be placed anywhere -- guarantees a genuine, unresolvable shortfall.
        when(timetableSkeletonService.placeCell(any(SkeletonCellPlacementRequest.class)))
            .thenThrow(new TimetableConstraintViolationException(List.of(
                new com.cms.dto.ConstraintViolation("SKELETON_CELL_COHORT_CLASH", "clash"))));

        List<DayOfWeek> blockDays = new ArrayList<>();
        lenient().when(timetableSkeletonService.saveLibraryBlockCells(any(), any(), any(), any(), any(), any()))
            .thenAnswer(inv -> {
                DayOfWeek day = inv.getArgument(2);
                blockDays.add(day);
                @SuppressWarnings("unchecked")
                List<Period> block = (List<Period>) inv.getArgument(3);
                List<ClassSchedule> saved = new ArrayList<>();
                for (Period period : block) {
                    ClassSchedule cs = new ClassSchedule();
                    cs.setSessionType(ClassSessionType.LIBRARY);
                    cs.setDayOfWeek(day);
                    cs.setPeriod(period);
                    cs.setId(950L + idSequence.getAndIncrement());
                    saved.add(cs);
                }
                return saved;
            });

        service.runGlobalAutoSchedule(10L, null);

        // Neither Library session places -- not the bonus (which the identical free-period count
        // earned in librarySecondSessionIsABonus_...), and not even the required 1/week quota --
        // because this cohort still genuinely owes Theory hours and Library, like Self-Study and
        // Sports, is advisory/co-curricular filler that must yield entirely to a real requirement.
        assertThat(blockDays).isEmpty();
    }

    /** Regression: a Library session {@code attemptBacktrack} displaces mid-run has no
     *  CourseOffering, so {@code restoreBumpedOrReportUnplaced}'s offering-based fallback search
     *  could never run for it -- before this fix, the moment its exact original slot was gone
     *  (taken by whatever displaced it), it was reported unplaced outright even when a different
     *  Monday-Friday day was still genuinely free. Exercises {@code tryRePlaceBumpedLibrarySession}
     *  directly: Monday (the bumped session's original day) is stubbed as no-longer-free, Wednesday
     *  as free, and asserts the session lands on Wednesday instead of being given up on. */
    @Test
    void tryRePlaceBumpedLibrarySessionFindsADifferentFreeDay_whenOriginalSlotIsGone() {
        Subject librarySubject = new Subject();
        librarySubject.setId(999L);
        librarySubject.setCode("SYSTEM-LIBRARY");
        when(subjectRepository.findByCode("SYSTEM-LIBRARY")).thenReturn(Optional.of(librarySubject));
        Classroom libraryRoom = new Classroom("Library Hall", null, null, 60);
        libraryRoom.setId(50L);
        when(classroomRepository.findByIsActiveTrueAndRoom_PurposeCategory_CodeOrderByNameAsc(any()))
            .thenReturn(List.of(libraryRoom));

        Period p2 = new Period("2nd Period", LocalTime.of(9, 50), LocalTime.of(10, 40), 2);
        p2.setId(2L);
        List<Period> periods = List.of(period1, p2);

        when(timetableSkeletonService.isSlotFreeForCohort(eq(1L), eq(10L), any(), any())).thenReturn(false);
        when(timetableSkeletonService.isSlotFreeForCohort(eq(1L), eq(10L), eq(DayOfWeek.WEDNESDAY), any())).thenReturn(true);

        when(timetableSkeletonService.saveLibraryBlockCells(any(), any(), eq(DayOfWeek.WEDNESDAY), any(), any(), any()))
            .thenAnswer(inv -> {
                @SuppressWarnings("unchecked")
                List<Period> block = (List<Period>) inv.getArgument(3);
                List<ClassSchedule> saved = new ArrayList<>();
                for (Period period : block) {
                    ClassSchedule cs = new ClassSchedule();
                    cs.setSessionType(ClassSessionType.LIBRARY);
                    cs.setDayOfWeek(DayOfWeek.WEDNESDAY);
                    cs.setPeriod(period);
                    cs.setId(900L + idSequence.getAndIncrement());
                    saved.add(cs);
                }
                return saved;
            });

        java.util.Map<DayOfWeek, Integer> dayLoad = new java.util.HashMap<>();
        for (DayOfWeek day : List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)) {
            dayLoad.put(day, 0);
        }

        TimetableGlobalAutoScheduleService.Placement bumped = new TimetableGlobalAutoScheduleService.Placement(
            700L, null, ClassSessionType.LIBRARY, null, null, null, "Library", "Whole cohort",
            DayOfWeek.MONDAY, List.of(1L, 2L));

        Optional<TimetableGlobalAutoScheduleService.Placement> result =
            service.tryRePlaceBumpedLibrarySession(bumped, 1L, termInstance, periods, dayLoad, new ArrayList<>());

        assertThat(result).isPresent();
        assertThat(result.get().dayOfWeek()).isEqualTo(DayOfWeek.WEDNESDAY);
    }

    /** Regression for the real incident this session found: a bumped idle-batch Library filler
     *  session (courseOfferingId null, exactly {@link #saveIdleBatchLibraryCell}'s shape) used to
     *  get actively relocated to any other free Monday-Friday slot by the method above -- with no
     *  way to know whether a not-yet-processed lower-priority row like Self-Study might need that
     *  exact slot, since Self-Study is deliberately placed LAST in Phase 2's {@code
     *  SHORTFALL_ROW_ORDER} and its own shortfall isn't known yet when an earlier mandatory row's
     *  backtrack bumps an idle-batch Library filler mid-Phase-2. A real run showed this relocation
     *  claiming a whole-section Thursday Library block ahead of a cohort's still-unplaced Self-
     *  Study/Co-curricular V, invisible to every later gate ({@code fillLibraryGaps}/{@code
     *  fillSportsGaps}) because it ran mid-Phase-2, long before either of those Phase 4 gates even
     *  starts. Asserts {@code restoreBumpedOrReportUnplaced} now simply reports the bumped session
     *  unplaced -- never relocated, never touching the Library classroom/slot-freedom lookups at
     *  all. */
    @Test
    void restoreBumpedOrReportUnplacedNeverRelocatesABumpedLibrarySession() {
        List<TimetableGlobalAutoScheduleService.Placement> placedThisCohortRun = new ArrayList<>();
        List<AutoPlaceUnplacedItem> unplacedForCohort = new ArrayList<>();
        java.util.Map<DayOfWeek, Integer> dayLoad = new java.util.HashMap<>();
        java.util.Map<String, Integer> theoryStillOwedRuns = new java.util.HashMap<>();

        TimetableGlobalAutoScheduleService.Placement bumped = new TimetableGlobalAutoScheduleService.Placement(
            700L, null, ClassSessionType.LIBRARY, null, null, null, "Library", "Whole cohort",
            DayOfWeek.THURSDAY, List.of(1L, 2L));

        service.restoreBumpedOrReportUnplaced(bumped, 1L, placedThisCohortRun, unplacedForCohort, dayLoad,
            termInstance, List.of(period1),
            new TimetableGlobalAutoScheduleService.TermDemandAggregation(100, 20, java.util.Map.of(), java.util.Map.of(), 0),
            new ArrayList<>(), theoryStillOwedRuns);

        assertThat(placedThisCohortRun).isEmpty();
        assertThat(unplacedForCohort).hasSize(1);
        AutoPlaceUnplacedItem item = unplacedForCohort.get(0);
        assertThat(item.subjectName()).isEqualTo("Library");
        assertThat(item.reason()).contains("displaced during a backtrack attempt");
        assertThat(item.advisoryOnly()).isTrue();
        verify(timetableSkeletonService, never()).saveLibraryBlockCells(any(), any(), any(), any(), any(), any());
        verify(timetableSkeletonService, never()).isSlotFreeForCohort(anyLong(), anyLong(), any(), any());
        verify(subjectRepository, never()).findByCode(anyString());
    }

    /** Regression for the real-world "Self-Study/Co-curricular V shows both 'displaced during a
     *  backtrack attempt' AND 'budget already fully placed' in the same run" incident: the idle-batch
     *  fallback (Phase 1.5) and a section's own regular Self-Study THEORY row (Phase 2) share the
     *  exact same {@code checkBudgetNotExceeded} (offering, THEORY, section) budget bucket. Before
     *  this fix, {@code resolveSelfStudyRowForFallback} picked whichever Self-Study subject matched
     *  first, with no regard for whether that subject's OWN curriculum requirement (per the pre-run
     *  skeleton snapshot) was already satisfied -- so an idle batch could burn the section's last
     *  required session as "filler," and Phase 2's later attempt to place that same real requirement
     *  would then hard-fail as "budget already met," even though nothing had genuinely delivered it
     *  yet (and worse, if the filler cell itself later got bumped by attemptBacktrack and couldn't
     *  relocate, the section ended up with FEWER delivered Self-Study sessions than required, with
     *  nothing left to retry it). Asserts the guard now skips a subject whose section-scoped budget
     *  still shows placedSessionsPerWeek &lt; requiredSessionsPerWeek. */
    @Test
    void resolveSelfStudyRowForFallbackSkipsASubjectWhoseSectionOwnRequirementIsNotYetMet() {
        Subject subject = new Subject();
        subject.setId(400L);
        subject.setName("Self-Study/Co-curricular V");
        CourseOffering offering = new CourseOffering();
        offering.setId(400L);
        offering.setSubject(subject);
        when(courseOfferingRepository.findById(400L)).thenReturn(Optional.of(offering));
        lenient().when(timetableSkeletonService.isElectiveOffering(offering)).thenReturn(false);

        CohortSection section = new CohortSection();
        section.setId(51L);

        // 3 required, only 2 placed so far -- a genuine unmet requirement this run's own Phase 2 is
        // still responsible for closing.
        SkeletonSubjectBudget unmetBudget = new SkeletonSubjectBudget(ClassSessionType.THEORY, null, null, 51L,
            "Section 1", 40, 20, 3, 2);
        SkeletonSubjectResponse subjectResponse = new SkeletonSubjectResponse(400L, "Self-Study/Co-curricular V",
            "SSCC-V", List.of(unmetBudget), null, null);
        SkeletonBuilderResponse skeleton = new SkeletonBuilderResponse(1L, "Cohort 1", "Term", List.of(subjectResponse),
            List.of(), List.of(), List.of(), 20, 0L, List.of(), false, List.of());

        TimetableGlobalAutoScheduleService.SelfStudyRow result = service.resolveSelfStudyRowForFallback(1L, section, skeleton,
            new TimetableGlobalAutoScheduleService.TermDemandAggregation(100, 20, java.util.Map.of(), java.util.Map.of(), 0));

        assertThat(result).isNull();
        verifyNoInteractions(courseOfferingSectionFacultyRepository);
    }

    /** The guard above is precise, not a blanket "never use Self-Study filler" -- when a cohort
     *  configures more than one Self-Study/Co-curricular subject, one whose own section-scoped
     *  requirement is already fully met (genuinely surplus capacity) is still picked normally. */
    @Test
    void resolveSelfStudyRowForFallbackUsesADifferentSubject_whoseOwnRequirementIsAlreadyMet() {
        Subject unmetSubject = new Subject();
        unmetSubject.setId(400L);
        unmetSubject.setName("Self-Study/Co-curricular V");
        CourseOffering unmetOffering = new CourseOffering();
        unmetOffering.setId(400L);
        unmetOffering.setSubject(unmetSubject);
        when(courseOfferingRepository.findById(400L)).thenReturn(Optional.of(unmetOffering));
        lenient().when(timetableSkeletonService.isElectiveOffering(unmetOffering)).thenReturn(false);

        Subject metSubject = new Subject();
        metSubject.setId(401L);
        metSubject.setName("Self-Study/Co-curricular VI");
        CourseOffering metOffering = new CourseOffering();
        metOffering.setId(401L);
        metOffering.setSubject(metSubject);
        when(courseOfferingRepository.findById(401L)).thenReturn(Optional.of(metOffering));
        lenient().when(timetableSkeletonService.isElectiveOffering(metOffering)).thenReturn(false);

        CohortSection section = new CohortSection();
        section.setId(51L);

        SkeletonSubjectBudget unmetBudget = new SkeletonSubjectBudget(ClassSessionType.THEORY, null, null, 51L,
            "Section 1", 40, 20, 3, 2);
        SkeletonSubjectBudget metBudget = new SkeletonSubjectBudget(ClassSessionType.THEORY, null, null, 51L,
            "Section 1", 40, 20, 2, 2);
        SkeletonSubjectResponse unmetSubjectResponse = new SkeletonSubjectResponse(400L, "Self-Study/Co-curricular V",
            "SSCC-V", List.of(unmetBudget), null, null);
        SkeletonSubjectResponse metSubjectResponse = new SkeletonSubjectResponse(401L, "Self-Study/Co-curricular VI",
            "SSCC-VI", List.of(metBudget), null, null);
        SkeletonBuilderResponse skeleton = new SkeletonBuilderResponse(1L, "Cohort 1", "Term",
            List.of(unmetSubjectResponse, metSubjectResponse), List.of(), List.of(), List.of(), 20, 0L, List.of(), false, List.of());

        TimetableGlobalAutoScheduleService.SelfStudyRow result = service.resolveSelfStudyRowForFallback(1L, section, skeleton,
            new TimetableGlobalAutoScheduleService.TermDemandAggregation(100, 20, java.util.Map.of(), java.util.Map.of(), 0));

        assertThat(result).isNotNull();
        assertThat(result.subjectName()).isEqualTo("Self-Study/Co-curricular VI");
    }

    /** fillSelfStudyGaps walks EVERY day/period in the week, so most placeCell refusals just mean
     *  "that slot already has a class" or "this cohort is on clinical duty" — normal, and reporting
     *  them as "N period(s) left empty" is flatly wrong (it produced a wall of false alarms against a
     *  grid that was in fact 100% full). Only a refusal leaving a genuinely usable slot unused counts.
     *  This asserts the reported side: a budget-cap refusal IS surfaced. The suppressed side is
     *  covered by every other run test in this suite — the setUp default makes this same overload
     *  throw SKELETON_CELL_ALREADY_PLACED, and those tests assert the unplaced list contains no
     *  gap-fill line at all. */
    @Test
    void fillSelfStudyGapsReportsOnlyGenuinelyFreePeriods_notSlotsAlreadyTaken() {
        facultyWithDailyCap(600L, "Coordinator", 6);
        Subject subject = new Subject();
        subject.setId(400L);
        subject.setName("Offering A");
        CourseOffering offering = new CourseOffering();
        offering.setId(400L);
        offering.setSubject(subject);
        when(courseOfferingRepository.findById(400L)).thenReturn(Optional.of(offering));
        lenient().when(timetableSkeletonService.isElectiveOffering(offering)).thenReturn(false);
        assignWholeCohort(400L, 1L, 600L);

        SkeletonSubjectBudget budget = new SkeletonSubjectBudget(ClassSessionType.THEORY, null, null, null, null, 40, 20, 5, 5);
        SkeletonSubjectResponse subjectResponse = new SkeletonSubjectResponse(400L, "Offering A", "OFFA", List.of(budget), null, null);
        SkeletonBuilderResponse skeleton = new SkeletonBuilderResponse(1L, "Cohort 1", "Term", List.of(subjectResponse),
            List.of(), List.of(), List.of(), 20, 0L, List.of(), false, List.of());

        // Budget cap = the one refusal that genuinely leaves a usable period unused.
        when(timetableSkeletonService.placeCell(any(SkeletonCellPlacementRequest.class), eq(false)))
            .thenThrow(new TimetableConstraintViolationException(List.of(
                new com.cms.dto.ConstraintViolation("SKELETON_CELL_BUDGET_EXCEEDED", "over budget"))));

        var dayLoad = new java.util.HashMap<DayOfWeek, Integer>();
        for (DayOfWeek day : DayOfWeek.values()) {
            dayLoad.put(day, 0);
        }
        var unplaced = new ArrayList<AutoPlaceUnplacedItem>();

        service.fillSelfStudyGaps(1L, skeleton, termInstance, List.of(period1), dayLoad, unplaced,
            new TimetableGlobalAutoScheduleService.TermDemandAggregation(100, 20, java.util.Map.of(), java.util.Map.of(), 0), false, java.util.Map.of());

        // Mon-Fri x 1 period, every one refused on budget -> one aggregated line naming that reason.
        assertThat(unplaced).extracting(AutoPlaceUnplacedItem::reason)
            .containsExactly("5 period(s) left empty — this subject's curriculum-hours budget for this session type is already fully placed");
    }

    /** A chosen working Saturday is a regular day (user's call, 2026-09-15), so a leftover empty
     *  Saturday period is exactly as unacceptable as a leftover empty weekday one -- fillSelfStudyGaps
     *  must cover it too. Uses the extra-hours-filler path (no genuine Self-Study offering
     *  configured); the same {@code weekdays} list drives a real Self-Study row identically. */
    @Test
    void fillSelfStudyGapsCoversSaturday_onceTermHasOptedIntoWorkingSaturdays() {
        termInstance.setWorkingSaturdayWeeks(Set.of(WeekOfMonth.FIRST));

        facultyWithDailyCap(600L, "Coordinator", 6);
        Subject subject = new Subject();
        subject.setId(400L);
        subject.setName("Offering A");
        CourseOffering offering = new CourseOffering();
        offering.setId(400L);
        offering.setSubject(subject);
        when(courseOfferingRepository.findById(400L)).thenReturn(Optional.of(offering));
        lenient().when(timetableSkeletonService.isElectiveOffering(offering)).thenReturn(false);
        assignWholeCohort(400L, 1L, 600L);

        SkeletonSubjectBudget budget = new SkeletonSubjectBudget(ClassSessionType.THEORY, null, null, null, null, 40, 20, 5, 5);
        SkeletonSubjectResponse subjectResponse = new SkeletonSubjectResponse(400L, "Offering A", "OFFA", List.of(budget), null, null);
        SkeletonBuilderResponse skeleton = new SkeletonBuilderResponse(1L, "Cohort 1", "Term", List.of(subjectResponse),
            List.of(), List.of(), List.of(), 20, 0L, List.of(), false, List.of());

        SkeletonCellResponse placedSat = new SkeletonCellResponse(950L, ClassSessionType.THEORY, DayOfWeek.SATURDAY, 1L,
            "1st Period", LocalTime.of(9, 0), LocalTime.of(9, 50), null, null, null, null, false, null, null, List.of(),
            400L, "Offering A", "OFFA", null, null, null, false, false);
        when(timetableSkeletonService.placeCell(any(SkeletonCellPlacementRequest.class), eq(false)))
            .thenReturn(placedSat);
        when(timetableStaffingService.staffCell(eq(950L), any(StaffingAssignmentRequest.class)))
            .thenReturn(new UnstaffedCellResponse(950L, 400L, "Offering A", "OFFA", null, null,
                ClassSessionType.THEORY, DayOfWeek.SATURDAY, 1L, "1st Period", LocalTime.of(9, 0), LocalTime.of(9, 50),
                null, null, null, null, null, false, List.of(), null, null));

        var dayLoad = new java.util.HashMap<DayOfWeek, Integer>();
        for (DayOfWeek day : DayOfWeek.values()) {
            dayLoad.put(day, 0);
        }

        var result = service.fillSelfStudyGaps(1L, skeleton, termInstance, List.of(period1), dayLoad, new ArrayList<>(),
            new TimetableGlobalAutoScheduleService.TermDemandAggregation(100, 20, java.util.Map.of(), java.util.Map.of(), 0), true, java.util.Map.of());

        assertThat(result.filled()).extracting(TimetableGlobalAutoScheduleService.Placement::dayOfWeek)
            .contains(DayOfWeek.SATURDAY);

        // Saturday not a working day (the caller passes saturdayIsWorkingDay(term) == false when no
        // pattern is chosen): filler never touches it.
        var monFriOnly = service.fillSelfStudyGaps(1L, skeleton, termInstance, List.of(period1), dayLoad, new ArrayList<>(),
            new TimetableGlobalAutoScheduleService.TermDemandAggregation(100, 20, java.util.Map.of(), java.util.Map.of(), 0), false, java.util.Map.of());
        assertThat(monFriOnly.filled()).extracting(TimetableGlobalAutoScheduleService.Placement::dayOfWeek)
            .doesNotContain(DayOfWeek.SATURDAY);
    }

    /** Regression: {@code fillLibraryGaps} runs as its own cross-cohort pass, most-constrained-cohort
     *  first (by total {@code dayLoad} already committed from Phase 1/2/3), rather than in whatever
     *  order {@code contexts} happens to iterate -- the same fairness principle Phase 1 already
     *  applies to a shared LAB/CLINICAL venue, now covering the one shared Library room too. Two
     *  cohorts, one shared Library room, one slot (Wednesday) both could theoretically use: cohort 2
     *  is heavily loaded (a Theory session already placed Monday, dayLoad sum &gt; 0) and has ONLY
     *  Wednesday free for Library; cohort 1 is empty (dayLoad sum 0) and has BOTH Wednesday and
     *  Thursday free. If cohort 1 (the naturally-first, lighter one) were processed first -- the
     *  pre-fix behavior -- it would grab Wednesday and strand cohort 2 with no fallback at all.
     *  Asserts both cohorts get their session: cohort 2 (heavier) claims Wednesday, cohort 1 falls
     *  back to Thursday. */
    @Test
    void runFillsLibraryMostConstrainedCohortFirst_soTheLighterCohortDoesntStrandTheHeavierOne() {
        when(studentTermEnrollmentRepository.findDistinctCohortIdsByTermInstanceId(10L, EnrollmentStatus.ENROLLED))
            .thenReturn(new java.util.LinkedHashSet<>(List.of(1L, 2L)));
        cohort(1L, "Cohort 1");
        cohort(2L, "Cohort 2");
        facultyWithDailyCap(600L, "Coordinator", 6);
        when(courseOfferingService.getOfferingsByTermInstanceAndCohort(10L, 1L)).thenReturn(List.of());
        when(courseOfferingService.getOfferingsByTermInstanceAndCohort(10L, 2L)).thenReturn(List.of(offeringDto(200L, "Offering B")));
        assignWholeCohort(200L, 2L, 600L);
        offeringEntity(200L, 10, 0, 0);
        when(timetableSkeletonService.resolveActiveSections(anyLong(), eq(10L))).thenReturn(List.of());
        when(batchRepository.findByCourseOfferingId(anyLong())).thenReturn(List.of());

        // Cohort 1: no offerings at all -- skeleton empty, dayLoad stays 0 all week (lighter, more
        // flexible). Cohort 2: one THEORY offering that places on Monday, giving it real dayLoad.
        when(timetableSkeletonService.getCohortSkeleton(10L, 1L))
            .thenReturn(new SkeletonBuilderResponse(1L, "Cohort 1", "Term", List.of(), List.of(), List.of(), List.of(), 25, 0L, List.of(), false, List.of()));
        SkeletonSubjectBudget budgetB = new SkeletonSubjectBudget(ClassSessionType.THEORY, null, null, null, null, 10, 10, 1, 0);
        SkeletonSubjectResponse subjectB = new SkeletonSubjectResponse(200L, "Offering B", "OFFB", List.of(budgetB), null, null);
        when(timetableSkeletonService.getCohortSkeleton(10L, 2L))
            .thenReturn(new SkeletonBuilderResponse(2L, "Cohort 2", "Term", List.of(subjectB), List.of(), List.of(), List.of(), 25, 0L, List.of(), false, List.of()));

        SkeletonCellResponse placedMon = new SkeletonCellResponse(900L, ClassSessionType.THEORY, DayOfWeek.MONDAY, 1L, "1st Period",
            LocalTime.of(9, 0), LocalTime.of(9, 50), null, null, null, null, false, null, null, List.of(),
            200L, "Offering B", "OFFB", null, null, null, false, false);
        when(timetableSkeletonService.placeCell(argThat(r -> r != null && r.courseOfferingId() != null && r.courseOfferingId().equals(200L))))
            .thenReturn(placedMon);
        when(timetableStaffingService.staffCell(eq(900L), any(StaffingAssignmentRequest.class)))
            .thenReturn(new UnstaffedCellResponse(900L, 200L, "Offering B", "OFFB", null, null,
                ClassSessionType.THEORY, DayOfWeek.MONDAY, 1L, "1st Period", LocalTime.of(9, 0), LocalTime.of(9, 50),
                null, null, null, null, null, false, List.of(), null, null));

        // Library: one classroom and a quota of 1 session/week (the default, stubbed explicitly).
        // Neither cohort has 8 free periods, so no bonus second session muddies the contested slot.
        Subject librarySubject = new Subject();
        librarySubject.setId(999L);
        librarySubject.setCode("SYSTEM-LIBRARY");
        when(subjectRepository.findByCode("SYSTEM-LIBRARY")).thenReturn(Optional.of(librarySubject));
        Classroom libraryRoom = new Classroom("Library Hall", null, null, 60);
        libraryRoom.setId(50L);
        when(classroomRepository.findByIsActiveTrueAndRoom_PurposeCategory_CodeOrderByNameAsc(any()))
            .thenReturn(List.of(libraryRoom));
        when(systemConfigurationService.findByKey("timetable.library_sessions_per_week"))
            .thenReturn(Optional.of(new SystemConfigurationResponse(null, "timetable.library_sessions_per_week", "1",
                null, null, null, null, null, null)));

        Period p2 = new Period("2nd Period", LocalTime.of(9, 50), LocalTime.of(10, 40), 2);
        p2.setId(2L);
        when(periodRepository.findByIsActiveTrueOrderByPeriodOrderAsc()).thenReturn(List.of(period1, p2));

        // Cohort 1 (light): free Wednesday and Thursday, not Monday/Tuesday/Friday.
        when(timetableSkeletonService.isSlotFreeForCohort(eq(1L), eq(10L), any(), any())).thenReturn(false);
        when(timetableSkeletonService.isSlotFreeForCohort(eq(1L), eq(10L), eq(DayOfWeek.WEDNESDAY), any())).thenReturn(true);
        when(timetableSkeletonService.isSlotFreeForCohort(eq(1L), eq(10L), eq(DayOfWeek.THURSDAY), any())).thenReturn(true);
        // Cohort 2 (heavy): free ONLY Wednesday -- Monday is its own Theory session, everything else assumed occupied too.
        when(timetableSkeletonService.isSlotFreeForCohort(eq(2L), eq(10L), any(), any())).thenReturn(false);
        when(timetableSkeletonService.isSlotFreeForCohort(eq(2L), eq(10L), eq(DayOfWeek.WEDNESDAY), any())).thenReturn(true);

        // One shared room: Wednesday becomes occupied the instant either cohort's session is saved there.
        Set<DayOfWeek> bookedLibraryDays = java.util.concurrent.ConcurrentHashMap.newKeySet();
        when(timetableStaffingService.checkRoomFree(any(), any(), any(), any(), any(), any(), any(), any()))
            .thenAnswer(inv -> {
                DayOfWeek day = inv.getArgument(5);
                return bookedLibraryDays.contains(day)
                    ? Optional.of(new com.cms.dto.ConstraintViolation("STAFFING_ROOM_CONFLICT", "taken"))
                    : Optional.empty();
            });
        when(timetableSkeletonService.saveLibraryBlockCells(any(), any(), any(), any(), any(), any()))
            .thenAnswer(inv -> {
                DayOfWeek day = inv.getArgument(2);
                bookedLibraryDays.add(day);
                @SuppressWarnings("unchecked")
                List<Period> block = (List<Period>) inv.getArgument(3);
                List<ClassSchedule> saved = new ArrayList<>();
                for (Period period : block) {
                    ClassSchedule cs = new ClassSchedule();
                    cs.setSessionType(ClassSessionType.LIBRARY);
                    cs.setDayOfWeek(day);
                    cs.setPeriod(period);
                    cs.setId(950L + idSequence.getAndIncrement());
                    saved.add(cs);
                }
                return saved;
            });

        var result = service.runGlobalAutoSchedule(10L, null);

        var summaryByCohort = result.cohortSummaries().stream()
            .collect(java.util.stream.Collectors.toMap(com.cms.dto.CohortPlacementSummary::cohortId, s -> s));
        // Cohort 2 (heavier, no fallback) must land on its only option, Wednesday -- proving it was
        // processed before cohort 1 despite being second in enrollment/insertion order. Cohort 1
        // (lighter, had Wednesday AND Thursday) must have fallen back to Thursday instead of
        // starving cohort 2 of its only slot. Neither cohort's unplaced list may contain a Library
        // item (the thing this test actually verifies) -- Self-Study/extra-hours-filler noise (e.g.
        // cohort 1 genuinely has no other Theory offering to use as filler) is out of scope here.
        assertThat(summaryByCohort.get(2L).unplaced()).extracting(AutoPlaceUnplacedItem::sessionType)
            .doesNotContain(ClassSessionType.LIBRARY);
        assertThat(summaryByCohort.get(1L).unplaced()).extracting(AutoPlaceUnplacedItem::sessionType)
            .doesNotContain(ClassSessionType.LIBRARY);
    }

    /** Every run rebuilds the whole DRAFT grid rather than adding to it — the fix for the real
     *  incident where an earlier run's single-period THEORY/LIBRARY cells sat immovably inside the
     *  only 4-period CLINICAL windows the week had (attemptBacktrack can only displace placements
     *  from the current run, so nothing could ever move them). PUBLISHED cells are never touched. */
    @Test
    void runClearsEveryExistingDraftCellBeforeRebuildingTheWeek() {
        when(studentTermEnrollmentRepository.findDistinctCohortIdsByTermInstanceId(10L, EnrollmentStatus.ENROLLED))
            .thenReturn(new HashSet<>(List.of(1L)));
        cohort(1L, "Cohort 1");
        facultyWithDailyCap(500L, "XYZ", 6);
        when(courseOfferingService.getOfferingsByTermInstanceAndCohort(10L, 1L)).thenReturn(List.of(offeringDto(100L, "Offering A")));
        assignWholeCohort(100L, 1L, 500L);
        offeringEntity(100L, 10, 0, 0);
        when(timetableSkeletonService.resolveActiveSections(1L, 10L)).thenReturn(List.of());
        when(batchRepository.findByCourseOfferingId(anyLong())).thenReturn(List.of());

        // Three DRAFT cells that are all individually within budget (1 required, 1 placed) plus one
        // PUBLISHED cell. The old narrower purge would have kept every one of them -- nothing here
        // is over budget, section-less, excess Library, or a truncated block -- which is exactly
        // how a previous run's cells came to squat in this week's clinical windows forever.
        SkeletonSubjectBudget budget = new SkeletonSubjectBudget(ClassSessionType.THEORY, null, null, null, null, 10, 10, 1, 1);
        SkeletonSubjectResponse subject = new SkeletonSubjectResponse(100L, "Offering A", "OFFE", List.of(budget), null, null);
        SkeletonCellResponse draft1 = skeletonCell(901L, com.cms.model.enums.ClassScheduleStatus.DRAFT);
        SkeletonCellResponse draft2 = skeletonCell(902L, com.cms.model.enums.ClassScheduleStatus.DRAFT);
        SkeletonCellResponse draft3 = skeletonCell(903L, com.cms.model.enums.ClassScheduleStatus.DRAFT);
        SkeletonCellResponse published = skeletonCell(904L, com.cms.model.enums.ClassScheduleStatus.PUBLISHED);
        SkeletonBuilderResponse skeleton = new SkeletonBuilderResponse(1L, "Cohort 1", "Term", List.of(subject),
            List.of(draft1, draft2, draft3, published), List.of(), List.of(), 25, 0L, List.of(), false, List.of());
        when(timetableSkeletonService.getCohortSkeleton(10L, 1L)).thenReturn(skeleton);

        ClassSchedule cleared1 = new ClassSchedule();
        cleared1.setId(901L);
        cleared1.setIsActive(true);
        ClassSchedule cleared2 = new ClassSchedule();
        cleared2.setId(902L);
        cleared2.setIsActive(true);
        ClassSchedule cleared3 = new ClassSchedule();
        cleared3.setId(903L);
        cleared3.setIsActive(true);
        ArgumentCaptor<Iterable<Long>> idsCaptor = ArgumentCaptor.captor();
        when(classScheduleRepository.findAllById(idsCaptor.capture())).thenReturn(List.of(cleared1, cleared2, cleared3));

        var result = service.runGlobalAutoSchedule(10L, null);

        assertThat(idsCaptor.getValue()).containsExactlyInAnyOrder(901L, 902L, 903L);
        assertThat(result.staleDraftsCleared()).isEqualTo(3);
        verify(classScheduleRepository).deleteAllInBatch(List.of(cleared1, cleared2, cleared3));
        assertThat(result.totalPlaced()).isEqualTo(0);
        assertThat(result.cohortSummaries().get(0).unplaced()).extracting(AutoPlaceUnplacedItem::reason)
            .containsExactly(NO_LIBRARY_CLASSROOM_REASON);
    }

    /** Minimal {@link SkeletonCellResponse} for Offering A / whole-cohort THEORY -- only {@code id}
     *  and {@code status} vary across the purge test's fixture cells. */
    /** Two members of one elective group (ids 300/301, group 77), both Theory-bearing and both
     *  unplaced, with the skeleton flagging the group so the run takes its elective branch. */
    private void twoMemberElectiveGroup() {
        when(studentTermEnrollmentRepository.findDistinctCohortIdsByTermInstanceId(10L, EnrollmentStatus.ENROLLED))
            .thenReturn(new HashSet<>(List.of(1L)));
        cohort(1L, "Cohort 1");
        when(courseOfferingService.getOfferingsByTermInstanceAndCohort(10L, 1L))
            .thenReturn(List.of(offeringDto(300L, "Elective One"), offeringDto(301L, "Elective Two")));
        when(timetableSkeletonService.resolveActiveSections(1L, 10L)).thenReturn(List.of());
        lenient().when(batchRepository.findByCourseOfferingId(anyLong())).thenReturn(List.of());

        List<CourseOffering> members = new java.util.ArrayList<>();
        for (long id : new long[] {300L, 301L}) {
            Subject subject = new Subject();
            subject.setId(id);
            subject.setName("Elective " + id);
            CurriculumSemesterCourse csc = new CurriculumSemesterCourse();
            csc.setTheoryHours(10);
            csc.setIsElective(true);
            CourseOffering member = new CourseOffering();
            member.setId(id);
            member.setSubject(subject);
            member.setCurriculumSemesterCourse(csc);
            member.setTermInstance(termInstance);
            member.setIsActive(true);
            lenient().when(courseOfferingRepository.findById(id)).thenReturn(Optional.of(member));
            lenient().when(timetableSkeletonService.isElectiveOffering(member)).thenReturn(true);
            lenient().when(courseOfferingSectionFacultyService.getForOffering(id)).thenReturn(
                new CourseOfferingSectionFacultyResponse(true, null, List.of(
                    new SectionFacultyAssignment(1L, null, "Cohort 1", null, 500L + id, "Staff", 0L))));
            lenient().when(courseRegistrationRepository.countByCourseOfferingIdAndStatus(eq(id), any())).thenReturn(20L);
            members.add(member);
        }
        when(courseOfferingRepository.findByTermInstanceIdAndCurriculumSemesterCourse_ElectiveGroupId(10L, 77L))
            .thenReturn(members);
        lenient().when(classScheduleRepository.findByTermInstanceIdAndCourseOfferingIdIn(eq(10L), any()))
            .thenReturn(List.of());

        SkeletonSubjectResponse s1 = new SkeletonSubjectResponse(300L, "Elective One", "EL01", List.of(), 77L, "Group A");
        SkeletonSubjectResponse s2 = new SkeletonSubjectResponse(301L, "Elective Two", "EL02", List.of(), 77L, "Group A");
        when(timetableSkeletonService.getCohortSkeleton(10L, 1L)).thenReturn(
            new SkeletonBuilderResponse(1L, "Cohort 1", "Term", List.of(s1, s2), List.of(), List.of(), List.of(),
                25, 0L, List.of(), false, List.of()));
    }

    private Classroom classroom(Long id, String name) {
        Classroom room = new Classroom(name, null, null, 60);
        room.setId(id);
        return room;
    }

    /** Regression for the elective room bug: the pass resolved ONE free classroom per candidate
     *  slot and handed the same one to every member of the group. Every option in a group runs
     *  simultaneously at the group's single shared slot, but they are different subjects taught by
     *  different faculty and cannot share a room — local dev had all 9 ELEC-II options booked into
     *  Library Hall at Monday Period 4 at once. Each member must now get its own room. */
    @Test
    void runGivesEveryElectiveGroupMemberItsOwnRoomRatherThanSharingOne() {
        twoMemberElectiveGroup();
        when(classroomRepository.findByIsActiveTrueOrderByNameAsc())
            .thenReturn(List.of(classroom(1L, "Room A"), classroom(2L, "Room B")));
        when(timetableStaffingService.checkRoomFree(eq(ClassSessionType.THEORY), anyLong(), any(), eq(10L),
            isNull(), any(), any(), any())).thenReturn(Optional.empty());
        when(timetableSkeletonService.placeCell(any(SkeletonCellPlacementRequest.class))).thenAnswer(inv -> {
            SkeletonCellPlacementRequest req = inv.getArgument(0);
            return skeletonCell(9000L + req.courseOfferingId(), com.cms.model.enums.ClassScheduleStatus.DRAFT);
        });

        service.runGlobalAutoSchedule(10L, null);

        ArgumentCaptor<StaffingAssignmentRequest> staffed = ArgumentCaptor.forClass(StaffingAssignmentRequest.class);
        verify(timetableStaffingService, times(2)).staffCell(anyLong(), staffed.capture());
        assertThat(staffed.getAllValues()).extracting(StaffingAssignmentRequest::classroomId)
            .containsExactlyInAnyOrder(1L, 2L);
    }

    /** The institution simply not owning enough rooms to run every option at one shared slot is a
     *  structural limit no rescheduling can fix, so the report has to name it — a bare "no slot
     *  found" sends the admin hunting for staffing capacity that was never the constraint. */
    @Test
    void runReportsTheRoomShortfallByNameWhenAGroupHasMoreOptionsThanFreeRooms() {
        twoMemberElectiveGroup();
        when(classroomRepository.findByIsActiveTrueOrderByNameAsc()).thenReturn(List.of(classroom(1L, "Room A")));
        when(timetableStaffingService.checkRoomFree(eq(ClassSessionType.THEORY), anyLong(), any(), eq(10L),
            isNull(), any(), any(), any())).thenReturn(Optional.empty());

        var result = service.runGlobalAutoSchedule(10L, null);

        assertThat(result.electiveUnplaced()).isNotEmpty();
        assertThat(result.electiveUnplaced().get(0).reason())
            .contains("2 options at one shared slot")
            .contains("the best any day/period offered was 1");
        verify(timetableStaffingService, never()).staffCell(anyLong(), any());
    }

    /** Stage B residual closer. A shift-configured subject whose duty roster already delivers
     *  nearly all its Clinical hours is left with a residual the weekly grid can only OVERSHOOT:
     *  480h at a 6h shift needs 80 occurrences, three duty days over 26 weeks give 78, so 12h are
     *  owed — and the smallest weekly grid row for it delivers ~86.7h over the term. Placing that
     *  row buys ~75h nobody asked for and occupies a slot another subject needs; failing to place
     *  it reports as an ordinary "no slot found" and sends the admin hunting for capacity that
     *  would not help. The run must decline the row and name the real remedy: extra duty days. */
    @Test
    void runReportsAClinicalResidualAsDutyDaysInsteadOfOvershootingItOnTheWeeklyGrid() {
        when(studentTermEnrollmentRepository.findDistinctCohortIdsByTermInstanceId(10L, EnrollmentStatus.ENROLLED))
            .thenReturn(new HashSet<>(List.of(1L)));
        Cohort theCohort = cohort(1L, "Cohort 1");
        theCohort.setDisplayName("BSc Nursing (2025-2029)");
        when(courseOfferingService.getOfferingsByTermInstanceAndCohort(10L, 1L)).thenReturn(List.of(offeringDto(100L, "Adult Health Nursing I")));
        when(timetableSkeletonService.resolveActiveSections(1L, 10L)).thenReturn(List.of());
        lenient().when(batchRepository.findByCourseOfferingId(anyLong())).thenReturn(List.of());

        // 174h raw; one 6h duty group over this fixture's 27-week term credits 162h, leaving 12h.
        CourseOffering offering = offeringEntity(100L, 0, 0, 174);
        offering.setClinicalShiftDurationMinutes(360);
        Subject clinicalSubject = new Subject();
        clinicalSubject.setId(100L);
        clinicalSubject.setClinicalSessionBlockPeriods(4);
        offering.setSubject(clinicalSubject);
        when(clinicalShiftGroupService.getGroupsForOffering(100L)).thenReturn(List.of(
            new com.cms.dto.ClinicalShiftGroupDto(7L, 100L, "Adult Health Nursing I", null, null, 10L, "Shift A",
                DayOfWeek.MONDAY, LocalTime.of(7, 0), LocalTime.of(13, 0), LocalTime.of(6, 0), LocalTime.of(14, 0),
                null, null, true, List.of(), List.of(), null, null)));

        // The real shape TimetableSkeletonService produces (OC-227): totalHours is the RAW curriculum
        // figure, and `required` is already 0 because one weekly 4-period clinical row (4 x 50min x
        // 26 weeks = 86.7h) would overshoot the 12h still owed. Two clinical batches -> two budget
        // rows for the same offering, which must still yield ONE residual item. Before OC-227 this
        // fixture used totalHours=12/required=1 -- a shape real data never has -- which is how the
        // closer's unreachability for every real subject went unnoticed.
        SkeletonSubjectBudget batchA = new SkeletonSubjectBudget(ClassSessionType.CLINICAL, 285L, "Clinical A", null, null, 174, 26, 0, 0);
        SkeletonSubjectBudget batchB = new SkeletonSubjectBudget(ClassSessionType.CLINICAL, 286L, "Clinical B", null, null, 174, 26, 0, 0);
        SkeletonSubjectResponse subject = new SkeletonSubjectResponse(100L, "Adult Health Nursing I", "AHN1", List.of(batchA, batchB), null, null);
        when(timetableSkeletonService.getCohortSkeleton(10L, 1L)).thenReturn(
            new SkeletonBuilderResponse(1L, "Cohort 1", "Term", List.of(subject), List.of(), List.of(), List.of(),
                26, 0L, List.of(), false, List.of()));

        var result = service.runGlobalAutoSchedule(10L, null);

        assertThat(result.clinicalResiduals()).hasSize(1);
        var residual = result.clinicalResiduals().get(0);
        assertThat(residual.courseOfferingId()).isEqualTo(100L);
        assertThat(residual.residualHours()).isEqualTo(12.0);
        assertThat(residual.hoursPerDutyDay()).isEqualTo(6.0);
        assertThat(residual.extraDutyDays()).isEqualTo(2);
        assertThat(residual.remedy()).contains("Clinical Shift group bounded to 2 week(s)");
        // The other remedy: 174h x 60 / 27 weeks = 386.7 -> 390 minutes closes it with the existing roster.
        assertThat(residual.currentDurationMinutes()).isEqualTo(360);
        assertThat(residual.suggestedDurationMinutes()).isEqualTo(390);
        // The whole point: it is NOT handed to the grid, so no ~75h of overshoot gets placed.
        verify(timetableSkeletonService, never()).placeCell(argThat(
            (SkeletonCellPlacementRequest r) -> r != null && r.sessionType() == ClassSessionType.CLINICAL));
    }

    /** The guard has to stay narrow: a subject genuinely short by a full weekly session's worth is
     *  ordinary work for the grid and must still go to it, residual machinery or not. */
    @Test
    void runStillPlacesAClinicalRowOnTheGridWhenOneWeeklySessionDoesNotOvershoot() {
        when(studentTermEnrollmentRepository.findDistinctCohortIdsByTermInstanceId(10L, EnrollmentStatus.ENROLLED))
            .thenReturn(new HashSet<>(List.of(1L)));
        cohort(1L, "Cohort 1");
        when(courseOfferingService.getOfferingsByTermInstanceAndCohort(10L, 1L)).thenReturn(List.of(offeringDto(100L, "Adult Health Nursing I")));
        when(timetableSkeletonService.resolveActiveSections(1L, 10L)).thenReturn(List.of());
        lenient().when(batchRepository.findByCourseOfferingId(anyLong())).thenReturn(List.of());

        // 362h raw - 162h credited by one 6h group over 27 weeks = 200h genuinely still owed.
        CourseOffering offering = offeringEntity(100L, 0, 0, 362);
        offering.setClinicalShiftDurationMinutes(360);
        Subject clinicalSubject = new Subject();
        clinicalSubject.setId(100L);
        clinicalSubject.setClinicalSessionBlockPeriods(4);
        offering.setSubject(clinicalSubject);
        lenient().when(clinicalShiftGroupService.getGroupsForOffering(100L)).thenReturn(List.of(
            new com.cms.dto.ClinicalShiftGroupDto(7L, 100L, "Adult Health Nursing I", null, null, 10L, "Shift A",
                DayOfWeek.MONDAY, LocalTime.of(7, 0), LocalTime.of(13, 0), LocalTime.of(6, 0), LocalTime.of(14, 0),
                null, null, true, List.of(), List.of(), null, null)));

        // 200h owed -- far more than one weekly row's 86.7h, so the grid is the right tool.
        SkeletonSubjectBudget budget = new SkeletonSubjectBudget(ClassSessionType.CLINICAL, null, null, null, null, 200, 26, 3, 0);
        SkeletonSubjectResponse subject = new SkeletonSubjectResponse(100L, "Adult Health Nursing I", "AHN1", List.of(budget), null, null);
        when(timetableSkeletonService.getCohortSkeleton(10L, 1L)).thenReturn(
            new SkeletonBuilderResponse(1L, "Cohort 1", "Term", List.of(subject), List.of(), List.of(), List.of(),
                26, 0L, List.of(), false, List.of()));

        var result = service.runGlobalAutoSchedule(10L, null);

        assertThat(result.clinicalResiduals()).isEmpty();
    }

    private SkeletonCellResponse skeletonCell(Long id, com.cms.model.enums.ClassScheduleStatus status) {
        return skeletonCell(id, status, false);
    }

    private SkeletonCellResponse skeletonCell(Long id, com.cms.model.enums.ClassScheduleStatus status, boolean pinned) {
        return new SkeletonCellResponse(id, ClassSessionType.THEORY, DayOfWeek.MONDAY, 1L, "1st Period",
            LocalTime.of(9, 0), LocalTime.of(9, 50), null, null, null, null, false, status, null, List.of(),
            100L, "Offering A", "OFFE", null, null, null, pinned, false);
    }

    /** Pinning regression. Every run rebuilds the DRAFT grid, and until now that cleared it
     *  unconditionally — so an admin's manual drag-move or swap was destroyed by the next run,
     *  which is incompatible with the draft-review model where the generated grid is a starting
     *  point a human reshapes before approving. A pinned DRAFT cell must now survive: it is neither
     *  deactivated nor even offered to {@code findAllById}, and the run reports how many it kept so
     *  preserving cells is never silent. The unpinned drafts alongside it must still be cleared,
     *  and a PUBLISHED cell is untouched as before. */
    @Test
    void runPreservesPinnedDraftCells_whileStillClearingUnpinnedOnes() {
        when(studentTermEnrollmentRepository.findDistinctCohortIdsByTermInstanceId(10L, EnrollmentStatus.ENROLLED))
            .thenReturn(new HashSet<>(List.of(1L)));
        cohort(1L, "Cohort 1");
        facultyWithDailyCap(500L, "XYZ", 6);
        when(courseOfferingService.getOfferingsByTermInstanceAndCohort(10L, 1L)).thenReturn(List.of(offeringDto(100L, "Offering A")));
        assignWholeCohort(100L, 1L, 500L);
        offeringEntity(100L, 10, 0, 0);
        when(timetableSkeletonService.resolveActiveSections(1L, 10L)).thenReturn(List.of());
        when(batchRepository.findByCourseOfferingId(anyLong())).thenReturn(List.of());

        SkeletonSubjectBudget budget = new SkeletonSubjectBudget(ClassSessionType.THEORY, null, null, null, null, 10, 10, 1, 1);
        SkeletonSubjectResponse subject = new SkeletonSubjectResponse(100L, "Offering A", "OFFE", List.of(budget), null, null);
        SkeletonCellResponse unpinned1 = skeletonCell(901L, com.cms.model.enums.ClassScheduleStatus.DRAFT, false);
        SkeletonCellResponse pinnedByAdmin = skeletonCell(902L, com.cms.model.enums.ClassScheduleStatus.DRAFT, true);
        SkeletonCellResponse unpinned2 = skeletonCell(903L, com.cms.model.enums.ClassScheduleStatus.DRAFT, false);
        SkeletonCellResponse published = skeletonCell(904L, com.cms.model.enums.ClassScheduleStatus.PUBLISHED);
        SkeletonBuilderResponse skeleton = new SkeletonBuilderResponse(1L, "Cohort 1", "Term", List.of(subject),
            List.of(unpinned1, pinnedByAdmin, unpinned2, published), List.of(), List.of(), 25, 0L, List.of(), false, List.of());
        when(timetableSkeletonService.getCohortSkeleton(10L, 1L)).thenReturn(skeleton);

        ClassSchedule cleared1 = new ClassSchedule();
        cleared1.setId(901L);
        cleared1.setIsActive(true);
        ClassSchedule cleared2 = new ClassSchedule();
        cleared2.setId(903L);
        cleared2.setIsActive(true);
        ArgumentCaptor<Iterable<Long>> idsCaptor = ArgumentCaptor.captor();
        when(classScheduleRepository.findAllById(idsCaptor.capture())).thenReturn(List.of(cleared1, cleared2));

        var result = service.runGlobalAutoSchedule(10L, null);

        // The pinned cell is never even a candidate for deactivation.
        assertThat(idsCaptor.getValue()).containsExactlyInAnyOrder(901L, 903L);
        assertThat(result.staleDraftsCleared()).isEqualTo(2);
        assertThat(result.pinnedCellsPreserved()).isEqualTo(1);
        verify(classScheduleRepository).deleteAllInBatch(List.of(cleared1, cleared2));
    }

    /** The occurrence/rotation cleanup a hard-delete needs (unswap an external Phase 7 swap
     *  partner, purge session_occurrences, purge rotation rows) now lives in {@link
     *  ClassScheduleCleanupService} -- see {@code ClassScheduleCleanupServiceTest} for that logic
     *  itself. This only proves the rebuild still delegates to it, in the right order, for the
     *  right ids, before deleting the class_schedules rows. */
    @Test
    void runDelegatesOccurrenceAndRotationCleanupToTheSharedServiceBeforeHardDeletingDraftCells() {
        when(studentTermEnrollmentRepository.findDistinctCohortIdsByTermInstanceId(10L, EnrollmentStatus.ENROLLED))
            .thenReturn(new HashSet<>(List.of(1L)));
        cohort(1L, "Cohort 1");
        facultyWithDailyCap(500L, "XYZ", 6);
        when(courseOfferingService.getOfferingsByTermInstanceAndCohort(10L, 1L)).thenReturn(List.of(offeringDto(100L, "Offering A")));
        assignWholeCohort(100L, 1L, 500L);
        offeringEntity(100L, 10, 0, 0);
        when(timetableSkeletonService.resolveActiveSections(1L, 10L)).thenReturn(List.of());
        when(batchRepository.findByCourseOfferingId(anyLong())).thenReturn(List.of());

        SkeletonSubjectBudget budget = new SkeletonSubjectBudget(ClassSessionType.THEORY, null, null, null, null, 10, 10, 1, 1);
        SkeletonSubjectResponse subject = new SkeletonSubjectResponse(100L, "Offering A", "OFFE", List.of(budget), null, null);
        SkeletonCellResponse draft1 = skeletonCell(901L, com.cms.model.enums.ClassScheduleStatus.DRAFT);
        SkeletonBuilderResponse skeleton = new SkeletonBuilderResponse(1L, "Cohort 1", "Term", List.of(subject),
            List.of(draft1), List.of(), List.of(), 25, 0L, List.of(), false, List.of());
        when(timetableSkeletonService.getCohortSkeleton(10L, 1L)).thenReturn(skeleton);

        ClassSchedule cleared1 = new ClassSchedule();
        cleared1.setId(901L);
        cleared1.setIsActive(true);
        when(classScheduleRepository.findAllById(any())).thenReturn(List.of(cleared1));

        service.runGlobalAutoSchedule(10L, null);

        var inOrder = inOrder(classScheduleCleanupService, classScheduleRepository);
        inOrder.verify(classScheduleCleanupService).purgeOccurrencesForCells(java.util.Set.of(901L));
        inOrder.verify(classScheduleCleanupService).purgeRotationRowsForCells(java.util.Set.of(901L));
        inOrder.verify(classScheduleRepository).deleteAllInBatch(List.of(cleared1));
    }

    @Test
    void runPlacesA4PeriodClinicalBlockAcrossARecessButOnlyOnTheHalfDayThatAvoidsLunch() {
        // Real forenoon/afternoon layout: P1-P4 form the forenoon (a 15-min recess sits between
        // P2/P3, no gap otherwise), P5 starts the afternoon after a 45-min LUNCH gap after P4 --
        // the day's single longest gap. A 4-period CLINICAL block (subject.clinicalSessionBlockPeriods)
        // must be allowed to land on P1-P4 (crosses only the recess) per the college's real rule
        // that a half-day clinical posting runs straight through a short recess -- unlike THEORY/LAB,
        // which stay strictly zero-gap (see TimetableSkeletonServiceTest's own coverage of that).
        when(studentTermEnrollmentRepository.findDistinctCohortIdsByTermInstanceId(10L, EnrollmentStatus.ENROLLED))
            .thenReturn(new HashSet<>(List.of(1L)));
        cohort(1L, "Cohort 1");
        facultyWithDailyCap(500L, "XYZ", 8);
        when(courseOfferingService.getOfferingsByTermInstanceAndCohort(10L, 1L)).thenReturn(List.of(offeringDto(100L, "Offering A")));
        assignWholeCohort(100L, 1L, 500L);
        CourseOffering offering = offeringEntity(100L, 0, 0, 40);
        Subject subject = new Subject();
        subject.setId(1L);
        subject.setClinicalSessionBlockPeriods(4);
        offering.setSubject(subject);
        when(timetableSkeletonService.resolveActiveSections(1L, 10L)).thenReturn(List.of());
        when(batchRepository.findByCourseOfferingId(anyLong())).thenReturn(List.of());

        Period p2 = new Period("2nd Period", LocalTime.of(9, 50), LocalTime.of(10, 40), 2);
        p2.setId(2L);
        Period p3 = new Period("3rd Period", LocalTime.of(10, 55), LocalTime.of(11, 45), 3);
        p3.setId(3L);
        Period p4 = new Period("4th Period", LocalTime.of(11, 45), LocalTime.of(12, 35), 4);
        p4.setId(4L);
        Period p5 = new Period("5th Period", LocalTime.of(13, 20), LocalTime.of(14, 10), 5);
        p5.setId(5L);
        when(periodRepository.findByIsActiveTrueOrderByPeriodOrderAsc()).thenReturn(List.of(period1, p2, p3, p4, p5));

        // requiredSessionsPerWeek is a SESSION count, not a period count -- 1 session of this
        // subject's 4-period clinical block is exactly what this test places and asserts on below.
        SkeletonSubjectBudget budget = new SkeletonSubjectBudget(ClassSessionType.CLINICAL, 55L, "Batch 1", null, null, 40, 10, 1, 0);
        SkeletonSubjectResponse subjectResponse = new SkeletonSubjectResponse(100L, "Offering A", "OFFE", List.of(budget), null, null);
        SkeletonBuilderResponse skeleton = new SkeletonBuilderResponse(1L, "Cohort 1", "Term", List.of(subjectResponse), List.of(), List.of(), List.of(), 25, 0L, List.of(), false, List.of());
        when(timetableSkeletonService.getCohortSkeleton(10L, 1L)).thenReturn(skeleton);

        SkeletonCellResponse placed = new SkeletonCellResponse(900L, ClassSessionType.CLINICAL, DayOfWeek.MONDAY, 1L, "1st Period",
            LocalTime.of(9, 0), LocalTime.of(9, 50), 55L, "Batch 1", null, null, false, null, null, List.of(),
            100L, "Offering A", "OFFE", null, null, null, false, false);
        when(timetableSkeletonService.placeCell(any(SkeletonCellPlacementRequest.class))).thenReturn(placed);
        when(timetableStaffingService.staffCell(eq(900L), any(StaffingAssignmentRequest.class)))
            .thenReturn(new UnstaffedCellResponse(900L, 100L, "Offering A", "OFFE", null, null,
                ClassSessionType.CLINICAL, DayOfWeek.MONDAY, 1L, "1st Period", LocalTime.of(9, 0), LocalTime.of(9, 50),
                null, null, null, null, null, false, List.of(), null, null));

        var result = service.runGlobalAutoSchedule(10L, null);

        assertThat(result.totalPlaced()).isEqualTo(1);
        assertThat(result.cohortSummaries().get(0).unplaced()).extracting(AutoPlaceUnplacedItem::reason)
            .containsExactly(NO_LIBRARY_CLASSROOM_REASON, NO_SELF_STUDY_OR_THEORY_OFFERING_REASON);
        verify(timetableSkeletonService).placeCell(argThat(r ->
            r.dayOfWeek() == DayOfWeek.MONDAY && r.periodId().equals(1L)
                && r.spanPeriodIds() != null && r.spanPeriodIds().equals(List.of(2L, 3L, 4L))));
    }

    /** Guards the PHASE ORDER itself (see the class's "Placement order" javadoc section), which
     *  nothing else here would catch: every other test mocks {@code placeCell} as an always-succeeds
     *  stub, so a run that placed its greedy extra-hours filler before its rigid multi-period blocks
     *  would still look perfectly healthy. This one gives the stub real occupancy state, so a slot
     *  taken by one pass genuinely blocks the next -- the only way a unit test can tell the two
     *  orderings apart.
     *
     *  <p>The week here is deliberately as tight as the real one: four periods forming a single
     *  unbroken forenoon run, so a 4-period CLINICAL block has exactly ONE legal position per day
     *  (start index 0 -- any later start runs off the end), and no working Saturday, so only
     *  Monday-Friday exist. {@code fillSelfStudyGaps} backfills EVERY remaining weekday period, all
     *  20 of them. Run in the correct order, Clinical claims Monday's whole forenoon and the filler
     *  (Offering B's extra hours) takes the other 16 slots. Move the filler (or Library) ahead of
     *  Phase 1 and there is no
     *  4-period run left anywhere in the week, so the Clinical block becomes unplaceable and the
     *  assertions below fail -- which is exactly the real-world regression this ordering prevents. */
    @Test
    void runPlacesRigidMultiPeriodBlocksBeforeGreedyFiller_soFillerCanNeverStrandThem() {
        when(studentTermEnrollmentRepository.findDistinctCohortIdsByTermInstanceId(10L, EnrollmentStatus.ENROLLED))
            .thenReturn(new HashSet<>(List.of(1L)));
        cohort(1L, "Cohort 1");
        facultyWithDailyCap(500L, "XYZ", 8);
        when(courseOfferingService.getOfferingsByTermInstanceAndCohort(10L, 1L))
            .thenReturn(List.of(offeringDto(100L, "Offering A"), offeringDto(200L, "Offering B")));
        assignWholeCohort(100L, 1L, 500L);
        assignWholeCohort(200L, 1L, 500L);
        CourseOffering clinicalOffering = offeringEntity(100L, 0, 0, 40);
        Subject clinicalSubject = new Subject();
        clinicalSubject.setId(1L);
        clinicalSubject.setClinicalSessionBlockPeriods(4);
        clinicalOffering.setSubject(clinicalSubject);
        offeringEntity(200L, 10, 0, 0);
        when(timetableSkeletonService.resolveActiveSections(1L, 10L)).thenReturn(List.of());
        when(batchRepository.findByCourseOfferingId(anyLong())).thenReturn(List.of());

        // One unbroken forenoon run: exactly one legal 4-period block position per day.
        Period p2 = new Period("2nd Period", LocalTime.of(9, 50), LocalTime.of(10, 40), 2);
        p2.setId(2L);
        Period p3 = new Period("3rd Period", LocalTime.of(10, 40), LocalTime.of(11, 30), 3);
        p3.setId(3L);
        Period p4 = new Period("4th Period", LocalTime.of(11, 30), LocalTime.of(12, 20), 4);
        p4.setId(4L);
        when(periodRepository.findByIsActiveTrueOrderByPeriodOrderAsc()).thenReturn(List.of(period1, p2, p3, p4));

        SkeletonSubjectBudget clinicalBudget = new SkeletonSubjectBudget(ClassSessionType.CLINICAL, 55L, "Batch 1", null, null, 40, 10, 1, 0);
        SkeletonSubjectResponse clinical = new SkeletonSubjectResponse(100L, "Offering A", "OFFE", List.of(clinicalBudget), null, null);
        // Offering B's own budget is already met (1 required, 1 placed) so it contributes NOTHING to
        // Phase 2 -- everything it places below comes purely from the greedy gap-fill pass, which is
        // what this test is actually about.
        SkeletonSubjectBudget selfStudyBudget = new SkeletonSubjectBudget(ClassSessionType.THEORY, null, null, null, null, 10, 10, 1, 1);
        SkeletonSubjectResponse selfStudy = new SkeletonSubjectResponse(200L, "Offering B", "OFFB", List.of(selfStudyBudget), null, null);
        SkeletonBuilderResponse skeleton = new SkeletonBuilderResponse(1L, "Cohort 1", "Term", List.of(clinical, selfStudy),
            List.of(), List.of(), List.of(), 25, 0L, List.of(), false, List.of());
        when(timetableSkeletonService.getCohortSkeleton(10L, 1L)).thenReturn(skeleton);

        // A placeCell stub that actually models slot exclusivity: a (day, period) already taken is
        // rejected exactly as the real service would reject it. Without this the test cannot
        // distinguish the two orderings at all.
        Set<String> occupiedSlots = new HashSet<>();
        // Only SUCCESSFUL placements land here. Asserting on Mockito's own call counts would be
        // wrong: a stranded row still ATTEMPTS its slot (and retries it once via attemptBacktrack),
        // so attempts stay non-zero even when nothing is actually placed.
        List<SkeletonCellPlacementRequest> successfulPlacements = new java.util.ArrayList<>();
        java.util.concurrent.atomic.AtomicLong nextCellId = new java.util.concurrent.atomic.AtomicLong(900L);
        org.mockito.stubbing.Answer<SkeletonCellResponse> occupancyAwarePlace = invocation -> {
            SkeletonCellPlacementRequest request = invocation.getArgument(0);
            List<Long> blockPeriodIds = new java.util.ArrayList<>();
            blockPeriodIds.add(request.periodId());
            if (request.spanPeriodIds() != null) {
                blockPeriodIds.addAll(request.spanPeriodIds());
            }
            for (Long periodId : blockPeriodIds) {
                if (occupiedSlots.contains(request.dayOfWeek() + ":" + periodId)) {
                    throw new TimetableConstraintViolationException(List.of(new com.cms.dto.ConstraintViolation(
                        "SKELETON_CELL_COHORT_CLASH", "already occupied")));
                }
            }
            blockPeriodIds.forEach(periodId -> occupiedSlots.add(request.dayOfWeek() + ":" + periodId));
            successfulPlacements.add(request);
            return new SkeletonCellResponse(nextCellId.incrementAndGet(), request.sessionType(), request.dayOfWeek(),
                request.periodId(), "Period", LocalTime.of(9, 0), LocalTime.of(9, 50), request.batchId(), "Batch 1",
                null, null, false, null, null, List.of(), request.courseOfferingId(), "Subject", "SUBJ", null, null, null, false, false);
        };
        when(timetableSkeletonService.placeCell(any(SkeletonCellPlacementRequest.class))).thenAnswer(occupancyAwarePlace);
        when(timetableSkeletonService.placeCell(any(SkeletonCellPlacementRequest.class), eq(false))).thenAnswer(occupancyAwarePlace);
        when(timetableStaffingService.staffCell(anyLong(), any(StaffingAssignmentRequest.class)))
            .thenAnswer(invocation -> new UnstaffedCellResponse(invocation.getArgument(0), 100L, "Subject", "SUBJ", null, null,
                ClassSessionType.CLINICAL, DayOfWeek.MONDAY, 1L, "Period", LocalTime.of(9, 0), LocalTime.of(9, 50),
                null, null, null, null, null, false, List.of(), null, null));

        var result = service.runGlobalAutoSchedule(10L, null);

        // The Clinical block was actually PLACED -- one full 4-period span, on Monday's only legal
        // position. Reorder the filler ahead of Phase 1 and this becomes zero elements, because the
        // only 4-period run in the week has been eaten one period at a time.
        assertThat(successfulPlacements).filteredOn(request -> request.sessionType() == ClassSessionType.CLINICAL)
            .singleElement()
            .satisfies(request -> {
                assertThat(request.dayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
                assertThat(request.periodId()).isEqualTo(1L);
                assertThat(request.spanPeriodIds()).containsExactly(2L, 3L, 4L);
            });
        assertThat(result.cohortSummaries().get(0).unplaced())
            .noneMatch(item -> item.sessionType() == ClassSessionType.CLINICAL);

        // ...and the filler really is greedy enough to have stranded it: the week ends up FULL --
        // Clinical's 4 Monday periods plus the 16 the filler took. 17 placements = 1 Clinical block
        // + 16 extra-hours periods. If either number ever drops, the guard above has gone slack and
        // would stop detecting a reordering.
        assertThat(occupiedSlots).hasSize(20);
        assertThat(occupiedSlots).contains("MONDAY:1", "MONDAY:2", "MONDAY:3", "MONDAY:4");
        assertThat(result.totalPlaced()).isEqualTo(17);
    }

    /** Regression for the unit mismatch behind "so many empty slots, yet so much unassigned":
     *  {@code requiredSessionsPerWeek} counts SESSIONS, but the placement loop used to spend the
     *  shortfall in PERIODS ({@code Math.min(blockSize, remaining)}, then {@code remaining -=
     *  blockSize}). A Clinical row owing 3 sessions of a 4-period block therefore placed one
     *  4-period block plus nothing else and declared itself done -- 4 periods delivered where 12
     *  were owed. Real seed data showed exactly this: every Clinical batch holding a single 4-cell
     *  group, every 2-period Lab holding one lone period. */
    @Test
    void runPlacesOneFullBlockPerOwedSession_notOneBlockPerOwedPeriod() {
        when(studentTermEnrollmentRepository.findDistinctCohortIdsByTermInstanceId(10L, EnrollmentStatus.ENROLLED))
            .thenReturn(new HashSet<>(List.of(1L)));
        cohort(1L, "Cohort 1");
        facultyWithDailyCap(500L, "XYZ", 8);
        when(courseOfferingService.getOfferingsByTermInstanceAndCohort(10L, 1L)).thenReturn(List.of(offeringDto(100L, "Offering A")));
        assignWholeCohort(100L, 1L, 500L);
        CourseOffering offering = offeringEntity(100L, 0, 0, 40);
        Subject clinicalSubject = new Subject();
        clinicalSubject.setId(1L);
        clinicalSubject.setClinicalSessionBlockPeriods(4);
        offering.setSubject(clinicalSubject);
        when(timetableSkeletonService.resolveActiveSections(1L, 10L)).thenReturn(List.of());
        when(batchRepository.findByCourseOfferingId(anyLong())).thenReturn(List.of());

        Period p2 = new Period("2nd Period", LocalTime.of(9, 50), LocalTime.of(10, 40), 2);
        p2.setId(2L);
        Period p3 = new Period("3rd Period", LocalTime.of(10, 40), LocalTime.of(11, 30), 3);
        p3.setId(3L);
        Period p4 = new Period("4th Period", LocalTime.of(11, 30), LocalTime.of(12, 20), 4);
        p4.setId(4L);
        when(periodRepository.findByIsActiveTrueOrderByPeriodOrderAsc()).thenReturn(List.of(period1, p2, p3, p4));

        // 3 sessions owed, each a 4-period block -> 3 placeCell calls, each spanning 3 extra periods.
        SkeletonSubjectBudget budget = new SkeletonSubjectBudget(ClassSessionType.CLINICAL, 55L, "Batch 1", null, null, 40, 10, 3, 0);
        SkeletonSubjectResponse subjectResponse = new SkeletonSubjectResponse(100L, "Offering A", "OFFE", List.of(budget), null, null);
        SkeletonBuilderResponse skeleton = new SkeletonBuilderResponse(1L, "Cohort 1", "Term", List.of(subjectResponse), List.of(), List.of(), List.of(), 25, 0L, List.of(), false, List.of());
        when(timetableSkeletonService.getCohortSkeleton(10L, 1L)).thenReturn(skeleton);

        SkeletonCellResponse placed = new SkeletonCellResponse(900L, ClassSessionType.CLINICAL, DayOfWeek.MONDAY, 1L, "1st Period",
            LocalTime.of(9, 0), LocalTime.of(9, 50), 55L, "Batch 1", null, null, false, null, null, List.of(),
            100L, "Offering A", "OFFE", null, null, null, false, false);
        when(timetableSkeletonService.placeCell(any(SkeletonCellPlacementRequest.class))).thenReturn(placed);
        when(timetableStaffingService.staffCell(eq(900L), any(StaffingAssignmentRequest.class)))
            .thenReturn(new UnstaffedCellResponse(900L, 100L, "Offering A", "OFFE", null, null,
                ClassSessionType.CLINICAL, DayOfWeek.MONDAY, 1L, "1st Period", LocalTime.of(9, 0), LocalTime.of(9, 50),
                null, null, null, null, null, false, List.of(), null, null));

        var result = service.runGlobalAutoSchedule(10L, null);

        assertThat(result.totalPlaced()).isEqualTo(3);
        ArgumentCaptor<SkeletonCellPlacementRequest> captor = ArgumentCaptor.forClass(SkeletonCellPlacementRequest.class);
        verify(timetableSkeletonService, times(3)).placeCell(captor.capture());
        // Every one a FULL 4-period block (primary + 3 span periods) -- never a truncated stub.
        assertThat(captor.getAllValues()).allSatisfy(r ->
            assertThat(r.spanPeriodIds()).hasSize(3));
    }

    /** Regression for the real-world "two parallel Clinical batches spread across 4 different days
     *  instead of sharing 2" incident: two batches of the same offering/sessionType deliver the SAME
     *  curriculum hours in parallel at two different venues, so they should land on the SAME day(s)
     *  -- see CohortRunContext#siblingDaysByOfferingAndType and placeShortfallRow's sibling-batch
     *  alignment step. Before this fix, each batch independently picked its own least-loaded day,
     *  which (since placing one batch raises that day's load) actively pushed the second batch onto
     *  a DIFFERENT day instead of the same one. */
    @Test
    void runAlignsSiblingClinicalBatchesOntoTheSameDay() {
        when(studentTermEnrollmentRepository.findDistinctCohortIdsByTermInstanceId(10L, EnrollmentStatus.ENROLLED))
            .thenReturn(new HashSet<>(List.of(1L)));
        cohort(1L, "Cohort 1");
        facultyWithDailyCap(500L, "XYZ", 8);
        when(courseOfferingService.getOfferingsByTermInstanceAndCohort(10L, 1L)).thenReturn(List.of(offeringDto(100L, "Offering A")));
        assignWholeCohort(100L, 1L, 500L);
        offeringEntity(100L, 0, 0, 40);
        when(timetableSkeletonService.resolveActiveSections(1L, 10L)).thenReturn(List.of());
        when(batchRepository.findByCourseOfferingId(anyLong())).thenReturn(List.of());

        SkeletonSubjectBudget batch1 = new SkeletonSubjectBudget(ClassSessionType.CLINICAL, 55L, "Batch 1", null, null, 10, 10, 1, 0);
        SkeletonSubjectBudget batch2 = new SkeletonSubjectBudget(ClassSessionType.CLINICAL, 56L, "Batch 2", null, null, 10, 10, 1, 0);
        SkeletonSubjectResponse subjectResponse = new SkeletonSubjectResponse(100L, "Offering A", "OFFE", List.of(batch1, batch2), null, null);
        SkeletonBuilderResponse skeleton = new SkeletonBuilderResponse(1L, "Cohort 1", "Term", List.of(subjectResponse), List.of(), List.of(), List.of(), 25, 0L, List.of(), false, List.of());
        when(timetableSkeletonService.getCohortSkeleton(10L, 1L)).thenReturn(skeleton);

        SkeletonCellResponse placed = new SkeletonCellResponse(900L, ClassSessionType.CLINICAL, DayOfWeek.MONDAY, 1L, "1st Period",
            LocalTime.of(9, 0), LocalTime.of(9, 50), null, null, null, null, false, null, null, List.of(),
            100L, "Offering A", "OFFE", null, null, null, false, false);
        when(timetableSkeletonService.placeCell(any(SkeletonCellPlacementRequest.class))).thenReturn(placed);
        when(timetableStaffingService.staffCell(eq(900L), any(StaffingAssignmentRequest.class)))
            .thenReturn(new UnstaffedCellResponse(900L, 100L, "Offering A", "OFFE", null, null,
                ClassSessionType.CLINICAL, DayOfWeek.MONDAY, 1L, "1st Period", LocalTime.of(9, 0), LocalTime.of(9, 50),
                null, null, null, null, null, false, List.of(), null, null));

        service.runGlobalAutoSchedule(10L, null);

        ArgumentCaptor<SkeletonCellPlacementRequest> captor = ArgumentCaptor.forClass(SkeletonCellPlacementRequest.class);
        verify(timetableSkeletonService, times(2)).placeCell(captor.capture());
        List<DayOfWeek> daysRequested = captor.getAllValues().stream().map(SkeletonCellPlacementRequest::dayOfWeek).toList();
        assertThat(daysRequested).hasSize(2);
        assertThat(daysRequested.get(0)).isEqualTo(daysRequested.get(1));
    }

    @Test
    void runReportsUnplacedInsteadOfThrowing_whenNoSlotWorksForBothPlacementAndStaffing() {
        when(studentTermEnrollmentRepository.findDistinctCohortIdsByTermInstanceId(10L, EnrollmentStatus.ENROLLED))
            .thenReturn(new HashSet<>(List.of(1L)));
        cohort(1L, "Cohort 1");
        facultyWithDailyCap(500L, "XYZ", 6);
        when(courseOfferingService.getOfferingsByTermInstanceAndCohort(10L, 1L)).thenReturn(List.of(offeringDto(100L, "Offering A")));
        assignWholeCohort(100L, 1L, 500L);
        offeringEntity(100L, 10, 0, 0);
        when(timetableSkeletonService.resolveActiveSections(1L, 10L)).thenReturn(List.of());
        when(batchRepository.findByCourseOfferingId(anyLong())).thenReturn(List.of());

        SkeletonSubjectBudget budget = new SkeletonSubjectBudget(ClassSessionType.THEORY, null, null, null, null, 10, 10, 1, 0);
        SkeletonSubjectResponse subject = new SkeletonSubjectResponse(100L, "Offering A", "OFFE", List.of(budget), null, null);
        SkeletonBuilderResponse skeleton = new SkeletonBuilderResponse(1L, "Cohort 1", "Term", List.of(subject), List.of(), List.of(), List.of(), 25, 0L, List.of(), false, List.of());
        when(timetableSkeletonService.getCohortSkeleton(10L, 1L)).thenReturn(skeleton);

        when(timetableSkeletonService.placeCell(any(SkeletonCellPlacementRequest.class)))
            .thenThrow(new TimetableConstraintViolationException(List.of(
                new com.cms.dto.ConstraintViolation("SKELETON_CELL_COHORT_CLASH", "clash"))));

        var result = service.runGlobalAutoSchedule(10L, null);

        assertThat(result.totalPlaced()).isEqualTo(0);
        assertThat(result.cohortSummaries()).hasSize(1);
        assertThat(result.cohortSummaries().get(0).unplaced()).hasSize(2);
        // The real blocking constraint (cohort clash) is now named, not a generic catch-all --
        // this is exactly the diagnostic gap that made a real shortfall look unexplainable
        // without pulling raw data by hand.
        assertThat(result.cohortSummaries().get(0).unplaced().get(0).reason())
            .contains("another mandatory session already occupies this audience's slot")
            .contains("5 of 5 day/period combinations tried");
        assertThat(result.cohortSummaries().get(0).unplaced().get(1).reason()).isEqualTo(NO_LIBRARY_CLASSROOM_REASON);

        // 5 days x 1 period exhausted -- Saturday is skipped outright since this term has no working-Saturday pattern configured.
        verify(timetableSkeletonService, times(5)).placeCell(any());
        verify(timetableStaffingService, never()).staffCell(anyLong(), any());
    }

    @Test
    void runReportsTheWorkloadCapByName_whenPlacementSucceedsButStaffingAlwaysFails() {
        // Reproduces the exact real-world case this feature was built for: placement itself works
        // fine everywhere (there's room in the grid), but the bound faculty's workload cap is what
        // actually blocks every single attempt -- the report must name that, not just "no slot".
        when(studentTermEnrollmentRepository.findDistinctCohortIdsByTermInstanceId(10L, EnrollmentStatus.ENROLLED))
            .thenReturn(new HashSet<>(List.of(1L)));
        cohort(1L, "Cohort 1");
        facultyWithDailyCap(500L, "XYZ", 6);
        when(courseOfferingService.getOfferingsByTermInstanceAndCohort(10L, 1L)).thenReturn(List.of(offeringDto(100L, "Offering A")));
        assignWholeCohort(100L, 1L, 500L);
        offeringEntity(100L, 10, 0, 0);
        when(timetableSkeletonService.resolveActiveSections(1L, 10L)).thenReturn(List.of());
        when(batchRepository.findByCourseOfferingId(anyLong())).thenReturn(List.of());

        SkeletonSubjectBudget budget = new SkeletonSubjectBudget(ClassSessionType.THEORY, null, null, null, null, 10, 10, 1, 0);
        SkeletonSubjectResponse subject = new SkeletonSubjectResponse(100L, "Offering A", "OFFE", List.of(budget), null, null);
        SkeletonBuilderResponse skeleton = new SkeletonBuilderResponse(1L, "Cohort 1", "Term", List.of(subject), List.of(), List.of(), List.of(), 25, 0L, List.of(), false, List.of());
        when(timetableSkeletonService.getCohortSkeleton(10L, 1L)).thenReturn(skeleton);

        SkeletonCellResponse placed = new SkeletonCellResponse(900L, ClassSessionType.THEORY, DayOfWeek.MONDAY, 1L, "1st Period",
            LocalTime.of(9, 0), LocalTime.of(9, 50), null, null, null, null, false, null, null, List.of(),
            100L, "Offering A", "OFFE", null, null, null, false, false);
        when(timetableSkeletonService.placeCell(any(SkeletonCellPlacementRequest.class))).thenReturn(placed);
        when(timetableStaffingService.staffCell(eq(900L), any(StaffingAssignmentRequest.class)))
            .thenThrow(new TimetableConstraintViolationException(List.of(
                new com.cms.dto.ConstraintViolation("STAFFING_WORKLOAD_DAILY_CAP_EXCEEDED", "over cap"))));

        var result = service.runGlobalAutoSchedule(10L, null);

        assertThat(result.totalPlaced()).isEqualTo(0);
        assertThat(result.cohortSummaries().get(0).unplaced().get(0).reason())
            .contains("the assigned faculty's daily workload cap was reached")
            .contains("5 of 5 day/period combinations tried");
        verify(timetableSkeletonService, times(5)).placeCell(any());
        verify(timetableSkeletonService, times(5)).removeCell(900L);
    }

    @Test
    void runReportsUnplacedInsteadOfThrowing_whenOfferingHasNoFacultyBound() {
        when(studentTermEnrollmentRepository.findDistinctCohortIdsByTermInstanceId(10L, EnrollmentStatus.ENROLLED))
            .thenReturn(new HashSet<>(List.of(1L)));
        cohort(1L, "Cohort 1");
        when(courseOfferingService.getOfferingsByTermInstanceAndCohort(10L, 1L)).thenReturn(List.of(offeringDto(100L, "Offering A")));
        offeringEntity(100L, 10, 0, 0);
        when(timetableSkeletonService.resolveActiveSections(1L, 10L)).thenReturn(List.of());
        when(batchRepository.findByCourseOfferingId(anyLong())).thenReturn(List.of());

        SkeletonSubjectBudget budget = new SkeletonSubjectBudget(ClassSessionType.THEORY, null, null, null, null, 10, 10, 1, 0);
        SkeletonSubjectResponse subject = new SkeletonSubjectResponse(100L, "Offering A", "OFFE", List.of(budget), null, null);
        SkeletonBuilderResponse skeleton = new SkeletonBuilderResponse(1L, "Cohort 1", "Term", List.of(subject), List.of(), List.of(), List.of(), 25, 0L, List.of(), false, List.of());
        when(timetableSkeletonService.getCohortSkeleton(10L, 1L)).thenReturn(skeleton);

        var result = service.runGlobalAutoSchedule(10L, null);

        assertThat(result.totalPlaced()).isEqualTo(0);
        assertThat(result.cohortSummaries().get(0).unplaced()).extracting(AutoPlaceUnplacedItem::reason)
            .containsExactly("no faculty assigned on its Course Offering", NO_LIBRARY_CLASSROOM_REASON);
        // Neither is a lack of free slots, so neither may raise the "open more Saturdays" alert.
        assertThat(result.cohortSummaries().get(0).unplaced()).noneMatch(AutoPlaceUnplacedItem::slotShortfall);
        verify(timetableSkeletonService, never()).placeCell(any());
    }

    @Test
    void runContinuesPlacingOtherCohorts_whenOneCohortHasAnUnplaceableSession() {
        when(studentTermEnrollmentRepository.findDistinctCohortIdsByTermInstanceId(10L, EnrollmentStatus.ENROLLED))
            .thenReturn(new HashSet<>(List.of(1L, 2L)));
        cohort(1L, "Cohort 1");
        cohort(2L, "Cohort 2");
        facultyWithDailyCap(500L, "Faculty A", 6);
        facultyWithDailyCap(600L, "Faculty B", 6);
        when(courseOfferingService.getOfferingsByTermInstanceAndCohort(10L, 1L)).thenReturn(List.of(offeringDto(100L, "Offering A")));
        when(courseOfferingService.getOfferingsByTermInstanceAndCohort(10L, 2L)).thenReturn(List.of(offeringDto(200L, "Offering B")));
        assignWholeCohort(100L, 1L, 500L);
        assignWholeCohort(200L, 2L, 600L);
        offeringEntity(100L, 10, 0, 0);
        offeringEntity(200L, 10, 0, 0);
        when(timetableSkeletonService.resolveActiveSections(anyLong(), eq(10L))).thenReturn(List.of());
        when(batchRepository.findByCourseOfferingId(anyLong())).thenReturn(List.of());

        SkeletonSubjectBudget budgetA = new SkeletonSubjectBudget(ClassSessionType.THEORY, null, null, null, null, 10, 10, 1, 0);
        SkeletonSubjectResponse subjectA = new SkeletonSubjectResponse(100L, "Offering A", "OFFA", List.of(budgetA), null, null);
        when(timetableSkeletonService.getCohortSkeleton(10L, 1L))
            .thenReturn(new SkeletonBuilderResponse(1L, "Cohort 1", "Term", List.of(subjectA), List.of(), List.of(), List.of(), 25, 0L, List.of(), false, List.of()));

        SkeletonSubjectBudget budgetB = new SkeletonSubjectBudget(ClassSessionType.THEORY, null, null, null, null, 10, 10, 1, 0);
        SkeletonSubjectResponse subjectB = new SkeletonSubjectResponse(200L, "Offering B", "OFFB", List.of(budgetB), null, null);
        when(timetableSkeletonService.getCohortSkeleton(10L, 2L))
            .thenReturn(new SkeletonBuilderResponse(2L, "Cohort 2", "Term", List.of(subjectB), List.of(), List.of(), List.of(), 25, 0L, List.of(), false, List.of()));

        // Cohort 1's offering (100L) can never be placed; cohort 2's (200L) succeeds every time.
        when(timetableSkeletonService.placeCell(argThat(r -> r != null && r.courseOfferingId().equals(100L))))
            .thenThrow(new TimetableConstraintViolationException(List.of(new com.cms.dto.ConstraintViolation("X", "no"))));
        SkeletonCellResponse placedB = new SkeletonCellResponse(901L, ClassSessionType.THEORY, DayOfWeek.MONDAY, 1L, "1st Period",
            LocalTime.of(9, 0), LocalTime.of(9, 50), null, null, null, null, false, null, null, List.of(),
            200L, "Offering B", "OFFB", null, null, null, false, false);
        when(timetableSkeletonService.placeCell(argThat(r -> r != null && r.courseOfferingId().equals(200L)))).thenReturn(placedB);
        when(timetableStaffingService.staffCell(eq(901L), any(StaffingAssignmentRequest.class)))
            .thenReturn(new UnstaffedCellResponse(901L, 200L, "Offering B", "OFFB", null, null,
                ClassSessionType.THEORY, DayOfWeek.MONDAY, 1L, "1st Period", LocalTime.of(9, 0), LocalTime.of(9, 50),
                null, null, null, null, null, false, List.of(), null, null));

        var result = service.runGlobalAutoSchedule(10L, null);

        assertThat(result.totalPlaced()).isEqualTo(1);
        var summaryByCohort = result.cohortSummaries().stream()
            .collect(java.util.stream.Collectors.toMap(com.cms.dto.CohortPlacementSummary::cohortId, s -> s));
        assertThat(summaryByCohort.get(1L).placedCount()).isEqualTo(0);
        assertThat(summaryByCohort.get(1L).unplaced()).hasSize(2);
        assertThat(summaryByCohort.get(2L).placedCount()).isEqualTo(1);
        assertThat(summaryByCohort.get(2L).unplaced()).extracting(AutoPlaceUnplacedItem::reason)
            .containsExactly(NO_LIBRARY_CLASSROOM_REASON);
    }

    @Test
    void runScopesToOneCohort_whenCohortIdProvided() {
        when(studentTermEnrollmentRepository.findDistinctCohortIdsByTermInstanceId(10L, EnrollmentStatus.ENROLLED))
            .thenReturn(new HashSet<>(List.of(1L, 2L)));
        cohort(1L, "Cohort 1");
        facultyWithDailyCap(500L, "Faculty A", 6);
        when(courseOfferingService.getOfferingsByTermInstanceAndCohort(10L, 1L)).thenReturn(List.of(offeringDto(100L, "Offering A")));
        assignWholeCohort(100L, 1L, 500L);
        offeringEntity(100L, 10, 0, 0);
        when(timetableSkeletonService.resolveActiveSections(1L, 10L)).thenReturn(List.of());
        when(batchRepository.findByCourseOfferingId(anyLong())).thenReturn(List.of());
        when(timetableSkeletonService.getCohortSkeleton(10L, 1L))
            .thenReturn(new SkeletonBuilderResponse(1L, "Cohort 1", "Term", List.of(), List.of(), List.of(), List.of(), 25, 0L, List.of(), false, List.of()));

        var result = service.runGlobalAutoSchedule(10L, 1L);

        assertThat(result.cohortSummaries()).hasSize(1);
        assertThat(result.cohortSummaries().get(0).cohortId()).isEqualTo(1L);
        verify(timetableSkeletonService, never()).getCohortSkeleton(10L, 2L);
    }

    // ── Published-term hard gate ────────────────────────────────────────

    @Test
    void runHardBlocksSingleCohortRequest_whenTermTimetableIsPublished() {
        when(studentTermEnrollmentRepository.findDistinctCohortIdsByTermInstanceId(10L, EnrollmentStatus.ENROLLED))
            .thenReturn(new HashSet<>(List.of(1L)));
        Cohort committed = cohort(1L, "Cohort 1");
        committed.setDisplayName("Cohort 1");
        when(timetableSkeletonService.getCohortActiveClassSchedules(10L, 1L, ClassScheduleStatus.PUBLISHED))
            .thenReturn(List.of(new ClassSchedule()));

        assertThatThrownBy(() -> service.runGlobalAutoSchedule(10L, 1L))
            .isInstanceOf(TimetableConstraintViolationException.class);

        verify(timetableSkeletonService, never()).getCohortSkeleton(anyLong(), anyLong());
        verify(timetableSkeletonService, never()).placeCell(any());
    }

    @Test
    void runAllCohorts_excludesEveryCohort_whenEveryCohortsOwnTimetableIsPublished_andReportsThemAsSkipped() {
        when(studentTermEnrollmentRepository.findDistinctCohortIdsByTermInstanceId(10L, EnrollmentStatus.ENROLLED))
            .thenReturn(new HashSet<>(List.of(1L, 2L)));
        Cohort cohort1 = cohort(1L, "Cohort 1");
        cohort1.setDisplayName("Cohort 1");
        Cohort cohort2 = cohort(2L, "Cohort 2");
        cohort2.setDisplayName("Cohort 2");
        when(timetableSkeletonService.getCohortActiveClassSchedules(10L, 1L, ClassScheduleStatus.PUBLISHED))
            .thenReturn(List.of(new ClassSchedule()));
        when(timetableSkeletonService.getCohortActiveClassSchedules(10L, 2L, ClassScheduleStatus.PUBLISHED))
            .thenReturn(List.of(new ClassSchedule()));

        var result = service.runGlobalAutoSchedule(10L, null);

        assertThat(result.skippedPublishedCohorts()).hasSize(2);
        assertThat(result.skippedPublishedCohorts()).extracting(SkippedPublishedCohort::cohortId)
            .containsExactlyInAnyOrder(1L, 2L);
        assertThat(result.cohortSummaries()).isEmpty();
        verify(timetableSkeletonService, never()).getCohortSkeleton(anyLong(), anyLong());
    }

    @Test
    void runAllCohorts_onlySkipsThePublishedCohort_leavingOtherCohortsInTheSameTermToRunNormally() {
        // Regression for the bug where one cohort's own timetable being approved (OC-258/OC-260
        // cohort-scoped Approve) silently caused every other cohort sharing that termInstanceId to be
        // wrongly skipped too, because the gate used to be a single term-wide exists() check instead
        // of a per-cohort one.
        when(studentTermEnrollmentRepository.findDistinctCohortIdsByTermInstanceId(10L, EnrollmentStatus.ENROLLED))
            .thenReturn(new HashSet<>(List.of(1L, 2L)));
        Cohort published = cohort(1L, "Published Cohort");
        published.setDisplayName("Published Cohort");
        Cohort pending = cohort(2L, "Pending Cohort");
        pending.setDisplayName("Pending Cohort");
        when(timetableSkeletonService.getCohortActiveClassSchedules(10L, 1L, ClassScheduleStatus.PUBLISHED))
            .thenReturn(List.of(new ClassSchedule()));
        when(timetableSkeletonService.getCohortActiveClassSchedules(10L, 2L, ClassScheduleStatus.PUBLISHED))
            .thenReturn(List.of());
        when(timetableSkeletonService.resolveActiveSections(2L, 10L)).thenReturn(List.of());
        when(timetableSkeletonService.getCohortSkeleton(10L, 2L))
            .thenReturn(new SkeletonBuilderResponse(2L, "Pending Cohort", "Term", List.of(), List.of(), List.of(), List.of(), 25, 0L, List.of(), false, List.of()));

        var result = service.runGlobalAutoSchedule(10L, null);

        assertThat(result.skippedPublishedCohorts()).extracting(SkippedPublishedCohort::cohortId)
            .containsExactly(1L);
        assertThat(result.cohortSummaries()).extracting(CohortPlacementSummary::cohortId)
            .containsExactly(2L);
        verify(timetableSkeletonService, never()).getCohortSkeleton(10L, 1L);
        verify(timetableSkeletonService, atLeastOnce()).getCohortSkeleton(10L, 2L);
    }

    // ── Pre-run "would this overwrite existing draft content" check ────

    @Test
    void hasExistingDraftContent_true_whenAnActiveDraftRowExistsInTheTerm() {
        when(classScheduleRepository.existsByTermInstanceIdAndStatusAndIsActiveTrue(10L, ClassScheduleStatus.DRAFT))
            .thenReturn(true);

        assertThat(service.hasExistingDraftContent(10L)).isTrue();
    }

    @Test
    void hasExistingDraftContent_false_whenNoActiveDraftRowExistsInTheTerm() {
        when(classScheduleRepository.existsByTermInstanceIdAndStatusAndIsActiveTrue(10L, ClassScheduleStatus.DRAFT))
            .thenReturn(false);

        assertThat(service.hasExistingDraftContent(10L)).isFalse();
    }

    // ── Live single-(faculty, cohort) capacity check (Course Offerings) ─

    @Test
    void checkFacultyCapacityForCohort_fitsWithinCapacity() {
        when(studentTermEnrollmentRepository.findDistinctCohortIdsByTermInstanceId(10L, EnrollmentStatus.ENROLLED))
            .thenReturn(new HashSet<>(List.of(1L)));
        cohort(1L, "Cohort 1");
        facultyWithDailyCap(500L, "XYZ", 6); // 600h capacity
        usePreciseOneHourPeriodFixture();
        when(courseOfferingService.getOfferingsByTermInstanceAndCohort(10L, 1L))
            .thenReturn(List.of(offeringDto(100L, "Offering A")));
        offeringEntity(100L, 10, 0, 0);
        when(timetableSkeletonService.resolveActiveSections(1L, 10L)).thenReturn(List.of());
        when(batchRepository.findByCourseOfferingId(anyLong())).thenReturn(List.of());

        FacultyCapacityCheckResult result = service.checkFacultyCapacityForCohort(100L, 1L, 500L);

        assertThat(result.overCapacity()).isFalse();
        assertThat(result.currentDemandHours()).isEqualTo(0.0);
        assertThat(result.offeringHours()).isEqualTo(10.0);
        assertThat(result.projectedTotalHours()).isEqualTo(10.0);
        assertThat(result.capacityHours()).isEqualTo(600.0);
    }

    @Test
    void checkFacultyCapacityForCohort_exceedsCapacity_sumsExistingDemandPlusThisOffering() {
        when(studentTermEnrollmentRepository.findDistinctCohortIdsByTermInstanceId(10L, EnrollmentStatus.ENROLLED))
            .thenReturn(new HashSet<>(List.of(1L)));
        cohort(1L, "Cohort 1");
        facultyWithDailyCap(500L, "XYZ", 2); // 200h capacity
        usePreciseOneHourPeriodFixture();
        when(courseOfferingService.getOfferingsByTermInstanceAndCohort(10L, 1L))
            .thenReturn(List.of(offeringDto(100L, "Offering A"), offeringDto(200L, "Offering B")));
        assignWholeCohort(100L, 1L, 500L); // already bound to 500 -- 150h existing demand
        offeringEntity(100L, 150, 0, 0);
        offeringEntity(200L, 90, 0, 0);  // being considered for 500 -- 90h, currently unbound
        when(timetableSkeletonService.resolveActiveSections(1L, 10L)).thenReturn(List.of());
        when(batchRepository.findByCourseOfferingId(anyLong())).thenReturn(List.of());

        FacultyCapacityCheckResult result = service.checkFacultyCapacityForCohort(200L, 1L, 500L);

        assertThat(result.currentDemandHours()).isEqualTo(150.0);
        assertThat(result.offeringHours()).isEqualTo(90.0);
        assertThat(result.projectedTotalHours()).isEqualTo(240.0);
        assertThat(result.overCapacity()).isTrue();
        assertThat(result.suggestedMinDailyHours()).isEqualTo(3.0); // ceil(240/100)
        // 1-hour periods (usePreciseOneHourPeriodFixture) -- sessions and hours coincide exactly.
        assertThat(result.suggestedMinDailySessions()).isEqualTo(3);
    }

    @Test
    void checkFacultyCapacityForCohort_reCheckingAlreadyAssignedFaculty_neverDoubleCounts() {
        when(studentTermEnrollmentRepository.findDistinctCohortIdsByTermInstanceId(10L, EnrollmentStatus.ENROLLED))
            .thenReturn(new HashSet<>(List.of(1L)));
        cohort(1L, "Cohort 1");
        facultyWithDailyCap(500L, "XYZ", 2); // 200h capacity
        when(courseOfferingService.getOfferingsByTermInstanceAndCohort(10L, 1L))
            .thenReturn(List.of(offeringDto(100L, "Offering A")));
        assignWholeCohort(100L, 1L, 500L);
        offeringEntity(100L, 150, 0, 0);
        when(timetableSkeletonService.resolveActiveSections(1L, 10L)).thenReturn(List.of());
        when(batchRepository.findByCourseOfferingId(anyLong())).thenReturn(List.of());

        // Re-checking the SAME faculty already bound to this SAME offering+cohort must not add its
        // own 150h contribution a second time (150+150=300 would wrongly exceed the 200h capacity).
        FacultyCapacityCheckResult result = service.checkFacultyCapacityForCohort(100L, 1L, 500L);

        assertThat(result.projectedTotalHours()).isEqualTo(150.0);
        assertThat(result.overCapacity()).isFalse();
    }

    // ── Faculty workload detail ────────────────────────────────────────

    @Test
    void getFacultyWorkloadReturnsEveryAssignment_notJustTopTwo() {
        // Same shape as the worked-example precheck test: offering A (2 sections, no override,
        // 90h each = 180h) + offering B (90h) + offering C (90h) = 4 distinct contribution rows,
        // all bound to faculty XYZ. The precheck's own topContributors would cap this at 2 for its
        // warning card -- this method must return all 4, proving it doesn't reuse that limit.
        when(studentTermEnrollmentRepository.findDistinctCohortIdsByTermInstanceId(10L, EnrollmentStatus.ENROLLED))
            .thenReturn(new HashSet<>(List.of(1L, 2L, 3L)));
        cohort(1L, "Cohort 1");
        cohort(2L, "Cohort 2");
        cohort(3L, "Cohort 3");

        facultyWithDailyCap(500L, "XYZ", 3); // 3h/day x 100 days = 300h capacity
        usePreciseOneHourPeriodFixture();
        when(courseOfferingService.getOfferingsByTermInstanceAndCohort(10L, 1L)).thenReturn(List.of(offeringDto(100L, "Offering A")));
        when(courseOfferingService.getOfferingsByTermInstanceAndCohort(10L, 2L)).thenReturn(List.of(offeringDto(200L, "Offering B")));
        when(courseOfferingService.getOfferingsByTermInstanceAndCohort(10L, 3L)).thenReturn(List.of(offeringDto(300L, "Offering C")));
        assignWholeCohort(100L, 1L, 500L);
        assignWholeCohort(200L, 2L, 500L);
        assignWholeCohort(300L, 3L, 500L);

        offeringEntity(100L, 90, 0, 0);
        offeringEntity(200L, 90, 0, 0);
        offeringEntity(300L, 90, 0, 0);

        CohortSection sectionA = new CohortSection();
        sectionA.setId(1L);
        CohortSection sectionB = new CohortSection();
        sectionB.setId(2L);
        when(timetableSkeletonService.resolveActiveSections(1L, 10L)).thenReturn(List.of(sectionA, sectionB));
        when(timetableSkeletonService.resolveActiveSections(2L, 10L)).thenReturn(List.of());
        when(timetableSkeletonService.resolveActiveSections(3L, 10L)).thenReturn(List.of());
        when(batchRepository.findByCourseOfferingId(anyLong())).thenReturn(List.of());

        FacultyWorkloadDetail result = service.getFacultyWorkload(500L, 10L);

        assertThat(result.facultyId()).isEqualTo(500L);
        assertThat(result.facultyName()).isEqualTo("XYZ Staff");
        assertThat(result.assignments()).hasSize(4);
        assertThat(result.totalDemandHours()).isEqualTo(360.0); // 90*2 + 90 + 90
        assertThat(result.termCapacityHours()).isEqualTo(300.0);
        assertThat(result.overCapacity()).isTrue();
        assertThat(result.shortfallHours()).isEqualTo(60.0);
    }

    @Test
    void getFacultyWorkloadReturnsEmptyAssignments_whenFacultyHasNothingThisTerm() {
        when(studentTermEnrollmentRepository.findDistinctCohortIdsByTermInstanceId(10L, EnrollmentStatus.ENROLLED))
            .thenReturn(new HashSet<>(List.of(1L)));
        cohort(1L, "Cohort 1");
        facultyWithDailyCap(999L, "Idle", 6);
        when(courseOfferingService.getOfferingsByTermInstanceAndCohort(10L, 1L)).thenReturn(List.of(offeringDto(100L, "Offering A")));
        assignWholeCohort(100L, 1L, 500L);
        offeringEntity(100L, 10, 0, 0);
        when(timetableSkeletonService.resolveActiveSections(1L, 10L)).thenReturn(List.of());
        when(batchRepository.findByCourseOfferingId(anyLong())).thenReturn(List.of());

        FacultyWorkloadDetail result = service.getFacultyWorkload(999L, 10L);

        assertThat(result.assignments()).isEmpty();
        assertThat(result.totalDemandHours()).isEqualTo(0.0);
        assertThat(result.overCapacity()).isFalse();
    }

    @Test
    void getFacultyWorkloadKeepsTheoryAndLabClinicalAsSeparateRows_evenWhenBothFallToPrimary() {
        // Unsectioned, unbatched offering with BOTH theory and lab/clinical hours -- before
        // threading sessionType into the merge key, both would land on the same (faculty, null,
        // null) key and silently combine into one row with no way to tell what type it was.
        when(studentTermEnrollmentRepository.findDistinctCohortIdsByTermInstanceId(10L, EnrollmentStatus.ENROLLED))
            .thenReturn(new HashSet<>(List.of(1L)));
        cohort(1L, "Cohort 1");
        facultyWithDailyCap(500L, "XYZ", 6);
        when(courseOfferingService.getOfferingsByTermInstanceAndCohort(10L, 1L)).thenReturn(List.of(offeringDto(100L, "Offering A")));
        assignWholeCohort(100L, 1L, 500L);
        offeringEntity(100L, 30, 20, 10); // 30 theory, 20 lab + 10 clinical = 30 combined
        when(timetableSkeletonService.resolveActiveSections(1L, 10L)).thenReturn(List.of());
        when(batchRepository.findByCourseOfferingId(anyLong())).thenReturn(List.of());

        FacultyWorkloadDetail result = service.getFacultyWorkload(500L, 10L);

        assertThat(result.assignments()).hasSize(2);
        assertThat(result.assignments()).extracting("sessionType", "termHoursContributed")
            .containsExactlyInAnyOrder(
                org.assertj.core.groups.Tuple.tuple("THEORY", 30.0),
                org.assertj.core.groups.Tuple.tuple("LAB_CLINICAL", 30.0));
    }

    @Test
    void getFacultyWorkloadSummariesRunsAggregationOnceAndCoversEveryRequestedId_includingIdle() {
        // Two cohorts sharing the term: faculty A (500, tiny cap) ends up over capacity; faculty B
        // (600, generous cap) fits. A third id (999, no demand this term at all) is requested too
        // -- must still come back with totalDemandHours == 0, not be silently dropped.
        when(studentTermEnrollmentRepository.findDistinctCohortIdsByTermInstanceId(10L, EnrollmentStatus.ENROLLED))
            .thenReturn(new HashSet<>(List.of(1L, 2L)));
        cohort(1L, "Cohort 1");
        cohort(2L, "Cohort 2");
        facultyWithDailyCap(500L, "Over", 1); // 100h capacity
        facultyWithDailyCap(600L, "Fits", 6); // 600h capacity
        facultyWithDailyCap(999L, "Idle", 6);
        usePreciseOneHourPeriodFixture();
        when(courseOfferingService.getOfferingsByTermInstanceAndCohort(10L, 1L)).thenReturn(List.of(offeringDto(100L, "Offering A")));
        when(courseOfferingService.getOfferingsByTermInstanceAndCohort(10L, 2L)).thenReturn(List.of(offeringDto(200L, "Offering B")));
        assignWholeCohort(100L, 1L, 500L);
        assignWholeCohort(200L, 2L, 600L);
        offeringEntity(100L, 200, 0, 0); // 200h > 100h cap
        offeringEntity(200L, 90, 0, 0);  // 90h < 600h cap
        when(timetableSkeletonService.resolveActiveSections(anyLong(), eq(10L))).thenReturn(List.of());
        when(batchRepository.findByCourseOfferingId(anyLong())).thenReturn(List.of());

        List<com.cms.dto.FacultyWorkloadSummary> result = service.getFacultyWorkloadSummaries(List.of(500L, 600L, 999L), 10L);

        assertThat(result).hasSize(3);
        var byId = result.stream().collect(java.util.stream.Collectors.toMap(
            com.cms.dto.FacultyWorkloadSummary::facultyId, s -> s));
        assertThat(byId.get(500L).overCapacity()).isTrue();
        assertThat(byId.get(500L).totalDemandHours()).isEqualTo(200.0);
        assertThat(byId.get(500L).shortfallHours()).isEqualTo(100.0);
        assertThat(byId.get(600L).overCapacity()).isFalse();
        assertThat(byId.get(600L).totalDemandHours()).isEqualTo(90.0);
        assertThat(byId.get(999L).overCapacity()).isFalse();
        assertThat(byId.get(999L).totalDemandHours()).isEqualTo(0.0);
    }

    // ── Prerequisite check ─────────────────────────────────────────────

    @Test
    void checkPrerequisitesReportsOfferingsWithoutFacultyAndOverCapacityFaculty() {
        when(studentTermEnrollmentRepository.findDistinctCohortIdsByTermInstanceId(10L, EnrollmentStatus.ENROLLED))
            .thenReturn(new HashSet<>(List.of(1L)));
        cohort(1L, "Cohort 1");
        facultyWithDailyCap(500L, "XYZ", 1); // 100h capacity
        when(courseOfferingService.getOfferingsByTermInstanceAndCohort(10L, 1L))
            .thenReturn(List.of(offeringDto(100L, "Offering A"), offeringDto(200L, "Offering B")));
        assignWholeCohort(200L, 1L, 500L);
        offeringEntity(100L, 10, 0, 0); // no faculty bound -- a real prerequisite gap
        offeringEntity(200L, 200, 0, 0); // bound, but 200h > 100h capacity -- over capacity
        when(timetableSkeletonService.resolveActiveSections(1L, 10L)).thenReturn(List.of());
        when(batchRepository.findByCourseOfferingId(anyLong())).thenReturn(List.of());
        when(courseOfferingSectionFacultyService.getAssignmentSummaryForTermInstance(10L)).thenReturn(List.of(
            new CourseOfferingFacultySummaryDto(100L, List.of(), OfferingAssignmentStatus.NONE),
            new CourseOfferingFacultySummaryDto(200L, List.of("XYZ Staff"), OfferingAssignmentStatus.FULL)));

        SkeletonSubjectBudget budgetA = new SkeletonSubjectBudget(ClassSessionType.THEORY, null, null, null, null, 10, 10, 1, 0);
        SkeletonSubjectResponse subjectA = new SkeletonSubjectResponse(100L, "Offering A", "OFFA", List.of(budgetA), null, null);
        SkeletonSubjectResponse subjectB = new SkeletonSubjectResponse(200L, "Offering B", "OFFB",
            List.of(new SkeletonSubjectBudget(ClassSessionType.THEORY, null, null, null, null, 10, 10, 1, 0)), null, null);
        when(timetableSkeletonService.getCohortSkeleton(10L, 1L))
            .thenReturn(new SkeletonBuilderResponse(1L, "Cohort 1", "Term", List.of(subjectA, subjectB), List.of(), List.of(), List.of(), 25, 0L, List.of(), false, List.of()));

        GlobalAutoSchedulePrerequisites result = service.checkPrerequisites(10L, null);

        assertThat(result.ready()).isFalse();
        assertThat(result.offeringsWithoutFaculty()).hasSize(1);
        assertThat(result.offeringsWithoutFaculty().get(0).courseOfferingId()).isEqualTo(100L);
        assertThat(result.capacityPrecheck().overCapacityFaculty()).extracting(FacultyOverCapacity::facultyId)
            .containsExactly(500L);
    }

    /** Builds an elective member offering (id 300, group 77) whose skeleton subject is flagged
     *  elective, and stubs the group-member list lookup {@link TimetableGlobalAutoScheduleService}'s
     *  elective branch needs. This member's own assignment status is left to the caller (via {@link
     *  #courseOfferingSectionFacultyService}'s stub) -- checkPrerequisites resolves elective gaps off
     *  the same real Assign Faculty/Publish-gate rollup as every other offering now, and so does
     *  {@code resolveElectiveMemberFacultyId} itself since it now delegates to {@link
     *  CourseOfferingSectionFacultyService#getForOffering} too (see the {@code
     *  resolveElectiveMemberFacultyId*} tests below for that method's own delegation behavior). */
    private CourseOffering electiveMember() {
        Subject subject = new Subject();
        subject.setId(300L);
        subject.setName("Elective: Human Values");

        CurriculumSemesterCourse csc = new CurriculumSemesterCourse();
        csc.setTheoryHours(10);
        csc.setIsElective(true);

        CourseOffering member = new CourseOffering();
        member.setId(300L);
        member.setSubject(subject);
        member.setCurriculumSemesterCourse(csc);
        member.setTermInstance(termInstance);
        member.setIsActive(true);
        when(courseOfferingRepository.findById(300L)).thenReturn(Optional.of(member));
        lenient().when(timetableSkeletonService.isElectiveOffering(member)).thenReturn(true);
        when(courseOfferingRepository.findByTermInstanceIdAndCurriculumSemesterCourse_ElectiveGroupId(10L, 77L))
            .thenReturn(List.of(member));

        SkeletonSubjectResponse electiveSubject = new SkeletonSubjectResponse(300L, "Elective: Human Values",
            "ELEC-I-HVAL", List.of(), 77L, "Group A");
        when(timetableSkeletonService.getCohortSkeleton(10L, 1L)).thenReturn(
            new SkeletonBuilderResponse(1L, "Cohort 1", "Term", List.of(electiveSubject), List.of(), List.of(), List.of(),
                25, 0L, List.of(), false, List.of()));
        return member;
    }

    @Test
    void checkPrerequisitesFlagsElectiveMember_whenAssignmentStatusIsPartial() {
        when(studentTermEnrollmentRepository.findDistinctCohortIdsByTermInstanceId(10L, EnrollmentStatus.ENROLLED))
            .thenReturn(new HashSet<>(List.of(1L)));
        cohort(1L, "Cohort 1");
        electiveMember();
        when(courseOfferingSectionFacultyService.getAssignmentSummaryForTermInstance(10L)).thenReturn(
            List.of(new CourseOfferingFacultySummaryDto(300L, List.of("Meera Iyer"), OfferingAssignmentStatus.PARTIAL)));

        GlobalAutoSchedulePrerequisites result = service.checkPrerequisites(10L, null);

        assertThat(result.offeringsWithoutFaculty()).hasSize(1);
        assertThat(result.offeringsWithoutFaculty().get(0).courseOfferingId()).isEqualTo(300L);
    }

    @Test
    void checkPrerequisitesDoesNotFlagElectiveMember_whenAssignmentStatusIsFull() {
        when(studentTermEnrollmentRepository.findDistinctCohortIdsByTermInstanceId(10L, EnrollmentStatus.ENROLLED))
            .thenReturn(new HashSet<>(List.of(1L)));
        cohort(1L, "Cohort 1");
        electiveMember();
        when(courseOfferingSectionFacultyService.getAssignmentSummaryForTermInstance(10L)).thenReturn(
            List.of(new CourseOfferingFacultySummaryDto(300L, List.of("Meera Iyer"), OfferingAssignmentStatus.FULL)));

        GlobalAutoSchedulePrerequisites result = service.checkPrerequisites(10L, null);

        assertThat(result.offeringsWithoutFaculty()).isEmpty();
    }

    @Test
    void resolveElectiveMemberFacultyIdReturnsTheResolvedFaculty_whenGetForOfferingAgreesOnOne() {
        // Regression for the production incident this closes: a whole-cohort (null cohortSectionId)
        // row assigned before a cohort ever split into sections, left behind once a later per-section
        // assignment was made for the same cohort. The old implementation queried
        // CourseOfferingSectionFacultyRepository directly and only excluded a row on a now-INACTIVE
        // CohortSection -- a null section was never "inactive", so it stayed forever, permanently
        // disagreeing with the real section-scoped assignment and reporting the elective as unstaffed
        // to the real placement pass even though checkPrerequisites/Assign Faculty (both backed by
        // getForOffering, which correctly stops looking at the whole-cohort row once real sections
        // exist) already showed it as fully staffed. Delegating to getForOffering here closes that gap.
        CourseOffering member = new CourseOffering();
        member.setId(300L);
        when(courseOfferingSectionFacultyService.getForOffering(300L)).thenReturn(
            new CourseOfferingSectionFacultyResponse(true, null, List.of(
                new SectionFacultyAssignment(4L, 51L, "BSc Nursing (2025-2029)", "Section 1", 36L, "Meera Iyer", 0L))));

        assertThat(service.resolveElectiveMemberFacultyId(member)).isEqualTo(36L);
    }

    @Test
    void resolveElectiveMemberFacultyIdReturnsNull_whenTwoResolvedSectionsGenuinelyDisagree() {
        // The delegation to getForOffering must not swallow a real disagreement -- two different
        // cohorts' own resolved sections pointing at two different faculty members is exactly the
        // ambiguous case this method exists to catch (see its own javadoc: "returns null rather than
        // guessing which one wins").
        CourseOffering member = new CourseOffering();
        member.setId(300L);
        when(courseOfferingSectionFacultyService.getForOffering(300L)).thenReturn(
            new CourseOfferingSectionFacultyResponse(true, null, List.of(
                new SectionFacultyAssignment(4L, 51L, "Cohort A", "Section 1", 30L, "Faculty A", 0L),
                new SectionFacultyAssignment(7L, 52L, "Cohort B", "Section 1", 26L, "Faculty B", 0L))));

        assertThat(service.resolveElectiveMemberFacultyId(member)).isNull();
    }

    @Test
    void checkPrerequisitesStillReportsARealGap_evenWhenTermTimetableIsPublished() {
        // A gap introduced by a post-publish faculty reassignment is exactly what this item exists to
        // surface -- unlike the placement pass, this checklist item no longer special-cases a
        // PUBLISHED term at all (Global Auto-Schedule itself is separately hard-blocked for a
        // published term regardless; "Assign Faculty" from this checklist still fixes the gap
        // directly), so it never even queries publish status -- verified below.
        when(studentTermEnrollmentRepository.findDistinctCohortIdsByTermInstanceId(10L, EnrollmentStatus.ENROLLED))
            .thenReturn(new HashSet<>(List.of(1L)));
        cohort(1L, "Cohort 1");
        offeringEntity(100L, 10, 0, 0);
        SkeletonSubjectBudget budget = new SkeletonSubjectBudget(ClassSessionType.THEORY, null, null, null, null, 10, 10, 1, 0);
        SkeletonSubjectResponse subject = new SkeletonSubjectResponse(100L, "Offering A", "OFFA", List.of(budget), null, null);
        when(timetableSkeletonService.getCohortSkeleton(10L, 1L))
            .thenReturn(new SkeletonBuilderResponse(1L, "Cohort 1", "Term", List.of(subject), List.of(), List.of(), List.of(), 25, 0L, List.of(), false, List.of()));
        when(courseOfferingSectionFacultyService.getAssignmentSummaryForTermInstance(10L)).thenReturn(
            List.of(new CourseOfferingFacultySummaryDto(100L, List.of(), OfferingAssignmentStatus.NONE)));

        GlobalAutoSchedulePrerequisites result = service.checkPrerequisites(10L, null);

        assertThat(result.ready()).isFalse();
        assertThat(result.offeringsWithoutFaculty()).hasSize(1);
        assertThat(result.offeringsWithoutFaculty().get(0).courseOfferingId()).isEqualTo(100L);
        verify(timetableSkeletonService).getCohortSkeleton(10L, 1L);
        verify(classScheduleRepository, never()).existsByTermInstanceIdAndStatus(anyLong(), any());
    }

    @Test
    void checkPrerequisitesReadyWhenNothingOutstanding() {
        when(studentTermEnrollmentRepository.findDistinctCohortIdsByTermInstanceId(10L, EnrollmentStatus.ENROLLED))
            .thenReturn(new HashSet<>(List.of(1L)));
        cohort(1L, "Cohort 1");
        facultyWithDailyCap(500L, "XYZ", 6); // plenty of capacity
        when(courseOfferingService.getOfferingsByTermInstanceAndCohort(10L, 1L)).thenReturn(List.of(offeringDto(100L, "Offering A")));
        assignWholeCohort(100L, 1L, 500L);
        offeringEntity(100L, 10, 0, 0);
        when(timetableSkeletonService.resolveActiveSections(1L, 10L)).thenReturn(List.of());
        when(batchRepository.findByCourseOfferingId(anyLong())).thenReturn(List.of());

        SkeletonSubjectBudget budget = new SkeletonSubjectBudget(ClassSessionType.THEORY, null, null, null, null, 10, 10, 1, 0);
        SkeletonSubjectResponse subject = new SkeletonSubjectResponse(100L, "Offering A", "OFFA", List.of(budget), null, null);
        when(timetableSkeletonService.getCohortSkeleton(10L, 1L))
            .thenReturn(new SkeletonBuilderResponse(1L, "Cohort 1", "Term", List.of(subject), List.of(), List.of(), List.of(), 25, 0L, List.of(), false, List.of()));

        GlobalAutoSchedulePrerequisites result = service.checkPrerequisites(10L, null);

        assertThat(result.ready()).isTrue();
        assertThat(result.offeringsWithoutFaculty()).isEmpty();
    }

    @Test
    void checkPrerequisitesIncludesLabClinicalVenueCapacity_readyReflectsOverCapacity() {
        when(studentTermEnrollmentRepository.findDistinctCohortIdsByTermInstanceId(10L, EnrollmentStatus.ENROLLED))
            .thenReturn(new HashSet<>(List.of(1L)));
        cohort(1L, "Cohort 1");
        facultyWithDailyCap(500L, "XYZ", 6); // plenty of capacity, faculty precheck passes
        when(courseOfferingService.getOfferingsByTermInstanceAndCohort(10L, 1L)).thenReturn(List.of(offeringDto(100L, "Offering A")));
        assignWholeCohort(100L, 1L, 500L);
        offeringEntity(100L, 10, 0, 0);
        when(timetableSkeletonService.resolveActiveSections(1L, 10L)).thenReturn(List.of());
        when(batchRepository.findByCourseOfferingId(anyLong())).thenReturn(List.of());

        SkeletonSubjectBudget budget = new SkeletonSubjectBudget(ClassSessionType.THEORY, null, null, null, null, 10, 10, 1, 0);
        SkeletonSubjectResponse subject = new SkeletonSubjectResponse(100L, "Offering A", "OFFA", List.of(budget), null, null);
        when(timetableSkeletonService.getCohortSkeleton(10L, 1L))
            .thenReturn(new SkeletonBuilderResponse(1L, "Cohort 1", "Term", List.of(subject), List.of(), List.of(), List.of(), 25, 0L, List.of(), false, List.of()));

        VenueOverCapacity overCapacity = new VenueOverCapacity(50L, "LAB", "Anatomy Lab", 30, 12, 13, 1, List.of("Anatomy"), List.of(100L));
        when(timetableCapacityPlanningService.computeLabClinicalVenueCapacity(eq(10L), any()))
            .thenReturn(new LabClinicalVenueCapacityResult(List.of(overCapacity), List.of()));

        GlobalAutoSchedulePrerequisites result = service.checkPrerequisites(10L, null);

        assertThat(result.ready()).isFalse();
        assertThat(result.offeringsWithoutFaculty()).isEmpty();
        assertThat(result.capacityPrecheck().overCapacityFaculty()).isEmpty();
        assertThat(result.labClinicalVenueCapacity().overCapacityVenues()).extracting(VenueOverCapacity::venueId)
            .containsExactly(50L);
    }

    // ── Eligible faculty picker (offering + section level) ────────────

    private Faculty activeFaculty(Long id, Speciality speciality, int dailyCapHours) {
        Faculty f = new Faculty();
        f.setId(id);
        f.setSpeciality(speciality);
        f.setStatus(FacultyStatus.ACTIVE);
        f.setPlannedDailySessionsOverride(dailyCapHours);
        return f;
    }

    @Test
    void hasEligibleFacultyPool_trueWhenSubjectHasNoSpeciality() {
        Subject subject = new Subject("Communicative English", "ENGL101", 4, 3, 1, null, 1);
        subject.setId(1L);

        assertThat(service.hasEligibleFacultyPool(subject)).isTrue();
        verify(facultyRepository, never()).findByStatus(any());
    }

    @Test
    void hasEligibleFacultyPool_falseWhenSpecialitySetAndNoActiveFacultyMatchOrListed() {
        Speciality nursing = new Speciality("Nursing", "NUR", "dept", null, null);
        nursing.setId(1L);
        Speciality other = new Speciality("Other", "OTH", "dept", null, null);
        other.setId(2L);
        Subject subject = new Subject("Nursing Foundations", "NF101", 4, 3, 1, nursing, 1);
        subject.setId(1L);

        Faculty ineligible = activeFaculty(800L, other, 6);
        when(facultyRepository.findByStatus(FacultyStatus.ACTIVE)).thenReturn(List.of(ineligible));

        assertThat(service.hasEligibleFacultyPool(subject)).isFalse();
    }

    @Test
    void hasEligibleFacultyPool_trueWhenSpecialityMatchExists() {
        Speciality nursing = new Speciality("Nursing", "NUR", "dept", null, null);
        nursing.setId(1L);
        Subject subject = new Subject("Nursing Foundations", "NF101", 4, 3, 1, nursing, 1);
        subject.setId(1L);

        Faculty matching = activeFaculty(500L, nursing, 6);
        when(facultyRepository.findByStatus(FacultyStatus.ACTIVE)).thenReturn(List.of(matching));

        assertThat(service.hasEligibleFacultyPool(subject)).isTrue();
    }

    @Test
    void getEligibleFacultyForOffering_excludesIneligibleFaculty_sortedMostFreeFirst() {
        Speciality nursing = new Speciality("Nursing", "NUR", "dept", null, null);
        nursing.setId(1L);
        Speciality other = new Speciality("Other", "OTH", "dept", null, null);
        other.setId(2L);
        Subject subject = new Subject("Nursing Foundations", "NF101", 4, 3, 1, nursing, 1);
        subject.setId(1L);

        CourseOffering offering = new CourseOffering();
        offering.setId(100L);
        offering.setSubject(subject);
        offering.setTermInstance(termInstance);
        when(courseOfferingRepository.findById(100L)).thenReturn(Optional.of(offering));

        Faculty lessFree = activeFaculty(500L, nursing, 2); // 200h capacity
        Faculty moreFree = activeFaculty(600L, nursing, 6); // 600h capacity
        Faculty widened = activeFaculty(700L, other, 4); // 400h capacity, via Eligible Faculty list
        Faculty ineligible = activeFaculty(800L, other, 6); // no speciality match, not on the list
        subject.setEligibleFaculty(new java.util.HashSet<>(java.util.Set.of(widened)));
        when(facultyRepository.findByStatus(FacultyStatus.ACTIVE)).thenReturn(List.of(lessFree, moreFree, widened, ineligible));
        when(studentTermEnrollmentRepository.findDistinctCohortIdsByTermInstanceId(10L, EnrollmentStatus.ENROLLED))
            .thenReturn(new HashSet<>());

        List<EligibleFacultyCandidateDto> candidates = service.getEligibleFacultyForOffering(100L);

        assertThat(candidates).extracting(EligibleFacultyCandidateDto::facultyId)
            .containsExactly(600L, 700L, 500L); // most-free-first: 600h, 400h, 200h
        assertThat(candidates).noneMatch(c -> c.facultyId().equals(800L));
        assertThat(candidates).filteredOn(c -> c.facultyId().equals(700L))
            .allSatisfy(c -> assertThat(c.viaEligibleList()).isTrue());
    }

    @Test
    void getEligibleFacultyForOffering_grandfathersCurrentlyAssignedFacultyEvenIfIneligible() {
        // Someone already holding a section/cohort of this offering must never silently disappear
        // from the picker just because they don't (or no longer) pass Speciality/Eligible-List
        // eligibility -- there's no separate pool-curation step anymore, so "currently assigned" is
        // the only grandfathering signal left.
        Speciality nursing = new Speciality("Nursing", "NUR", "dept", null, null);
        nursing.setId(1L);
        Speciality other = new Speciality("Other", "OTH", "dept", null, null);
        other.setId(2L);
        Subject subject = new Subject("Nursing Foundations", "NF101", 4, 3, 1, nursing, 1);
        subject.setId(1L);
        Faculty grandfathered = activeFaculty(900L, other, 6);

        CourseOffering offering = new CourseOffering();
        offering.setId(100L);
        offering.setSubject(subject);
        offering.setTermInstance(termInstance);
        when(courseOfferingRepository.findById(100L)).thenReturn(Optional.of(offering));
        when(facultyRepository.findByStatus(FacultyStatus.ACTIVE)).thenReturn(List.of());
        when(facultyRepository.findById(900L)).thenReturn(Optional.of(grandfathered));
        CourseOfferingSectionFaculty currentlyAssignedRow = new CourseOfferingSectionFaculty();
        currentlyAssignedRow.setFaculty(grandfathered);
        when(courseOfferingSectionFacultyRepository.findByCourseOfferingId(100L)).thenReturn(List.of(currentlyAssignedRow));
        when(studentTermEnrollmentRepository.findDistinctCohortIdsByTermInstanceId(10L, EnrollmentStatus.ENROLLED))
            .thenReturn(new HashSet<>());

        List<EligibleFacultyCandidateDto> candidates = service.getEligibleFacultyForOffering(100L);

        assertThat(candidates).hasSize(1);
        assertThat(candidates.get(0).facultyId()).isEqualTo(900L);
        assertThat(candidates.get(0).currentlyAssigned()).isTrue();
        assertThat(candidates.get(0).specialityMatch()).isFalse();
        assertThat(candidates.get(0).viaEligibleList()).isFalse();
    }

    @Test
    void getEligibleFacultyForSection_grandfathersCurrentSectionHolderEvenIfIneligible() {
        Speciality nursing = new Speciality("Nursing", "NUR", "dept", null, null);
        nursing.setId(1L);
        Speciality other = new Speciality("Other", "OTH", "dept", null, null);
        other.setId(2L);
        Subject subject = new Subject("Nursing Foundations", "NF101", 4, 3, 1, nursing, 1);
        subject.setId(1L);
        Faculty currentHolder = activeFaculty(500L, other, 6); // not speciality-matched

        CourseOffering offering = new CourseOffering();
        offering.setId(100L);
        offering.setSubject(subject);
        offering.setTermInstance(termInstance);
        when(courseOfferingRepository.findById(100L)).thenReturn(Optional.of(offering));
        when(facultyRepository.findByStatus(FacultyStatus.ACTIVE)).thenReturn(List.of());
        when(facultyRepository.findById(500L)).thenReturn(Optional.of(currentHolder));
        CourseOfferingSectionFaculty sectionRow = new CourseOfferingSectionFaculty();
        sectionRow.setFaculty(currentHolder);
        when(courseOfferingSectionFacultyRepository.findByCourseOfferingIdAndCohortSectionId(100L, 1L))
            .thenReturn(Optional.of(sectionRow));
        when(studentTermEnrollmentRepository.findDistinctCohortIdsByTermInstanceId(10L, EnrollmentStatus.ENROLLED))
            .thenReturn(new HashSet<>());

        List<EligibleFacultyCandidateDto> candidates = service.getEligibleFacultyForSection(100L, 1L);

        assertThat(candidates).hasSize(1);
        assertThat(candidates.get(0).facultyId()).isEqualTo(500L);
        assertThat(candidates.get(0).currentlyAssigned()).isTrue();
    }

    @Test
    void checkFacultyCapacityForSection_flagsOverCapacity() {
        Speciality nursing = new Speciality("Nursing", "NUR", "dept", null, null);
        nursing.setId(1L);
        Subject subject = new Subject("Nursing Foundations", "NF101", 4, 3, 1, nursing, 1);
        subject.setId(1L);
        CurriculumSemesterCourse csc = new CurriculumSemesterCourse();
        csc.setTheoryHours(50);
        CourseOffering offering = new CourseOffering();
        offering.setId(100L);
        offering.setSubject(subject);
        offering.setTermInstance(termInstance);
        offering.setCurriculumSemesterCourse(csc);
        when(courseOfferingRepository.findById(100L)).thenReturn(Optional.of(offering));
        when(courseOfferingSectionFacultyRepository.findByCourseOfferingIdAndCohortSectionId(100L, 1L))
            .thenReturn(Optional.empty());

        Faculty candidate = activeFaculty(500L, nursing, 0); // 0h capacity -- any assignment is over
        when(facultyRepository.findById(500L)).thenReturn(Optional.of(candidate));
        when(studentTermEnrollmentRepository.findDistinctCohortIdsByTermInstanceId(10L, EnrollmentStatus.ENROLLED))
            .thenReturn(new HashSet<>());

        FacultyCapacityCheckResult result = service.checkFacultyCapacityForSection(100L, 1L, 500L);

        assertThat(result.overCapacity()).isTrue();
        assertThat(result.offeringHours()).isEqualTo(50.0);
        // Regression for the real-world confusion this caused: with 50-minute periods (period1,
        // this suite's default fixture -- NOT the one-hour override other tests opt into), a
        // plausible-looking round number of hours does not translate 1:1 to sessions/periods, the
        // unit the Raise Cap field (Faculty#plannedDailySessionsOverride) actually accepts. Asserts
        // the conversion invariant directly (ceil against the real period duration) rather than a
        // hardcoded session count, since this test doesn't otherwise pin workingDaysInTerm.
        double avgPeriodHours = 50.0 / 60.0;
        assertThat(result.suggestedMinDailySessions())
            .isEqualTo((int) Math.ceil(result.suggestedMinDailyHours() / avgPeriodHours));
    }

    @Test
    void runResolvesEachSectionsOwnFacultyOverride_leavesUnoverriddenSectionsUnplaced() {
        // Each split section is now assigned strictly on its own -- an unoverridden section has no
        // offering-wide primary to fall back to anymore, so it's reported unplaced rather than
        // silently defaulting to somebody.
        when(studentTermEnrollmentRepository.findDistinctCohortIdsByTermInstanceId(10L, EnrollmentStatus.ENROLLED))
            .thenReturn(new HashSet<>(List.of(1L)));
        cohort(1L, "Cohort 1");
        Faculty sectionBFaculty = new Faculty();
        sectionBFaculty.setId(700L);
        when(courseOfferingService.getOfferingsByTermInstanceAndCohort(10L, 1L)).thenReturn(List.of(offeringDto(100L, "Offering A")));
        offeringEntity(100L, 10, 0, 0);
        when(timetableSkeletonService.resolveActiveSections(1L, 10L)).thenReturn(List.of());
        when(batchRepository.findByCourseOfferingId(anyLong())).thenReturn(List.of());

        CourseOfferingSectionFaculty override = new CourseOfferingSectionFaculty();
        override.setFaculty(sectionBFaculty);
        when(courseOfferingSectionFacultyRepository.findByCourseOfferingIdAndCohortSectionId(100L, 2L))
            .thenReturn(Optional.of(override));
        when(courseOfferingSectionFacultyRepository.findByCourseOfferingIdAndCohortSectionId(100L, 1L))
            .thenReturn(Optional.empty());

        SkeletonSubjectBudget budgetSectionA = new SkeletonSubjectBudget(ClassSessionType.THEORY, null, null, 1L, "A", 10, 10, 1, 0);
        SkeletonSubjectBudget budgetSectionB = new SkeletonSubjectBudget(ClassSessionType.THEORY, null, null, 2L, "B", 10, 10, 1, 0);
        SkeletonSubjectResponse subject = new SkeletonSubjectResponse(100L, "Offering A", "OFFE", List.of(budgetSectionA, budgetSectionB), null, null);
        SkeletonBuilderResponse skeleton = new SkeletonBuilderResponse(1L, "Cohort 1", "Term", List.of(subject), List.of(), List.of(), List.of(), 25, 0L, List.of(), false, List.of());
        when(timetableSkeletonService.getCohortSkeleton(10L, 1L)).thenReturn(skeleton);

        SkeletonCellResponse placedB = new SkeletonCellResponse(902L, ClassSessionType.THEORY, DayOfWeek.MONDAY, 1L, "1st Period",
            LocalTime.of(9, 0), LocalTime.of(9, 50), null, null, 2L, "B", false, null, null, List.of(),
            100L, "Offering A", "OFFE", null, null, null, false, false);
        when(timetableSkeletonService.placeCell(argThat(r -> r != null && java.util.Objects.equals(r.cohortSectionId(), 2L))))
            .thenReturn(placedB);

        var result = service.runGlobalAutoSchedule(10L, null);

        assertThat(result.totalPlaced()).isEqualTo(1);
        verify(timetableStaffingService).staffCell(902L, new StaffingAssignmentRequest(700L, null));
        verify(timetableStaffingService, never()).staffCell(eq(901L), any());
        assertThat(result.cohortSummaries().get(0).unplaced())
            .extracting(AutoPlaceUnplacedItem::reason)
            .contains("no faculty assigned on its Course Offering");
    }

    // --- reconcileUnplacedAgainstFinalPlacements: the report reflects the FINISHED grid ---------

    private static final String DISPLACED = "displaced during a backtrack attempt and could not be placed at its "
        + "original slot or any other free day/period";

    private static TimetableGlobalAutoScheduleService.Placement theoryPlacement(Long offeringId, Long sectionId) {
        return new TimetableGlobalAutoScheduleService.Placement(1L, offeringId, ClassSessionType.THEORY, null, sectionId,
            36L, "Pathology I", "Section 1", DayOfWeek.FRIDAY, List.of(6L));
    }

    private static SkeletonSubjectBudget sectionTheoryBudget(Long sectionId, String label, int required, int placedBeforeRun) {
        return new SkeletonSubjectBudget(ClassSessionType.THEORY, null, null, sectionId, label, 20, 26, required, placedBeforeRun);
    }

    /** Regression for 2026-09-18: real evidence from a BSc Nursing 2026-2027 ODD run showed two
     *  Theory subjects reported "displaced during a backtrack attempt... could not be placed at its
     *  original slot or any other free day/period" while the cohort's own report showed hundreds of
     *  term-wide spare hours -- the exact complaint that follow-up fix targets. Root cause: {@code
     *  attemptBacktrack}'s own retry (placing the row a backtrack was run FOR) already tries a full
     *  ranked fallback-faculty pool, but {@code restoreBumpedOrReportUnplaced} -- putting the BUMPED
     *  session back somewhere -- used to try ONLY that session's own original bound faculty, never a
     *  fallback, so a bumped session was reported unplaced the moment its own faculty happened to be
     *  unavailable at the one day still genuinely open for it, even though another already-eligible
     *  faculty member was free right there.
     *
     * <p>Builds a fully deterministic 5-day x 3-period grid (15 slots) with four Theory offerings so
     *  every step is forced, not incidental: BIG and FILLER (5 sessions/week each) fully occupy
     *  periods 1 and 2 on every weekday; FILLER2 (4 sessions/week, the row that ends up bumped) takes
     *  period 3 on Monday-Thursday, leaving Friday's period 3 as the week's only untouched slot;
     *  TRIGGER (1 session/week, restricted via a Speciality to its own sole faculty 900) finds every
     *  day fully booked except Friday, where its only eligible faculty is stubbed unavailable --
     *  forcing {@code attemptBacktrack} to fire, which bumps FILLER2's Thursday session and places
     *  TRIGGER there instead. FILLER2's own bound faculty (650) is then stubbed unavailable on Friday
     *  too (its only remaining candidate day), so restoring it can only succeed via a fallback. */
    @Test
    void restoreBumpedSessionTriesFallbackFaculty_whenItsOwnOriginalFacultyIsUnavailable() {
        when(studentTermEnrollmentRepository.findDistinctCohortIdsByTermInstanceId(10L, EnrollmentStatus.ENROLLED))
            .thenReturn(new HashSet<>(List.of(1L)));
        cohort(1L, "Cohort 1");

        Speciality sp1 = new Speciality();
        sp1.setId(1L);
        sp1.setName("Restricted");
        sp1.setCode("SP1");
        Speciality sp2 = new Speciality();
        sp2.setId(2L);
        sp2.setName("General");
        sp2.setCode("SP2");

        // Not every one of these five ends up looked up individually via facultyRepository.findById
        // (only whichever lands in a facultySubstitutionTip does, via facultyDisplayName) -- lenient
        // so the ones that don't aren't flagged as unnecessary stubbing.
        Faculty f500 = facultyWithDailyCap(500L, "Big", 8);
        f500.setSpeciality(sp2);
        Faculty f600 = facultyWithDailyCap(600L, "Filler", 8);
        f600.setSpeciality(sp2);
        Faculty f650 = facultyWithDailyCap(650L, "Filler2", 8);
        f650.setSpeciality(sp2);
        Faculty f700 = facultyWithDailyCap(700L, "Fallback", 8);
        f700.setSpeciality(sp2);
        Faculty f900 = facultyWithDailyCap(900L, "Trigger", 8);
        f900.setSpeciality(sp1);
        lenient().when(facultyRepository.findById(500L)).thenReturn(Optional.of(f500));
        lenient().when(facultyRepository.findById(600L)).thenReturn(Optional.of(f600));
        lenient().when(facultyRepository.findById(650L)).thenReturn(Optional.of(f650));
        lenient().when(facultyRepository.findById(700L)).thenReturn(Optional.of(f700));
        lenient().when(facultyRepository.findById(900L)).thenReturn(Optional.of(f900));
        when(facultyRepository.findByStatus(FacultyStatus.ACTIVE)).thenReturn(List.of(f500, f600, f650, f700, f900));

        when(courseOfferingService.getOfferingsByTermInstanceAndCohort(10L, 1L)).thenReturn(List.of(
            offeringDto(100L, "Bignis"), offeringDto(200L, "Filler"), offeringDto(300L, "Filler2"), offeringDto(400L, "Trigger")));
        assignWholeCohort(100L, 1L, 500L);
        assignWholeCohort(200L, 1L, 600L);
        assignWholeCohort(300L, 1L, 650L);
        assignWholeCohort(400L, 1L, 900L);
        offeringEntity(100L, 20, 0, 0);
        offeringEntity(200L, 20, 0, 0);
        offeringEntity(300L, 20, 0, 0);
        CourseOffering triggerOffering = offeringEntity(400L, 20, 0, 0);
        Subject triggerSubject = new Subject();
        triggerSubject.setId(50L);
        triggerSubject.setSpeciality(sp1);
        triggerOffering.setSubject(triggerSubject);

        when(timetableSkeletonService.resolveActiveSections(1L, 10L)).thenReturn(List.of());
        when(batchRepository.findByCourseOfferingId(anyLong())).thenReturn(List.of());

        Period p2 = new Period("2nd Period", LocalTime.of(9, 50), LocalTime.of(10, 40), 2);
        p2.setId(2L);
        Period p3 = new Period("3rd Period", LocalTime.of(10, 40), LocalTime.of(11, 30), 3);
        p3.setId(3L);
        when(periodRepository.findByIsActiveTrueOrderByPeriodOrderAsc()).thenReturn(List.of(period1, p2, p3));

        SkeletonSubjectBudget bigBudget = new SkeletonSubjectBudget(ClassSessionType.THEORY, null, null, null, null, 20, 10, 5, 0);
        SkeletonSubjectBudget fillerBudget = new SkeletonSubjectBudget(ClassSessionType.THEORY, null, null, null, null, 20, 10, 5, 0);
        SkeletonSubjectBudget filler2Budget = new SkeletonSubjectBudget(ClassSessionType.THEORY, null, null, null, null, 20, 10, 4, 0);
        SkeletonSubjectBudget triggerBudget = new SkeletonSubjectBudget(ClassSessionType.THEORY, null, null, null, null, 20, 10, 1, 0);
        SkeletonBuilderResponse skeleton = new SkeletonBuilderResponse(1L, "Cohort 1", "Term", List.of(
            new SkeletonSubjectResponse(100L, "Big", "BIG", List.of(bigBudget), null, null),
            new SkeletonSubjectResponse(200L, "Filler", "FILL", List.of(fillerBudget), null, null),
            new SkeletonSubjectResponse(300L, "Filler2", "FIL2", List.of(filler2Budget), null, null),
            new SkeletonSubjectResponse(400L, "Trigger", "TRIG", List.of(triggerBudget), null, null)),
            List.of(), List.of(), List.of(), 25, 0L, List.of(), false, List.of());
        when(timetableSkeletonService.getCohortSkeleton(10L, 1L)).thenReturn(skeleton);

        // Occupancy-aware placeCell: tracks which (day, periodId) slots are taken, exactly as the
        // real service would reject an already-occupied one -- the same mechanic used by
        // runPlacesRigidMultiPeriodBlocksBeforeGreedyFiller_soFillerCanNeverStrandThem above.
        java.util.Map<Long, SkeletonCellPlacementRequest> placedRequestsByCellId = new java.util.concurrent.ConcurrentHashMap<>();
        Set<String> occupiedSlots = new HashSet<>();
        java.util.concurrent.atomic.AtomicLong nextCellId = new java.util.concurrent.atomic.AtomicLong(900L);
        org.mockito.stubbing.Answer<SkeletonCellResponse> occupancyAwarePlace = invocation -> {
            SkeletonCellPlacementRequest request = invocation.getArgument(0);
            List<Long> blockPeriodIds = new ArrayList<>();
            blockPeriodIds.add(request.periodId());
            if (request.spanPeriodIds() != null) {
                blockPeriodIds.addAll(request.spanPeriodIds());
            }
            for (Long periodId : blockPeriodIds) {
                if (occupiedSlots.contains(request.dayOfWeek() + ":" + periodId)) {
                    throw new TimetableConstraintViolationException(List.of(new com.cms.dto.ConstraintViolation(
                        "SKELETON_CELL_COHORT_CLASH", "already occupied")));
                }
            }
            blockPeriodIds.forEach(periodId -> occupiedSlots.add(request.dayOfWeek() + ":" + periodId));
            long cellId = nextCellId.incrementAndGet();
            placedRequestsByCellId.put(cellId, request);
            return new SkeletonCellResponse(cellId, request.sessionType(), request.dayOfWeek(), request.periodId(),
                "Period", LocalTime.of(9, 0), LocalTime.of(9, 50), request.batchId(), "Batch", request.cohortSectionId(), null,
                false, null, null, List.of(), request.courseOfferingId(), "Subject", "SUBJ", null, null, null, false, false);
        };
        when(timetableSkeletonService.placeCell(any(SkeletonCellPlacementRequest.class))).thenAnswer(occupancyAwarePlace);
        org.mockito.stubbing.Answer<Void> freeCell = invocation -> {
            Long cellId = invocation.getArgument(0);
            SkeletonCellPlacementRequest request = placedRequestsByCellId.remove(cellId);
            if (request != null) {
                List<Long> blockPeriodIds = new ArrayList<>();
                blockPeriodIds.add(request.periodId());
                if (request.spanPeriodIds() != null) {
                    blockPeriodIds.addAll(request.spanPeriodIds());
                }
                blockPeriodIds.forEach(periodId -> occupiedSlots.remove(request.dayOfWeek() + ":" + periodId));
            }
            return null;
        };
        org.mockito.Mockito.doAnswer(freeCell).when(timetableSkeletonService).removeCell(anyLong());
        org.mockito.Mockito.doAnswer(freeCell).when(timetableSkeletonService).forceRemoveCell(anyLong());

        // Faculty 650 (Filler2's own) and 900 (Trigger's own, and only eligible) are both stubbed
        // unavailable specifically on Friday -- everyone else (including the fallback pool) is fine
        // any day, so a successful Friday restore can only happen via a fallback faculty.
        Set<Long> fridayRestrictedFaculty = Set.of(650L, 900L);
        when(timetableStaffingService.staffCell(anyLong(), any(StaffingAssignmentRequest.class))).thenAnswer(invocation -> {
            Long cellId = invocation.getArgument(0);
            StaffingAssignmentRequest request = invocation.getArgument(1);
            SkeletonCellPlacementRequest placedRequest = placedRequestsByCellId.get(cellId);
            DayOfWeek day = placedRequest != null ? placedRequest.dayOfWeek() : null;
            if (day == DayOfWeek.FRIDAY && fridayRestrictedFaculty.contains(request.facultyId())) {
                throw new TimetableConstraintViolationException(List.of(new com.cms.dto.ConstraintViolation(
                    "STAFFING_WORKLOAD_DAILY_CAP_EXCEEDED", "unavailable on Friday")));
            }
            return new UnstaffedCellResponse(cellId, 100L, "Subject", "SUBJ", null, null,
                ClassSessionType.THEORY, day, 1L, "Period", LocalTime.of(9, 0), LocalTime.of(9, 50),
                null, null, null, null, null, false, List.of(), null, null);
        });

        var result = service.runGlobalAutoSchedule(10L, null);

        // Filler2's bumped Thursday session was successfully restored -- via a fallback faculty,
        // since its own (650) is unavailable on the only day still open for it (Friday) -- so it must
        // NOT show up in the unplaced list, and a substitution tip must explain why a different
        // faculty ended up covering it.
        assertThat(result.cohortSummaries()).hasSize(1);
        assertThat(result.cohortSummaries().get(0).unplaced())
            .noneMatch(item -> "Filler2".equals(item.subjectName()) && item.slotShortfall());
        assertThat(result.facultySubstitutionTips())
            .anyMatch(tip -> tip.courseOfferingId().equals(300L) && tip.originalFacultyId().equals(650L)
                && !tip.substituteFacultyId().equals(650L));
    }

    /** Regression for 2026-09-18 (second fix, same real BSc Nursing 2026-2027 ODD run): the first
     *  fix above made a bumped session's restore try a fallback faculty pool, but when NO fallback
     *  exists either (a genuine, real Monday-Friday room ceiling for that specific subject), the
     *  session is still permanently lost -- and that loss used to vanish completely from {@code
     *  theoryStillOwedRuns}, since that map is only ever written from {@code placeShortfallRow}'s
     *  own return value for whichever row is CURRENTLY being placed, never from a row that already
     *  finished its own turn earlier and only lost a session later to a DIFFERENT row's backtrack.
     *  The real-world consequence: the same cohort's run then handed out a bonus second Library
     *  session (gated on {@code theoryStillOwedRuns} showing "0 owed" -- see the first Library-bonus
     *  fix) immediately after this exact mechanism had just reopened a genuine Theory gap, and this
     *  repeated identically across three separate restarts+re-runs because the gate could never see
     *  what {@code restoreBumpedOrReportUnplaced} only discovered after the gate had already run.
     *
     * <p>Same grid shape as the test above, except Filler2's subject is now eligibility-restricted
     *  to its own sole faculty (a third Speciality only faculty 650 holds) -- so unlike that test, no
     *  fallback exists and the restore permanently fails, exactly like the real run's two stranded
     *  subjects. A Library classroom is configured with the bonus session's free-period threshold
     *  trivially met, so the old bug (Library fires anyway) and the fix (Library withheld entirely
     *  because {@code theoryStillOwedRuns} now correctly shows Filler2's reopened gap, and Library's
     *  own required quota -- not just its bonus -- is now gated on it too) are distinguishable by a
     *  single count: zero Library blocks placed, not one or two. */
    @Test
    void bumpedTheorySessionWithNoFallback_reopensTheoryStillOwedRuns_soLibraryIsWithheldEntirely() {
        when(studentTermEnrollmentRepository.findDistinctCohortIdsByTermInstanceId(10L, EnrollmentStatus.ENROLLED))
            .thenReturn(new HashSet<>(List.of(1L)));
        cohort(1L, "Cohort 1");

        Speciality sp1 = new Speciality();
        sp1.setId(1L);
        sp1.setName("Restricted");
        sp1.setCode("SP1");
        Speciality sp2 = new Speciality();
        sp2.setId(2L);
        sp2.setName("General");
        sp2.setCode("SP2");
        Speciality sp3 = new Speciality();
        sp3.setId(3L);
        sp3.setName("Filler2Only");
        sp3.setCode("SP3");

        Faculty f500 = facultyWithDailyCap(500L, "Big", 8);
        f500.setSpeciality(sp2);
        Faculty f600 = facultyWithDailyCap(600L, "Filler", 8);
        f600.setSpeciality(sp2);
        Faculty f650 = facultyWithDailyCap(650L, "Filler2", 8);
        f650.setSpeciality(sp3);
        Faculty f900 = facultyWithDailyCap(900L, "Trigger", 8);
        f900.setSpeciality(sp1);
        lenient().when(facultyRepository.findById(500L)).thenReturn(Optional.of(f500));
        lenient().when(facultyRepository.findById(600L)).thenReturn(Optional.of(f600));
        lenient().when(facultyRepository.findById(650L)).thenReturn(Optional.of(f650));
        lenient().when(facultyRepository.findById(900L)).thenReturn(Optional.of(f900));
        when(facultyRepository.findByStatus(FacultyStatus.ACTIVE)).thenReturn(List.of(f500, f600, f650, f900));

        when(courseOfferingService.getOfferingsByTermInstanceAndCohort(10L, 1L)).thenReturn(List.of(
            offeringDto(100L, "Bignis"), offeringDto(200L, "Filler"), offeringDto(300L, "Filler2"), offeringDto(400L, "Trigger")));
        assignWholeCohort(100L, 1L, 500L);
        assignWholeCohort(200L, 1L, 600L);
        assignWholeCohort(300L, 1L, 650L);
        assignWholeCohort(400L, 1L, 900L);
        offeringEntity(100L, 20, 0, 0);
        offeringEntity(200L, 20, 0, 0);
        CourseOffering filler2Offering = offeringEntity(300L, 20, 0, 0);
        Subject filler2Subject = new Subject();
        filler2Subject.setId(51L);
        filler2Subject.setSpeciality(sp3);
        filler2Offering.setSubject(filler2Subject);
        CourseOffering triggerOffering = offeringEntity(400L, 20, 0, 0);
        Subject triggerSubject = new Subject();
        triggerSubject.setId(50L);
        triggerSubject.setSpeciality(sp1);
        triggerOffering.setSubject(triggerSubject);

        when(timetableSkeletonService.resolveActiveSections(1L, 10L)).thenReturn(List.of());
        when(batchRepository.findByCourseOfferingId(anyLong())).thenReturn(List.of());

        Period p2 = new Period("2nd Period", LocalTime.of(9, 50), LocalTime.of(10, 40), 2);
        p2.setId(2L);
        Period p3 = new Period("3rd Period", LocalTime.of(10, 40), LocalTime.of(11, 30), 3);
        p3.setId(3L);
        when(periodRepository.findByIsActiveTrueOrderByPeriodOrderAsc()).thenReturn(List.of(period1, p2, p3));

        SkeletonSubjectBudget bigBudget = new SkeletonSubjectBudget(ClassSessionType.THEORY, null, null, null, null, 20, 10, 5, 0);
        SkeletonSubjectBudget fillerBudget = new SkeletonSubjectBudget(ClassSessionType.THEORY, null, null, null, null, 20, 10, 5, 0);
        SkeletonSubjectBudget filler2Budget = new SkeletonSubjectBudget(ClassSessionType.THEORY, null, null, null, null, 20, 10, 4, 0);
        SkeletonSubjectBudget triggerBudget = new SkeletonSubjectBudget(ClassSessionType.THEORY, null, null, null, null, 20, 10, 1, 0);
        SkeletonBuilderResponse skeleton = new SkeletonBuilderResponse(1L, "Cohort 1", "Term", List.of(
            new SkeletonSubjectResponse(100L, "Big", "BIG", List.of(bigBudget), null, null),
            new SkeletonSubjectResponse(200L, "Filler", "FILL", List.of(fillerBudget), null, null),
            new SkeletonSubjectResponse(300L, "Filler2", "FIL2", List.of(filler2Budget), null, null),
            new SkeletonSubjectResponse(400L, "Trigger", "TRIG", List.of(triggerBudget), null, null)),
            List.of(), List.of(), List.of(), 25, 0L, List.of(), false, List.of());
        when(timetableSkeletonService.getCohortSkeleton(10L, 1L)).thenReturn(skeleton);

        java.util.Map<Long, SkeletonCellPlacementRequest> placedRequestsByCellId = new java.util.concurrent.ConcurrentHashMap<>();
        Set<String> occupiedSlots = new HashSet<>();
        java.util.concurrent.atomic.AtomicLong nextCellId = new java.util.concurrent.atomic.AtomicLong(900L);
        org.mockito.stubbing.Answer<SkeletonCellResponse> occupancyAwarePlace = invocation -> {
            SkeletonCellPlacementRequest request = invocation.getArgument(0);
            List<Long> blockPeriodIds = new ArrayList<>();
            blockPeriodIds.add(request.periodId());
            if (request.spanPeriodIds() != null) {
                blockPeriodIds.addAll(request.spanPeriodIds());
            }
            for (Long periodId : blockPeriodIds) {
                if (occupiedSlots.contains(request.dayOfWeek() + ":" + periodId)) {
                    throw new TimetableConstraintViolationException(List.of(new com.cms.dto.ConstraintViolation(
                        "SKELETON_CELL_COHORT_CLASH", "already occupied")));
                }
            }
            blockPeriodIds.forEach(periodId -> occupiedSlots.add(request.dayOfWeek() + ":" + periodId));
            long cellId = nextCellId.incrementAndGet();
            placedRequestsByCellId.put(cellId, request);
            return new SkeletonCellResponse(cellId, request.sessionType(), request.dayOfWeek(), request.periodId(),
                "Period", LocalTime.of(9, 0), LocalTime.of(9, 50), request.batchId(), "Batch", request.cohortSectionId(), null,
                false, null, null, List.of(), request.courseOfferingId(), "Subject", "SUBJ", null, null, null, false, false);
        };
        when(timetableSkeletonService.placeCell(any(SkeletonCellPlacementRequest.class))).thenAnswer(occupancyAwarePlace);
        org.mockito.stubbing.Answer<Void> freeCell = invocation -> {
            Long cellId = invocation.getArgument(0);
            SkeletonCellPlacementRequest request = placedRequestsByCellId.remove(cellId);
            if (request != null) {
                List<Long> blockPeriodIds = new ArrayList<>();
                blockPeriodIds.add(request.periodId());
                if (request.spanPeriodIds() != null) {
                    blockPeriodIds.addAll(request.spanPeriodIds());
                }
                blockPeriodIds.forEach(periodId -> occupiedSlots.remove(request.dayOfWeek() + ":" + periodId));
            }
            return null;
        };
        org.mockito.Mockito.doAnswer(freeCell).when(timetableSkeletonService).removeCell(anyLong());
        org.mockito.Mockito.doAnswer(freeCell).when(timetableSkeletonService).forceRemoveCell(anyLong());

        // Faculty 650 (Filler2's own -- and only eligible, no fallback exists) and 900 (Trigger's
        // own, and only eligible) are both stubbed unavailable specifically on Friday, forcing the
        // exact same backtrack as the test above, but with no fallback for Filler2's restore to fall
        // back on -- its loss is permanent.
        Set<Long> fridayRestrictedFaculty = Set.of(650L, 900L);
        when(timetableStaffingService.staffCell(anyLong(), any(StaffingAssignmentRequest.class))).thenAnswer(invocation -> {
            Long cellId = invocation.getArgument(0);
            StaffingAssignmentRequest request = invocation.getArgument(1);
            SkeletonCellPlacementRequest placedRequest = placedRequestsByCellId.get(cellId);
            DayOfWeek day = placedRequest != null ? placedRequest.dayOfWeek() : null;
            if (day == DayOfWeek.FRIDAY && fridayRestrictedFaculty.contains(request.facultyId())) {
                throw new TimetableConstraintViolationException(List.of(new com.cms.dto.ConstraintViolation(
                    "STAFFING_WORKLOAD_DAILY_CAP_EXCEEDED", "unavailable on Friday")));
            }
            return new UnstaffedCellResponse(cellId, 100L, "Subject", "SUBJ", null, null,
                ClassSessionType.THEORY, day, 1L, "Period", LocalTime.of(9, 0), LocalTime.of(9, 50),
                null, null, null, null, null, false, List.of(), null, null);
        });

        // Library: one classroom, default 1/week quota, and a trivially-met bonus threshold -- the
        // old bug hands out the bonus regardless of Filler2's reopened gap; the fix withholds it.
        Subject librarySubject = new Subject();
        librarySubject.setId(999L);
        librarySubject.setCode("SYSTEM-LIBRARY");
        when(subjectRepository.findByCode("SYSTEM-LIBRARY")).thenReturn(Optional.of(librarySubject));
        Classroom libraryRoom = new Classroom("Library Hall", null, null, 200);
        libraryRoom.setId(50L);
        when(classroomRepository.findByIsActiveTrueAndRoom_PurposeCategory_CodeOrderByNameAsc(any()))
            .thenReturn(List.of(libraryRoom));
        // lenient(): the theoryStillOwedRuns gate now returns before Library ever checks slot
        // freedom or saves a cell.
        lenient().when(timetableSkeletonService.isSlotFreeForCohort(eq(1L), eq(10L), any(), any())).thenReturn(true);
        libraryConfig("timetable.library_extra_session_min_free_periods", "1");
        List<DayOfWeek> libraryBlockDays = new ArrayList<>();
        lenient().when(timetableSkeletonService.saveLibraryBlockCells(any(), any(), any(), any(), any(), any()))
            .thenAnswer(inv -> {
                DayOfWeek day = inv.getArgument(2);
                libraryBlockDays.add(day);
                @SuppressWarnings("unchecked")
                List<Period> block = (List<Period>) inv.getArgument(3);
                List<ClassSchedule> saved = new ArrayList<>();
                for (Period period : block) {
                    ClassSchedule cs = new ClassSchedule();
                    cs.setSessionType(ClassSessionType.LIBRARY);
                    cs.setDayOfWeek(day);
                    cs.setPeriod(period);
                    cs.setId(950L + idSequence.getAndIncrement());
                    saved.add(cs);
                }
                return saved;
            });

        var result = service.runGlobalAutoSchedule(10L, null);

        // Filler2's bumped Thursday session had no fallback and is permanently lost -- it must show
        // up as a genuine shortfall...
        assertThat(result.cohortSummaries()).hasSize(1);
        assertThat(result.cohortSummaries().get(0).unplaced())
            .anyMatch(item -> "Filler2".equals(item.subjectName()) && item.slotShortfall());
        // ...and that reopened gap must be visible to Library's own gate: neither the required
        // session nor the bonus second one is placed, even though the free-period threshold alone
        // (met trivially above) would otherwise allow the bonus.
        assertThat(libraryBlockDays).isEmpty();
    }

    @Test
    void displacedSessionThatWasReplacedLaterInTheRunIsNotReportedUnplaced() {
        // The real 2025-2029 incident: Pathology I was bumped by a backtrack and logged "displaced",
        // then placed again later in the same run -- the grid was fine, the report still said unplaced.
        SkeletonSubjectResponse pathology = new SkeletonSubjectResponse(100L, "Pathology I", "PATH",
            List.of(sectionTheoryBudget(51L, "Section 1", 1, 0)), null, null);
        List<AutoPlaceUnplacedItem> logged = List.of(
            new AutoPlaceUnplacedItem("Pathology I", ClassSessionType.THEORY, "Section 1", DISPLACED, 100L, true, false));

        List<AutoPlaceUnplacedItem> reconciled = TimetableGlobalAutoScheduleService.reconcileUnplacedAgainstFinalPlacements(
            logged, List.of(pathology), List.of(theoryPlacement(100L, 51L)), termInstance);

        assertThat(reconciled).isEmpty();
    }

    @Test
    void pinnedSessionsSurvivingThePurgeCountTowardTheQuota() {
        SkeletonSubjectResponse pathology = new SkeletonSubjectResponse(100L, "Pathology I", "PATH",
            List.of(sectionTheoryBudget(51L, "Section 1", 1, 2)), null, null);
        List<AutoPlaceUnplacedItem> logged = List.of(
            new AutoPlaceUnplacedItem("Pathology I", ClassSessionType.THEORY, "Section 1", DISPLACED, 100L, true, false));

        assertThat(TimetableGlobalAutoScheduleService.reconcileUnplacedAgainstFinalPlacements(
            logged, List.of(pathology), List.of(), termInstance)).isEmpty();
    }

    @Test
    void genuinelyShortSubjectKeepsOnlyAsManyItemsAsSessionsStillOwed() {
        // Needs 3/week, 1 placed this run -> 2 still owed; 3 failure events were logged along the way.
        SkeletonSubjectResponse chn = new SkeletonSubjectResponse(100L, "Community Health Nursing I", "CHN",
            List.of(sectionTheoryBudget(51L, "Section 1", 3, 0)), null, null);
        List<AutoPlaceUnplacedItem> logged = List.of(
            new AutoPlaceUnplacedItem("Community Health Nursing I", ClassSessionType.THEORY, "Section 1", "first", 100L, true, false),
            new AutoPlaceUnplacedItem("Community Health Nursing I", ClassSessionType.THEORY, "Section 1", "second", 100L, true, false),
            new AutoPlaceUnplacedItem("Community Health Nursing I", ClassSessionType.THEORY, "Section 1", "third", 100L, true, false));

        List<AutoPlaceUnplacedItem> reconciled = TimetableGlobalAutoScheduleService.reconcileUnplacedAgainstFinalPlacements(
            logged, List.of(chn), List.of(theoryPlacement(100L, 51L)), termInstance);

        assertThat(reconciled).extracting(AutoPlaceUnplacedItem::reason).containsExactly("first", "second");
    }

    @Test
    void eachSectionIsReconciledAgainstItsOwnQuota() {
        SkeletonSubjectResponse subject = new SkeletonSubjectResponse(100L, "Applied Anatomy", "ANAT",
            List.of(sectionTheoryBudget(51L, "Section 1", 1, 0), sectionTheoryBudget(52L, "Section 2", 1, 0)), null, null);
        List<AutoPlaceUnplacedItem> logged = List.of(
            new AutoPlaceUnplacedItem("Applied Anatomy", ClassSessionType.THEORY, "Section 1", DISPLACED, 100L, true, false),
            new AutoPlaceUnplacedItem("Applied Anatomy", ClassSessionType.THEORY, "Section 2", DISPLACED, 100L, true, false));

        List<AutoPlaceUnplacedItem> reconciled = TimetableGlobalAutoScheduleService.reconcileUnplacedAgainstFinalPlacements(
            logged, List.of(subject), List.of(theoryPlacement(100L, 51L)), termInstance);

        assertThat(reconciled).extracting(AutoPlaceUnplacedItem::occupantLabel).containsExactly("Section 2");
    }

    @Test
    void labBudgetIsMatchedPerBatch() {
        SkeletonSubjectBudget batchA = new SkeletonSubjectBudget(ClassSessionType.LAB, 285L, "Batch A", 51L, null, 40, 26, 1, 0);
        SkeletonSubjectBudget batchB = new SkeletonSubjectBudget(ClassSessionType.LAB, 286L, "Batch B", 51L, null, 40, 26, 1, 0);
        SkeletonSubjectResponse ahn = new SkeletonSubjectResponse(100L, "Adult Health Nursing I", "AHN",
            List.of(batchA, batchB), null, null);
        TimetableGlobalAutoScheduleService.Placement placedA = new TimetableGlobalAutoScheduleService.Placement(1L, 100L,
            ClassSessionType.LAB, 285L, 51L, 34L, "Adult Health Nursing I", "Batch A", DayOfWeek.TUESDAY, List.of(1L, 2L));
        List<AutoPlaceUnplacedItem> logged = List.of(
            new AutoPlaceUnplacedItem("Adult Health Nursing I", ClassSessionType.LAB, "Batch A", DISPLACED, 100L, true, false),
            new AutoPlaceUnplacedItem("Adult Health Nursing I", ClassSessionType.LAB, "Batch B", DISPLACED, 100L, true, false));

        List<AutoPlaceUnplacedItem> reconciled = TimetableGlobalAutoScheduleService.reconcileUnplacedAgainstFinalPlacements(
            logged, List.of(ahn), List.of(placedA), termInstance);

        assertThat(reconciled).extracting(AutoPlaceUnplacedItem::occupantLabel).containsExactly("Batch B");
    }

    @Test
    void libraryGapFillAndUnmatchedItemsAreNeverReconciledAway() {
        // A subject literally named like the gap-fill note, whole-cohort (null occupant), already at
        // quota: its period-level "N period(s) left empty" note must still survive -- it describes
        // empty periods, not this subject's weekly quota.
        SkeletonSubjectResponse selfStudy = new SkeletonSubjectResponse(100L, "Self-Study/Co-curricular", "SSCC",
            List.of(new SkeletonSubjectBudget(ClassSessionType.THEORY, null, null, null, null, 20, 26, 1, 1)), null, null);
        List<AutoPlaceUnplacedItem> logged = List.of(
            new AutoPlaceUnplacedItem("Library", ClassSessionType.LIBRARY, null, "1 of this cohort's weekly Library session(s)", null, false, true),
            new AutoPlaceUnplacedItem("Self-Study/Co-curricular", ClassSessionType.THEORY, null, "2 period(s) left genuinely empty", 100L, false, true),
            new AutoPlaceUnplacedItem("Gap-fill", ClassSessionType.THEORY, null, "1 period(s) left empty", 100L, false, true),
            new AutoPlaceUnplacedItem("Unknown Subject", ClassSessionType.THEORY, "Section 1", DISPLACED, 999L, true, false));

        List<AutoPlaceUnplacedItem> reconciled = TimetableGlobalAutoScheduleService.reconcileUnplacedAgainstFinalPlacements(
            logged, List.of(selfStudy), List.of(), termInstance);

        assertThat(reconciled).isEqualTo(logged);
    }

    // --- OC-227: Monday-Friday first ---------------------------------------------------------------

    /** SKSCON's real day: 8 x 50-minute periods with a tea break and a lunch break. */
    private static List<Period> skscPeriods() {
        int[][] times = {{9, 0, 9, 50}, {9, 50, 10, 40}, {10, 55, 11, 45}, {11, 45, 12, 35},
            {13, 20, 14, 10}, {14, 10, 15, 0}, {15, 15, 16, 5}, {16, 5, 16, 55}};
        List<Period> periods = new ArrayList<>();
        for (int i = 0; i < times.length; i++) {
            Period p = new Period("Period " + (i + 1), LocalTime.of(times[i][0], times[i][1]), LocalTime.of(times[i][2], times[i][3]), i + 1);
            p.setId((long) (i + 1));
            periods.add(p);
        }
        return periods;
    }

    @Test
    void dutyFitLengthensASixHourShiftToCloseTheHoursAtZeroTimetableCost() {
        // Real Adult Health Nursing I numbers: 480h over three weekly duty groups x 26 weeks at 6h,
        // 07:00 start, 60-minute bus each way -> 468h. 370 minutes closes it, and the bus is back at
        // 14:10 -- exactly when Period 6 starts, so nothing that's free today becomes blocked.
        var fit = TimetableGlobalAutoScheduleService.computeDutyFit(480, 360, 60,
            List.of(LocalTime.of(7, 0), LocalTime.of(7, 0), LocalTime.of(7, 0)), List.of(26, 26, 26), skscPeriods());

        assertThat(fit.suggestedMinutes()).isEqualTo(370);
        assertThat(fit.costsNoPeriods()).isTrue();
    }

    @Test
    void dutyFitFlagsATimetableCostWhenTheLaterBusReachesAFreePeriod() {
        // 400h on two groups x 26 weeks -> 465 minutes; 07:00 + 7h45m + 60 min = 15:45, into Periods 6-7.
        var fit = TimetableGlobalAutoScheduleService.computeDutyFit(400, 360, 60,
            List.of(LocalTime.of(7, 0), LocalTime.of(7, 0)), List.of(26, 26), skscPeriods());

        assertThat(fit.suggestedMinutes()).isEqualTo(465);
        assertThat(fit.costsNoPeriods()).isFalse();
    }

    @Test
    void noDutyFitWhenTheRosterAlreadyDeliversTheHours() {
        // Mental Health Nursing I: 80h, one 6h group x 26 weeks already delivers 156h.
        assertThat(TimetableGlobalAutoScheduleService.computeDutyFit(80, 360, 60,
            List.of(LocalTime.of(7, 0)), List.of(26), skscPeriods())).isNull();
    }

    @Test
    void applyClinicalDutyFitRecomputesTheMinutesServerSideAndSavesThroughTheOffering() {
        CourseOffering offering = offeringEntity(300L, 0, 0, 200);
        offering.setClinicalShiftDurationMinutes(360);
        offering.setClinicalTravelBufferMinutes(60);
        when(clinicalShiftGroupService.getGroupsForOffering(300L)).thenReturn(List.of(new com.cms.dto.ClinicalShiftGroupDto(
            1L, 300L, "Subject", null, null, 10L, "Shift", DayOfWeek.MONDAY, LocalTime.of(7, 0), null, null, null,
            null, null, true, List.of(), List.of(), null, null)));

        service.applyClinicalDutyFit(300L);

        // Fixture term 2025-06-01..2025-11-30 = 27 weeks: 200h x 60 / 27 = 444.4 -> 445 minutes.
        verify(courseOfferingService).updateClinicalShiftConfig(300L,
            new com.cms.dto.ClinicalShiftConfigUpdateRequest(445, 60));
    }

    @Test
    void applyClinicalDutyFitRefusesWhenNothingNeedsFitting() {
        CourseOffering offering = offeringEntity(301L, 0, 0, 80);
        offering.setClinicalShiftDurationMinutes(360);
        when(clinicalShiftGroupService.getGroupsForOffering(301L)).thenReturn(List.of(new com.cms.dto.ClinicalShiftGroupDto(
            1L, 301L, "Subject", null, null, 10L, "Shift", DayOfWeek.MONDAY, LocalTime.of(7, 0), null, null, null,
            null, null, true, List.of(), List.of(), null, null)));

        assertThatThrownBy(() -> service.applyClinicalDutyFit(301L)).isInstanceOf(IllegalStateException.class);
        verify(courseOfferingService, never()).updateClinicalShiftConfig(anyLong(), any());
    }

    /** Hours are planned against the term's total (2026-09-15): the 2026-2027 ODD term's "1st Saturday
     *  only" pattern gives 6 working Saturdays in 26 weeks, so a subject whose one weekly session sits
     *  on Saturday has delivered 6 of its 26 runs and still owes a weekly session. */
    @Test
    void aFirstSaturdayOnlySessionCountsForItsRealRunsInTheReconciledReport() {
        TermInstance term = new TermInstance();
        term.setStartDate(LocalDate.of(2026, 10, 1));
        term.setEndDate(LocalDate.of(2027, 3, 31));
        term.setWorkingSaturdayWeeks(Set.of(WeekOfMonth.FIRST));
        SkeletonSubjectResponse chn = new SkeletonSubjectResponse(100L, "Community Health Nursing I", "CHN",
            List.of(sectionTheoryBudget(51L, "Section 1", 1, 0)), null, null);
        List<AutoPlaceUnplacedItem> logged = List.of(
            new AutoPlaceUnplacedItem("Community Health Nursing I", ClassSessionType.THEORY, "Section 1", "short", 100L, true, false));
        var saturday = new TimetableGlobalAutoScheduleService.Placement(5L, 100L, ClassSessionType.THEORY, null, 51L, 27L,
            "Community Health Nursing I", "Section 1", DayOfWeek.SATURDAY, List.of(1L));

        assertThat(TimetableGlobalAutoScheduleService.reconcileUnplacedAgainstFinalPlacements(
            logged, List.of(chn), List.of(saturday), term)).hasSize(1);

        // Every Saturday working: a Saturday session runs every week, so the quota is met.
        term.setWorkingSaturdayWeeks(java.util.EnumSet.allOf(WeekOfMonth.class));
        assertThat(TimetableGlobalAutoScheduleService.reconcileUnplacedAgainstFinalPlacements(
            logged, List.of(chn), List.of(saturday), term)).isEmpty();
    }

    /** One-cohort, one-subject run with real day+period occupancy, so a slot a session already holds
     *  genuinely refuses the next one. Returns every placement request that succeeded. */
    private List<SkeletonCellPlacementRequest> runSingleSubjectWithOccupancy(int requiredPerWeek, List<Period> periods) {
        termInstance.setWorkingSaturdayWeeks(Set.of(WeekOfMonth.FIRST));
        when(periodRepository.findByIsActiveTrueOrderByPeriodOrderAsc()).thenReturn(periods);
        when(studentTermEnrollmentRepository.findDistinctCohortIdsByTermInstanceId(10L, EnrollmentStatus.ENROLLED))
            .thenReturn(new HashSet<>(List.of(1L)));
        cohort(1L, "Cohort 1");
        facultyWithDailyCap(500L, "XYZ", 8);
        when(courseOfferingService.getOfferingsByTermInstanceAndCohort(10L, 1L)).thenReturn(List.of(offeringDto(100L, "Offering A")));
        assignWholeCohort(100L, 1L, 500L);
        offeringEntity(100L, 20 * requiredPerWeek, 0, 0);
        when(timetableSkeletonService.resolveActiveSections(1L, 10L)).thenReturn(List.of());
        when(batchRepository.findByCourseOfferingId(anyLong())).thenReturn(List.of());
        SkeletonSubjectBudget budget = new SkeletonSubjectBudget(ClassSessionType.THEORY, null, null, null, null,
            20 * requiredPerWeek, 26, requiredPerWeek, 0);
        SkeletonSubjectResponse subject = new SkeletonSubjectResponse(100L, "Offering A", "OFFE", List.of(budget), null, null);
        when(timetableSkeletonService.getCohortSkeleton(10L, 1L)).thenReturn(new SkeletonBuilderResponse(1L, "Cohort 1", "Term",
            List.of(subject), List.of(), List.of(), List.of(), 26, 6L, List.of(), false, List.of()));

        Set<String> occupied = new HashSet<>();
        List<SkeletonCellPlacementRequest> placedRequests = new ArrayList<>();
        java.util.concurrent.atomic.AtomicLong nextCellId = new java.util.concurrent.atomic.AtomicLong(900L);
        when(timetableSkeletonService.placeCell(any(SkeletonCellPlacementRequest.class))).thenAnswer(invocation -> {
            SkeletonCellPlacementRequest request = invocation.getArgument(0);
            if (!occupied.add(request.dayOfWeek() + ":" + request.periodId())) {
                throw new TimetableConstraintViolationException(List.of(new com.cms.dto.ConstraintViolation(
                    "SKELETON_CELL_COHORT_CLASH", "already occupied")));
            }
            placedRequests.add(request);
            return new SkeletonCellResponse(nextCellId.incrementAndGet(), request.sessionType(), request.dayOfWeek(),
                request.periodId(), "Period", LocalTime.of(9, 0), LocalTime.of(9, 50), null, null, null, null, false, null,
                null, List.of(), request.courseOfferingId(), "Offering A", "OFFE", null, null, null, false, false);
        });
        when(timetableStaffingService.staffCell(anyLong(), any(StaffingAssignmentRequest.class)))
            .thenAnswer(invocation -> new UnstaffedCellResponse(invocation.getArgument(0), 100L, "Offering A", "OFFE", null, null,
                ClassSessionType.THEORY, DayOfWeek.MONDAY, 1L, "Period", LocalTime.of(9, 0), LocalTime.of(9, 50),
                null, null, null, null, null, false, List.of(), null, null));
        return placedRequests;
    }

    private static java.util.Map<DayOfWeek, Long> sessionsPerDay(List<SkeletonCellPlacementRequest> placed) {
        return placed.stream().collect(java.util.stream.Collectors.groupingBy(SkeletonCellPlacementRequest::dayOfWeek,
            java.util.stream.Collectors.counting()));
    }

    /** A chosen working Saturday is a regular day, counted for the runs it really has (2026-09-15):
     *  with two free periods a day, the sessions go one a day Monday-Saturday, and because the
     *  first-Saturday session runs 6 times rather than 26 (5 x 26 + 6 = 136 of the 156 runs six
     *  weekly sessions need), the subject gets one more weekday session instead of being left short. */
    @Test
    void aFirstSaturdaySessionCountsItsRealRuns_soTheSubjectGetsOneMoreWeekdaySession() {
        Period period2 = new Period("2nd Period", LocalTime.of(9, 50), LocalTime.of(10, 40), 2);
        period2.setId(2L);
        List<SkeletonCellPlacementRequest> placed = runSingleSubjectWithOccupancy(6, List.of(period1, period2));

        var result = service.runGlobalAutoSchedule(10L, null);

        assertThat(result.totalPlaced()).isEqualTo(7);
        assertThat(sessionsPerDay(placed)).containsOnlyKeys(DayOfWeek.values()).containsEntry(DayOfWeek.SATURDAY, 1L);
        assertThat(sessionsPerDay(placed).values()).filteredOn(count -> count == 2L).hasSize(1);
        assertThat(result.cohortSummaries().get(0).usedSaturday()).isTrue();
        assertThat(result.cohortSummaries().get(0).unplaced()).extracting(AutoPlaceUnplacedItem::subjectName)
            .doesNotContain("Offering A");
    }

    /** "Split evenly": twelve weekly sessions over six working days is two a day, Saturday included. */
    @Test
    void twelveWeeklySessionsSpreadTwoOnEveryWorkingDayIncludingSaturday() {
        Period period2 = new Period("2nd Period", LocalTime.of(9, 50), LocalTime.of(10, 40), 2);
        period2.setId(2L);
        List<SkeletonCellPlacementRequest> placed = runSingleSubjectWithOccupancy(12, List.of(period1, period2));

        var result = service.runGlobalAutoSchedule(10L, null);

        assertThat(result.totalPlaced()).isEqualTo(12);
        assertThat(sessionsPerDay(placed)).containsOnlyKeys(DayOfWeek.values()).allSatisfy((day, count) -> assertThat(count).isEqualTo(2L));
    }

    /** No Saturday pattern chosen: Saturday isn't a working day, so the sixth session doubles up on a
     *  weekday instead. */
    @Test
    void sixthWeeklySessionDoublesUpOnAWeekday_whenNoSaturdayIsChosen() {
        Period period2 = new Period("2nd Period", LocalTime.of(9, 50), LocalTime.of(10, 40), 2);
        period2.setId(2L);
        List<SkeletonCellPlacementRequest> placed = runSingleSubjectWithOccupancy(6, List.of(period1, period2));
        termInstance.setWorkingSaturdayWeeks(Set.of());

        var result = service.runGlobalAutoSchedule(10L, null);

        assertThat(result.totalPlaced()).isEqualTo(6);
        assertThat(placed).extracting(SkeletonCellPlacementRequest::dayOfWeek).doesNotContain(DayOfWeek.SATURDAY);
        assertThat(result.cohortSummaries().get(0).usedSaturday()).isFalse();
    }

    /** One period a day holds 5 x 26 + 6 = 136 runs against the 156 six weekly sessions need, so the
     *  report names the genuine shortfall rather than calling the subject placed. */
    @Test
    void theGenuineShortfallIsReportedWhenTheTermsTotalPeriodsCannotHoldTheHours() {
        List<SkeletonCellPlacementRequest> placed = runSingleSubjectWithOccupancy(6, List.of(period1));

        var result = service.runGlobalAutoSchedule(10L, null);

        assertThat(result.totalPlaced()).isEqualTo(6);
        assertThat(placed.stream().filter(r -> r.dayOfWeek() == DayOfWeek.SATURDAY).count()).isEqualTo(1);
        assertThat(result.cohortSummaries().get(0).unplaced())
            .filteredOn(u -> "Offering A".equals(u.subjectName()))
            .singleElement().satisfies(u -> {
                assertThat(u.reason()).contains("still unplaced");
                // The only kind the report's "didn't have room" alert may raise on.
                assertThat(u.slotShortfall()).isTrue();
            });
    }

    // --- OC-227: management-selected electives are common cohort subjects -------------------------

    private com.cms.model.CurriculumElectiveGroup managementSelectedGroup() {
        com.cms.model.CurriculumElectiveGroup group = new com.cms.model.CurriculumElectiveGroup();
        group.setId(12L);
        group.setGroupName("Elective II");
        group.setSelectionMode(com.cms.model.enums.ElectiveSelectionMode.INSTITUTION_DECIDED);
        return group;
    }

    private CourseOffering electiveOption(Long id, com.cms.model.CurriculumElectiveGroup group) {
        CurriculumSemesterCourse csc = new CurriculumSemesterCourse();
        csc.setTheoryHours(20);
        csc.setLabHours(0);
        csc.setClinicalHours(0);
        csc.setIsElective(true);
        csc.setElectiveGroup(group);
        CourseOffering offering = new CourseOffering();
        offering.setId(id);
        offering.setCurriculumSemesterCourse(csc);
        offering.setTermInstance(termInstance);
        when(courseOfferingRepository.findById(id)).thenReturn(Optional.of(offering));
        return offering;
    }

    private SkeletonSubjectResponse electiveSubject(Long offeringId, String name) {
        return new SkeletonSubjectResponse(offeringId, name, "EL" + offeringId,
            List.of(new SkeletonSubjectBudget(ClassSessionType.THEORY, null, null, null, null, 20, 26, 1, 0)), 12L, "Elective II");
    }

    private void oneCohortWithElectiveOptions(SkeletonSubjectResponse... subjects) {
        when(studentTermEnrollmentRepository.findDistinctCohortIdsByTermInstanceId(10L, EnrollmentStatus.ENROLLED))
            .thenReturn(new HashSet<>(List.of(1L)));
        cohort(1L, "Cohort 1");
        lenient().when(timetableSkeletonService.resolveActiveSections(1L, 10L)).thenReturn(List.of());
        lenient().when(batchRepository.findByCourseOfferingId(anyLong())).thenReturn(List.of());
        when(timetableSkeletonService.getCohortSkeleton(10L, 1L)).thenReturn(new SkeletonBuilderResponse(1L, "Cohort 1", "Term",
            List.of(subjects), List.of(), List.of(), List.of(), 26, 0L, List.of(), false, List.of()));
    }

    @Test
    void managementSelectedElectiveIsPlacedAsARegularCohortSubject_andUnchosenOptionsNeverRun() {
        // Elective II's real shape: several options, all 60 students bulk-assigned to one of them.
        com.cms.model.CurriculumElectiveGroup group = managementSelectedGroup();
        electiveOption(81L, group);
        electiveOption(84L, group);
        when(courseRegistrationRepository.countByCourseOfferingIdAndStatus(81L, com.cms.model.enums.RegistrationStatus.REGISTERED))
            .thenReturn(60L);
        facultyWithDailyCap(500L, "Elective Faculty", 8);
        lenient().when(courseOfferingService.getOfferingsByTermInstanceAndCohort(10L, 1L)).thenReturn(List.of());
        assignWholeCohort(81L, 1L, 500L);
        oneCohortWithElectiveOptions(electiveSubject(81L, "Addiction Psychiatry"), electiveSubject(84L, "Accreditation"));
        stubPlaceCellAlwaysSucceeds();

        var result = service.runGlobalAutoSchedule(10L, null);

        ArgumentCaptor<SkeletonCellPlacementRequest> placed = ArgumentCaptor.forClass(SkeletonCellPlacementRequest.class);
        verify(timetableSkeletonService, org.mockito.Mockito.atLeastOnce()).placeCell(placed.capture());
        assertThat(placed.getAllValues()).extracting(SkeletonCellPlacementRequest::courseOfferingId).containsOnly(81L);
        assertThat(result.totalPlaced()).isEqualTo(1);
        // Never the old shared-slot hunt for a room per option.
        verify(classroomRepository, never()).findByIsActiveTrueOrderByNameAsc();
        assertThat(result.electiveUnplaced()).isEmpty();
    }

    @Test
    void managementSelectedGroupWithNoOptionAssignedYetIsFlaggedOnce_andNothingIsPlaced() {
        com.cms.model.CurriculumElectiveGroup group = managementSelectedGroup();
        CourseOffering optionA = electiveOption(81L, group);
        CourseOffering optionB = electiveOption(84L, group);
        when(courseOfferingRepository.findByTermInstanceIdAndCurriculumSemesterCourse_ElectiveGroupId(10L, 12L))
            .thenReturn(List.of(optionA, optionB));
        lenient().when(courseOfferingService.getOfferingsByTermInstanceAndCohort(10L, 1L)).thenReturn(List.of());
        oneCohortWithElectiveOptions(electiveSubject(81L, "Addiction Psychiatry"), electiveSubject(84L, "Accreditation"));

        var result = service.runGlobalAutoSchedule(10L, null);

        assertThat(result.cohortSummaries().get(0).unplaced())
            .filteredOn(u -> u.reason().contains("management-selected but no option has been assigned"))
            .singleElement()
            .extracting(AutoPlaceUnplacedItem::subjectName).isEqualTo("Elective II");
        verify(timetableSkeletonService, never()).placeCell(any(SkeletonCellPlacementRequest.class));
    }

    @Test
    void libraryThatCannotFitShrinksToAnInfoNote_notAnUnplacedWarning() {
        when(studentTermEnrollmentRepository.findDistinctCohortIdsByTermInstanceId(10L, EnrollmentStatus.ENROLLED))
            .thenReturn(new HashSet<>(List.of(1L)));
        cohort(1L, "Cohort 1");
        when(courseOfferingService.getOfferingsByTermInstanceAndCohort(10L, 1L)).thenReturn(List.of());
        when(timetableSkeletonService.resolveActiveSections(1L, 10L)).thenReturn(List.of());
        Subject librarySubject = new Subject();
        librarySubject.setId(999L);
        librarySubject.setCode("SYSTEM-LIBRARY");
        when(subjectRepository.findByCode("SYSTEM-LIBRARY")).thenReturn(Optional.of(librarySubject));
        Classroom libraryRoom = new Classroom("Library Hall", null, null, 60);
        libraryRoom.setId(50L);
        when(classroomRepository.findByIsActiveTrueAndRoom_PurposeCategory_CodeOrderByNameAsc(any())).thenReturn(List.of(libraryRoom));
        Period p2 = new Period("2nd Period", LocalTime.of(9, 50), LocalTime.of(10, 40), 2);
        p2.setId(2L);
        when(periodRepository.findByIsActiveTrueOrderByPeriodOrderAsc()).thenReturn(List.of(period1, p2));
        // Curriculum has taken every slot: nothing is free for this cohort anywhere in the week.
        when(timetableSkeletonService.isSlotFreeForCohort(eq(1L), eq(10L), any(), any())).thenReturn(false);
        when(timetableSkeletonService.getCohortSkeleton(10L, 1L)).thenReturn(new SkeletonBuilderResponse(1L, "Cohort 1", "Term",
            List.of(), List.of(), List.of(), List.of(), 26, 0L, List.of(), false, List.of()));

        var result = service.runGlobalAutoSchedule(10L, null);

        var summary = result.cohortSummaries().get(0);
        assertThat(summary.unplaced()).extracting(AutoPlaceUnplacedItem::sessionType).doesNotContain(ClassSessionType.LIBRARY);
        // No Sports subject in this fixture, so a separate "Sports not placed" note sits beside it.
        assertThat(summary.infoNotes()).filteredOn(note -> note.startsWith("Library"))
            .singleElement().asString().startsWith("Library reduced to 0 of 1 weekly session(s)");
    }

    // --- 2026-09-15: Sports, and leftover periods for curriculum subjects only ---------------------

    /** A cohort with no curriculum at all, so Sports is the only thing the run can place. */
    private Subject sportsFixture(Set<Faculty> peFaculty, List<Classroom> sportsRooms, List<Period> periods) {
        when(studentTermEnrollmentRepository.findDistinctCohortIdsByTermInstanceId(10L, EnrollmentStatus.ENROLLED))
            .thenReturn(new HashSet<>(List.of(1L)));
        cohort(1L, "Cohort 1");
        when(courseOfferingService.getOfferingsByTermInstanceAndCohort(10L, 1L)).thenReturn(List.of());
        when(timetableSkeletonService.resolveActiveSections(1L, 10L)).thenReturn(List.of());
        when(periodRepository.findByIsActiveTrueOrderByPeriodOrderAsc()).thenReturn(periods);
        when(timetableSkeletonService.getCohortSkeleton(10L, 1L)).thenReturn(new SkeletonBuilderResponse(1L, "Cohort 1", "Term",
            List.of(), List.of(), List.of(), List.of(), 26, 0L, List.of(), false, List.of()));
        Subject sports = new Subject();
        sports.setId(998L);
        sports.setCode("SYSTEM-SPORTS");
        sports.setEligibleFaculty(new HashSet<>(peFaculty));
        when(subjectRepository.findByCode("SYSTEM-SPORTS")).thenReturn(Optional.of(sports));
        when(classroomRepository.findByIsActiveTrueAndRoom_PurposeCategory_CodeOrderByNameAsc(
            com.cms.model.enums.RoomPurposeCategoryCode.SPORTS)).thenReturn(sportsRooms);
        return sports;
    }

    private static Faculty activeFaculty(Long id, String name) {
        Faculty faculty = new Faculty();
        faculty.setId(id);
        faculty.setFirstName(name);
        faculty.setLastName("Staff");
        faculty.setStatus(FacultyStatus.ACTIVE);
        faculty.setPlannedDailySessionsOverride(6);
        return faculty;
    }

    @Test
    void sportsGoesInTheDaysLastTwoPeriods_taughtOnlyByFacultyOnTheSportsSubjectsEligibleList() {
        Period p2 = new Period("2nd Period", LocalTime.of(9, 50), LocalTime.of(10, 40), 2);
        p2.setId(2L);
        Period p3 = new Period("3rd Period", LocalTime.of(10, 40), LocalTime.of(11, 30), 3);
        p3.setId(3L);
        Faculty pe = activeFaculty(700L, "PE");
        // Active but not on the Sports subject's list: a subject with no speciality would otherwise
        // count every faculty as eligible, so this one must never be asked to take Sports.
        Faculty notPe = activeFaculty(701L, "Other");
        Classroom ground = new Classroom("Ground", null, null, 120);
        ground.setId(60L);
        Subject sports = sportsFixture(Set.of(pe), List.of(ground), List.of(period1, p2, p3));
        when(facultyRepository.findByStatus(FacultyStatus.ACTIVE)).thenReturn(List.of(notPe, pe));
        when(timetableSkeletonService.isSlotFreeForCohort(eq(1L), eq(10L), any(), any())).thenReturn(true);
        ClassSchedule firstRow = new ClassSchedule();
        firstRow.setId(951L);
        when(timetableSkeletonService.saveSportsBlockCells(any(), any(), any(), any(), any(), any())).thenReturn(List.of(firstRow));

        var result = service.runGlobalAutoSchedule(10L, null);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Period>> block = ArgumentCaptor.forClass(List.class);
        verify(timetableSkeletonService).saveSportsBlockCells(eq(sports), eq(termInstance), eq(DayOfWeek.MONDAY),
            block.capture(), isNull(), eq(ground));
        assertThat(block.getValue()).extracting(Period::getId).containsExactly(2L, 3L);
        verify(timetableStaffingService).staffCell(eq(951L), argThat((StaffingAssignmentRequest r) -> r.facultyId().equals(700L)));
        verify(timetableStaffingService, never()).staffCell(anyLong(), argThat((StaffingAssignmentRequest r) -> r.facultyId().equals(701L)));
        assertThat(result.cohortSummaries().get(0).infoNotes()).noneMatch(note -> note.startsWith("Sports"));
    }

    @Test
    void sportsWithNoPeFacultyOnTheSportsSubjectIsAGreyNote_neverAnUnplacedWarning() {
        Classroom ground = new Classroom("Ground", null, null, 120);
        ground.setId(60L);
        sportsFixture(Set.of(), List.of(ground), List.of(period1));

        var result = service.runGlobalAutoSchedule(10L, null);

        var summary = result.cohortSummaries().get(0);
        assertThat(summary.infoNotes()).contains(
            "Sports not placed — no active PE faculty with spare capacity is on the Sports subject's eligible-faculty list");
        assertThat(summary.unplaced()).extracting(AutoPlaceUnplacedItem::sessionType).doesNotContain(ClassSessionType.SPORTS);
        verify(timetableSkeletonService, never()).saveSportsBlockCells(any(), any(), any(), any(), any(), any());
    }

    @Test
    void sportsWithNoSportsTaggedRoomIsAGreyNoteNamingTheMissingRoom() {
        sportsFixture(Set.of(activeFaculty(700L, "PE")), List.of(), List.of(period1));

        var result = service.runGlobalAutoSchedule(10L, null);

        assertThat(result.cohortSummaries().get(0).infoNotes())
            .anyMatch(note -> note.startsWith("Sports not placed — no room is tagged Sports & Recreation"));
        verify(timetableSkeletonService, never()).saveSportsBlockCells(any(), any(), any(), any(), any(), any());
    }

    /** Sports' counterpart of {@link #libraryIsWithheldEntirely_whenTheCohortStillHasAGenuineTheoryShortfall}:
     *  Sports (advisory/co-curricular filler, same as Library and Self-Study) must yield its whole
     *  required quota, not just claim whatever's free, while this cohort still genuinely owes a real
     *  Theory offering it can never place. Room/faculty setup is trivially satisfiable, so the only
     *  thing that can withhold Sports is the {@code theoryStillOwedRuns} gate. */
    @Test
    void sportsIsWithheldEntirely_whenTheCohortStillHasAGenuineTheoryShortfall() {
        when(studentTermEnrollmentRepository.findDistinctCohortIdsByTermInstanceId(10L, EnrollmentStatus.ENROLLED))
            .thenReturn(new HashSet<>(List.of(1L)));
        cohort(1L, "Cohort 1");
        facultyWithDailyCap(500L, "XYZ", 6);
        when(courseOfferingService.getOfferingsByTermInstanceAndCohort(10L, 1L)).thenReturn(List.of(offeringDto(100L, "Offering A")));
        assignWholeCohort(100L, 1L, 500L);
        offeringEntity(100L, 10, 0, 0);
        when(timetableSkeletonService.resolveActiveSections(1L, 10L)).thenReturn(List.of());
        when(batchRepository.findByCourseOfferingId(anyLong())).thenReturn(List.of());

        SkeletonSubjectBudget theoryBudget = new SkeletonSubjectBudget(ClassSessionType.THEORY, null, null, null, null, 10, 10, 1, 0);
        SkeletonSubjectResponse theorySubject = new SkeletonSubjectResponse(100L, "Offering A", "OFFE", List.of(theoryBudget), null, null);
        SkeletonBuilderResponse skeleton = new SkeletonBuilderResponse(1L, "Cohort 1", "Term", List.of(theorySubject),
            List.of(), List.of(), List.of(), 25, 0L, List.of(), false, List.of());
        when(timetableSkeletonService.getCohortSkeleton(10L, 1L)).thenReturn(skeleton);

        // Theory can never be placed anywhere -- guarantees a genuine, unresolvable shortfall.
        when(timetableSkeletonService.placeCell(any(SkeletonCellPlacementRequest.class)))
            .thenThrow(new TimetableConstraintViolationException(List.of(
                new com.cms.dto.ConstraintViolation("SKELETON_CELL_COHORT_CLASH", "clash"))));

        Faculty pe = activeFaculty(700L, "PE");
        when(facultyRepository.findByStatus(FacultyStatus.ACTIVE)).thenReturn(List.of(pe));
        Classroom ground = new Classroom("Ground", null, null, 120);
        ground.setId(60L);
        Subject sports = new Subject();
        sports.setId(998L);
        sports.setCode("SYSTEM-SPORTS");
        sports.setEligibleFaculty(new HashSet<>(Set.of(pe)));
        // lenient(): the theoryStillOwedRuns gate now returns before Sports ever looks up its
        // subject/room or checks slot freedom.
        lenient().when(subjectRepository.findByCode("SYSTEM-SPORTS")).thenReturn(Optional.of(sports));
        lenient().when(classroomRepository.findByIsActiveTrueAndRoom_PurposeCategory_CodeOrderByNameAsc(
            com.cms.model.enums.RoomPurposeCategoryCode.SPORTS)).thenReturn(List.of(ground));
        lenient().when(timetableSkeletonService.isSlotFreeForCohort(eq(1L), eq(10L), any(), any())).thenReturn(true);

        var result = service.runGlobalAutoSchedule(10L, null);

        verify(timetableSkeletonService, never()).saveSportsBlockCells(any(), any(), any(), any(), any(), any());
        assertThat(result.cohortSummaries().get(0).infoNotes()).anyMatch(note -> note.startsWith("Sports reduced to 0 of"));
    }

    /** Leftover periods are extra revision for the cohort's other curriculum subjects, shared
     *  equally; Self-Study already has its curriculum hours and takes none (2026-09-15). */
    @Test
    void leftoverPeriodsGoEquallyToCurriculumSubjects_neverToSelfStudy() {
        facultyWithDailyCap(600L, "Coordinator", 6);
        List<SkeletonSubjectResponse> subjects = new ArrayList<>();
        for (long offeringId : new long[] {400L, 500L}) {
            Subject subject = new Subject();
            subject.setId(offeringId);
            subject.setName("Offering " + offeringId);
            CourseOffering offering = new CourseOffering();
            offering.setId(offeringId);
            offering.setSubject(subject);
            when(courseOfferingRepository.findById(offeringId)).thenReturn(Optional.of(offering));
            lenient().when(timetableSkeletonService.isElectiveOffering(offering)).thenReturn(false);
            assignWholeCohort(offeringId, 1L, 600L);
            subjects.add(new SkeletonSubjectResponse(offeringId, "Offering " + offeringId, "OF" + offeringId,
                List.of(new SkeletonSubjectBudget(ClassSessionType.THEORY, null, null, null, null, 40, 20, 5, 5)), null, null));
        }
        subjects.add(1, new SkeletonSubjectResponse(600L, "Self-Study/Co-curricular I", "SSCC",
            List.of(new SkeletonSubjectBudget(ClassSessionType.THEORY, null, null, null, null, 40, 20, 5, 5)), null, null));
        SkeletonBuilderResponse skeleton = new SkeletonBuilderResponse(1L, "Cohort 1", "Term", subjects,
            List.of(), List.of(), List.of(), 20, 0L, List.of(), false, List.of());
        java.util.concurrent.atomic.AtomicLong nextCellId = new java.util.concurrent.atomic.AtomicLong(900L);
        when(timetableSkeletonService.placeCell(any(SkeletonCellPlacementRequest.class), eq(false))).thenAnswer(invocation -> {
            SkeletonCellPlacementRequest request = invocation.getArgument(0);
            return new SkeletonCellResponse(nextCellId.incrementAndGet(), ClassSessionType.THEORY, request.dayOfWeek(), 1L,
                "1st Period", LocalTime.of(9, 0), LocalTime.of(9, 50), null, null, null, null, false, null, null, List.of(),
                request.courseOfferingId(), "Subject", "SUBJ", null, null, null, false, false);
        });
        var dayLoad = new java.util.HashMap<DayOfWeek, Integer>();
        for (DayOfWeek day : DayOfWeek.values()) {
            dayLoad.put(day, 0);
        }

        var result = service.fillSelfStudyGaps(1L, skeleton, termInstance, List.of(period1), dayLoad, new ArrayList<>(),
            new TimetableGlobalAutoScheduleService.TermDemandAggregation(100, 20, java.util.Map.of(), java.util.Map.of(), 0), false, java.util.Map.of());

        // Monday-Friday x 1 period: 5 leftover periods, none of them Self-Study, split 3/2 at worst.
        List<Long> filledOfferings = result.filled().stream()
            .map(TimetableGlobalAutoScheduleService.Placement::courseOfferingId).toList();
        assertThat(filledOfferings).hasSize(5).doesNotContain(600L);
        long toA = filledOfferings.stream().filter(id -> id == 400L).count();
        long toB = filledOfferings.stream().filter(id -> id == 500L).count();
        assertThat(Math.abs(toA - toB)).isLessThanOrEqualTo(1L);
    }
}
