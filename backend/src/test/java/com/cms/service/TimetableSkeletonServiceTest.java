package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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

import com.cms.dto.CourseOfferingDto;
import com.cms.dto.SkeletonBuilderResponse;
import com.cms.dto.SkeletonCellPlacementRequest;
import com.cms.dto.SkeletonCellResponse;
import com.cms.dto.SkeletonPlacementCandidateResponse;
import com.cms.dto.SkeletonSubjectBudget;
import com.cms.exception.LifecycleConflictException;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.AcademicYear;
import com.cms.model.Batch;
import com.cms.model.BlockedPeriod;
import com.cms.model.CalendarEvent;
import com.cms.model.ClassSchedule;
import com.cms.model.Cohort;
import com.cms.model.CourseOffering;
import com.cms.model.CurriculumSemesterCourse;
import com.cms.model.Faculty;
import com.cms.model.Period;
import com.cms.model.Subject;
import com.cms.model.TermInstance;
import com.cms.model.enums.BlockType;
import com.cms.model.enums.ClassScheduleStatus;
import com.cms.model.enums.ClassSessionType;
import com.cms.model.enums.DayOfWeek;
import com.cms.model.enums.TermInstanceStatus;
import com.cms.model.enums.TermType;
import com.cms.repository.BatchRepository;
import com.cms.repository.BlockedPeriodRepository;
import com.cms.repository.ClassScheduleRepository;
import com.cms.repository.CohortRepository;
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
    @Mock private BlockedPeriodRepository blockedPeriodRepository;
    @Mock private com.cms.repository.RotationSlotRepository rotationSlotRepository;
    @Mock private RotationResolverService rotationResolverService;
    @Mock private CourseOfferingService courseOfferingService;
    @Mock private CohortRepository cohortRepository;
    @Mock private TermInstanceRepository termInstanceRepository;

    private TimetableSkeletonService service;

    private TermInstance termInstance;
    private Cohort cohort;
    private CourseOffering offering;
    private CourseOffering otherOffering;
    private CurriculumSemesterCourse csc;
    private Period period;

    @BeforeEach
    void setUp() {
        service = new TimetableSkeletonService(courseOfferingRepository, classScheduleRepository,
            periodRepository, batchRepository, batchService, blockedPeriodRepository,
            rotationSlotRepository, rotationResolverService, courseOfferingService,
            cohortRepository, termInstanceRepository);

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
            1, null, null, true, null, elective, null, null, null, null, null, null, null);
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
        assertThat(response.cohortName()).isEqualTo("BSc Nursing 2024");
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
    void shouldFilterOutElectiveOfferingsFromCohortSkeleton() {
        when(cohortRepository.findById(5L)).thenReturn(Optional.of(cohort));
        when(termInstanceRepository.findById(10L)).thenReturn(Optional.of(termInstance));
        when(courseOfferingService.getOfferingsByTermInstanceAndCohort(10L, 5L))
            .thenReturn(List.of(offeringDto(100L, false), offeringDto(200L, true)));
        when(courseOfferingRepository.findById(100L)).thenReturn(Optional.of(offering));
        when(periodRepository.findByIsActiveTrueOrderByPeriodOrderAsc()).thenReturn(List.of(period));
        when(classScheduleRepository.findByTermInstanceIdAndCourseOfferingIdIn(10L, List.of(100L))).thenReturn(Collections.emptyList());
        when(batchRepository.findByCourseOfferingId(100L)).thenReturn(Collections.emptyList());
        when(batchService.getBatchesForOffering(100L)).thenReturn(List.of());

        SkeletonBuilderResponse response = service.getCohortSkeleton(10L, 5L);

        assertThat(response.subjects()).hasSize(1);
        assertThat(response.subjects().get(0).courseOfferingId()).isEqualTo(100L);
        verify(courseOfferingRepository, never()).findById(200L);
    }

    // ── placeCell ──────────────────────────────────────────────────────

    @Test
    void shouldPlaceATheoryCellWithNoFacultyOrRoom() {
        SkeletonCellPlacementRequest request = new SkeletonCellPlacementRequest(100L, ClassSessionType.THEORY, DayOfWeek.MONDAY, 1L, null, 5L);
        when(courseOfferingRepository.findById(100L)).thenReturn(Optional.of(offering));
        when(periodRepository.findById(1L)).thenReturn(Optional.of(period));
        when(classScheduleRepository.findByCourseOfferingId(100L)).thenReturn(Collections.emptyList());
        when(classScheduleRepository.save(any(ClassSchedule.class))).thenAnswer(inv -> inv.getArgument(0));

        SkeletonCellResponse response = service.placeCell(request);

        assertThat(response.sessionType()).isEqualTo(ClassSessionType.THEORY);
        assertThat(response.isStaffed()).isFalse();
        assertThat(response.status()).isEqualTo(ClassScheduleStatus.DRAFT);
    }

    @Test
    void shouldRequireBatchForLabPlacement() {
        SkeletonCellPlacementRequest request = new SkeletonCellPlacementRequest(100L, ClassSessionType.LAB, DayOfWeek.MONDAY, 1L, null, 5L);
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

        SkeletonCellPlacementRequest request = new SkeletonCellPlacementRequest(100L, ClassSessionType.LAB, DayOfWeek.MONDAY, 1L, 500L, 5L);
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

        SkeletonCellPlacementRequest request = new SkeletonCellPlacementRequest(100L, ClassSessionType.THEORY, DayOfWeek.MONDAY, 1L, null, 5L);
        when(courseOfferingRepository.findById(100L)).thenReturn(Optional.of(offering));
        when(periodRepository.findById(1L)).thenReturn(Optional.of(period));
        when(classScheduleRepository.findByCourseOfferingId(100L)).thenReturn(List.of(existing));

        assertThatThrownBy(() -> service.placeCell(request))
            .isInstanceOf(LifecycleConflictException.class);
    }

    @Test
    void shouldBlockTheoryPlacementWhenAnotherSubjectAlreadyOccupiesThatCohortSlot() {
        SkeletonCellPlacementRequest request = new SkeletonCellPlacementRequest(100L, ClassSessionType.THEORY, DayOfWeek.MONDAY, 1L, null, 5L);
        when(courseOfferingRepository.findById(100L)).thenReturn(Optional.of(offering));
        when(periodRepository.findById(1L)).thenReturn(Optional.of(period));
        when(classScheduleRepository.findByCourseOfferingId(100L)).thenReturn(Collections.emptyList());
        when(courseOfferingService.getOfferingsByTermInstanceAndCohort(10L, 5L))
            .thenReturn(List.of(offeringDto(100L, false), offeringDto(200L, false)));
        when(classScheduleRepository.findByTermInstanceIdAndCourseOfferingIdIn(10L, List.of(100L, 200L)))
            .thenReturn(List.of(rowFor(otherOffering, ClassSessionType.LAB, null, DayOfWeek.MONDAY, period)));

        assertThatThrownBy(() -> service.placeCell(request))
            .isInstanceOf(LifecycleConflictException.class)
            .hasMessageContaining("mandatory");
    }

    @Test
    void shouldBlockLabPlacementWhenATheorySessionOccupiesThatCohortSlotForAnotherSubject() {
        Batch batchA = new Batch();
        batchA.setId(400L);
        batchA.setCourseOffering(offering);

        SkeletonCellPlacementRequest request = new SkeletonCellPlacementRequest(100L, ClassSessionType.LAB, DayOfWeek.MONDAY, 1L, 400L, 5L);
        when(courseOfferingRepository.findById(100L)).thenReturn(Optional.of(offering));
        when(periodRepository.findById(1L)).thenReturn(Optional.of(period));
        when(batchRepository.findById(400L)).thenReturn(Optional.of(batchA));
        when(classScheduleRepository.findByCourseOfferingId(100L)).thenReturn(Collections.emptyList());
        when(courseOfferingService.getOfferingsByTermInstanceAndCohort(10L, 5L))
            .thenReturn(List.of(offeringDto(100L, false), offeringDto(200L, false)));
        when(classScheduleRepository.findByTermInstanceIdAndCourseOfferingIdIn(10L, List.of(100L, 200L)))
            .thenReturn(List.of(rowFor(otherOffering, ClassSessionType.THEORY, null, DayOfWeek.MONDAY, period)));

        assertThatThrownBy(() -> service.placeCell(request))
            .isInstanceOf(LifecycleConflictException.class)
            .hasMessageContaining("mandatory Theory session");
    }

    @Test
    void shouldAllowLabPlacementFromDifferentSubjectsInTheSameCohortSlot() {
        Batch batchA = new Batch();
        batchA.setId(400L);
        batchA.setCourseOffering(offering);

        SkeletonCellPlacementRequest request = new SkeletonCellPlacementRequest(100L, ClassSessionType.LAB, DayOfWeek.MONDAY, 1L, 400L, 5L);
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
        BlockedPeriod block = new BlockedPeriod();
        block.setBlockType(BlockType.RECURRING);
        block.setDayOfWeek(DayOfWeek.MONDAY);
        block.setRangeStartDate(LocalDate.of(2024, 6, 1));
        block.setRangeEndDate(LocalDate.of(2024, 11, 30));
        block.setReason("Staff meeting");

        SkeletonCellPlacementRequest request = new SkeletonCellPlacementRequest(100L, ClassSessionType.THEORY, DayOfWeek.MONDAY, 1L, null, 5L);
        when(courseOfferingRepository.findById(100L)).thenReturn(Optional.of(offering));
        when(periodRepository.findById(1L)).thenReturn(Optional.of(period));
        when(classScheduleRepository.findByCourseOfferingId(100L)).thenReturn(Collections.emptyList());
        when(blockedPeriodRepository.findOverlappingRecurringBlocks(
            DayOfWeek.MONDAY, 1L, termInstance.getStartDate(), termInstance.getEndDate()))
            .thenReturn(List.of(block));

        assertThatThrownBy(() -> service.placeCell(request))
            .isInstanceOf(LifecycleConflictException.class)
            .hasMessageContaining("Staff meeting");
    }

    @Test
    void shouldRejectPlacementAtAHolidayDerivedOneOffBlock() {
        CalendarEvent holiday = new CalendarEvent();
        holiday.setTitle("Independence Day");
        BlockedPeriod block = new BlockedPeriod();
        block.setBlockType(BlockType.ONE_OFF);
        block.setSpecificDate(LocalDate.of(2024, 8, 5)); // a Monday
        block.setReason("Auto-blocked — Independence Day");
        block.setSourceCalendarEvent(holiday);

        SkeletonCellPlacementRequest request = new SkeletonCellPlacementRequest(100L, ClassSessionType.THEORY, DayOfWeek.MONDAY, 1L, null, 5L);
        when(courseOfferingRepository.findById(100L)).thenReturn(Optional.of(offering));
        when(periodRepository.findById(1L)).thenReturn(Optional.of(period));
        when(classScheduleRepository.findByCourseOfferingId(100L)).thenReturn(Collections.emptyList());
        when(blockedPeriodRepository.findHolidayOneOffBlocksInRange(
            1L, termInstance.getStartDate(), termInstance.getEndDate()))
            .thenReturn(List.of(block));

        assertThatThrownBy(() -> service.placeCell(request))
            .isInstanceOf(LifecycleConflictException.class)
            .hasMessageContaining("Auto-blocked");
    }

    @Test
    void shouldAllowPlacementWhenOnlyAManuallyCreatedOneOffBlockExists() {
        // A manually-created ONE_OFF block (no sourceCalendarEvent) never reaches
        // findHolidayOneOffBlocksInRange's result set -- the repository query itself filters to
        // sourceCalendarEventId IS NOT NULL, so this simulates that by returning empty here even
        // though a manual ONE_OFF block for this exact period/date exists in the DB.
        SkeletonCellPlacementRequest request = new SkeletonCellPlacementRequest(100L, ClassSessionType.THEORY, DayOfWeek.MONDAY, 1L, null, 5L);
        when(courseOfferingRepository.findById(100L)).thenReturn(Optional.of(offering));
        when(periodRepository.findById(1L)).thenReturn(Optional.of(period));
        when(classScheduleRepository.findByCourseOfferingId(100L)).thenReturn(Collections.emptyList());
        when(classScheduleRepository.save(any(ClassSchedule.class))).thenAnswer(inv -> inv.getArgument(0));

        SkeletonCellResponse response = service.placeCell(request);

        assertThat(response.sessionType()).isEqualTo(ClassSessionType.THEORY);
    }

    // ── suggestCandidates ──────────────────────────────────────────────

    @Test
    void shouldSuggestCandidateSlotsUpToShortfall() {
        when(courseOfferingRepository.findById(100L)).thenReturn(Optional.of(offering));
        when(periodRepository.findByIsActiveTrueOrderByPeriodOrderAsc()).thenReturn(List.of(period));
        when(classScheduleRepository.findByCourseOfferingId(100L)).thenReturn(Collections.emptyList());

        List<SkeletonPlacementCandidateResponse> candidates = service.suggestCandidates(100L, ClassSessionType.THEORY, null);

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

        List<SkeletonPlacementCandidateResponse> candidates = service.suggestCandidates(100L, ClassSessionType.THEORY, null);

        assertThat(candidates).hasSize(2);
        assertThat(candidates).extracting(SkeletonPlacementCandidateResponse::dayOfWeek).doesNotContain(DayOfWeek.MONDAY);
    }

    @Test
    void shouldReturnEmptyCandidatesWhenNoHoursNeededForSessionType() {
        when(courseOfferingRepository.findById(100L)).thenReturn(Optional.of(offering));
        when(periodRepository.findByIsActiveTrueOrderByPeriodOrderAsc()).thenReturn(List.of(period));

        List<SkeletonPlacementCandidateResponse> candidates = service.suggestCandidates(100L, ClassSessionType.CLINICAL, null);

        assertThat(candidates).isEmpty();
    }

    @Test
    void shouldReturnEmptyCandidatesWhenCurriculumMappingMissing() {
        offering.setCurriculumSemesterCourse(null);
        when(courseOfferingRepository.findById(100L)).thenReturn(Optional.of(offering));

        List<SkeletonPlacementCandidateResponse> candidates = service.suggestCandidates(100L, ClassSessionType.THEORY, null);

        assertThat(candidates).isEmpty();
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
