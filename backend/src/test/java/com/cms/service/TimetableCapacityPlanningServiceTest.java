package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.dto.CapacityPlanResponse;
import com.cms.dto.SuggestedBatchResponse;
import com.cms.dto.SuggestedSectionResponse;
import com.cms.dto.TermCapacityOverviewResponse;
import com.cms.dto.VenueOptionResponse;
import com.cms.model.AcademicYear;
import com.cms.model.ClassSchedule;
import com.cms.model.Classroom;
import com.cms.model.ClinicalVenue;
import com.cms.model.Cohort;
import com.cms.model.CourseOffering;
import com.cms.model.CurriculumSemesterCourse;
import com.cms.model.Lab;
import com.cms.model.Period;
import com.cms.model.StudentTermEnrollment;
import com.cms.model.Subject;
import com.cms.model.TermInstance;
import com.cms.model.enums.ClassScheduleStatus;
import com.cms.model.enums.ClassSessionType;
import com.cms.model.enums.CohortRoomAllocationStatus;
import com.cms.model.enums.EnrollmentStatus;
import com.cms.model.enums.PlanningBasis;
import com.cms.model.enums.TermType;
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

@ExtendWith(MockitoExtension.class)
class TimetableCapacityPlanningServiceTest {

    @Mock private CohortRepository cohortRepository;
    @Mock private CohortSectionRepository cohortSectionRepository;
    @Mock private TermInstanceRepository termInstanceRepository;
    @Mock private StudentTermEnrollmentRepository studentTermEnrollmentRepository;
    @Mock private ClassroomRepository classroomRepository;
    @Mock private LabRepository labRepository;
    @Mock private ClinicalVenueRepository clinicalVenueRepository;
    @Mock private PeriodRepository periodRepository;
    @Mock private ClassScheduleRepository classScheduleRepository;
    @Mock private CalendarEventRepository calendarEventRepository;
    @Mock private CourseOfferingRepository courseOfferingRepository;
    @Mock private BlockedPeriodRepository blockedPeriodRepository;
    @Mock private CohortRoomAllocationRepository cohortRoomAllocationRepository;

    private TimetableCapacityPlanningService service;

    @BeforeEach
    void setUp() {
        service = new TimetableCapacityPlanningService(cohortRepository, cohortSectionRepository, termInstanceRepository,
            studentTermEnrollmentRepository, classroomRepository, labRepository, clinicalVenueRepository, periodRepository,
            classScheduleRepository, calendarEventRepository, courseOfferingRepository, blockedPeriodRepository,
            cohortRoomAllocationRepository);
    }

    private VenueOptionResponse venue(long id, String name, int capacity) {
        return new VenueOptionResponse(id, name, capacity);
    }

    private CourseOffering offering(long id, String subjectName, int labHours, int clinicalHours) {
        CourseOffering offering = new CourseOffering();
        offering.setId(id);
        Subject subject = new Subject();
        subject.setName(subjectName);
        offering.setSubject(subject);
        CurriculumSemesterCourse csc = new CurriculumSemesterCourse();
        csc.setLabHours(labHours);
        csc.setClinicalHours(clinicalHours);
        offering.setCurriculumSemesterCourse(csc);
        return offering;
    }

    @Test
    void suggestSections_splitsEquallyAcrossFewestRoomsNotGreedyFillToCapacity() {
        // 45 students, rooms of 40/30/20 -- filling Hall A to its own 40-cap first (the old
        // behavior) would produce a lopsided 40+5. Two rooms (Hall A, Room B) can each hold an
        // equal ~23-student share, so that's used instead -- Room C is never touched.
        List<VenueOptionResponse> pool = List.of(
            venue(1L, "Hall A", 40),
            venue(2L, "Room B", 30),
            venue(3L, "Room C", 20));

        List<SuggestedSectionResponse> sections = service.suggestSections(pool, 45);

        assertThat(sections).hasSize(2);
        assertThat(sections.get(0).classroomId()).isEqualTo(1L);
        assertThat(sections.get(0).plannedSize()).isEqualTo(23);
        assertThat(sections.get(1).classroomId()).isEqualTo(2L);
        assertThat(sections.get(1).plannedSize()).isEqualTo(22);
        assertThat(sections.stream().mapToInt(SuggestedSectionResponse::plannedSize).sum()).isEqualTo(45);
    }

    @Test
    void suggestSections_neverProducesAFractionalSplit() {
        // 95 across 2 rooms is 48+47, never 47.5 each.
        List<VenueOptionResponse> pool = List.of(venue(1L, "Room A", 60), venue(2L, "Room B", 60));

        List<SuggestedSectionResponse> sections = service.suggestSections(pool, 95);

        assertThat(sections).extracting(SuggestedSectionResponse::plannedSize).containsExactly(48, 47);
    }

    @Test
    void suggestSections_oneRoomAlreadyFitsWholeCohort() {
        List<VenueOptionResponse> pool = List.of(venue(1L, "Hall A", 60));

        List<SuggestedSectionResponse> sections = service.suggestSections(pool, 50);

        assertThat(sections).hasSize(1);
        assertThat(sections.get(0).plannedSize()).isEqualTo(50);
    }

