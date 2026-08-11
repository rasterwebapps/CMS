package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.dto.ConstraintViolation;
import com.cms.dto.CourseOfferingDto;
import com.cms.dto.SkeletonBuilderResponse;
import com.cms.dto.SkeletonCellMoveRequest;
import com.cms.dto.SkeletonCellPlacementRequest;
import com.cms.dto.SkeletonCellResponse;
import com.cms.dto.SkeletonPlacementCandidateResponse;
import com.cms.dto.SkeletonSubjectBudget;
import com.cms.exception.LifecycleConflictException;
import com.cms.exception.ResourceNotFoundException;
import com.cms.exception.TimetableConstraintViolationException;
import com.cms.model.AcademicYear;
import com.cms.model.Batch;
import com.cms.model.ClassSchedule;
import com.cms.model.Classroom;
import com.cms.model.Cohort;
import com.cms.model.CohortRoomAllocation;
import com.cms.model.CohortSection;
import com.cms.model.CourseOffering;
import com.cms.model.CurriculumElectiveGroup;
import com.cms.model.CurriculumSemesterCourse;
import com.cms.model.Faculty;
import com.cms.model.Period;
import com.cms.model.Subject;
import com.cms.model.TermInstance;
import com.cms.model.enums.ClassScheduleStatus;
import com.cms.model.enums.ClassSessionType;
import com.cms.model.enums.CohortRoomAllocationStatus;
import com.cms.model.enums.DayOfWeek;
import com.cms.model.enums.TermInstanceStatus;
import com.cms.model.enums.TermType;
import com.cms.repository.BatchRepository;
import com.cms.repository.ClassScheduleRepository;
import com.cms.repository.CohortRepository;
import com.cms.repository.CohortRoomAllocationRepository;
import com.cms.repository.CohortSectionRepository;
import com.cms.repository.CourseOfferingRepository;
import com.cms.repository.PeriodRepository;
import com.cms.repository.TermInstanceRepository;

@ExtendWith(MockitoExtension.class)
class TimetableSkeletonServiceTest {

    @Mock private CourseOfferingRepository courseOfferingRepository;
    @Mock private ClassScheduleRepository classScheduleRepository;
    @Mock private PeriodRepository periodRepository;
    @Mock private BatchRepository batchRepository;
    @Mock private BatchService batchService;
    @Mock private TimetableBlockedPeriodChecker blockedPeriodChecker;
    @Mock private com.cms.repository.RotationSlotRepository rotationSlotRepository;
    @Mock private RotationResolverService rotationResolverService;
    @Mock private CourseOfferingService courseOfferingService;
    @Mock private CohortRepository cohortRepository;
    @Mock private TermInstanceRepository termInstanceRepository;
    @Mock private CohortRoomAllocationRepository cohortRoomAllocationRepository;
    @Mock private CohortSectionRepository cohortSectionRepository;
    @Mock private TimetableStaffingService timetableStaffingService;

    private TimetableSkeletonService service;

    private TermInstance termInstance;
    private Cohort cohort;
    private CourseOffering offering;
    private CourseOffering otherOffering;
    private CurriculumSemesterCourse csc;
    private Period period;
    private Period period2;

    @BeforeEach
    void setUp() {
        service = new TimetableSkeletonService(courseOfferingRepository, classScheduleRepository,
            periodRepository, batchRepository, batchService, blockedPeriodChecker,
            rotationSlotRepository, rotationResolverService, courseOfferingService,
            cohortRepository, termInstanceRepository, cohortRoomAllocationRepository, cohortSectionRepository,
            timetableStaffingService);

        AcademicYear ay = new AcademicYear("2024-2025", LocalDate.of(2024, 6, 1), LocalDate.of(2025, 5, 31), false);
        ay.setId(1L);
        termInstance = new TermInstance(ay, TermType.ODD, LocalDate.of(2024, 6, 1), LocalDate.of(2024, 11, 30), TermInstanceStatus.OPEN);
        termInstance.setId(10L);
        // 183 days = 27 whole weeks, matching TimetableGenerationServiceTest's old fixture exactly.

        cohort = new Cohort();
        cohort.setId(5L);
        cohort.setDisplayName("BSc Nursing 2024");

        Subject subject = new Subject("Anatomy", "ANAT101", 4, 3, 1, null, 1);
        subject.setId(1L);

        csc = new CurriculumSemesterCourse();
        csc.setTheoryHours(54); // ceil((54*60/50)/27) = 3 weekly sessions at a 50-min period
        csc.setLabHours(27);    // ceil((27*60/50)/27) = 2 weekly sessions
        csc.setClinicalHours(0);

        offering = new CourseOffering();
        offering.setId(100L);
        offering.setSubject(subject);
        offering.setTermInstance(termInstance);
        offering.setCurriculumSemesterCourse(csc);

        Subject otherSubject = new Subject("Physiology", "PHY101", 3, 2, 0, null, 1);
        otherSubject.setId(2L);
        otherOffering = new CourseOffering();
        otherOffering.setId(200L);
        otherOffering.setSubject(otherSubject);
        otherOffering.setTermInstance(termInstance);

        period = new Period("1st Period", LocalTime.of(9, 0), LocalTime.of(9, 50), 1);
        period.setId(1L);
        period.setDurationMinutes(50);

        period2 = new Period("2nd Period", LocalTime.of(10, 0), LocalTime.of(10, 50), 2);
        period2.setId(2L);
        period2.setDurationMinutes(50);
    }

    private ClassSchedule existingRow(ClassSessionType type, Batch batch, boolean staffed) {
        ClassSchedule cs = new ClassSchedule();
        cs.setSessionType(type);
        cs.setBatch(batch);
        cs.setPeriod(period);
        cs.setDayOfWeek(DayOfWeek.MONDAY);
        cs.setStatus(staffed ? ClassScheduleStatus.PUBLISHED : ClassScheduleStatus.DRAFT);
        cs.setCourseOffering(offering);
        cs.setSubject(offering.getSubject());
        if (staffed) {
            Faculty f = new Faculty();
            f.setId(9L);
            cs.setFaculty(f);
        }
        return cs;
    }

    private ClassSchedule rowFor(CourseOffering off, ClassSessionType type, Batch batch, DayOfWeek day, Period p) {
        ClassSchedule cs = new ClassSchedule();
        cs.setSessionType(type);
        cs.setBatch(batch);
        cs.setPeriod(p);
        cs.setDayOfWeek(day);
        cs.setStatus(ClassScheduleStatus.DRAFT);
        cs.setCourseOffering(off);
        cs.setSubject(off.getSubject());
        return cs;
    }

    private CourseOfferingDto offeringDto(Long id, boolean elective) {
        return new CourseOfferingDto(id, 10L, "2024-2025 ODD", null, null, null, null, null, null, null,
            1, null, null, null, true, null, elective, null, null, null, null, null, null, null);
    }

    private CohortSection section(Long id, String label) {
        Classroom classroom = new Classroom("Room " + label, "Main Block", label, 60);
        classroom.setId(id * 10);
        CohortSection s = new CohortSection(new CohortRoomAllocation(), termInstance, label, classroom, 30);
        s.setId(id);
        s.setIsActive(true);
        return s;
    }

    /** Stubs a committed allocation with the given active sections for (cohortId=5L, termInstanceId=10L). */
    private void stubSections(List<CohortSection> sections) {
        CohortRoomAllocation allocation = new CohortRoomAllocation();
        allocation.setId(900L);
        when(cohortRoomAllocationRepository.findByCohortIdAndTermInstanceIdAndStatus(5L, 10L, CohortRoomAllocationStatus.COMMITTED))
            .thenReturn(Optional.of(allocation));
        when(cohortSectionRepository.findByCohortRoomAllocationIdAndIsActiveTrue(900L)).thenReturn(sections);
    }

    private CourseOffering electiveOffering(Long id, CurriculumElectiveGroup group) {
        CurriculumSemesterCourse electiveCsc = new CurriculumSemesterCourse();
        electiveCsc.setIsElective(true);
        electiveCsc.setElectiveGroup(group);
        Subject subj = new Subject("Elective " + id, "ELEC" + id, 2, 0, 0, null, 1);
        subj.setId(id);
        CourseOffering off = new CourseOffering();
        off.setId(id);
        off.setSubject(subj);
        off.setTermInstance(termInstance);
        off.setCurriculumSemesterCourse(electiveCsc);
        return off;
    }

