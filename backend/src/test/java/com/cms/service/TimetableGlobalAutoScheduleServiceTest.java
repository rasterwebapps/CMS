package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.dto.AutoPlaceUnplacedItem;
import com.cms.dto.CourseOfferingDto;
import com.cms.dto.EligibleFacultyCandidateDto;
import com.cms.dto.LabClinicalVenueCapacityResult;
import com.cms.dto.VenueOverCapacity;
import com.cms.dto.FacultyCapacityCheckResult;
import com.cms.dto.FacultyOverCapacity;
import com.cms.dto.FacultyTightCapacity;
import com.cms.dto.FacultyWorkloadDetail;
import com.cms.dto.GlobalAutoSchedulePrerequisites;
import com.cms.dto.GlobalCapacityPrecheckResult;
import com.cms.dto.SkeletonBuilderResponse;
import com.cms.dto.SkeletonCellPlacementRequest;
import com.cms.dto.SkeletonCellResponse;
import com.cms.dto.SkeletonSubjectBudget;
import com.cms.dto.SkeletonSubjectResponse;
import com.cms.dto.StaffingAssignmentRequest;
import com.cms.dto.UnstaffedCellResponse;
import com.cms.exception.TimetableConstraintViolationException;
import com.cms.model.AcademicYear;
import com.cms.model.Batch;
import com.cms.model.Cohort;
import com.cms.model.ClassSchedule;
import com.cms.model.CohortSection;
import com.cms.model.CourseOffering;
import com.cms.model.CourseOfferingSectionFaculty;
import com.cms.model.CurriculumSemesterCourse;
import com.cms.model.DesignationMaster;
import com.cms.model.Faculty;
import com.cms.model.Period;
import com.cms.model.Speciality;
import com.cms.model.Subject;
import com.cms.model.TermInstance;
import com.cms.model.enums.ClassSessionType;
import com.cms.model.enums.DayOfWeek;
import com.cms.model.enums.EnrollmentStatus;
import com.cms.model.enums.FacultyStatus;
import com.cms.model.enums.TermInstanceStatus;
import com.cms.model.enums.TermType;
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

    /** None of these fixtures configure a Self-Study/Co-curricular offering, so the gap-fill pass
     *  now correctly reports this once per cohort per run (see {@code fillSelfStudyGaps}) instead
     *  of silently leaving Monday-Friday periods unaccounted for. */
    private static final String NO_SELF_STUDY_OFFERING_REASON =
        "no Self-Study/Co-curricular offering is configured for this cohort to use as gap-fill — "
            + "every remaining Monday-Friday period stays empty until one is added";

    /** None of these fixtures configure a Library classroom either, so {@code fillLibraryGaps} now
     *  correctly reports this once per cohort per run too — added ahead of the Self-Study reason in
     *  every {@code unplaced} list, since Library runs first (see the class's placement-order note). */
    private static final String NO_LIBRARY_CLASSROOM_REASON =
        "no Library classroom is configured (a Classroom linked to a Room tagged with the Library "
            + "Purpose Category) — every cohort's Library quota stays unplaced until one is added";

    private TimetableGlobalAutoScheduleService service;
    private TermInstance termInstance;
    private Period period1;

    @BeforeEach
    void setUp() {
        service = new TimetableGlobalAutoScheduleService(timetableSkeletonService, timetableStaffingService,
            timetableCapacityPlanningService, courseOfferingService, courseOfferingRepository, classScheduleRepository,
            studentTermEnrollmentRepository, cohortRepository, batchRepository, courseOfferingSectionFacultyRepository,
            facultyRepository, termInstanceRepository, periodRepository, blockedPeriodChecker, classroomRepository,
            courseRegistrationRepository, subjectRepository, systemConfigurationService);
        lenient().when(courseOfferingSectionFacultyRepository.findByCourseOfferingId(anyLong())).thenReturn(List.of());
        // No fixture configures a Library classroom (Classroom linked to a Room tagged Library
        // Purpose Category), so fillLibraryGaps correctly reports this once per cohort per run —
        // see NO_LIBRARY_CLASSROOM_REASON below.
        lenient().when(subjectRepository.findByCode("SYSTEM-LIBRARY")).thenReturn(Optional.empty());
        lenient().when(classroomRepository.findByIsActiveTrueAndRoom_PurposeCategory_CodeOrderByNameAsc(any()))
            .thenReturn(List.of());

        AcademicYear ay = new AcademicYear("2025-2026", LocalDate.of(2025, 6, 1), LocalDate.of(2026, 5, 31), false);
        ay.setId(1L);
        termInstance = new TermInstance(ay, TermType.ODD, LocalDate.of(2025, 6, 1), LocalDate.of(2025, 11, 30), TermInstanceStatus.OPEN);
        termInstance.setId(10L);
        when(termInstanceRepository.findById(10L)).thenReturn(Optional.of(termInstance));

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
    }

    private Faculty facultyWithDailyCap(Long id, String name, Integer plannedDailyHoursOverride) {
        Faculty faculty = new Faculty();
        faculty.setId(id);
        faculty.setFirstName(name);
        faculty.setLastName("Staff");
        faculty.setStatus(FacultyStatus.ACTIVE);
        faculty.setPlannedDailyHoursOverride(plannedDailyHoursOverride);
        when(facultyRepository.findById(id)).thenReturn(Optional.of(faculty));
        return faculty;
    }

    private Cohort cohort(Long id, String name) {
        Cohort cohort = new Cohort();
        cohort.setId(id);
        when(cohortRepository.findById(id)).thenReturn(Optional.of(cohort));
        return cohort;
    }

    private CourseOfferingDto offeringDto(Long id, String subjectName) {
        return new CourseOfferingDto(id, 10L, null, null, null, id, subjectName, subjectName.substring(0, 4).toUpperCase(),
            null, null, List.of(), 1, true, null, false, null, null, null, null, null, null, null, null, null, null, List.of());
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
        SkeletonBuilderResponse skeleton = new SkeletonBuilderResponse(1L, "Cohort 1", "Term", List.of(subject), List.of(), List.of(), List.of(), 25, 0L);
        when(timetableSkeletonService.getCohortSkeleton(10L, 1L)).thenReturn(skeleton);

        SkeletonCellResponse placed = new SkeletonCellResponse(900L, ClassSessionType.THEORY, DayOfWeek.MONDAY, 1L, "1st Period",
            LocalTime.of(9, 0), LocalTime.of(9, 50), null, null, null, null, false, null, null, List.of(),
            100L, "Offering A", "OFFE", null, null, null);
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
            .containsExactly(NO_LIBRARY_CLASSROOM_REASON, NO_SELF_STUDY_OFFERING_REASON);
        assertThat(result.cohortSummaries().get(0).usedSaturday()).isFalse();
        assertThat(result.electiveUnplaced()).isEmpty();
        verify(timetableStaffingService).staffCell(900L, new StaffingAssignmentRequest(500L, null));
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
            true, null, null, List.of(), null, null, null, null, null, null);
        SkeletonBuilderResponse skeleton = new SkeletonBuilderResponse(1L, "Cohort 1", "Term", List.of(subject),
            List.of(preExistingLibraryCell), List.of(), List.of(), 25, 0L);
        when(timetableSkeletonService.getCohortSkeleton(10L, 1L)).thenReturn(skeleton);

        SkeletonCellResponse placed = new SkeletonCellResponse(900L, ClassSessionType.THEORY, DayOfWeek.MONDAY, 1L, "1st Period",
            LocalTime.of(9, 0), LocalTime.of(9, 50), null, null, null, null, false, null, null, List.of(),
            100L, "Offering A", "OFFE", null, null, null);
        when(timetableSkeletonService.placeCell(any(SkeletonCellPlacementRequest.class))).thenReturn(placed);
        when(timetableStaffingService.staffCell(eq(900L), any(StaffingAssignmentRequest.class)))
            .thenReturn(new UnstaffedCellResponse(900L, 100L, "Offering A", "OFFE", null, null,
                ClassSessionType.THEORY, DayOfWeek.MONDAY, 1L, "1st Period", LocalTime.of(9, 0), LocalTime.of(9, 50),
                null, null, null, null, null, false, List.of(), null, null));

        var result = service.runGlobalAutoSchedule(10L, null);

        assertThat(result.totalPlaced()).isEqualTo(1);
        assertThat(result.totalStaffed()).isEqualTo(1);
    }

    @Test
    void runClearsStaleOverBudgetDraftsBeforePlacingAnything_temporarySafetyNet() {
        when(studentTermEnrollmentRepository.findDistinctCohortIdsByTermInstanceId(10L, EnrollmentStatus.ENROLLED))
            .thenReturn(new HashSet<>(List.of(1L)));
        cohort(1L, "Cohort 1");
        facultyWithDailyCap(500L, "XYZ", 6);
        when(courseOfferingService.getOfferingsByTermInstanceAndCohort(10L, 1L)).thenReturn(List.of(offeringDto(100L, "Offering A")));
        assignWholeCohort(100L, 1L, 500L);
        offeringEntity(100L, 10, 0, 0);
        when(timetableSkeletonService.resolveActiveSections(1L, 10L)).thenReturn(List.of());
        when(batchRepository.findByCourseOfferingId(anyLong())).thenReturn(List.of());

        // 3 already placed against a budget of 1 required -- 2 stale excess DRAFT sessions left
        // over from before checkBudgetNotExceeded existed. Shortfall (1 - 3) is negative, so this
        // row would never be touched by ordinary placement -- only the purge should act on it.
        SkeletonSubjectBudget budget = new SkeletonSubjectBudget(ClassSessionType.THEORY, null, null, null, null, 10, 10, 1, 3);
        SkeletonSubjectResponse subject = new SkeletonSubjectResponse(100L, "Offering A", "OFFE", List.of(budget), null, null);
        SkeletonCellResponse cellKept = skeletonCell(901L, com.cms.model.enums.ClassScheduleStatus.DRAFT);
        SkeletonCellResponse cellExcess1 = skeletonCell(902L, com.cms.model.enums.ClassScheduleStatus.DRAFT);
        SkeletonCellResponse cellExcess2 = skeletonCell(903L, com.cms.model.enums.ClassScheduleStatus.DRAFT);
        SkeletonBuilderResponse skeleton = new SkeletonBuilderResponse(1L, "Cohort 1", "Term", List.of(subject),
            List.of(cellKept, cellExcess1, cellExcess2), List.of(), List.of(), 25, 0L);
        when(timetableSkeletonService.getCohortSkeleton(10L, 1L)).thenReturn(skeleton);

        ClassSchedule excess1 = new ClassSchedule();
        excess1.setId(902L);
        excess1.setIsActive(true);
        ClassSchedule excess2 = new ClassSchedule();
        excess2.setId(903L);
        excess2.setIsActive(true);
        when(classScheduleRepository.findAllById(any())).thenReturn(List.of(excess1, excess2));

        var result = service.runGlobalAutoSchedule(10L, null);

        assertThat(result.staleDraftsCleared()).isEqualTo(2);
        assertThat(excess1.getIsActive()).isFalse();
        assertThat(excess2.getIsActive()).isFalse();
        verify(classScheduleRepository).save(excess1);
        verify(classScheduleRepository).save(excess2);
        assertThat(result.totalPlaced()).isEqualTo(0);
        assertThat(result.cohortSummaries().get(0).unplaced()).extracting(AutoPlaceUnplacedItem::reason)
            .containsExactly(NO_LIBRARY_CLASSROOM_REASON, NO_SELF_STUDY_OFFERING_REASON);
    }

    /** Minimal {@link SkeletonCellResponse} for Offering A / whole-cohort THEORY -- only {@code id}
     *  and {@code status} vary across the purge test's fixture cells. */
    private SkeletonCellResponse skeletonCell(Long id, com.cms.model.enums.ClassScheduleStatus status) {
        return new SkeletonCellResponse(id, ClassSessionType.THEORY, DayOfWeek.MONDAY, 1L, "1st Period",
            LocalTime.of(9, 0), LocalTime.of(9, 50), null, null, null, null, false, status, null, List.of(),
            100L, "Offering A", "OFFE", null, null, null);
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

        SkeletonSubjectBudget budget = new SkeletonSubjectBudget(ClassSessionType.CLINICAL, 55L, "Batch 1", null, null, 40, 10, 4, 0);
        SkeletonSubjectResponse subjectResponse = new SkeletonSubjectResponse(100L, "Offering A", "OFFE", List.of(budget), null, null);
        SkeletonBuilderResponse skeleton = new SkeletonBuilderResponse(1L, "Cohort 1", "Term", List.of(subjectResponse), List.of(), List.of(), List.of(), 25, 0L);
        when(timetableSkeletonService.getCohortSkeleton(10L, 1L)).thenReturn(skeleton);

        SkeletonCellResponse placed = new SkeletonCellResponse(900L, ClassSessionType.CLINICAL, DayOfWeek.MONDAY, 1L, "1st Period",
            LocalTime.of(9, 0), LocalTime.of(9, 50), 55L, "Batch 1", null, null, false, null, null, List.of(),
            100L, "Offering A", "OFFE", null, null, null);
        when(timetableSkeletonService.placeCell(any(SkeletonCellPlacementRequest.class))).thenReturn(placed);
        when(timetableStaffingService.staffCell(eq(900L), any(StaffingAssignmentRequest.class)))
            .thenReturn(new UnstaffedCellResponse(900L, 100L, "Offering A", "OFFE", null, null,
                ClassSessionType.CLINICAL, DayOfWeek.MONDAY, 1L, "1st Period", LocalTime.of(9, 0), LocalTime.of(9, 50),
                null, null, null, null, null, false, List.of(), null, null));

        var result = service.runGlobalAutoSchedule(10L, null);

        assertThat(result.totalPlaced()).isEqualTo(1);
        assertThat(result.cohortSummaries().get(0).unplaced()).extracting(AutoPlaceUnplacedItem::reason)
            .containsExactly(NO_LIBRARY_CLASSROOM_REASON, NO_SELF_STUDY_OFFERING_REASON);
        verify(timetableSkeletonService).placeCell(argThat(r ->
            r.dayOfWeek() == DayOfWeek.MONDAY && r.periodId().equals(1L)
                && r.spanPeriodIds() != null && r.spanPeriodIds().equals(List.of(2L, 3L, 4L))));
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
        SkeletonBuilderResponse skeleton = new SkeletonBuilderResponse(1L, "Cohort 1", "Term", List.of(subject), List.of(), List.of(), List.of(), 25, 0L);
        when(timetableSkeletonService.getCohortSkeleton(10L, 1L)).thenReturn(skeleton);

        when(timetableSkeletonService.placeCell(any(SkeletonCellPlacementRequest.class)))
            .thenThrow(new TimetableConstraintViolationException(List.of(
                new com.cms.dto.ConstraintViolation("SKELETON_CELL_COHORT_CLASH", "clash"))));

        var result = service.runGlobalAutoSchedule(10L, null);

        assertThat(result.totalPlaced()).isEqualTo(0);
        assertThat(result.cohortSummaries()).hasSize(1);
        assertThat(result.cohortSummaries().get(0).unplaced()).hasSize(3);
        // The real blocking constraint (cohort clash) is now named, not a generic catch-all --
        // this is exactly the diagnostic gap that made a real shortfall look unexplainable
        // without pulling raw data by hand.
        assertThat(result.cohortSummaries().get(0).unplaced().get(0).reason())
            .contains("another mandatory session already occupies this audience's slot")
            .contains("5 of 5 day/period combinations tried");
        assertThat(result.cohortSummaries().get(0).unplaced().get(1).reason()).isEqualTo(NO_LIBRARY_CLASSROOM_REASON);
        assertThat(result.cohortSummaries().get(0).unplaced().get(2).reason()).isEqualTo(NO_SELF_STUDY_OFFERING_REASON);

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
        SkeletonBuilderResponse skeleton = new SkeletonBuilderResponse(1L, "Cohort 1", "Term", List.of(subject), List.of(), List.of(), List.of(), 25, 0L);
        when(timetableSkeletonService.getCohortSkeleton(10L, 1L)).thenReturn(skeleton);

        SkeletonCellResponse placed = new SkeletonCellResponse(900L, ClassSessionType.THEORY, DayOfWeek.MONDAY, 1L, "1st Period",
            LocalTime.of(9, 0), LocalTime.of(9, 50), null, null, null, null, false, null, null, List.of(),
            100L, "Offering A", "OFFE", null, null, null);
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
        SkeletonBuilderResponse skeleton = new SkeletonBuilderResponse(1L, "Cohort 1", "Term", List.of(subject), List.of(), List.of(), List.of(), 25, 0L);
        when(timetableSkeletonService.getCohortSkeleton(10L, 1L)).thenReturn(skeleton);

        var result = service.runGlobalAutoSchedule(10L, null);

        assertThat(result.totalPlaced()).isEqualTo(0);
        assertThat(result.cohortSummaries().get(0).unplaced()).extracting(AutoPlaceUnplacedItem::reason)
            .containsExactly("no faculty assigned on its Course Offering", NO_LIBRARY_CLASSROOM_REASON, NO_SELF_STUDY_OFFERING_REASON);
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
            .thenReturn(new SkeletonBuilderResponse(1L, "Cohort 1", "Term", List.of(subjectA), List.of(), List.of(), List.of(), 25, 0L));

        SkeletonSubjectBudget budgetB = new SkeletonSubjectBudget(ClassSessionType.THEORY, null, null, null, null, 10, 10, 1, 0);
        SkeletonSubjectResponse subjectB = new SkeletonSubjectResponse(200L, "Offering B", "OFFB", List.of(budgetB), null, null);
        when(timetableSkeletonService.getCohortSkeleton(10L, 2L))
            .thenReturn(new SkeletonBuilderResponse(2L, "Cohort 2", "Term", List.of(subjectB), List.of(), List.of(), List.of(), 25, 0L));

        // Cohort 1's offering (100L) can never be placed; cohort 2's (200L) succeeds every time.
        when(timetableSkeletonService.placeCell(argThat(r -> r != null && r.courseOfferingId().equals(100L))))
            .thenThrow(new TimetableConstraintViolationException(List.of(new com.cms.dto.ConstraintViolation("X", "no"))));
        SkeletonCellResponse placedB = new SkeletonCellResponse(901L, ClassSessionType.THEORY, DayOfWeek.MONDAY, 1L, "1st Period",
            LocalTime.of(9, 0), LocalTime.of(9, 50), null, null, null, null, false, null, null, List.of(),
            200L, "Offering B", "OFFB", null, null, null);
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
        assertThat(summaryByCohort.get(1L).unplaced()).hasSize(3);
        assertThat(summaryByCohort.get(2L).placedCount()).isEqualTo(1);
        assertThat(summaryByCohort.get(2L).unplaced()).extracting(AutoPlaceUnplacedItem::reason)
            .containsExactly(NO_LIBRARY_CLASSROOM_REASON, NO_SELF_STUDY_OFFERING_REASON);
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
            .thenReturn(new SkeletonBuilderResponse(1L, "Cohort 1", "Term", List.of(), List.of(), List.of(), List.of(), 25, 0L));

        var result = service.runGlobalAutoSchedule(10L, 1L);

        assertThat(result.cohortSummaries()).hasSize(1);
        assertThat(result.cohortSummaries().get(0).cohortId()).isEqualTo(1L);
        verify(timetableSkeletonService, never()).getCohortSkeleton(10L, 2L);
    }

    // ── Live single-(faculty, cohort) capacity check (Course Offerings) ─

    @Test
    void checkFacultyCapacityForCohort_fitsWithinCapacity() {
        when(studentTermEnrollmentRepository.findDistinctCohortIdsByTermInstanceId(10L, EnrollmentStatus.ENROLLED))
            .thenReturn(new HashSet<>(List.of(1L)));
        cohort(1L, "Cohort 1");
        facultyWithDailyCap(500L, "XYZ", 6); // 600h capacity
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

        SkeletonSubjectBudget budgetA = new SkeletonSubjectBudget(ClassSessionType.THEORY, null, null, null, null, 10, 10, 1, 0);
        SkeletonSubjectResponse subjectA = new SkeletonSubjectResponse(100L, "Offering A", "OFFA", List.of(budgetA), null, null);
        SkeletonSubjectResponse subjectB = new SkeletonSubjectResponse(200L, "Offering B", "OFFB",
            List.of(new SkeletonSubjectBudget(ClassSessionType.THEORY, null, null, null, null, 10, 10, 1, 0)), null, null);
        when(timetableSkeletonService.getCohortSkeleton(10L, 1L))
            .thenReturn(new SkeletonBuilderResponse(1L, "Cohort 1", "Term", List.of(subjectA, subjectB), List.of(), List.of(), List.of(), 25, 0L));

        GlobalAutoSchedulePrerequisites result = service.checkPrerequisites(10L, null);

        assertThat(result.ready()).isFalse();
        assertThat(result.offeringsWithoutFaculty()).hasSize(1);
        assertThat(result.offeringsWithoutFaculty().get(0).courseOfferingId()).isEqualTo(100L);
        assertThat(result.capacityPrecheck().overCapacityFaculty()).extracting(FacultyOverCapacity::facultyId)
            .containsExactly(500L);
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
            .thenReturn(new SkeletonBuilderResponse(1L, "Cohort 1", "Term", List.of(subject), List.of(), List.of(), List.of(), 25, 0L));

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
            .thenReturn(new SkeletonBuilderResponse(1L, "Cohort 1", "Term", List.of(subject), List.of(), List.of(), List.of(), 25, 0L));

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
        f.setPlannedDailyHoursOverride(dailyCapHours);
        return f;
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
    void getEligibleFacultyForOffering_flagsInPoolMembersOnly() {
        Speciality nursing = new Speciality("Nursing", "NUR", "dept", null, null);
        nursing.setId(1L);
        Subject subject = new Subject("Nursing Foundations", "NF101", 4, 3, 1, nursing, 1);
        subject.setId(1L);

        Faculty pooled = activeFaculty(500L, nursing, 6);
        Faculty notPooled = activeFaculty(600L, nursing, 6);
        CourseOffering offering = new CourseOffering();
        offering.setId(100L);
        offering.setSubject(subject);
        offering.setTermInstance(termInstance);
        offering.setFacultyPool(new java.util.HashSet<>(java.util.Set.of(pooled)));
        when(courseOfferingRepository.findById(100L)).thenReturn(Optional.of(offering));
        when(facultyRepository.findByStatus(FacultyStatus.ACTIVE)).thenReturn(List.of(pooled, notPooled));
        when(studentTermEnrollmentRepository.findDistinctCohortIdsByTermInstanceId(10L, EnrollmentStatus.ENROLLED))
            .thenReturn(new HashSet<>());

        List<EligibleFacultyCandidateDto> candidates = service.getEligibleFacultyForOffering(100L);

        assertThat(candidates).filteredOn(c -> c.facultyId().equals(500L)).allSatisfy(c -> assertThat(c.inPool()).isTrue());
        assertThat(candidates).filteredOn(c -> c.facultyId().equals(600L)).allSatisfy(c -> assertThat(c.inPool()).isFalse());
    }

    @Test
    void getEligibleFacultyForOffering_excludesCurrentlyAssignedFacultyNotInPool() {
        // Someone merely holding a section/cohort of this offering, without being eligible or
        // actually saved into the pool, can never be added to the pool anyway (updateFacultyPool
        // rejects them) -- showing them as a checkable pool candidate was misleading, so the
        // checklist must leave them off entirely rather than grandfather them in.
        Speciality nursing = new Speciality("Nursing", "NUR", "dept", null, null);
        nursing.setId(1L);
        Speciality other = new Speciality("Other", "OTH", "dept", null, null);
        other.setId(2L);
        Subject subject = new Subject("Nursing Foundations", "NF101", 4, 3, 1, nursing, 1);
        subject.setId(1L);
        Faculty notInPool = activeFaculty(900L, other, 6);

        CourseOffering offering = new CourseOffering();
        offering.setId(100L);
        offering.setSubject(subject);
        offering.setTermInstance(termInstance);
        when(courseOfferingRepository.findById(100L)).thenReturn(Optional.of(offering));
        when(facultyRepository.findByStatus(FacultyStatus.ACTIVE)).thenReturn(List.of());
        CourseOfferingSectionFaculty currentlyAssignedRow = new CourseOfferingSectionFaculty();
        currentlyAssignedRow.setFaculty(notInPool);
        when(courseOfferingSectionFacultyRepository.findByCourseOfferingId(100L)).thenReturn(List.of(currentlyAssignedRow));
        when(studentTermEnrollmentRepository.findDistinctCohortIdsByTermInstanceId(10L, EnrollmentStatus.ENROLLED))
            .thenReturn(new HashSet<>());

        List<EligibleFacultyCandidateDto> candidates = service.getEligibleFacultyForOffering(100L);

        assertThat(candidates).isEmpty();
    }

    @Test
    void getEligibleFacultyForOffering_grandfathersExistingPoolMemberNoLongerEligible() {
        // A faculty already saved into the pool before the subject's eligibility setup tightened
        // must never silently disappear (and become unremovable) from the checklist.
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
        offering.setFacultyPool(new java.util.HashSet<>(java.util.Set.of(grandfathered)));
        when(courseOfferingRepository.findById(100L)).thenReturn(Optional.of(offering));
        when(facultyRepository.findByStatus(FacultyStatus.ACTIVE)).thenReturn(List.of());
        when(facultyRepository.findById(900L)).thenReturn(Optional.of(grandfathered));
        when(courseOfferingSectionFacultyRepository.findByCourseOfferingId(100L)).thenReturn(List.of());
        when(studentTermEnrollmentRepository.findDistinctCohortIdsByTermInstanceId(10L, EnrollmentStatus.ENROLLED))
            .thenReturn(new HashSet<>());

        List<EligibleFacultyCandidateDto> candidates = service.getEligibleFacultyForOffering(100L);

        assertThat(candidates).hasSize(1);
        assertThat(candidates.get(0).facultyId()).isEqualTo(900L);
        assertThat(candidates.get(0).inPool()).isTrue();
        assertThat(candidates.get(0).specialityMatch()).isFalse();
        assertThat(candidates.get(0).viaEligibleList()).isFalse();
    }

    @Test
    void getEligibleFacultyForSection_includesPoolMemberNotHoldingThisSection() {
        // The whole point of the Faculty Pool is build-once-assign-anywhere: a pool member must show
        // up as a candidate for every section/cohort row of the offering, not just the one they
        // happen to already hold.
        Speciality nursing = new Speciality("Nursing", "NUR", "dept", null, null);
        nursing.setId(1L);
        Speciality other = new Speciality("Other", "OTH", "dept", null, null);
        other.setId(2L);
        Subject subject = new Subject("Nursing Foundations", "NF101", 4, 3, 1, nursing, 1);
        subject.setId(1L);
        Faculty pooled = activeFaculty(500L, other, 6); // not speciality-matched, only in via pool

        CourseOffering offering = new CourseOffering();
        offering.setId(100L);
        offering.setSubject(subject);
        offering.setTermInstance(termInstance);
        offering.setFacultyPool(new java.util.HashSet<>(java.util.Set.of(pooled)));
        when(courseOfferingRepository.findById(100L)).thenReturn(Optional.of(offering));
        when(facultyRepository.findByStatus(FacultyStatus.ACTIVE)).thenReturn(List.of());
        when(facultyRepository.findById(500L)).thenReturn(Optional.of(pooled));
        when(courseOfferingSectionFacultyRepository.findByCourseOfferingIdAndCohortSectionId(100L, 1L))
            .thenReturn(Optional.empty());
        when(studentTermEnrollmentRepository.findDistinctCohortIdsByTermInstanceId(10L, EnrollmentStatus.ENROLLED))
            .thenReturn(new HashSet<>());

        List<EligibleFacultyCandidateDto> candidates = service.getEligibleFacultyForSection(100L, 1L);

        assertThat(candidates).hasSize(1);
        assertThat(candidates.get(0).facultyId()).isEqualTo(500L);
        assertThat(candidates.get(0).inPool()).isTrue();
        assertThat(candidates.get(0).currentlyAssigned()).isFalse();
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
        SkeletonBuilderResponse skeleton = new SkeletonBuilderResponse(1L, "Cohort 1", "Term", List.of(subject), List.of(), List.of(), List.of(), 25, 0L);
        when(timetableSkeletonService.getCohortSkeleton(10L, 1L)).thenReturn(skeleton);

        SkeletonCellResponse placedB = new SkeletonCellResponse(902L, ClassSessionType.THEORY, DayOfWeek.MONDAY, 1L, "1st Period",
            LocalTime.of(9, 0), LocalTime.of(9, 50), null, null, 2L, "B", false, null, null, List.of(),
            100L, "Offering A", "OFFE", null, null, null);
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
}