    @Test
    void suggestSections_fallsBackToGreedyFillWhenNoEqualSplitIsFeasible() {
        // Genuine shortage: 100 students, but only an 80-cap room free -- no N (even using every
        // room) can support an equal split, so this falls back to greedy fill-to-capacity and
        // legitimately under-covers (80 of 100), which the term-wide sufficiency check is meant to
        // catch -- it must never silently claim more seats than the room actually has.
        List<VenueOptionResponse> pool = List.of(venue(1L, "Room A", 80));

        List<SuggestedSectionResponse> sections = service.suggestSections(pool, 100);

        assertThat(sections).hasSize(1);
        assertThat(sections.get(0).plannedSize()).isEqualTo(80);
    }

    private CourseOffering offeringWithEligibleLab(long id, String subjectName, int labHours, Lab eligibleLab) {
        CourseOffering offering = offering(id, subjectName, labHours, 0);
        offering.getSubject().setEligibleLabs(new HashSet<>(List.of(eligibleLab)));
        return offering;
    }

    private CourseOffering offeringWithEligibleLabs(long id, String subjectName, int labHours, List<VenueOptionResponse> eligibleLabs) {
        CourseOffering offering = offering(id, subjectName, labHours, 0);
        Set<Lab> labs = new HashSet<>();
        for (VenueOptionResponse v : eligibleLabs) {
            Lab lab = new Lab();
            lab.setId(v.id());
            labs.add(lab);
        }
        offering.getSubject().setEligibleLabs(labs);
        return offering;
    }

    private CourseOffering offeringWithEligibleClinicalVenues(long id, String subjectName, int clinicalHours,
                                                                List<VenueOptionResponse> eligibleVenues) {
        CourseOffering offering = offering(id, subjectName, 0, clinicalHours);
        Set<ClinicalVenue> venues = new HashSet<>();
        for (VenueOptionResponse v : eligibleVenues) {
            ClinicalVenue cv = new ClinicalVenue();
            cv.setId(v.id());
            venues.add(cv);
        }
        offering.getSubject().setEligibleClinicalVenues(venues);
        return offering;
    }

    @Test
    void suggestLabClinicalBatches_sharesOneVenueWhenItFitsEverySection() {
        List<SuggestedSectionResponse> sections = List.of(
            new SuggestedSectionResponse("Section 1", 1L, "Hall A", 40, 30),
            new SuggestedSectionResponse("Section 2", 2L, "Room B", 30, 25));
        List<VenueOptionResponse> labs = List.of(
            venue(10L, "Small Lab", 20),
            venue(11L, "Medium Lab", 30),
            venue(12L, "Big Lab", 60));
        CourseOffering offering = offeringWithEligibleLabs(100L, "Anatomy", 4, labs);

        TimetableCapacityPlanningService.LabClinicalSuggestion result = service.suggestLabClinicalBatches(List.of(offering), sections, labs, List.of());

        assertThat(result.mappingIssues()).isEmpty();
        List<SuggestedBatchResponse> batches = result.batches();
        assertThat(batches).hasSize(2);
        assertThat(batches).allMatch(b -> b.sessionType() == ClassSessionType.LAB);
        // Smallest venue that still fits the LARGEST section (30) -- shared across both rows.
        assertThat(batches).allMatch(b -> b.venueId() == 11L);
        assertThat(batches).allMatch(b -> b.batchLabel() == null);
        assertThat(batches.stream().mapToInt(SuggestedBatchResponse::plannedSize).sum()).isEqualTo(55);
    }

    @Test
    void suggestLabClinicalBatches_splitsEquallyWhenNoSingleVenueFitsTheSection() {
        // 35 students, two 20-cap wards -- equal split is 18+17 (not a lopsided fill-to-capacity
        // like 20+15), and never a fractional 17.5 each.
        List<SuggestedSectionResponse> sections = List.of(
            new SuggestedSectionResponse("Section 1", 1L, "Hall A", 35, 35));
        List<VenueOptionResponse> clinicalVenues = List.of(
            venue(20L, "Ward A", 20),
            venue(21L, "Ward B", 20));
        CourseOffering offering = offeringWithEligibleClinicalVenues(200L, "Fundamentals of Nursing", 6, clinicalVenues);

        TimetableCapacityPlanningService.LabClinicalSuggestion result = service.suggestLabClinicalBatches(List.of(offering), sections, List.of(), clinicalVenues);

        assertThat(result.mappingIssues()).isEmpty();
        List<SuggestedBatchResponse> batches = result.batches();
        assertThat(batches).hasSize(2);
        assertThat(batches).allMatch(b -> b.sessionType() == ClassSessionType.CLINICAL);
        assertThat(batches).allMatch(b -> "Section 1".equals(b.sectionLabel()));
        assertThat(batches.get(0).batchLabel()).isEqualTo("Batch 1");
        assertThat(batches.get(0).plannedSize()).isEqualTo(18);
        assertThat(batches.get(1).batchLabel()).isEqualTo("Batch 2");
        assertThat(batches.get(1).plannedSize()).isEqualTo(17);
        // Exactly covers the section's headcount, never more or less.
        assertThat(batches.stream().mapToInt(SuggestedBatchResponse::plannedSize).sum()).isEqualTo(35);
        assertThat(batches).allMatch(b -> b.plannedSize() <= 20);
    }

    @Test
    void suggestLabClinicalBatches_skipsOfferingsWithNoLabOrClinicalHours() {
        List<SuggestedSectionResponse> sections = List.of(new SuggestedSectionResponse("Section 1", 1L, "Hall A", 50, 50));
        CourseOffering theoryOnly = offering(300L, "Nursing Ethics", 0, 0);

        TimetableCapacityPlanningService.LabClinicalSuggestion result = service.suggestLabClinicalBatches(
            List.of(theoryOnly), sections, List.of(venue(10L, "Lab", 60)), List.of());

        assertThat(result.batches()).isEmpty();
        assertThat(result.mappingIssues()).isEmpty();
    }