    // ── getCohortSkeleton ──────────────────────────────────────────────

    @Test
    void shouldComputeTheoryBudgetAccountingForShortPeriodDuration() {
        when(cohortRepository.findById(5L)).thenReturn(Optional.of(cohort));
        when(termInstanceRepository.findById(10L)).thenReturn(Optional.of(termInstance));
        when(courseOfferingService.getOfferingsByTermInstanceAndCohort(10L, 5L)).thenReturn(List.of(offeringDto(100L, false)));
        when(courseOfferingRepository.findById(100L)).thenReturn(Optional.of(offering));
        when(periodRepository.findByIsActiveTrueOrderByPeriodOrderAsc()).thenReturn(List.of(period));
        when(classScheduleRepository.findByTermInstanceIdAndCourseOfferingIdIn(10L, List.of(100L))).thenReturn(Collections.emptyList());
        when(batchRepository.findByCourseOfferingId(100L)).thenReturn(Collections.emptyList());
        when(batchService.getBatchesForOffering(100L)).thenReturn(List.of());

        SkeletonBuilderResponse response = service.getCohortSkeleton(10L, 5L);

        SkeletonSubjectBudget theory = response.subjects().get(0).budgets().stream()
            .filter(b -> b.sessionType() == ClassSessionType.THEORY).findFirst().orElseThrow();
        assertThat(theory.requiredSessionsPerWeek()).isEqualTo(3);
        assertThat(theory.placedSessionsPerWeek()).isEqualTo(0);
        assertThat(theory.weeksInTerm()).isEqualTo(27);
        assertThat(theory.cohortSectionId()).isNull();
        assertThat(response.cohortName()).isEqualTo("BSc Nursing 2024");
        assertThat(response.sections()).isEmpty();
    }

    @Test
    void shouldTrackTheoryPlacedCountFromExistingRows() {
        when(cohortRepository.findById(5L)).thenReturn(Optional.of(cohort));
        when(termInstanceRepository.findById(10L)).thenReturn(Optional.of(termInstance));
        when(courseOfferingService.getOfferingsByTermInstanceAndCohort(10L, 5L)).thenReturn(List.of(offeringDto(100L, false)));
        when(courseOfferingRepository.findById(100L)).thenReturn(Optional.of(offering));
        when(periodRepository.findByIsActiveTrueOrderByPeriodOrderAsc()).thenReturn(List.of(period));
        when(classScheduleRepository.findByTermInstanceIdAndCourseOfferingIdIn(10L, List.of(100L)))
            .thenReturn(List.of(existingRow(ClassSessionType.THEORY, null, false)));
        when(batchRepository.findByCourseOfferingId(100L)).thenReturn(Collections.emptyList());
        when(batchService.getBatchesForOffering(100L)).thenReturn(List.of());

        SkeletonBuilderResponse response = service.getCohortSkeleton(10L, 5L);

        SkeletonSubjectBudget theory = response.subjects().get(0).budgets().stream()
            .filter(b -> b.sessionType() == ClassSessionType.THEORY).findFirst().orElseThrow();
        assertThat(theory.placedSessionsPerWeek()).isEqualTo(1);
        assertThat(response.cells()).hasSize(1);
        SkeletonCellResponse cell = response.cells().get(0);
        assertThat(cell.isStaffed()).isFalse();
        assertThat(cell.courseOfferingId()).isEqualTo(100L);
        assertThat(cell.subjectName()).isEqualTo("Anatomy");
        assertThat(cell.subjectCode()).isEqualTo("ANAT101");
        assertThat(cell.cohortSectionId()).isNull();
    }

    @Test
    void shouldProduceOneLabBudgetRowPerBatchIndependently() {
        Batch batchA = new Batch();
        batchA.setId(400L);
        batchA.setName("Batch A");
        Batch batchB = new Batch();
        batchB.setId(401L);
        batchB.setName("Batch B");

        when(cohortRepository.findById(5L)).thenReturn(Optional.of(cohort));
        when(termInstanceRepository.findById(10L)).thenReturn(Optional.of(termInstance));
        when(courseOfferingService.getOfferingsByTermInstanceAndCohort(10L, 5L)).thenReturn(List.of(offeringDto(100L, false)));
        when(courseOfferingRepository.findById(100L)).thenReturn(Optional.of(offering));
        when(periodRepository.findByIsActiveTrueOrderByPeriodOrderAsc()).thenReturn(List.of(period));
        when(classScheduleRepository.findByTermInstanceIdAndCourseOfferingIdIn(10L, List.of(100L)))
            .thenReturn(List.of(existingRow(ClassSessionType.LAB, batchA, false)));
        when(batchRepository.findByCourseOfferingId(100L)).thenReturn(List.of(batchA, batchB));
        when(batchService.getBatchesForOffering(100L)).thenReturn(List.of());

        SkeletonBuilderResponse response = service.getCohortSkeleton(10L, 5L);

        List<SkeletonSubjectBudget> labBudgets = response.subjects().get(0).budgets().stream()
            .filter(b -> b.sessionType() == ClassSessionType.LAB).toList();
        assertThat(labBudgets).hasSize(2);
        SkeletonSubjectBudget forA = labBudgets.stream().filter(b -> b.batchId().equals(400L)).findFirst().orElseThrow();
        SkeletonSubjectBudget forB = labBudgets.stream().filter(b -> b.batchId().equals(401L)).findFirst().orElseThrow();
        assertThat(forA.requiredSessionsPerWeek()).isEqualTo(2);
        assertThat(forA.placedSessionsPerWeek()).isEqualTo(1);
        // Batch B needs its own full quota independently -- not satisfied by Batch A's session.
        assertThat(forB.placedSessionsPerWeek()).isEqualTo(0);
    }

    @Test
    void shouldFlagLabHoursNeededWhenNoBatchesExistYet() {
        when(cohortRepository.findById(5L)).thenReturn(Optional.of(cohort));
        when(termInstanceRepository.findById(10L)).thenReturn(Optional.of(termInstance));
        when(courseOfferingService.getOfferingsByTermInstanceAndCohort(10L, 5L)).thenReturn(List.of(offeringDto(100L, false)));
        when(courseOfferingRepository.findById(100L)).thenReturn(Optional.of(offering));
        when(periodRepository.findByIsActiveTrueOrderByPeriodOrderAsc()).thenReturn(List.of(period));
        when(classScheduleRepository.findByTermInstanceIdAndCourseOfferingIdIn(10L, List.of(100L))).thenReturn(Collections.emptyList());
        when(batchRepository.findByCourseOfferingId(100L)).thenReturn(Collections.emptyList());
        when(batchService.getBatchesForOffering(100L)).thenReturn(List.of());

        SkeletonBuilderResponse response = service.getCohortSkeleton(10L, 5L);

        SkeletonSubjectBudget lab = response.subjects().get(0).budgets().stream()
            .filter(b -> b.sessionType() == ClassSessionType.LAB).findFirst().orElseThrow();
        assertThat(lab.batchId()).isNull();
        assertThat(lab.requiredSessionsPerWeek()).isEqualTo(2);
    }

    @Test
    void shouldSkipBudgetsButKeepSubjectWhenCurriculumMappingMissing() {
        offering.setCurriculumSemesterCourse(null);
        when(cohortRepository.findById(5L)).thenReturn(Optional.of(cohort));
        when(termInstanceRepository.findById(10L)).thenReturn(Optional.of(termInstance));
        when(courseOfferingService.getOfferingsByTermInstanceAndCohort(10L, 5L)).thenReturn(List.of(offeringDto(100L, false)));
        when(courseOfferingRepository.findById(100L)).thenReturn(Optional.of(offering));
        when(periodRepository.findByIsActiveTrueOrderByPeriodOrderAsc()).thenReturn(List.of(period));
        when(classScheduleRepository.findByTermInstanceIdAndCourseOfferingIdIn(10L, List.of(100L))).thenReturn(Collections.emptyList());
        when(batchRepository.findByCourseOfferingId(100L)).thenReturn(Collections.emptyList());
        when(batchService.getBatchesForOffering(100L)).thenReturn(List.of());

        SkeletonBuilderResponse response = service.getCohortSkeleton(10L, 5L);

        assertThat(response.subjects()).hasSize(1);
        assertThat(response.subjects().get(0).budgets()).isEmpty();
    }

