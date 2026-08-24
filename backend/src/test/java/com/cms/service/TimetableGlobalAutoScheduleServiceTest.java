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
import com.cms.dto.FacultyCapacityCheckResult;
import com.cms.dto.FacultyOverCapacity;
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
import com.cms.model.CohortSection;
import com.cms.model.CourseOffering;
import com.cms.model.CourseOfferingSectionFaculty;
import com.cms.model.CurriculumSemesterCourse;
import com.cms.model.DesignationMaster;
import com.cms.model.Faculty;
import com.cms.model.Period;
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

    private TimetableGlobalAutoScheduleService service;
    private TermInstance termInstance;
    private Period period1;

    @BeforeEach
    void setUp() {
        service = new TimetableGlobalAutoScheduleService(timetableSkeletonService, timetableStaffingService,
            timetableCapacityPlanningService, courseOfferingService, courseOfferingRepository, classScheduleRepository,
            studentTermEnrollmentRepository, cohortRepository, batchRepository, courseOfferingSectionFacultyRepository,
            facultyRepository, termInstanceRepository, periodRepository, blockedPeriodChecker, classroomRepository,
            courseRegistrationRepository);
        lenient().when(courseOfferingSectionFacultyRepository.findByCourseOfferingId(anyLong())).thenReturn(List.of());

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

    private CourseOfferingDto offeringDto(Long id, String subjectName, Long facultyId) {
        return new CourseOfferingDto(id, 10L, null, null, null, id, subjectName, subjectName.substring(0, 4).toUpperCase(),
            null, null, 1, facultyId, null, true, null, false, null, null, null, null, null, null, null, null, List.of());
    }

    private CourseOffering offeringEntity(Long id, int theoryHours, int labHours, int clinicalHours, Long facultyId) {
        CurriculumSemesterCourse csc = new CurriculumSemesterCourse();
        csc.setTheoryHours(theoryHours);
        csc.setLabHours(labHours);
        csc.setClinicalHours(clinicalHours);
        csc.setIsElective(false);
        CourseOffering offering = new CourseOffering();
        offering.setId(id);
        offering.setCurriculumSemesterCourse(csc);
        offering.setFacultyId(facultyId);
        when(courseOfferingRepository.findById(id)).thenReturn(Optional.of(offering));
        lenient().when(timetableSkeletonService.isElectiveOffering(offering)).thenReturn(false);
        return offering;
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
        when(courseOfferingService.getOfferingsByTermInstanceAndCohort(10L, 1L)).thenReturn(List.of(offeringDto(100L, "Offering A", 500L)));
        when(courseOfferingService.getOfferingsByTermInstanceAndCohort(10L, 2L)).thenReturn(List.of(offeringDto(200L, "Offering B", 500L)));
        when(courseOfferingService.getOfferingsByTermInstanceAndCohort(10L, 3L)).thenReturn(List.of(offeringDto(300L, "Offering C", 500L)));

        CourseOffering offeringA = offeringEntity(100L, 90, 0, 0, 500L);
        offeringEntity(200L, 90, 0, 0, 500L);
        offeringEntity(300L, 90, 0, 0, 500L);

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
        when(courseOfferingService.getOfferingsByTermInstanceAndCohort(10L, 1L)).thenReturn(List.of(offeringDto(100L, "Offering A", 500L)));
        offeringEntity(100L, 90, 0, 0, 500L);
        when(timetableSkeletonService.resolveActiveSections(1L, 10L)).thenReturn(List.of());
        when(batchRepository.findByCourseOfferingId(anyLong())).thenReturn(List.of());

        GlobalCapacityPrecheckResult result = service.precheckCapacity(10L);

        assertThat(result.overCapacityFaculty()).isEmpty();
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
        when(courseOfferingService.getOfferingsByTermInstanceAndCohort(10L, 1L)).thenReturn(List.of(offeringDto(100L, "Offering A", 500L)));
        offeringEntity(100L, 0, 90, 60, 500L);
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
        when(courseOfferingService.getOfferingsByTermInstanceAndCohort(10L, 1L)).thenReturn(List.of(offeringDto(100L, "Offering A", 500L)));
        offeringEntity(100L, 0, 40, 60, 500L);
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
    void precheckCreditsEachSectionsOwnFacultyOverrideForTheoryHours_notThePrimaryFaculty() {
        // Offering: 100 theory hours, 0 lab/clinical, primary faculty XYZ (500) with a 1h/day cap
        // (100h capacity). Cohort has two active sections; Section B has an override to faculty
        // 700 (also 1h/day, 100h capacity). Section A has no override, so its 100h falls to XYZ.
        // XYZ ends up with exactly 100h (Section A only, not both sections' 200h) and fits within
        // their 100h cap; faculty 700 ends up with exactly 100h (Section B only) and also fits --
        // neither is flagged, proving the override actually moved Section B's hours off XYZ rather
        // than just adding a second person on top of the full total.
        when(studentTermEnrollmentRepository.findDistinctCohortIdsByTermInstanceId(10L, EnrollmentStatus.ENROLLED))
            .thenReturn(new HashSet<>(List.of(1L)));
        cohort(1L, "Cohort 1");
        facultyWithDailyCap(500L, "XYZ", 1);
        Faculty sectionBFaculty = facultyWithDailyCap(700L, "Section B Faculty", 1);
        when(courseOfferingService.getOfferingsByTermInstanceAndCohort(10L, 1L)).thenReturn(List.of(offeringDto(100L, "Offering A", 500L)));
        offeringEntity(100L, 100, 0, 0, 500L);

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
        when(courseOfferingService.getOfferingsByTermInstanceAndCohort(10L, 1L)).thenReturn(List.of(offeringDto(100L, "Offering A", 500L)));
        offeringEntity(100L, 200, 0, 0, 500L); // 200h demand vs 100h capacity -> over
        when(timetableSkeletonService.resolveActiveSections(1L, 10L)).thenReturn(List.of());
        when(batchRepository.findByCourseOfferingId(anyLong())).thenReturn(List.of());

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
        when(courseOfferingService.getOfferingsByTermInstanceAndCohort(10L, 1L)).thenReturn(List.of(offeringDto(100L, "Offering A", 500L)));
        CourseOffering offering = offeringEntity(100L, 10, 0, 0, 500L);
        when(timetableSkeletonService.resolveActiveSections(1L, 10L)).thenReturn(List.of());
        when(batchRepository.findByCourseOfferingId(anyLong())).thenReturn(List.of());

        SkeletonSubjectBudget budget = new SkeletonSubjectBudget(ClassSessionType.THEORY, null, null, null, null, 10, 10, 1, 0);
        SkeletonSubjectResponse subject = new SkeletonSubjectResponse(100L, "Offering A", "OFFE", List.of(budget), null, null);
        SkeletonBuilderResponse skeleton = new SkeletonBuilderResponse(1L, "Cohort 1", "Term", List.of(subject), List.of(), List.of(), List.of());
        when(timetableSkeletonService.getCohortSkeleton(10L, 1L)).thenReturn(skeleton);

        SkeletonCellResponse placed = new SkeletonCellResponse(900L, ClassSessionType.THEORY, DayOfWeek.MONDAY, 1L, "1st Period",
            LocalTime.of(9, 0), LocalTime.of(9, 50), null, null, null, null, false, null, null, List.of(),
            100L, "Offering A", "OFFE", null, null, null);
        when(timetableSkeletonService.placeCell(any(SkeletonCellPlacementRequest.class))).thenReturn(placed);
        when(timetableStaffingService.staffCell(eq(900L), any(StaffingAssignmentRequest.class)))
            .thenReturn(new UnstaffedCellResponse(900L, 100L, "Offering A", "OFFE", null, null,
                ClassSessionType.THEORY, DayOfWeek.MONDAY, 1L, "1st Period", LocalTime.of(9, 0), LocalTime.of(9, 50),
                null, null, null, null, null, false, List.of(), null));

        var result = service.runGlobalAutoSchedule(10L, null);

        assertThat(result.totalPlaced()).isEqualTo(1);
        assertThat(result.totalStaffed()).isEqualTo(1);
        assertThat(result.cohortSummaries()).hasSize(1);
        assertThat(result.cohortSummaries().get(0).unplaced()).isEmpty();
        assertThat(result.cohortSummaries().get(0).usedSaturday()).isFalse();
        assertThat(result.electiveUnplaced()).isEmpty();
        verify(timetableStaffingService).staffCell(900L, new StaffingAssignmentRequest(500L, null));
    }

    @Test
    void runReportsUnplacedInsteadOfThrowing_whenNoSlotWorksForBothPlacementAndStaffing() {
        when(studentTermEnrollmentRepository.findDistinctCohortIdsByTermInstanceId(10L, EnrollmentStatus.ENROLLED))
            .thenReturn(new HashSet<>(List.of(1L)));
        cohort(1L, "Cohort 1");
        facultyWithDailyCap(500L, "XYZ", 6);
        when(courseOfferingService.getOfferingsByTermInstanceAndCohort(10L, 1L)).thenReturn(List.of(offeringDto(100L, "Offering A", 500L)));
        offeringEntity(100L, 10, 0, 0, 500L);
        when(timetableSkeletonService.resolveActiveSections(1L, 10L)).thenReturn(List.of());
        when(batchRepository.findByCourseOfferingId(anyLong())).thenReturn(List.of());

        SkeletonSubjectBudget budget = new SkeletonSubjectBudget(ClassSessionType.THEORY, null, null, null, null, 10, 10, 1, 0);
        SkeletonSubjectResponse subject = new SkeletonSubjectResponse(100L, "Offering A", "OFFE", List.of(budget), null, null);
        SkeletonBuilderResponse skeleton = new SkeletonBuilderResponse(1L, "Cohort 1", "Term", List.of(subject), List.of(), List.of(), List.of());
        when(timetableSkeletonService.getCohortSkeleton(10L, 1L)).thenReturn(skeleton);

        when(timetableSkeletonService.placeCell(any(SkeletonCellPlacementRequest.class)))
            .thenThrow(new TimetableConstraintViolationException(List.of(new com.cms.dto.ConstraintViolation("X", "no"))));

        var result = service.runGlobalAutoSchedule(10L, null);

        assertThat(result.totalPlaced()).isEqualTo(0);
        assertThat(result.cohortSummaries()).hasSize(1);
        assertThat(result.cohortSummaries().get(0).unplaced()).hasSize(1);
        assertThat(result.cohortSummaries().get(0).unplaced().get(0).reason())
            .contains("no day/period found where both placement and staffing succeed");

        // 6 days x 1 period exhausted.
        verify(timetableSkeletonService, times(6)).placeCell(any());
        verify(timetableStaffingService, never()).staffCell(anyLong(), any());
    }

    @Test
    void runReportsUnplacedInsteadOfThrowing_whenOfferingHasNoFacultyBound() {
        when(studentTermEnrollmentRepository.findDistinctCohortIdsByTermInstanceId(10L, EnrollmentStatus.ENROLLED))
            .thenReturn(new HashSet<>(List.of(1L)));
        cohort(1L, "Cohort 1");
        when(courseOfferingService.getOfferingsByTermInstanceAndCohort(10L, 1L)).thenReturn(List.of(offeringDto(100L, "Offering A", null)));
        offeringEntity(100L, 10, 0, 0, null);
        when(timetableSkeletonService.resolveActiveSections(1L, 10L)).thenReturn(List.of());
        when(batchRepository.findByCourseOfferingId(anyLong())).thenReturn(List.of());

        SkeletonSubjectBudget budget = new SkeletonSubjectBudget(ClassSessionType.THEORY, null, null, null, null, 10, 10, 1, 0);
        SkeletonSubjectResponse subject = new SkeletonSubjectResponse(100L, "Offering A", "OFFE", List.of(budget), null, null);
        SkeletonBuilderResponse skeleton = new SkeletonBuilderResponse(1L, "Cohort 1", "Term", List.of(subject), List.of(), List.of(), List.of());
        when(timetableSkeletonService.getCohortSkeleton(10L, 1L)).thenReturn(skeleton);

        var result = service.runGlobalAutoSchedule(10L, null);

        assertThat(result.totalPlaced()).isEqualTo(0);
        assertThat(result.cohortSummaries().get(0).unplaced()).extracting(AutoPlaceUnplacedItem::reason)
            .containsExactly("no faculty assigned on its Course Offering");
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
        when(courseOfferingService.getOfferingsByTermInstanceAndCohort(10L, 1L)).thenReturn(List.of(offeringDto(100L, "Offering A", 500L)));
        when(courseOfferingService.getOfferingsByTermInstanceAndCohort(10L, 2L)).thenReturn(List.of(offeringDto(200L, "Offering B", 600L)));
        offeringEntity(100L, 10, 0, 0, 500L);
        offeringEntity(200L, 10, 0, 0, 600L);
        when(timetableSkeletonService.resolveActiveSections(anyLong(), eq(10L))).thenReturn(List.of());
        when(batchRepository.findByCourseOfferingId(anyLong())).thenReturn(List.of());

        SkeletonSubjectBudget budgetA = new SkeletonSubjectBudget(ClassSessionType.THEORY, null, null, null, null, 10, 10, 1, 0);
        SkeletonSubjectResponse subjectA = new SkeletonSubjectResponse(100L, "Offering A", "OFFA", List.of(budgetA), null, null);
        when(timetableSkeletonService.getCohortSkeleton(10L, 1L))
            .thenReturn(new SkeletonBuilderResponse(1L, "Cohort 1", "Term", List.of(subjectA), List.of(), List.of(), List.of()));

        SkeletonSubjectBudget budgetB = new SkeletonSubjectBudget(ClassSessionType.THEORY, null, null, null, null, 10, 10, 1, 0);
        SkeletonSubjectResponse subjectB = new SkeletonSubjectResponse(200L, "Offering B", "OFFB", List.of(budgetB), null, null);
        when(timetableSkeletonService.getCohortSkeleton(10L, 2L))
            .thenReturn(new SkeletonBuilderResponse(2L, "Cohort 2", "Term", List.of(subjectB), List.of(), List.of(), List.of()));

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
                null, null, null, null, null, false, List.of(), null));

        var result = service.runGlobalAutoSchedule(10L, null);

        assertThat(result.totalPlaced()).isEqualTo(1);
        var summaryByCohort = result.cohortSummaries().stream()
            .collect(java.util.stream.Collectors.toMap(com.cms.dto.CohortPlacementSummary::cohortId, s -> s));
        assertThat(summaryByCohort.get(1L).placedCount()).isEqualTo(0);
        assertThat(summaryByCohort.get(1L).unplaced()).hasSize(1);
        assertThat(summaryByCohort.get(2L).placedCount()).isEqualTo(1);
        assertThat(summaryByCohort.get(2L).unplaced()).isEmpty();
    }

    @Test
    void runScopesToOneCohort_whenCohortIdProvided() {
        when(studentTermEnrollmentRepository.findDistinctCohortIdsByTermInstanceId(10L, EnrollmentStatus.ENROLLED))
            .thenReturn(new HashSet<>(List.of(1L, 2L)));
        cohort(1L, "Cohort 1");
        facultyWithDailyCap(500L, "Faculty A", 6);
        when(courseOfferingService.getOfferingsByTermInstanceAndCohort(10L, 1L)).thenReturn(List.of(offeringDto(100L, "Offering A", 500L)));
        offeringEntity(100L, 10, 0, 0, 500L);
        when(timetableSkeletonService.resolveActiveSections(1L, 10L)).thenReturn(List.of());
        when(batchRepository.findByCourseOfferingId(anyLong())).thenReturn(List.of());
        when(timetableSkeletonService.getCohortSkeleton(10L, 1L))
            .thenReturn(new SkeletonBuilderResponse(1L, "Cohort 1", "Term", List.of(), List.of(), List.of(), List.of()));

        var result = service.runGlobalAutoSchedule(10L, 1L);

        assertThat(result.cohortSummaries()).hasSize(1);
        assertThat(result.cohortSummaries().get(0).cohortId()).isEqualTo(1L);
        verify(timetableSkeletonService, never()).getCohortSkeleton(10L, 2L);
    }

    // ── Live single-faculty capacity check (Course Offerings) ─────────

    @Test
    void checkFacultyCapacity_fitsWithinCapacity() {
        when(studentTermEnrollmentRepository.findDistinctCohortIdsByTermInstanceId(10L, EnrollmentStatus.ENROLLED))
            .thenReturn(new HashSet<>(List.of(1L)));
        cohort(1L, "Cohort 1");
        facultyWithDailyCap(500L, "XYZ", 6); // 600h capacity
        when(courseOfferingService.getOfferingsByTermInstanceAndCohort(10L, 1L))
            .thenReturn(List.of(offeringDto(100L, "Offering A", null)));
        offeringEntity(100L, 10, 0, 0, null);
        when(timetableSkeletonService.resolveActiveSections(1L, 10L)).thenReturn(List.of());
        when(batchRepository.findByCourseOfferingId(anyLong())).thenReturn(List.of());

        FacultyCapacityCheckResult result = service.checkFacultyCapacityForOffering(10L, 100L, 500L);

        assertThat(result.overCapacity()).isFalse();
        assertThat(result.currentDemandHours()).isEqualTo(0.0);
        assertThat(result.offeringHours()).isEqualTo(10.0);
        assertThat(result.projectedTotalHours()).isEqualTo(10.0);
        assertThat(result.capacityHours()).isEqualTo(600.0);
    }

    @Test
    void checkFacultyCapacity_exceedsCapacity_sumsExistingDemandPlusThisOffering() {
        when(studentTermEnrollmentRepository.findDistinctCohortIdsByTermInstanceId(10L, EnrollmentStatus.ENROLLED))
            .thenReturn(new HashSet<>(List.of(1L)));
        cohort(1L, "Cohort 1");
        facultyWithDailyCap(500L, "XYZ", 2); // 200h capacity
        when(courseOfferingService.getOfferingsByTermInstanceAndCohort(10L, 1L))
            .thenReturn(List.of(offeringDto(100L, "Offering A", 500L), offeringDto(200L, "Offering B", null)));
        offeringEntity(100L, 150, 0, 0, 500L); // already bound to 500 -- 150h existing demand
        offeringEntity(200L, 90, 0, 0, null);  // being considered for 500 -- 90h
        when(timetableSkeletonService.resolveActiveSections(1L, 10L)).thenReturn(List.of());
        when(batchRepository.findByCourseOfferingId(anyLong())).thenReturn(List.of());

        FacultyCapacityCheckResult result = service.checkFacultyCapacityForOffering(10L, 200L, 500L);

        assertThat(result.currentDemandHours()).isEqualTo(150.0);
        assertThat(result.offeringHours()).isEqualTo(90.0);
        assertThat(result.projectedTotalHours()).isEqualTo(240.0);
        assertThat(result.overCapacity()).isTrue();
        assertThat(result.suggestedMinDailyHours()).isEqualTo(3.0); // ceil(240/100)
    }

    @Test
    void checkFacultyCapacity_reCheckingAlreadyAssignedFaculty_neverDoubleCounts() {
        when(studentTermEnrollmentRepository.findDistinctCohortIdsByTermInstanceId(10L, EnrollmentStatus.ENROLLED))
            .thenReturn(new HashSet<>(List.of(1L)));
        cohort(1L, "Cohort 1");
        facultyWithDailyCap(500L, "XYZ", 2); // 200h capacity
        when(courseOfferingService.getOfferingsByTermInstanceAndCohort(10L, 1L))
            .thenReturn(List.of(offeringDto(100L, "Offering A", 500L)));
        offeringEntity(100L, 150, 0, 0, 500L);
        when(timetableSkeletonService.resolveActiveSections(1L, 10L)).thenReturn(List.of());
        when(batchRepository.findByCourseOfferingId(anyLong())).thenReturn(List.of());

        // Re-checking the SAME faculty already bound to this SAME offering must not add its own
        // 150h contribution a second time (150+150=300 would wrongly exceed the 200h capacity).
        FacultyCapacityCheckResult result = service.checkFacultyCapacityForOffering(10L, 100L, 500L);

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
        when(courseOfferingService.getOfferingsByTermInstanceAndCohort(10L, 1L)).thenReturn(List.of(offeringDto(100L, "Offering A", 500L)));
        when(courseOfferingService.getOfferingsByTermInstanceAndCohort(10L, 2L)).thenReturn(List.of(offeringDto(200L, "Offering B", 500L)));
        when(courseOfferingService.getOfferingsByTermInstanceAndCohort(10L, 3L)).thenReturn(List.of(offeringDto(300L, "Offering C", 500L)));

        offeringEntity(100L, 90, 0, 0, 500L);
        offeringEntity(200L, 90, 0, 0, 500L);
        offeringEntity(300L, 90, 0, 0, 500L);

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
        when(courseOfferingService.getOfferingsByTermInstanceAndCohort(10L, 1L)).thenReturn(List.of(offeringDto(100L, "Offering A", 500L)));
        offeringEntity(100L, 10, 0, 0, 500L);
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
        when(courseOfferingService.getOfferingsByTermInstanceAndCohort(10L, 1L)).thenReturn(List.of(offeringDto(100L, "Offering A", 500L)));
        offeringEntity(100L, 30, 20, 10, 500L); // 30 theory, 20 lab + 10 clinical = 30 combined
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
        when(courseOfferingService.getOfferingsByTermInstanceAndCohort(10L, 1L)).thenReturn(List.of(offeringDto(100L, "Offering A", 500L)));
        when(courseOfferingService.getOfferingsByTermInstanceAndCohort(10L, 2L)).thenReturn(List.of(offeringDto(200L, "Offering B", 600L)));
        offeringEntity(100L, 200, 0, 0, 500L); // 200h > 100h cap
        offeringEntity(200L, 90, 0, 0, 600L);  // 90h < 600h cap
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
            .thenReturn(List.of(offeringDto(100L, "Offering A", null), offeringDto(200L, "Offering B", 500L)));
        offeringEntity(100L, 10, 0, 0, null); // no faculty bound -- a real prerequisite gap
        offeringEntity(200L, 200, 0, 0, 500L); // bound, but 200h > 100h capacity -- over capacity
        when(timetableSkeletonService.resolveActiveSections(1L, 10L)).thenReturn(List.of());
        when(batchRepository.findByCourseOfferingId(anyLong())).thenReturn(List.of());

        SkeletonSubjectBudget budgetA = new SkeletonSubjectBudget(ClassSessionType.THEORY, null, null, null, null, 10, 10, 1, 0);
        SkeletonSubjectResponse subjectA = new SkeletonSubjectResponse(100L, "Offering A", "OFFA", List.of(budgetA), null, null);
        SkeletonSubjectResponse subjectB = new SkeletonSubjectResponse(200L, "Offering B", "OFFB",
            List.of(new SkeletonSubjectBudget(ClassSessionType.THEORY, null, null, null, null, 10, 10, 1, 0)), null, null);
        when(timetableSkeletonService.getCohortSkeleton(10L, 1L))
            .thenReturn(new SkeletonBuilderResponse(1L, "Cohort 1", "Term", List.of(subjectA, subjectB), List.of(), List.of(), List.of()));

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
        when(courseOfferingService.getOfferingsByTermInstanceAndCohort(10L, 1L)).thenReturn(List.of(offeringDto(100L, "Offering A", 500L)));
        offeringEntity(100L, 10, 0, 0, 500L);
        when(timetableSkeletonService.resolveActiveSections(1L, 10L)).thenReturn(List.of());
        when(batchRepository.findByCourseOfferingId(anyLong())).thenReturn(List.of());

        SkeletonSubjectBudget budget = new SkeletonSubjectBudget(ClassSessionType.THEORY, null, null, null, null, 10, 10, 1, 0);
        SkeletonSubjectResponse subject = new SkeletonSubjectResponse(100L, "Offering A", "OFFA", List.of(budget), null, null);
        when(timetableSkeletonService.getCohortSkeleton(10L, 1L))
            .thenReturn(new SkeletonBuilderResponse(1L, "Cohort 1", "Term", List.of(subject), List.of(), List.of(), List.of()));

        GlobalAutoSchedulePrerequisites result = service.checkPrerequisites(10L, null);

        assertThat(result.ready()).isTrue();
        assertThat(result.offeringsWithoutFaculty()).isEmpty();
    }
}