    @Test
    void suggestLabClinicalBatches_bothLabAndClinicalHoursProduceTwoGroups() {
        List<SuggestedSectionResponse> sections = List.of(new SuggestedSectionResponse("Section 1", 1L, "Hall A", 50, 50));
        List<VenueOptionResponse> labs = List.of(venue(10L, "Lab", 60));
        List<VenueOptionResponse> wards = List.of(venue(20L, "Ward", 60));
        CourseOffering clinicalSubject = offering(400L, "Fundamentals of Nursing", 3, 6);
        clinicalSubject.getSubject().setEligibleLabs(new HashSet<>(List.of(labWithId(10L))));
        clinicalSubject.getSubject().setEligibleClinicalVenues(new HashSet<>(List.of(clinicalVenueWithId(20L))));

        TimetableCapacityPlanningService.LabClinicalSuggestion result = service.suggestLabClinicalBatches(
            List.of(clinicalSubject), sections, labs, wards);

        assertThat(result.mappingIssues()).isEmpty();
        List<SuggestedBatchResponse> batches = result.batches();
        assertThat(batches).hasSize(2);
        assertThat(batches).anyMatch(b -> b.sessionType() == ClassSessionType.LAB && b.venueId() == 10L);
        assertThat(batches).anyMatch(b -> b.sessionType() == ClassSessionType.CLINICAL && b.venueId() == 20L);
    }

    private Lab labWithId(long id) {
        Lab lab = new Lab();
        lab.setId(id);
        return lab;
    }

    private ClinicalVenue clinicalVenueWithId(long id) {
        ClinicalVenue venue = new ClinicalVenue();
        venue.setId(id);
        return venue;
    }

    @Test
    void suggestLabClinicalBatches_prefersEligibleLabWhenItCoversTheDemand() {
        List<SuggestedSectionResponse> sections = List.of(new SuggestedSectionResponse("Section 1", 1L, "Hall A", 60, 50));
        Lab obgLab = new Lab();
        obgLab.setId(11L);
        CourseOffering offering = offeringWithEligibleLab(500L, "OBG Nursing", 4, obgLab);
        // A bigger, non-eligible lab also exists -- the subject's own eligible lab (60-cap, enough
        // for the 50-strong section) must still be used, never the bigger unrelated one.
        List<VenueOptionResponse> allLabs = List.of(venue(10L, "General Lab", 100), venue(11L, "OBG Lab", 60));

        TimetableCapacityPlanningService.LabClinicalSuggestion result = service.suggestLabClinicalBatches(List.of(offering), sections, allLabs, List.of());

        assertThat(result.mappingIssues()).isEmpty();
        assertThat(result.batches()).hasSize(1);
        assertThat(result.batches().get(0).venueId()).isEqualTo(11L);
        assertThat(result.batches().get(0).eligibleVenueIds()).containsExactly(11L);
    }

    @Test
    void suggestLabClinicalBatches_reusesSingleDesignatedLabAcrossSequentialTurnsWhenTooSmallForOneBatch() {
        List<SuggestedSectionResponse> sections = List.of(new SuggestedSectionResponse("Section 1", 1L, "Hall A", 60, 50));
        Lab obgLab = new Lab();
        obgLab.setId(11L);
        CourseOffering offering = offeringWithEligibleLab(500L, "OBG Nursing", 4, obgLab);
        // The subject's only eligible lab (30-cap) can't seat the 50-strong section in one sitting --
        // two batches of the same subject are two SEPARATE scheduled sessions, not simultaneous ones,
        // so the same designated lab is reused across sequential turns (never a fractional split,
        // 25+25 not two under-covering batches) rather than falling back to an unrelated venue or
        // leaving the cohort under-covered. Never touches the bigger non-eligible "General Lab".
        List<VenueOptionResponse> allLabs = List.of(venue(10L, "General Lab", 100), venue(11L, "OBG Lab", 30));

        TimetableCapacityPlanningService.LabClinicalSuggestion result = service.suggestLabClinicalBatches(List.of(offering), sections, allLabs, List.of());

        assertThat(result.mappingIssues()).isEmpty();
        List<SuggestedBatchResponse> batches = result.batches();
        assertThat(batches).hasSize(2);
        assertThat(batches).allMatch(b -> b.venueId() == 11L);
        assertThat(batches).extracting(SuggestedBatchResponse::batchLabel).containsExactly("Batch 1", "Batch 2");
        assertThat(batches).extracting(SuggestedBatchResponse::plannedSize).containsExactly(25, 25);
        assertThat(batches.stream().mapToInt(SuggestedBatchResponse::plannedSize).sum()).isEqualTo(50);
    }

    @Test
    void suggestLabClinicalBatches_producesNoBatchAndRecordsIssueWhenSubjectHasNoDesignatedLab() {
        List<SuggestedSectionResponse> sections = List.of(new SuggestedSectionResponse("Section 1", 1L, "Hall A", 60, 50));
        CourseOffering offering = offering(500L, "Anatomy", 4, 0);

        TimetableCapacityPlanningService.LabClinicalSuggestion result = service.suggestLabClinicalBatches(
            List.of(offering), sections, List.of(venue(10L, "General Lab", 100)), List.of());

        assertThat(result.batches()).isEmpty();
        assertThat(result.mappingIssues()).containsExactly("'Anatomy' has no designated Lab configured");
    }