    @Test
    void shouldReturnEmptyResponseWhenCohortHasNoNonElectiveOfferings() {
        when(cohortRepository.findById(5L)).thenReturn(Optional.of(cohort));
        when(termInstanceRepository.findById(10L)).thenReturn(Optional.of(termInstance));
        when(courseOfferingService.getOfferingsByTermInstanceAndCohort(10L, 5L)).thenReturn(List.of());

        SkeletonBuilderResponse response = service.getCohortSkeleton(10L, 5L);

        assertThat(response.subjects()).isEmpty();
        assertThat(response.cells()).isEmpty();
        assertThat(response.cohortName()).isEqualTo("BSc Nursing 2024");
    }

    @Test
    void shouldIncludeBothNonElectiveAndElectiveOfferingsInCohortSkeleton() {
        // Since R3.3, electives are no longer excluded from the skeleton -- they're placed
        // alongside regular subjects, just exempt from the cohort-wide Theory hard-lock and
        // subject to the elective-group same-slot check instead (see the placeCell tests below).
        CurriculumSemesterCourse electiveCsc = new CurriculumSemesterCourse();
        electiveCsc.setIsElective(true);
        Subject electiveSubject = new Subject("Open Elective", "OPEN200", 2, 0, 0, null, 1);
        electiveSubject.setId(9L);
        CourseOffering electiveOff = new CourseOffering();
        electiveOff.setId(200L);
        electiveOff.setSubject(electiveSubject);
        electiveOff.setTermInstance(termInstance);
        electiveOff.setCurriculumSemesterCourse(electiveCsc);

        when(cohortRepository.findById(5L)).thenReturn(Optional.of(cohort));
        when(termInstanceRepository.findById(10L)).thenReturn(Optional.of(termInstance));
        when(courseOfferingService.getOfferingsByTermInstanceAndCohort(10L, 5L))
            .thenReturn(List.of(offeringDto(100L, false), offeringDto(200L, true)));
        when(courseOfferingRepository.findById(100L)).thenReturn(Optional.of(offering));
        when(courseOfferingRepository.findById(200L)).thenReturn(Optional.of(electiveOff));
        when(periodRepository.findByIsActiveTrueOrderByPeriodOrderAsc()).thenReturn(List.of(period));
        when(classScheduleRepository.findByTermInstanceIdAndCourseOfferingIdIn(10L, List.of(100L, 200L))).thenReturn(Collections.emptyList());
        when(batchRepository.findByCourseOfferingId(100L)).thenReturn(Collections.emptyList());
        when(batchService.getBatchesForOffering(100L)).thenReturn(List.of());
        when(batchRepository.findByCourseOfferingId(200L)).thenReturn(Collections.emptyList());
        when(batchService.getBatchesForOffering(200L)).thenReturn(List.of());

        SkeletonBuilderResponse response = service.getCohortSkeleton(10L, 5L);

        assertThat(response.subjects()).hasSize(2);
        assertThat(response.subjects()).extracting(s -> s.courseOfferingId()).containsExactlyInAnyOrder(100L, 200L);
    }

    @Test
    void shouldProduceOneTheoryBudgetRowPerActiveSectionWhenCohortIsSectioned() {
        CohortSection sectionA = section(700L, "Section A");
        CohortSection sectionB = section(701L, "Section B");
        stubSections(List.of(sectionA, sectionB));

        ClassSchedule theoryForA = existingRow(ClassSessionType.THEORY, null, false);
        theoryForA.setCohortSection(sectionA);

        when(cohortRepository.findById(5L)).thenReturn(Optional.of(cohort));
        when(termInstanceRepository.findById(10L)).thenReturn(Optional.of(termInstance));
        when(courseOfferingService.getOfferingsByTermInstanceAndCohort(10L, 5L)).thenReturn(List.of(offeringDto(100L, false)));
        when(courseOfferingRepository.findById(100L)).thenReturn(Optional.of(offering));
        when(periodRepository.findByIsActiveTrueOrderByPeriodOrderAsc()).thenReturn(List.of(period));
        when(classScheduleRepository.findByTermInstanceIdAndCourseOfferingIdIn(10L, List.of(100L)))
            .thenReturn(List.of(theoryForA));
        when(batchRepository.findByCourseOfferingId(100L)).thenReturn(Collections.emptyList());
        when(batchService.getBatchesForOffering(100L)).thenReturn(List.of());

        SkeletonBuilderResponse response = service.getCohortSkeleton(10L, 5L);

        List<SkeletonSubjectBudget> theoryRows = response.subjects().get(0).budgets().stream()
            .filter(b -> b.sessionType() == ClassSessionType.THEORY).toList();
        assertThat(theoryRows).hasSize(2);
        SkeletonSubjectBudget rowA = theoryRows.stream().filter(b -> b.cohortSectionId().equals(700L)).findFirst().orElseThrow();
        SkeletonSubjectBudget rowB = theoryRows.stream().filter(b -> b.cohortSectionId().equals(701L)).findFirst().orElseThrow();
        assertThat(rowA.cohortSectionLabel()).isEqualTo("Section A");
        assertThat(rowA.placedSessionsPerWeek()).isEqualTo(1);
        assertThat(rowB.placedSessionsPerWeek()).isEqualTo(0);
        assertThat(response.sections()).hasSize(2);
    }

    @Test
    void shouldTagElectiveSubjectAndCellWithGroupInfoInCohortSkeleton() {
        CurriculumElectiveGroup group = new CurriculumElectiveGroup();
        group.setId(950L);
        group.setGroupName("Nursing Electives");
        CourseOffering electiveOff = electiveOffering(300L, group);

        when(cohortRepository.findById(5L)).thenReturn(Optional.of(cohort));
        when(termInstanceRepository.findById(10L)).thenReturn(Optional.of(termInstance));
        when(courseOfferingService.getOfferingsByTermInstanceAndCohort(10L, 5L))
            .thenReturn(List.of(offeringDto(300L, true)));
        when(courseOfferingRepository.findById(300L)).thenReturn(Optional.of(electiveOff));
        when(periodRepository.findByIsActiveTrueOrderByPeriodOrderAsc()).thenReturn(List.of(period));
        when(classScheduleRepository.findByTermInstanceIdAndCourseOfferingIdIn(10L, List.of(300L)))
            .thenReturn(List.of(rowFor(electiveOff, ClassSessionType.THEORY, null, DayOfWeek.MONDAY, period)));
        when(batchRepository.findByCourseOfferingId(300L)).thenReturn(Collections.emptyList());
        when(batchService.getBatchesForOffering(300L)).thenReturn(List.of());

        SkeletonBuilderResponse response = service.getCohortSkeleton(10L, 5L);

        assertThat(response.subjects()).hasSize(1);
        assertThat(response.subjects().get(0).electiveGroupId()).isEqualTo(950L);
        assertThat(response.subjects().get(0).electiveGroupName()).isEqualTo("Nursing Electives");
        assertThat(response.cells()).hasSize(1);
        assertThat(response.cells().get(0).electiveGroupId()).isEqualTo(950L);
        assertThat(response.cells().get(0).electiveGroupName()).isEqualTo("Nursing Electives");
    }

    // ── placeCell ──────────────────────────────────────────────────────

    @Test
    void shouldPlaceATheoryCellWithNoFacultyOrRoom() {
        SkeletonCellPlacementRequest request = new SkeletonCellPlacementRequest(100L, ClassSessionType.THEORY, DayOfWeek.MONDAY, 1L, null, 5L, null);
        when(courseOfferingRepository.findById(100L)).thenReturn(Optional.of(offering));
        when(periodRepository.findById(1L)).thenReturn(Optional.of(period));
        when(classScheduleRepository.findByCourseOfferingId(100L)).thenReturn(Collections.emptyList());
        when(classScheduleRepository.save(any(ClassSchedule.class))).thenAnswer(inv -> inv.getArgument(0));

        SkeletonCellResponse response = service.placeCell(request);

        assertThat(response.sessionType()).isEqualTo(ClassSessionType.THEORY);
        assertThat(response.isStaffed()).isFalse();
        assertThat(response.status()).isEqualTo(ClassScheduleStatus.DRAFT);
        assertThat(response.cohortSectionId()).isNull();
    }