    @Test
    void suggestLabClinicalBatches_recordsDifferentMessageWhenNoActiveVenueOfTypeExistsAtAll() {
        List<SuggestedSectionResponse> sections = List.of(new SuggestedSectionResponse("Section 1", 1L, "Hall A", 60, 50));
        CourseOffering offering = offering(500L, "Anatomy", 4, 0);

        TimetableCapacityPlanningService.LabClinicalSuggestion result = service.suggestLabClinicalBatches(List.of(offering), sections, List.of(), List.of());

        assertThat(result.batches()).isEmpty();
        assertThat(result.mappingIssues()).containsExactly("'Anatomy' needs a Lab but none exist yet -- create one");
    }

    private Cohort cohort(long id, String displayName) {
        Cohort cohort = new Cohort();
        cohort.setId(id);
        cohort.setDisplayName(displayName);
        return cohort;
    }

    @Test
    void getTermOverview_computesTheorySufficiencyAndRoomInventory() {
        TermInstance term = new TermInstance();
        term.setId(10L);
        AcademicYear year = new AcademicYear();
        year.setId(1L);
        year.setName("2026-27");
        term.setAcademicYear(year);
        term.setTermType(TermType.ODD);
        term.setStartDate(LocalDate.of(2026, 1, 1));
        term.setEndDate(LocalDate.of(2026, 1, 28));
        when(termInstanceRepository.findById(10L)).thenReturn(Optional.of(term));

        when(studentTermEnrollmentRepository.findDistinctCohortIdsByTermInstanceId(10L, EnrollmentStatus.ENROLLED))
            .thenReturn(Set.of(1L, 2L));

        Cohort cohortA = cohort(1L, "Cohort A");
        Cohort cohortB = cohort(2L, "Cohort B");
        when(cohortRepository.findAllById(any())).thenReturn(List.of(cohortA, cohortB));
        when(cohortRepository.findByIdWithCourse(1L)).thenReturn(Optional.of(cohortA));
        when(cohortRepository.findByIdWithCourse(2L)).thenReturn(Optional.of(cohortB));
        when(studentTermEnrollmentRepository.countByTermInstanceIdAndCohortIdAndStatus(10L, 1L, EnrollmentStatus.ENROLLED))
            .thenReturn(40L);
        when(studentTermEnrollmentRepository.countByTermInstanceIdAndCohortIdAndStatus(10L, 2L, EnrollmentStatus.ENROLLED))
            .thenReturn(30L);
        when(studentTermEnrollmentRepository.findFirstByTermInstanceIdAndCohortIdAndStatus(10L, 1L, EnrollmentStatus.ENROLLED))
            .thenReturn(Optional.empty());
        when(studentTermEnrollmentRepository.findFirstByTermInstanceIdAndCohortIdAndStatus(10L, 2L, EnrollmentStatus.ENROLLED))
            .thenReturn(Optional.empty());

        // Cohort B already has a committed allocation this term -- excluded from demand and
        // suggestions, even though it would otherwise fit fine.
        when(cohortRoomAllocationRepository.existsByCohortIdAndTermInstanceIdAndStatus(1L, 10L, CohortRoomAllocationStatus.COMMITTED))
            .thenReturn(false);
        when(cohortRoomAllocationRepository.existsByCohortIdAndTermInstanceIdAndStatus(2L, 10L, CohortRoomAllocationStatus.COMMITTED))
            .thenReturn(true);

        Classroom hallA = new Classroom();
        hallA.setId(1L);
        hallA.setName("Hall A");
        hallA.setCapacity(100);
        when(classroomRepository.findByIsActiveTrueOrderByNameAsc()).thenReturn(List.of(hallA));
        when(labRepository.findAll()).thenReturn(List.of());
        when(clinicalVenueRepository.findByIsActiveTrueOrderByNameAsc()).thenReturn(List.of());
        when(periodRepository.findByIsActiveTrueOrderByPeriodOrderAsc()).thenReturn(List.of());
        when(cohortSectionRepository.findByTermInstanceIdAndIsActiveTrue(10L)).thenReturn(List.of());
        when(calendarEventRepository.findNonTeachingDaysOverlapping(1L, term.getStartDate(), term.getEndDate())).thenReturn(List.of());
        when(blockedPeriodRepository.findApplicableInRange(term.getStartDate(), term.getEndDate())).thenReturn(List.of());
        when(classScheduleRepository.findByTermInstanceIdAndStatus(eq(10L), any())).thenReturn(List.of());
        // semesterNumber resolves to null for both cohorts (no enrollment stubbed above), so
        // getPlan() short-circuits nonElectiveOfferings() to List.of() without ever querying
        // courseOfferingRepository -- no stub needed for it here.

        TermCapacityOverviewResponse overview = service.getTermOverview(10L, PlanningBasis.ENROLLED);

        // Hall A (cap 100) is unclaimed and free; only Cohort A (40) still needs planning --
        // Cohort B's 30 is excluded since it's already committed. 100 >= 40, so sufficient.
        assertThat(overview.theorySufficient()).isTrue();
        assertThat(overview.totalFreeClassroomCapacity()).isEqualTo(100);
        assertThat(overview.totalNotPlannedStrength()).isEqualTo(40);
        assertThat(overview.theorySufficiencyMessage()).isNull();

        assertThat(overview.cohorts()).hasSize(2);
        var cohortARow = overview.cohorts().stream().filter(r -> r.cohortId().equals(1L)).findFirst().orElseThrow();
        var cohortBRow = overview.cohorts().stream().filter(r -> r.cohortId().equals(2L)).findFirst().orElseThrow();
        assertThat(cohortARow.hasCommittedAllocation()).isFalse();
        assertThat(cohortARow.suggestedSections()).hasSize(1);
        assertThat(cohortBRow.hasCommittedAllocation()).isTrue();
        assertThat(cohortBRow.suggestedSections()).isEmpty();

        assertThat(overview.roomInventory()).hasSize(1);
        var hallARow = overview.roomInventory().get(0);
        assertThat(hallARow.roomType()).isEqualTo("CLASSROOM");
        assertThat(hallARow.claimedByCohortLabel()).isNull();
        // Only Cohort A's suggestion references Hall A -- Cohort B is committed and contributes none.
        assertThat(hallARow.suggestedBookingCount()).isEqualTo(1);
    }

    @Test
    void getTermOverview_doesNotSuggestTheSameRoomToTwoNotPlannedCohorts() {
        TermInstance term = new TermInstance();
        term.setId(10L);
        AcademicYear year = new AcademicYear();
        year.setId(1L);
        year.setName("2026-27");
        term.setAcademicYear(year);
        term.setTermType(TermType.ODD);
        term.setStartDate(LocalDate.of(2026, 1, 1));
        term.setEndDate(LocalDate.of(2026, 1, 28));
        when(termInstanceRepository.findById(10L)).thenReturn(Optional.of(term));

        when(studentTermEnrollmentRepository.findDistinctCohortIdsByTermInstanceId(10L, EnrollmentStatus.ENROLLED))
            .thenReturn(Set.of(1L, 2L));

        // "BSc Nursing (2025-2029)" and "BSc Nursing (2026-2030)" from the real bug report -- two
        // different, both-Not-Planned cohorts reviewed side by side on the same bulk screen.
        Cohort cohortA = cohort(1L, "BSc Nursing (2025-2029)");
        Cohort cohortB = cohort(2L, "BSc Nursing (2026-2030)");
        when(cohortRepository.findAllById(any())).thenReturn(List.of(cohortA, cohortB));
        when(cohortRepository.findByIdWithCourse(1L)).thenReturn(Optional.of(cohortA));
        when(cohortRepository.findByIdWithCourse(2L)).thenReturn(Optional.of(cohortB));
        when(studentTermEnrollmentRepository.countByTermInstanceIdAndCohortIdAndStatus(10L, 1L, EnrollmentStatus.ENROLLED))
            .thenReturn(100L);
        when(studentTermEnrollmentRepository.countByTermInstanceIdAndCohortIdAndStatus(10L, 2L, EnrollmentStatus.ENROLLED))
            .thenReturn(100L);
        when(studentTermEnrollmentRepository.findFirstByTermInstanceIdAndCohortIdAndStatus(10L, 1L, EnrollmentStatus.ENROLLED))
            .thenReturn(Optional.empty());
        when(studentTermEnrollmentRepository.findFirstByTermInstanceIdAndCohortIdAndStatus(10L, 2L, EnrollmentStatus.ENROLLED))
            .thenReturn(Optional.empty());

        // Neither cohort has committed yet -- both are still "Not Planned".
        when(cohortRoomAllocationRepository.existsByCohortIdAndTermInstanceIdAndStatus(1L, 10L, CohortRoomAllocationStatus.COMMITTED))
            .thenReturn(false);
        when(cohortRoomAllocationRepository.existsByCohortIdAndTermInstanceIdAndStatus(2L, 10L, CohortRoomAllocationStatus.COMMITTED))
            .thenReturn(false);

        Classroom room101 = new Classroom("Room 101", null, null, 80);
        room101.setId(1L);
        Classroom room102 = new Classroom("Room 102", null, null, 80);
        room102.setId(2L);
        when(classroomRepository.findByIsActiveTrueOrderByNameAsc()).thenReturn(List.of(room101, room102));
        when(labRepository.findAll()).thenReturn(List.of());
        when(clinicalVenueRepository.findByIsActiveTrueOrderByNameAsc()).thenReturn(List.of());
        when(periodRepository.findByIsActiveTrueOrderByPeriodOrderAsc()).thenReturn(List.of());
        // No real CohortSection claims exist yet for either cohort -- both are still Not Planned.
        when(cohortSectionRepository.findByTermInstanceIdAndIsActiveTrue(10L)).thenReturn(List.of());
        when(calendarEventRepository.findNonTeachingDaysOverlapping(1L, term.getStartDate(), term.getEndDate())).thenReturn(List.of());
        when(blockedPeriodRepository.findApplicableInRange(term.getStartDate(), term.getEndDate())).thenReturn(List.of());
        when(classScheduleRepository.findByTermInstanceIdAndStatus(eq(10L), any())).thenReturn(List.of());

        TermCapacityOverviewResponse overview = service.getTermOverview(10L, PlanningBasis.ENROLLED);

        var cohortARow = overview.cohorts().stream().filter(r -> r.cohortId().equals(1L)).findFirst().orElseThrow();
        var cohortBRow = overview.cohorts().stream().filter(r -> r.cohortId().equals(2L)).findFirst().orElseThrow();

        // Cohort A (planned first, alphabetically) claims both rooms across its two sections.
        assertThat(cohortARow.suggestedSections()).extracting(SuggestedSectionResponse::classroomId)
            .containsExactlyInAnyOrder(1L, 2L);

        // Cohort B must NOT be handed either room already claimed by Cohort A in this same pass --
        // with only two classrooms in the whole inventory and both already taken, Cohort B gets no
        // suggested sections at all (correctly reflecting genuine insufficient capacity) rather than
        // the pre-fix bug of independently re-suggesting Room 101 / Room 102 right back to it.
        assertThat(cohortBRow.suggestedSections()).isEmpty();

        Set<Long> roomsClaimedByA = cohortARow.suggestedSections().stream()
            .map(SuggestedSectionResponse::classroomId).collect(java.util.stream.Collectors.toSet());
        Set<Long> roomsClaimedByB = cohortBRow.suggestedSections().stream()
            .map(SuggestedSectionResponse::classroomId).collect(java.util.stream.Collectors.toSet());
        assertThat(java.util.Collections.disjoint(roomsClaimedByA, roomsClaimedByB)).isTrue();
    }