    @Test
    void shouldRequireBatchForLabPlacement() {
        SkeletonCellPlacementRequest request = new SkeletonCellPlacementRequest(100L, ClassSessionType.LAB, DayOfWeek.MONDAY, 1L, null, 5L, null);
        when(courseOfferingRepository.findById(100L)).thenReturn(Optional.of(offering));
        when(periodRepository.findById(1L)).thenReturn(Optional.of(period));

        assertThatThrownBy(() -> service.placeCell(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("batch is required");
    }

    @Test
    void shouldRejectABatchBelongingToADifferentOffering() {
        CourseOffering otherOff = new CourseOffering();
        otherOff.setId(999L);
        Batch foreignBatch = new Batch();
        foreignBatch.setId(500L);
        foreignBatch.setCourseOffering(otherOff);

        SkeletonCellPlacementRequest request = new SkeletonCellPlacementRequest(100L, ClassSessionType.LAB, DayOfWeek.MONDAY, 1L, 500L, 5L, null);
        when(courseOfferingRepository.findById(100L)).thenReturn(Optional.of(offering));
        when(periodRepository.findById(1L)).thenReturn(Optional.of(period));
        when(batchRepository.findById(500L)).thenReturn(Optional.of(foreignBatch));

        assertThatThrownBy(() -> service.placeCell(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("does not belong");
    }

    @Test
    void shouldRejectExactDuplicatePlacement() {
        ClassSchedule existing = existingRow(ClassSessionType.THEORY, null, false);
        existing.setPeriod(period);

        SkeletonCellPlacementRequest request = new SkeletonCellPlacementRequest(100L, ClassSessionType.THEORY, DayOfWeek.MONDAY, 1L, null, 5L, null);
        when(courseOfferingRepository.findById(100L)).thenReturn(Optional.of(offering));
        when(periodRepository.findById(1L)).thenReturn(Optional.of(period));
        when(classScheduleRepository.findByCourseOfferingId(100L)).thenReturn(List.of(existing));

        assertThatThrownBy(() -> service.placeCell(request))
            .isInstanceOf(TimetableConstraintViolationException.class);
    }

    @Test
    void shouldBlockTheoryPlacementWhenAnotherSubjectAlreadyOccupiesThatCohortSlot() {
        SkeletonCellPlacementRequest request = new SkeletonCellPlacementRequest(100L, ClassSessionType.THEORY, DayOfWeek.MONDAY, 1L, null, 5L, null);
        when(courseOfferingRepository.findById(100L)).thenReturn(Optional.of(offering));
        when(periodRepository.findById(1L)).thenReturn(Optional.of(period));
        when(classScheduleRepository.findByCourseOfferingId(100L)).thenReturn(Collections.emptyList());
        when(courseOfferingService.getOfferingsByTermInstanceAndCohort(10L, 5L))
            .thenReturn(List.of(offeringDto(100L, false), offeringDto(200L, false)));
        when(classScheduleRepository.findByTermInstanceIdAndCourseOfferingIdIn(10L, List.of(100L, 200L)))
            .thenReturn(List.of(rowFor(otherOffering, ClassSessionType.LAB, null, DayOfWeek.MONDAY, period)));

        assertThatThrownBy(() -> service.placeCell(request))
            .isInstanceOf(TimetableConstraintViolationException.class)
            .hasMessageContaining("mandatory");
    }

    @Test
    void shouldBlockLabPlacementWhenATheorySessionOccupiesThatCohortSlotForAnotherSubject() {
        Batch batchA = new Batch();
        batchA.setId(400L);
        batchA.setCourseOffering(offering);

        SkeletonCellPlacementRequest request = new SkeletonCellPlacementRequest(100L, ClassSessionType.LAB, DayOfWeek.MONDAY, 1L, 400L, 5L, null);
        when(courseOfferingRepository.findById(100L)).thenReturn(Optional.of(offering));
        when(periodRepository.findById(1L)).thenReturn(Optional.of(period));
        when(batchRepository.findById(400L)).thenReturn(Optional.of(batchA));
        when(classScheduleRepository.findByCourseOfferingId(100L)).thenReturn(Collections.emptyList());
        when(courseOfferingService.getOfferingsByTermInstanceAndCohort(10L, 5L))
            .thenReturn(List.of(offeringDto(100L, false), offeringDto(200L, false)));
        when(classScheduleRepository.findByTermInstanceIdAndCourseOfferingIdIn(10L, List.of(100L, 200L)))
            .thenReturn(List.of(rowFor(otherOffering, ClassSessionType.THEORY, null, DayOfWeek.MONDAY, period)));

        assertThatThrownBy(() -> service.placeCell(request))
            .isInstanceOf(TimetableConstraintViolationException.class)
            .hasMessageContaining("mandatory Theory session");
    }

    @Test
    void shouldAllowLabPlacementFromDifferentSubjectsInTheSameCohortSlot() {
        Batch batchA = new Batch();
        batchA.setId(400L);
        batchA.setCourseOffering(offering);

        SkeletonCellPlacementRequest request = new SkeletonCellPlacementRequest(100L, ClassSessionType.LAB, DayOfWeek.MONDAY, 1L, 400L, 5L, null);
        when(courseOfferingRepository.findById(100L)).thenReturn(Optional.of(offering));
        when(periodRepository.findById(1L)).thenReturn(Optional.of(period));
        when(batchRepository.findById(400L)).thenReturn(Optional.of(batchA));
        when(classScheduleRepository.findByCourseOfferingId(100L)).thenReturn(Collections.emptyList());
        when(courseOfferingService.getOfferingsByTermInstanceAndCohort(10L, 5L))
            .thenReturn(List.of(offeringDto(100L, false), offeringDto(200L, false)));
        when(classScheduleRepository.findByTermInstanceIdAndCourseOfferingIdIn(10L, List.of(100L, 200L)))
            .thenReturn(List.of(rowFor(otherOffering, ClassSessionType.LAB, null, DayOfWeek.MONDAY, period)));
        when(classScheduleRepository.save(any(ClassSchedule.class))).thenAnswer(inv -> inv.getArgument(0));

        SkeletonCellResponse response = service.placeCell(request);

        assertThat(response.sessionType()).isEqualTo(ClassSessionType.LAB);
    }

    @Test
    void shouldRejectPlacementAtARecurringBlockedPeriod() {
        SkeletonCellPlacementRequest request = new SkeletonCellPlacementRequest(100L, ClassSessionType.THEORY, DayOfWeek.MONDAY, 1L, null, 5L, null);
        when(courseOfferingRepository.findById(100L)).thenReturn(Optional.of(offering));
        when(periodRepository.findById(1L)).thenReturn(Optional.of(period));
        when(classScheduleRepository.findByCourseOfferingId(100L)).thenReturn(Collections.emptyList());
        when(blockedPeriodChecker.blockReason(
            DayOfWeek.MONDAY, 1L, termInstance.getStartDate(), termInstance.getEndDate()))
            .thenReturn(Optional.of("Staff meeting"));

        assertThatThrownBy(() -> service.placeCell(request))
            .isInstanceOf(TimetableConstraintViolationException.class)
            .hasMessageContaining("Staff meeting");
    }

    @Test
    void shouldRejectPlacementAtAHolidayDerivedOneOffBlock() {
        SkeletonCellPlacementRequest request = new SkeletonCellPlacementRequest(100L, ClassSessionType.THEORY, DayOfWeek.MONDAY, 1L, null, 5L, null);
        when(courseOfferingRepository.findById(100L)).thenReturn(Optional.of(offering));
        when(periodRepository.findById(1L)).thenReturn(Optional.of(period));
        when(classScheduleRepository.findByCourseOfferingId(100L)).thenReturn(Collections.emptyList());
        when(blockedPeriodChecker.blockReason(
            DayOfWeek.MONDAY, 1L, termInstance.getStartDate(), termInstance.getEndDate()))
            .thenReturn(Optional.of("Auto-blocked — Independence Day"));

        assertThatThrownBy(() -> service.placeCell(request))
            .isInstanceOf(TimetableConstraintViolationException.class)
            .hasMessageContaining("Auto-blocked");
    }

    @Test
    void shouldAllowPlacementWhenOnlyAManuallyCreatedOneOffBlockExists() {
        // A manually-created ONE_OFF block (no sourceCalendarEvent) never reaches
        // findHolidayOneOffBlocksInRange's result set -- the repository query itself filters to
        // sourceCalendarEventId IS NOT NULL, so this simulates that by returning empty here even
        // though a manual ONE_OFF block for this exact period/date exists in the DB.
        SkeletonCellPlacementRequest request = new SkeletonCellPlacementRequest(100L, ClassSessionType.THEORY, DayOfWeek.MONDAY, 1L, null, 5L, null);
        when(courseOfferingRepository.findById(100L)).thenReturn(Optional.of(offering));
        when(periodRepository.findById(1L)).thenReturn(Optional.of(period));
        when(classScheduleRepository.findByCourseOfferingId(100L)).thenReturn(Collections.emptyList());
        when(classScheduleRepository.save(any(ClassSchedule.class))).thenAnswer(inv -> inv.getArgument(0));

        SkeletonCellResponse response = service.placeCell(request);

        assertThat(response.sessionType()).isEqualTo(ClassSessionType.THEORY);
    }

    @Test
    void shouldRequireCohortSectionForTheoryWhenCohortIsSectioned() {
        // The cohort-section check runs before the self-duplicate lookup, so
        // findByCourseOfferingId is never reached here -- not stubbed, matching that.
        stubSections(List.of(section(700L, "Section A"), section(701L, "Section B")));
        SkeletonCellPlacementRequest request = new SkeletonCellPlacementRequest(100L, ClassSessionType.THEORY, DayOfWeek.MONDAY, 1L, null, 5L, null);
        when(courseOfferingRepository.findById(100L)).thenReturn(Optional.of(offering));
        when(periodRepository.findById(1L)).thenReturn(Optional.of(period));

        assertThatThrownBy(() -> service.placeCell(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("cohort section is required");
    }

    @Test
    void shouldRejectUnknownCohortSectionId() {
        // Same as above -- this throws before the self-duplicate lookup runs.
        stubSections(List.of(section(700L, "Section A")));
        SkeletonCellPlacementRequest request = new SkeletonCellPlacementRequest(100L, ClassSessionType.THEORY, DayOfWeek.MONDAY, 1L, null, 5L, 999L);
        when(courseOfferingRepository.findById(100L)).thenReturn(Optional.of(offering));
        when(periodRepository.findById(1L)).thenReturn(Optional.of(period));

        assertThatThrownBy(() -> service.placeCell(request))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldAllowTwoDifferentSectionsTheoryInTheSameSlot() {
        CohortSection sectionA = section(700L, "Section A");
        CohortSection sectionB = section(701L, "Section B");
        stubSections(List.of(sectionA, sectionB));

        ClassSchedule theoryForA = rowFor(offering, ClassSessionType.THEORY, null, DayOfWeek.MONDAY, period);
        theoryForA.setCohortSection(sectionA);

        SkeletonCellPlacementRequest request = new SkeletonCellPlacementRequest(100L, ClassSessionType.THEORY, DayOfWeek.MONDAY, 1L, null, 5L, 701L);
        when(courseOfferingRepository.findById(100L)).thenReturn(Optional.of(offering));
        when(periodRepository.findById(1L)).thenReturn(Optional.of(period));
        when(classScheduleRepository.findByCourseOfferingId(100L)).thenReturn(Collections.emptyList());
        when(courseOfferingService.getOfferingsByTermInstanceAndCohort(10L, 5L)).thenReturn(List.of(offeringDto(100L, false)));
        when(classScheduleRepository.findByTermInstanceIdAndCourseOfferingIdIn(10L, List.of(100L)))
            .thenReturn(List.of(theoryForA));
        when(classScheduleRepository.save(any(ClassSchedule.class))).thenAnswer(inv -> inv.getArgument(0));

        SkeletonCellResponse response = service.placeCell(request);

        assertThat(response.sessionType()).isEqualTo(ClassSessionType.THEORY);
        assertThat(response.cohortSectionId()).isEqualTo(701L);
    }

    @Test
    void shouldStillBlockSameSectionTheoryInTheSameSlot() {
        CohortSection sectionA = section(700L, "Section A");
        stubSections(List.of(sectionA));

        // A different subject (otherOffering) so this exercises checkCohortExclusivity's
        // section-scoped conflict logic, not the earlier same-offering alreadyPlaced short-circuit.
        ClassSchedule theoryForA = rowFor(otherOffering, ClassSessionType.THEORY, null, DayOfWeek.MONDAY, period);
        theoryForA.setCohortSection(sectionA);

        SkeletonCellPlacementRequest request = new SkeletonCellPlacementRequest(100L, ClassSessionType.THEORY, DayOfWeek.MONDAY, 1L, null, 5L, 700L);
        when(courseOfferingRepository.findById(100L)).thenReturn(Optional.of(offering));
        when(periodRepository.findById(1L)).thenReturn(Optional.of(period));
        when(classScheduleRepository.findByCourseOfferingId(100L)).thenReturn(Collections.emptyList());
        when(courseOfferingService.getOfferingsByTermInstanceAndCohort(10L, 5L))
            .thenReturn(List.of(offeringDto(100L, false), offeringDto(200L, false)));
        when(classScheduleRepository.findByTermInstanceIdAndCourseOfferingIdIn(10L, List.of(100L, 200L)))
            .thenReturn(List.of(theoryForA));

        assertThatThrownBy(() -> service.placeCell(request))
            .isInstanceOf(TimetableConstraintViolationException.class)
            .hasMessageContaining("mandatory");
    }

    @Test
    void shouldAllowLabScopedToOtherSectionWhileTheoryOccupiesThisSlot() {
        // This places a LAB, not a THEORY session -- placeCell only calls resolveActiveSections
        // (and therefore the cohortRoomAllocation/cohortSection repositories) for THEORY, so no
        // stubSections(...) call here; these two CohortSection objects are just fixture data for
        // tagging the batch/existing-row below, not repository-backed lookups.
        CohortSection sectionA = section(700L, "Section A");
        CohortSection sectionB = section(701L, "Section B");

        Batch batchScopedToB = new Batch();
        batchScopedToB.setId(400L);
        batchScopedToB.setCourseOffering(offering);
        batchScopedToB.setCohortSection(sectionB);

        ClassSchedule theoryForA = rowFor(otherOffering, ClassSessionType.THEORY, null, DayOfWeek.MONDAY, period);
        theoryForA.setCohortSection(sectionA);

        SkeletonCellPlacementRequest request = new SkeletonCellPlacementRequest(100L, ClassSessionType.LAB, DayOfWeek.MONDAY, 1L, 400L, 5L, null);
        when(courseOfferingRepository.findById(100L)).thenReturn(Optional.of(offering));
        when(periodRepository.findById(1L)).thenReturn(Optional.of(period));
        when(batchRepository.findById(400L)).thenReturn(Optional.of(batchScopedToB));
        when(classScheduleRepository.findByCourseOfferingId(100L)).thenReturn(Collections.emptyList());
        when(courseOfferingService.getOfferingsByTermInstanceAndCohort(10L, 5L))
            .thenReturn(List.of(offeringDto(100L, false), offeringDto(200L, false)));
        when(classScheduleRepository.findByTermInstanceIdAndCourseOfferingIdIn(10L, List.of(100L, 200L)))
            .thenReturn(List.of(theoryForA));
        when(classScheduleRepository.save(any(ClassSchedule.class))).thenAnswer(inv -> inv.getArgument(0));

        SkeletonCellResponse response = service.placeCell(request);

        assertThat(response.sessionType()).isEqualTo(ClassSessionType.LAB);
    }

    @Test
    void shouldNotFalselyDedupeSameSlotAcrossDifferentSections() {
        CohortSection sectionA = section(700L, "Section A");
        CohortSection sectionB = section(701L, "Section B");
        stubSections(List.of(sectionA, sectionB));

        ClassSchedule theoryForA = existingRow(ClassSessionType.THEORY, null, false);
        theoryForA.setCohortSection(sectionA);

        SkeletonCellPlacementRequest request = new SkeletonCellPlacementRequest(100L, ClassSessionType.THEORY, DayOfWeek.MONDAY, 1L, null, 5L, 701L);
        when(courseOfferingRepository.findById(100L)).thenReturn(Optional.of(offering));
        when(periodRepository.findById(1L)).thenReturn(Optional.of(period));
        // The alreadyPlaced dedupe check queries findByCourseOfferingId -- must include section A's
        // row (same offering) to prove the fix distinguishes it from the section B request by id.
        when(classScheduleRepository.findByCourseOfferingId(100L)).thenReturn(List.of(theoryForA));
        when(courseOfferingService.getOfferingsByTermInstanceAndCohort(10L, 5L)).thenReturn(List.of(offeringDto(100L, false)));
        when(classScheduleRepository.findByTermInstanceIdAndCourseOfferingIdIn(10L, List.of(100L)))
            .thenReturn(List.of(theoryForA));
        when(classScheduleRepository.save(any(ClassSchedule.class))).thenAnswer(inv -> inv.getArgument(0));

        SkeletonCellResponse response = service.placeCell(request);

        assertThat(response.cohortSectionId()).isEqualTo(701L);
    }

    @Test
    void shouldAllowFirstElectivePlacementToFreelyDefineTheGroupSlot() {
        CurriculumElectiveGroup group = new CurriculumElectiveGroup();
        group.setId(950L);
        CourseOffering electiveOff = electiveOffering(300L, group);

        SkeletonCellPlacementRequest request = new SkeletonCellPlacementRequest(300L, ClassSessionType.THEORY, DayOfWeek.MONDAY, 1L, null, 5L, null);
        when(courseOfferingRepository.findById(300L)).thenReturn(Optional.of(electiveOff));
        when(periodRepository.findById(1L)).thenReturn(Optional.of(period));
        when(classScheduleRepository.findByCourseOfferingId(300L)).thenReturn(Collections.emptyList());
        when(courseOfferingRepository.findByTermInstanceIdAndCurriculumSemesterCourse_ElectiveGroupId(10L, 950L))
            .thenReturn(List.of(electiveOff));
        when(classScheduleRepository.findByTermInstanceIdAndCourseOfferingIdIn(10L, List.of(300L)))
            .thenReturn(Collections.emptyList());
        when(classScheduleRepository.save(any(ClassSchedule.class))).thenAnswer(inv -> inv.getArgument(0));

        SkeletonCellResponse response = service.placeCell(request);

        assertThat(response.sessionType()).isEqualTo(ClassSessionType.THEORY);
    }

    @Test
    void shouldAllowSecondElectiveInSameGroupAtTheSameSlot() {
        CurriculumElectiveGroup group = new CurriculumElectiveGroup();
        group.setId(950L);
        CourseOffering electiveA = electiveOffering(300L, group);
        CourseOffering electiveB = electiveOffering(301L, group);
        ClassSchedule existingForA = rowFor(electiveA, ClassSessionType.THEORY, null, DayOfWeek.MONDAY, period);

        SkeletonCellPlacementRequest request = new SkeletonCellPlacementRequest(301L, ClassSessionType.THEORY, DayOfWeek.MONDAY, 1L, null, 5L, null);
        when(courseOfferingRepository.findById(301L)).thenReturn(Optional.of(electiveB));
        when(periodRepository.findById(1L)).thenReturn(Optional.of(period));
        when(classScheduleRepository.findByCourseOfferingId(301L)).thenReturn(Collections.emptyList());
        when(courseOfferingRepository.findByTermInstanceIdAndCurriculumSemesterCourse_ElectiveGroupId(10L, 950L))
            .thenReturn(List.of(electiveA, electiveB));
        when(classScheduleRepository.findByTermInstanceIdAndCourseOfferingIdIn(10L, List.of(300L, 301L)))
            .thenReturn(List.of(existingForA));
        when(classScheduleRepository.save(any(ClassSchedule.class))).thenAnswer(inv -> inv.getArgument(0));

        SkeletonCellResponse response = service.placeCell(request);

        assertThat(response.sessionType()).isEqualTo(ClassSessionType.THEORY);
    }

    @Test
    void shouldRejectSecondElectiveInSameGroupAtADifferentSlot() {
        CurriculumElectiveGroup group = new CurriculumElectiveGroup();
        group.setId(950L);
        CourseOffering electiveA = electiveOffering(300L, group);
        CourseOffering electiveB = electiveOffering(301L, group);
        ClassSchedule existingForA = rowFor(electiveA, ClassSessionType.THEORY, null, DayOfWeek.MONDAY, period);

        SkeletonCellPlacementRequest request = new SkeletonCellPlacementRequest(301L, ClassSessionType.THEORY, DayOfWeek.TUESDAY, 1L, null, 5L, null);
        when(courseOfferingRepository.findById(301L)).thenReturn(Optional.of(electiveB));
        when(periodRepository.findById(1L)).thenReturn(Optional.of(period));
        when(classScheduleRepository.findByCourseOfferingId(301L)).thenReturn(Collections.emptyList());
        when(courseOfferingRepository.findByTermInstanceIdAndCurriculumSemesterCourse_ElectiveGroupId(10L, 950L))
            .thenReturn(List.of(electiveA, electiveB));
        when(classScheduleRepository.findByTermInstanceIdAndCourseOfferingIdIn(10L, List.of(300L, 301L)))
            .thenReturn(List.of(existingForA));

        assertThatThrownBy(() -> service.placeCell(request))
            .isInstanceOf(TimetableConstraintViolationException.class)
            .hasMessageContaining("already scheduled");
    }

    @Test
    void shouldAllowUngroupedElectiveToPlaceFreely() {
        CurriculumSemesterCourse ungroupedCsc = new CurriculumSemesterCourse();
        ungroupedCsc.setIsElective(true); // electiveGroup left null -- nothing to enforce
        Subject subj = new Subject("Open Elective", "OPEN101", 2, 0, 0, null, 1);
        subj.setId(400L);
        CourseOffering ungroupedOffering = new CourseOffering();
        ungroupedOffering.setId(400L);
        ungroupedOffering.setSubject(subj);
        ungroupedOffering.setTermInstance(termInstance);
        ungroupedOffering.setCurriculumSemesterCourse(ungroupedCsc);

        SkeletonCellPlacementRequest request = new SkeletonCellPlacementRequest(400L, ClassSessionType.THEORY, DayOfWeek.MONDAY, 1L, null, 5L, null);
        when(courseOfferingRepository.findById(400L)).thenReturn(Optional.of(ungroupedOffering));
        when(periodRepository.findById(1L)).thenReturn(Optional.of(period));
        when(classScheduleRepository.findByCourseOfferingId(400L)).thenReturn(Collections.emptyList());
        when(classScheduleRepository.save(any(ClassSchedule.class))).thenAnswer(inv -> inv.getArgument(0));

        SkeletonCellResponse response = service.placeCell(request);

        assertThat(response.sessionType()).isEqualTo(ClassSessionType.THEORY);
    }

    @Test
    void shouldNotBlockElectivePlacementAgainstAnExistingNonElectiveTheoryCellForTheSameCohort() {
        CurriculumElectiveGroup group = new CurriculumElectiveGroup();
        group.setId(950L);
        CourseOffering electiveA = electiveOffering(300L, group);

        SkeletonCellPlacementRequest request = new SkeletonCellPlacementRequest(300L, ClassSessionType.THEORY, DayOfWeek.MONDAY, 1L, null, 5L, null);
        when(courseOfferingRepository.findById(300L)).thenReturn(Optional.of(electiveA));
        when(periodRepository.findById(1L)).thenReturn(Optional.of(period));
        when(classScheduleRepository.findByCourseOfferingId(300L)).thenReturn(Collections.emptyList());
        when(courseOfferingRepository.findByTermInstanceIdAndCurriculumSemesterCourse_ElectiveGroupId(10L, 950L))
            .thenReturn(List.of(electiveA));
        when(classScheduleRepository.findByTermInstanceIdAndCourseOfferingIdIn(10L, List.of(300L)))
            .thenReturn(Collections.emptyList());
        when(classScheduleRepository.save(any(ClassSchedule.class))).thenAnswer(inv -> inv.getArgument(0));

        SkeletonCellResponse response = service.placeCell(request);

        assertThat(response.sessionType()).isEqualTo(ClassSessionType.THEORY);
        // Confirms checkCohortExclusivity (and its nonElectiveOfferingIds -> courseOfferingService
        // call) is never consulted for an elective placement -- electives are exempt entirely.
        verifyNoInteractions(courseOfferingService);
    }

    // ── suggestCandidates ──────────────────────────────────────────────

    @Test
    void shouldSuggestCandidateSlotsUpToShortfall() {
        when(courseOfferingRepository.findById(100L)).thenReturn(Optional.of(offering));
        when(periodRepository.findByIsActiveTrueOrderByPeriodOrderAsc()).thenReturn(List.of(period));
        when(classScheduleRepository.findByCourseOfferingId(100L)).thenReturn(Collections.emptyList());

        List<SkeletonPlacementCandidateResponse> candidates = service.suggestCandidates(100L, ClassSessionType.THEORY, null, null);

        assertThat(candidates).hasSize(3); // required=3 for the fixture's 54 theory hours
        assertThat(candidates).extracting(SkeletonPlacementCandidateResponse::periodId).containsOnly(1L);
        assertThat(candidates.stream().map(SkeletonPlacementCandidateResponse::dayOfWeek).distinct()).hasSize(3);
    }

    @Test
    void shouldSkipDaysAlreadyUsedBySameSubjectWhenSuggesting() {
        ClassSchedule already = existingRow(ClassSessionType.THEORY, null, false); // MONDAY by default
        when(courseOfferingRepository.findById(100L)).thenReturn(Optional.of(offering));
        when(periodRepository.findByIsActiveTrueOrderByPeriodOrderAsc()).thenReturn(List.of(period));
        when(classScheduleRepository.findByCourseOfferingId(100L)).thenReturn(List.of(already));

        List<SkeletonPlacementCandidateResponse> candidates = service.suggestCandidates(100L, ClassSessionType.THEORY, null, null);

        assertThat(candidates).hasSize(2);
        assertThat(candidates).extracting(SkeletonPlacementCandidateResponse::dayOfWeek).doesNotContain(DayOfWeek.MONDAY);
    }

    @Test
    void shouldReturnEmptyCandidatesWhenNoHoursNeededForSessionType() {
        when(courseOfferingRepository.findById(100L)).thenReturn(Optional.of(offering));
        when(periodRepository.findByIsActiveTrueOrderByPeriodOrderAsc()).thenReturn(List.of(period));

        List<SkeletonPlacementCandidateResponse> candidates = service.suggestCandidates(100L, ClassSessionType.CLINICAL, null, null);

        assertThat(candidates).isEmpty();
    }

    @Test
    void shouldReturnEmptyCandidatesWhenCurriculumMappingMissing() {
        offering.setCurriculumSemesterCourse(null);
        when(courseOfferingRepository.findById(100L)).thenReturn(Optional.of(offering));

        List<SkeletonPlacementCandidateResponse> candidates = service.suggestCandidates(100L, ClassSessionType.THEORY, null, null);

        assertThat(candidates).isEmpty();
    }

    @Test
    void shouldScopeSuggestShortfallToASingleCohortSection() {
        ClassSchedule sectionAAlreadyPlaced = existingRow(ClassSessionType.THEORY, null, false);
        sectionAAlreadyPlaced.setCohortSection(section(700L, "Section A"));

        when(courseOfferingRepository.findById(100L)).thenReturn(Optional.of(offering));
        when(periodRepository.findByIsActiveTrueOrderByPeriodOrderAsc()).thenReturn(List.of(period));
        when(classScheduleRepository.findByCourseOfferingId(100L)).thenReturn(List.of(sectionAAlreadyPlaced));

        // Section B has nothing placed yet -- its shortfall must still be the full 3, unaffected
        // by Section A's one placement.
        List<SkeletonPlacementCandidateResponse> forSectionB = service.suggestCandidates(100L, ClassSessionType.THEORY, null, 701L);
        assertThat(forSectionB).hasSize(3);

        // Section A already has its one MONDAY session -- shortfall for A is 2, and MONDAY must
        // not be suggested again (same-day clustering guard, scoped correctly to A's own rows).
        List<SkeletonPlacementCandidateResponse> forSectionA = service.suggestCandidates(100L, ClassSessionType.THEORY, null, 700L);
        assertThat(forSectionA).hasSize(2);
        assertThat(forSectionA).extracting(SkeletonPlacementCandidateResponse::dayOfWeek).doesNotContain(DayOfWeek.MONDAY);
    }

    // ── moveCell ───────────────────────────────────────────────────────

    @Test
    void shouldMoveAnUnstaffedCellToAFreeSlot() {
        ClassSchedule cs = existingRow(ClassSessionType.THEORY, null, false);
        cs.setId(100L);

        SkeletonCellMoveRequest request = new SkeletonCellMoveRequest(DayOfWeek.TUESDAY, 2L, 5L);
        when(classScheduleRepository.findById(100L)).thenReturn(Optional.of(cs));
        when(periodRepository.findById(2L)).thenReturn(Optional.of(period2));
        when(classScheduleRepository.save(any(ClassSchedule.class))).thenAnswer(inv -> inv.getArgument(0));

        SkeletonCellResponse response = service.moveCell(100L, request);

        assertThat(response.dayOfWeek()).isEqualTo(DayOfWeek.TUESDAY);
        assertThat(response.periodId()).isEqualTo(2L);
        assertThat(cs.getDayOfWeek()).isEqualTo(DayOfWeek.TUESDAY);
        assertThat(cs.getPeriod()).isEqualTo(period2);
    }

    @Test
    void shouldRejectMovingUnstaffedCellIntoABlockedPeriod() {
        ClassSchedule cs = existingRow(ClassSessionType.THEORY, null, false);
        cs.setId(100L);

        SkeletonCellMoveRequest request = new SkeletonCellMoveRequest(DayOfWeek.TUESDAY, 2L, 5L);
        when(classScheduleRepository.findById(100L)).thenReturn(Optional.of(cs));
        when(periodRepository.findById(2L)).thenReturn(Optional.of(period2));
        when(blockedPeriodChecker.blockReason(DayOfWeek.TUESDAY, 2L, termInstance.getStartDate(), termInstance.getEndDate()))
            .thenReturn(Optional.of("Staff meeting"));

        assertThatThrownBy(() -> service.moveCell(100L, request))
            .isInstanceOf(TimetableConstraintViolationException.class)
            .hasMessageContaining("Staff meeting");
    }

    @Test
    void shouldRejectMovingUnstaffedCellIntoACohortExclusivityClash() {
        ClassSchedule cs = existingRow(ClassSessionType.THEORY, null, false);
        cs.setId(100L);

        SkeletonCellMoveRequest request = new SkeletonCellMoveRequest(DayOfWeek.TUESDAY, 2L, 5L);
        when(classScheduleRepository.findById(100L)).thenReturn(Optional.of(cs));
        when(periodRepository.findById(2L)).thenReturn(Optional.of(period2));
        when(courseOfferingService.getOfferingsByTermInstanceAndCohort(10L, 5L))
            .thenReturn(List.of(offeringDto(100L, false), offeringDto(200L, false)));
        when(classScheduleRepository.findByTermInstanceIdAndCourseOfferingIdIn(10L, List.of(100L, 200L)))
            .thenReturn(List.of(rowFor(otherOffering, ClassSessionType.LAB, null, DayOfWeek.TUESDAY, period2)));

        assertThatThrownBy(() -> service.moveCell(100L, request))
            .isInstanceOf(TimetableConstraintViolationException.class)
            .hasMessageContaining("mandatory");
    }

    @Test
    void shouldRejectMovingAnElectiveCellToADifferentSlotThanItsGroup() {
        CurriculumElectiveGroup group = new CurriculumElectiveGroup();
        group.setId(950L);
        CourseOffering electiveA = electiveOffering(300L, group);
        CourseOffering electiveB = electiveOffering(301L, group);
        ClassSchedule cs = rowFor(electiveB, ClassSessionType.THEORY, null, DayOfWeek.MONDAY, period);
        cs.setId(301L);
        ClassSchedule existingForA = rowFor(electiveA, ClassSessionType.THEORY, null, DayOfWeek.MONDAY, period);

        SkeletonCellMoveRequest request = new SkeletonCellMoveRequest(DayOfWeek.TUESDAY, 2L, 5L);
        when(classScheduleRepository.findById(301L)).thenReturn(Optional.of(cs));
        when(periodRepository.findById(2L)).thenReturn(Optional.of(period2));
        when(courseOfferingRepository.findByTermInstanceIdAndCurriculumSemesterCourse_ElectiveGroupId(10L, 950L))
            .thenReturn(List.of(electiveA, electiveB));
        when(classScheduleRepository.findByTermInstanceIdAndCourseOfferingIdIn(10L, List.of(300L, 301L)))
            .thenReturn(List.of(existingForA));

        assertThatThrownBy(() -> service.moveCell(301L, request))
            .isInstanceOf(TimetableConstraintViolationException.class)
            .hasMessageContaining("already scheduled");
    }

    @Test
    void shouldRejectMovingCellOntoAnAlreadyPlacedIdenticalSession() {
        ClassSchedule cs = existingRow(ClassSessionType.THEORY, null, false);
        cs.setId(100L);
        ClassSchedule occupantAtTarget = existingRow(ClassSessionType.THEORY, null, false);
        occupantAtTarget.setId(101L);
        occupantAtTarget.setDayOfWeek(DayOfWeek.TUESDAY);
        occupantAtTarget.setPeriod(period2);

        SkeletonCellMoveRequest request = new SkeletonCellMoveRequest(DayOfWeek.TUESDAY, 2L, 5L);
        when(classScheduleRepository.findById(100L)).thenReturn(Optional.of(cs));
        when(periodRepository.findById(2L)).thenReturn(Optional.of(period2));
        when(classScheduleRepository.findByCourseOfferingId(100L)).thenReturn(List.of(cs, occupantAtTarget));

        assertThatThrownBy(() -> service.moveCell(100L, request))
            .isInstanceOf(TimetableConstraintViolationException.class);
    }

    @Test
    void shouldRejectMovingCellToItsOwnCurrentSlot() {
        ClassSchedule cs = existingRow(ClassSessionType.THEORY, null, false);
        cs.setId(100L);

        SkeletonCellMoveRequest request = new SkeletonCellMoveRequest(DayOfWeek.MONDAY, 1L, 5L);
        when(classScheduleRepository.findById(100L)).thenReturn(Optional.of(cs));

        assertThatThrownBy(() -> service.moveCell(100L, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("same as");
    }

    @Test
    void shouldRejectMovingANonDraftCell() {
        ClassSchedule cs = existingRow(ClassSessionType.THEORY, null, false);
        cs.setId(100L);
        cs.setStatus(ClassScheduleStatus.PUBLISHED);

        SkeletonCellMoveRequest request = new SkeletonCellMoveRequest(DayOfWeek.TUESDAY, 2L, 5L);
        when(classScheduleRepository.findById(100L)).thenReturn(Optional.of(cs));

        assertThatThrownBy(() -> service.moveCell(100L, request))
            .isInstanceOf(LifecycleConflictException.class);
    }

    @Test
    void shouldMoveAStaffedCellToAFreeSlot() {
        ClassSchedule cs = existingRow(ClassSessionType.THEORY, null, false);
        cs.setId(100L);
        Faculty faculty = new Faculty();
        faculty.setId(9L);
        cs.setFaculty(faculty);

        SkeletonCellMoveRequest request = new SkeletonCellMoveRequest(DayOfWeek.TUESDAY, 2L, 5L);
        when(classScheduleRepository.findById(100L)).thenReturn(Optional.of(cs));
        when(periodRepository.findById(2L)).thenReturn(Optional.of(period2));
        when(classScheduleRepository.save(any(ClassSchedule.class))).thenAnswer(inv -> inv.getArgument(0));

        SkeletonCellResponse response = service.moveCell(100L, request);

        assertThat(response.dayOfWeek()).isEqualTo(DayOfWeek.TUESDAY);
        assertThat(cs.getFaculty()).isEqualTo(faculty);
    }

    @Test
    void shouldRejectMovingAStaffedCellWhenFacultyAlreadyBusyAtTarget() {
        ClassSchedule cs = existingRow(ClassSessionType.THEORY, null, false);
        cs.setId(100L);
        Faculty faculty = new Faculty();
        faculty.setId(9L);
        cs.setFaculty(faculty);

        SkeletonCellMoveRequest request = new SkeletonCellMoveRequest(DayOfWeek.TUESDAY, 2L, 5L);
        when(classScheduleRepository.findById(100L)).thenReturn(Optional.of(cs));
        when(periodRepository.findById(2L)).thenReturn(Optional.of(period2));
        when(timetableStaffingService.checkFacultyFree(9L, cs, DayOfWeek.TUESDAY, period2.getStartTime(), period2.getEndTime()))
            .thenReturn(Optional.of(new ConstraintViolation("STAFFING_FACULTY_CONFLICT", "Already busy")));

        assertThatThrownBy(() -> service.moveCell(100L, request))
            .isInstanceOf(TimetableConstraintViolationException.class)
            .hasMessageContaining("Already busy");
    }

    @Test
    void shouldRejectMovingAStaffedCellWhenRoomAlreadyOccupiedAtTarget() {
        ClassSchedule cs = existingRow(ClassSessionType.THEORY, null, false);
        cs.setId(100L);
        Faculty faculty = new Faculty();
        faculty.setId(9L);
        cs.setFaculty(faculty);
        Classroom classroom = new Classroom("Room 101", "Main Block", "101", 60);
        classroom.setId(1L);
        cs.setClassroom(classroom);

        SkeletonCellMoveRequest request = new SkeletonCellMoveRequest(DayOfWeek.TUESDAY, 2L, 5L);
        when(classScheduleRepository.findById(100L)).thenReturn(Optional.of(cs));
        when(periodRepository.findById(2L)).thenReturn(Optional.of(period2));
        when(timetableStaffingService.checkRoomFree(eq(ClassSessionType.THEORY), eq(1L), any(), eq(cs),
                eq(DayOfWeek.TUESDAY), eq(period2.getStartTime()), eq(period2.getEndTime())))
            .thenReturn(Optional.of(new ConstraintViolation("STAFFING_ROOM_CONFLICT", "Room busy")));

        assertThatThrownBy(() -> service.moveCell(100L, request))
            .isInstanceOf(TimetableConstraintViolationException.class)
            .hasMessageContaining("Room busy");
    }

    @Test
    void shouldRejectMovingAStaffedCellWhenWorkloadCapExceededAtTarget() {
        ClassSchedule cs = existingRow(ClassSessionType.THEORY, null, false);
        cs.setId(100L);
        Faculty faculty = new Faculty();
        faculty.setId(9L);
        cs.setFaculty(faculty);

        SkeletonCellMoveRequest request = new SkeletonCellMoveRequest(DayOfWeek.TUESDAY, 2L, 5L);
        when(classScheduleRepository.findById(100L)).thenReturn(Optional.of(cs));
        when(periodRepository.findById(2L)).thenReturn(Optional.of(period2));
        when(timetableStaffingService.checkWithinWorkloadCaps(faculty, cs, DayOfWeek.TUESDAY, period2.getStartTime(), period2.getEndTime()))
            .thenReturn(List.of(new ConstraintViolation("STAFFING_WORKLOAD_DAILY_CAP_EXCEEDED", "Over cap")));

        assertThatThrownBy(() -> service.moveCell(100L, request))
            .isInstanceOf(TimetableConstraintViolationException.class)
            .hasMessageContaining("Over cap");
    }

    // ── removeCell ─────────────────────────────────────────────────────

    @Test
    void shouldRemoveAnUnstaffedDraftCell() {
        ClassSchedule cs = existingRow(ClassSessionType.THEORY, null, false);
        cs.setId(1L);
        when(classScheduleRepository.findById(1L)).thenReturn(Optional.of(cs));

        service.removeCell(1L);

        verify(classScheduleRepository).deleteById(1L);
    }

    @Test
    void shouldRefuseToRemoveAStaffedRow() {
        ClassSchedule cs = existingRow(ClassSessionType.THEORY, null, true);
        cs.setId(1L);
        when(classScheduleRepository.findById(1L)).thenReturn(Optional.of(cs));

        assertThatThrownBy(() -> service.removeCell(1L))
            .isInstanceOf(LifecycleConflictException.class);

        verify(classScheduleRepository, never()).deleteById(any());
    }

    @Test
    void shouldThrowWhenRemovingNonExistentCell() {
        when(classScheduleRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.removeCell(999L))
            .isInstanceOf(ResourceNotFoundException.class);
    }
}