    @Test
    void getTermOverview_theorySufficiencyReflectsRealBinPackingNotNaiveCapacitySum() {
        TermInstance term = new TermInstance();
        term.setId(10L);
        AcademicYear year = new AcademicYear();
        year.setId(1L);
        year.setName("2026-27");
        term.setAcademicYear(year);
        term.setTermType(TermType.ODD);
        term.setStartDate(LocalDate.of(2026, 1, 1));
        term.setEndDate(LocalDate.of(2026, 1, 28));
        when(termInstanceRepository.findById(10L)).thenReturn(Optional.of(term));

        when(studentTermEnrollmentRepository.findDistinctCohortIdsByTermInstanceId(10L, EnrollmentStatus.ENROLLED))
            .thenReturn(Set.of(1L, 2L));

        // Two 100-student cohorts (200 total demand) against three 80-cap rooms (240 total
        // capacity) -- a naive SUM(capacity) >= SUM(demand) check (240 >= 200) would wrongly call
        // this sufficient. Reality: Cohort A (planned first, alphabetically) needs TWO of the three
        // rooms to seat its own 100 (80+20, since no single room seats 100 -- exactly the
        // real-world case the user reported: "100 students, 60/80-cap rooms, needs at least two
        // classrooms"). That leaves Cohort B only one room (80-cap) for its own 100 -- it can't
        // borrow the unused seats sitting in Cohort A's rooms, so it comes up 20 short.
        Cohort cohortA = cohort(1L, "Cohort A");
        Cohort cohortB = cohort(2L, "Cohort B");
        when(cohortRepository.findAllById(any())).thenReturn(List.of(cohortA, cohortB));
        when(cohortRepository.findByIdWithCourse(1L)).thenReturn(Optional.of(cohortA));
        when(cohortRepository.findByIdWithCourse(2L)).thenReturn(Optional.of(cohortB));
        when(studentTermEnrollmentRepository.countByTermInstanceIdAndCohortIdAndStatus(10L, 1L, EnrollmentStatus.ENROLLED))
            .thenReturn(100L);
        when(studentTermEnrollmentRepository.countByTermInstanceIdAndCohortIdAndStatus(10L, 2L, EnrollmentStatus.ENROLLED))
            .thenReturn(100L);
        when(studentTermEnrollmentRepository.findFirstByTermInstanceIdAndCohortIdAndStatus(10L, 1L, EnrollmentStatus.ENROLLED))
            .thenReturn(Optional.empty());
        when(studentTermEnrollmentRepository.findFirstByTermInstanceIdAndCohortIdAndStatus(10L, 2L, EnrollmentStatus.ENROLLED))
            .thenReturn(Optional.empty());
        when(cohortRoomAllocationRepository.existsByCohortIdAndTermInstanceIdAndStatus(1L, 10L, CohortRoomAllocationStatus.COMMITTED))
            .thenReturn(false);
        when(cohortRoomAllocationRepository.existsByCohortIdAndTermInstanceIdAndStatus(2L, 10L, CohortRoomAllocationStatus.COMMITTED))
            .thenReturn(false);

        Classroom room1 = new Classroom("Room 1", null, null, 80);
        room1.setId(1L);
        Classroom room2 = new Classroom("Room 2", null, null, 80);
        room2.setId(2L);
        Classroom room3 = new Classroom("Room 3", null, null, 80);
        room3.setId(3L);
        when(classroomRepository.findByIsActiveTrueOrderByNameAsc()).thenReturn(List.of(room1, room2, room3));
        when(labRepository.findAll()).thenReturn(List.of());
        when(clinicalVenueRepository.findByIsActiveTrueOrderByNameAsc()).thenReturn(List.of());
        when(periodRepository.findByIsActiveTrueOrderByPeriodOrderAsc()).thenReturn(List.of());
        when(cohortSectionRepository.findByTermInstanceIdAndIsActiveTrue(10L)).thenReturn(List.of());
        when(calendarEventRepository.findNonTeachingDaysOverlapping(1L, term.getStartDate(), term.getEndDate())).thenReturn(List.of());
        when(blockedPeriodRepository.findApplicableInRange(term.getStartDate(), term.getEndDate())).thenReturn(List.of());
        when(classScheduleRepository.findByTermInstanceIdAndStatus(eq(10L), any())).thenReturn(List.of());

        TermCapacityOverviewResponse overview = service.getTermOverview(10L, PlanningBasis.ENROLLED);

        // The naive sum (240 >= 200) would have said "sufficient" -- the real per-cohort bin-packing
        // must catch that Cohort B is still 20 seats short and correctly report insufficient.
        assertThat(overview.totalFreeClassroomCapacity()).isEqualTo(240);
        assertThat(overview.totalNotPlannedStrength()).isEqualTo(200);
        assertThat(overview.theorySufficient()).isFalse();
        // Names the exact shortfall (20), not just a vague "increase capacity" -- a small bump
        // (e.g. 60->80) could still leave the cohort short if it doesn't close this real gap.
        assertThat(overview.theorySufficiencyMessage())
            .contains("Cohort B").contains("80 of 100 seated").contains("20 short");
        assertThat(overview.theorySufficiencyMessage()).doesNotContain("Cohort A");
    }

    @Test
    void getPlan_excludesSharedCapacityClassroomsFromTheorySectioning() {
        TermInstance term = new TermInstance();
        term.setId(10L);
        AcademicYear year = new AcademicYear();
        year.setId(1L);
        year.setName("2026-27");
        term.setAcademicYear(year);
        term.setTermType(TermType.ODD);
        term.setStartDate(LocalDate.of(2026, 1, 1));
        term.setEndDate(LocalDate.of(2026, 1, 28));
        when(termInstanceRepository.findById(10L)).thenReturn(Optional.of(term));

        Cohort cohortA = cohort(1L, "Cohort A");
        when(cohortRepository.findByIdWithCourse(1L)).thenReturn(Optional.of(cohortA));
        when(studentTermEnrollmentRepository.countByTermInstanceIdAndCohortIdAndStatus(10L, 1L, EnrollmentStatus.ENROLLED))
            .thenReturn(40L);
        when(studentTermEnrollmentRepository.findFirstByTermInstanceIdAndCohortIdAndStatus(10L, 1L, EnrollmentStatus.ENROLLED))
            .thenReturn(Optional.empty());

        Classroom regularRoom = new Classroom("Room 101", null, null, 100);
        regularRoom.setId(1L);
        Classroom sharedHall = new Classroom("Drawing Hall", null, null, 200);
        sharedHall.setId(2L);
        sharedHall.setAllowsConcurrentSharing(true);
        when(classroomRepository.findByIsActiveTrueOrderByNameAsc()).thenReturn(List.of(regularRoom, sharedHall));
        when(labRepository.findAll()).thenReturn(List.of());
        when(clinicalVenueRepository.findByIsActiveTrueOrderByNameAsc()).thenReturn(List.of());
        when(periodRepository.findByIsActiveTrueOrderByPeriodOrderAsc()).thenReturn(List.of());
        when(cohortSectionRepository.findByTermInstanceIdAndIsActiveTrue(10L)).thenReturn(List.of());
        when(calendarEventRepository.findNonTeachingDaysOverlapping(1L, term.getStartDate(), term.getEndDate())).thenReturn(List.of());
        when(blockedPeriodRepository.findApplicableInRange(term.getStartDate(), term.getEndDate())).thenReturn(List.of());
        when(classScheduleRepository.findByTermInstanceIdAndStatus(eq(10L), any())).thenReturn(List.of());

        CapacityPlanResponse plan = service.getPlan(10L, 1L, PlanningBasis.ENROLLED);

        // The 200-cap Drawing Hall would otherwise be the obvious single-room fit for 40 students --
        // it must never appear as a Theory-sectioning candidate, only the regular 100-cap room.
        assertThat(plan.fittingClassrooms()).extracting(VenueOptionResponse::id).containsExactly(1L);
        assertThat(plan.classroomsForSectioning()).extracting(VenueOptionResponse::id).containsExactly(1L);
        assertThat(plan.suggestedSections()).hasSize(1);
        assertThat(plan.suggestedSections().get(0).classroomId()).isEqualTo(1L);
    }

    @Test
    void getPlan_flagsInsufficientLabClinicalMappingWithoutAffectingTheory() {
        TermInstance term = new TermInstance();
        term.setId(10L);
        AcademicYear year = new AcademicYear();
        year.setId(1L);
        year.setName("2026-27");
        term.setAcademicYear(year);
        term.setTermType(TermType.ODD);
        term.setStartDate(LocalDate.of(2026, 1, 1));
        term.setEndDate(LocalDate.of(2026, 1, 28));
        when(termInstanceRepository.findById(10L)).thenReturn(Optional.of(term));

        Cohort cohortA = cohort(1L, "Cohort A");
        when(cohortRepository.findByIdWithCourse(1L)).thenReturn(Optional.of(cohortA));
        when(studentTermEnrollmentRepository.countByTermInstanceIdAndCohortIdAndStatus(10L, 1L, EnrollmentStatus.ENROLLED))
            .thenReturn(40L);
        StudentTermEnrollment enrollment = new StudentTermEnrollment();
        enrollment.setSemesterNumber(3);
        when(studentTermEnrollmentRepository.findFirstByTermInstanceIdAndCohortIdAndStatus(10L, 1L, EnrollmentStatus.ENROLLED))
            .thenReturn(Optional.of(enrollment));

        Classroom room = new Classroom("Room 101", null, null, 100);
        room.setId(1L);
        when(classroomRepository.findByIsActiveTrueOrderByNameAsc()).thenReturn(List.of(room));
        // No active labs anywhere -- Anatomy's Lab-hour requirement can't be mapped to anything.
        when(labRepository.findAll()).thenReturn(List.of());
        when(clinicalVenueRepository.findByIsActiveTrueOrderByNameAsc()).thenReturn(List.of());
        when(periodRepository.findByIsActiveTrueOrderByPeriodOrderAsc()).thenReturn(List.of());
        when(cohortSectionRepository.findByTermInstanceIdAndIsActiveTrue(10L)).thenReturn(List.of());
        when(calendarEventRepository.findNonTeachingDaysOverlapping(1L, term.getStartDate(), term.getEndDate())).thenReturn(List.of());
        when(blockedPeriodRepository.findApplicableInRange(term.getStartDate(), term.getEndDate())).thenReturn(List.of());
        when(classScheduleRepository.findByTermInstanceIdAndStatus(eq(10L), any())).thenReturn(List.of());

        CourseOffering offering = offering(900L, "Anatomy", 4, 0);
        when(courseOfferingRepository.findByTermInstanceIdAndSemesterNumber(10L, 3)).thenReturn(List.of(offering));

        CapacityPlanResponse plan = service.getPlan(10L, 1L, PlanningBasis.ENROLLED);

        assertThat(plan.theoryFits()).isTrue();
        assertThat(plan.suggestedLabClinicalBatches()).isEmpty();
        assertThat(plan.labClinicalMappingSufficient()).isFalse();
        assertThat(plan.labClinicalMappingIssuesMessage())
            .contains("Anatomy").contains("needs a Lab but none exist yet");
    }

    @Test
    void getTermOverview_labOccupancyUsesFiveDayBaselineAndCanExceed100PercentOnSaturdayOverflow() {
        TermInstance term = new TermInstance();
        term.setId(10L);
        AcademicYear year = new AcademicYear();
        year.setId(1L);
        year.setName("2026-27");
        term.setAcademicYear(year);
        term.setTermType(TermType.ODD);
        term.setStartDate(LocalDate.of(2026, 1, 1));
        term.setEndDate(LocalDate.of(2026, 1, 28));
        when(termInstanceRepository.findById(10L)).thenReturn(Optional.of(term));

        when(studentTermEnrollmentRepository.findDistinctCohortIdsByTermInstanceId(10L, EnrollmentStatus.ENROLLED))
            .thenReturn(Set.of(1L));
        Cohort cohortA = cohort(1L, "Cohort A");
        when(cohortRepository.findAllById(any())).thenReturn(List.of(cohortA));
        when(cohortRepository.findByIdWithCourse(1L)).thenReturn(Optional.of(cohortA));
        when(studentTermEnrollmentRepository.countByTermInstanceIdAndCohortIdAndStatus(10L, 1L, EnrollmentStatus.ENROLLED))
            .thenReturn(10L);
        when(studentTermEnrollmentRepository.findFirstByTermInstanceIdAndCohortIdAndStatus(10L, 1L, EnrollmentStatus.ENROLLED))
            .thenReturn(Optional.empty());
        when(cohortRoomAllocationRepository.existsByCohortIdAndTermInstanceIdAndStatus(1L, 10L, CohortRoomAllocationStatus.COMMITTED))
            .thenReturn(false);

        when(classroomRepository.findByIsActiveTrueOrderByNameAsc()).thenReturn(List.of());
        when(clinicalVenueRepository.findByIsActiveTrueOrderByNameAsc()).thenReturn(List.of());
        when(cohortSectionRepository.findByTermInstanceIdAndIsActiveTrue(10L)).thenReturn(List.of());
        when(calendarEventRepository.findNonTeachingDaysOverlapping(1L, term.getStartDate(), term.getEndDate())).thenReturn(List.of());
        when(blockedPeriodRepository.findApplicableInRange(term.getStartDate(), term.getEndDate())).thenReturn(List.of());

        // 1 active period -> 5-day baseline totalSlots = 1 * 5 = 5.
        Period period = new Period();
        period.setId(1L);
        period.setDurationMinutes(60);
        when(periodRepository.findByIsActiveTrueOrderByPeriodOrderAsc()).thenReturn(List.of(period));

        Lab lab = new Lab();
        lab.setId(50L);
        lab.setName("Computer Lab");
        lab.setCapacity(30);
        lab.setStatus(com.cms.model.enums.LabStatus.ACTIVE);
        when(labRepository.findAll()).thenReturn(List.of(lab));

        // 6 occupied weekly LAB slots in this one lab -- one more than the 5-slot baseline, i.e.
        // genuine Saturday overflow.
        List<ClassSchedule> occupying = new java.util.ArrayList<>();
        for (int i = 0; i < 6; i++) {
            ClassSchedule cs = new ClassSchedule();
            cs.setSessionType(ClassSessionType.LAB);
            cs.setLab(lab);
            occupying.add(cs);
        }
        when(classScheduleRepository.findByTermInstanceIdAndStatus(10L, ClassScheduleStatus.PUBLISHED)).thenReturn(occupying);
        when(classScheduleRepository.findByTermInstanceIdAndStatus(10L, ClassScheduleStatus.DRAFT)).thenReturn(List.of());

        TermCapacityOverviewResponse overview = service.getTermOverview(10L, PlanningBasis.ENROLLED);

        var labRow = overview.roomInventory().stream().filter(r -> r.roomType().equals("LAB")).findFirst().orElseThrow();
        assertThat(labRow.totalSlots()).isEqualTo(5);
        assertThat(labRow.occupiedSlots()).isEqualTo(6);
        assertThat(labRow.utilizationPercent()).isEqualTo(120.0);
    }
}
