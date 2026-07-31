package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

import com.cms.dto.BatchDto;
import com.cms.dto.SkeletonBuilderResponse;
import com.cms.dto.SkeletonCellPlacementRequest;
import com.cms.dto.SkeletonCellResponse;
import com.cms.dto.SkeletonSubjectBudget;
import com.cms.exception.LifecycleConflictException;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.AcademicYear;
import com.cms.model.Batch;
import com.cms.model.ClassSchedule;
import com.cms.model.CourseOffering;
import com.cms.model.CurriculumSemesterCourse;
import com.cms.model.Faculty;
import com.cms.model.Period;
import com.cms.model.Subject;
import com.cms.model.TermInstance;
import com.cms.model.enums.ClassScheduleStatus;
import com.cms.model.enums.ClassSessionType;
import com.cms.model.enums.DayOfWeek;
import com.cms.model.enums.TermInstanceStatus;
import com.cms.model.enums.TermType;
import com.cms.repository.BatchRepository;
import com.cms.repository.ClassScheduleRepository;
import com.cms.repository.CourseOfferingRepository;
import com.cms.repository.PeriodRepository;

@ExtendWith(MockitoExtension.class)
class TimetableSkeletonServiceTest {

    @Mock private CourseOfferingRepository courseOfferingRepository;
    @Mock private ClassScheduleRepository classScheduleRepository;
    @Mock private PeriodRepository periodRepository;
    @Mock private BatchRepository batchRepository;
    @Mock private BatchService batchService;

    private TimetableSkeletonService service;

    private TermInstance termInstance;
    private CourseOffering offering;
    private CurriculumSemesterCourse csc;
    private Period period;

    @BeforeEach
    void setUp() {
        service = new TimetableSkeletonService(courseOfferingRepository, classScheduleRepository,
            periodRepository, batchRepository, batchService);

        AcademicYear ay = new AcademicYear("2024-2025", LocalDate.of(2024, 6, 1), LocalDate.of(2025, 5, 31), false);
        ay.setId(1L);
        termInstance = new TermInstance(ay, TermType.ODD, LocalDate.of(2024, 6, 1), LocalDate.of(2024, 11, 30), TermInstanceStatus.OPEN);
        termInstance.setId(10L);
        // 183 days = 27 whole weeks, matching TimetableGenerationServiceTest's fixture exactly.

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
        if (staffed) {
            Faculty f = new Faculty();
            f.setId(9L);
            cs.setFaculty(f);
        }
        return cs;
    }

    @Test
    void shouldComputeTheoryBudgetAccountingForShortPeriodDuration() {
        when(courseOfferingRepository.findById(100L)).thenReturn(Optional.of(offering));
        when(periodRepository.findByIsActiveTrueOrderByPeriodOrderAsc()).thenReturn(List.of(period));
        when(classScheduleRepository.findByCourseOfferingId(100L)).thenReturn(Collections.emptyList());
        when(batchRepository.findByCourseOfferingId(100L)).thenReturn(Collections.emptyList());
        when(batchService.getBatchesForOffering(100L)).thenReturn(List.of());

        SkeletonBuilderResponse response = service.getSkeleton(100L);

        SkeletonSubjectBudget theory = response.budgets().stream()
            .filter(b -> b.sessionType() == ClassSessionType.THEORY).findFirst().orElseThrow();
        assertThat(theory.requiredSessionsPerWeek()).isEqualTo(3);
        assertThat(theory.placedSessionsPerWeek()).isEqualTo(0);
        assertThat(theory.weeksInTerm()).isEqualTo(27);
    }

    @Test
    void shouldTrackTheoryPlacedCountFromExistingRows() {
        when(courseOfferingRepository.findById(100L)).thenReturn(Optional.of(offering));
        when(periodRepository.findByIsActiveTrueOrderByPeriodOrderAsc()).thenReturn(List.of(period));
        when(classScheduleRepository.findByCourseOfferingId(100L))
            .thenReturn(List.of(existingRow(ClassSessionType.THEORY, null, false)));
        when(batchRepository.findByCourseOfferingId(100L)).thenReturn(Collections.emptyList());
        when(batchService.getBatchesForOffering(100L)).thenReturn(List.of());

        SkeletonBuilderResponse response = service.getSkeleton(100L);

        SkeletonSubjectBudget theory = response.budgets().stream()
            .filter(b -> b.sessionType() == ClassSessionType.THEORY).findFirst().orElseThrow();
        assertThat(theory.placedSessionsPerWeek()).isEqualTo(1);
        assertThat(response.cells()).hasSize(1);
        assertThat(response.cells().get(0).isStaffed()).isFalse();
    }

    @Test
    void shouldProduceOneLabBudgetRowPerBatchIndependently() {
        Batch batchA = new Batch();
        batchA.setId(400L);
        batchA.setName("Batch A");
        Batch batchB = new Batch();
        batchB.setId(401L);
        batchB.setName("Batch B");

        when(courseOfferingRepository.findById(100L)).thenReturn(Optional.of(offering));
        when(periodRepository.findByIsActiveTrueOrderByPeriodOrderAsc()).thenReturn(List.of(period));
        when(classScheduleRepository.findByCourseOfferingId(100L))
            .thenReturn(List.of(existingRow(ClassSessionType.LAB, batchA, false)));
        when(batchRepository.findByCourseOfferingId(100L)).thenReturn(List.of(batchA, batchB));
        when(batchService.getBatchesForOffering(100L)).thenReturn(List.of());

        SkeletonBuilderResponse response = service.getSkeleton(100L);

        List<SkeletonSubjectBudget> labBudgets = response.budgets().stream()
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
        when(courseOfferingRepository.findById(100L)).thenReturn(Optional.of(offering));
        when(periodRepository.findByIsActiveTrueOrderByPeriodOrderAsc()).thenReturn(List.of(period));
        when(classScheduleRepository.findByCourseOfferingId(100L)).thenReturn(Collections.emptyList());
        when(batchRepository.findByCourseOfferingId(100L)).thenReturn(Collections.emptyList());
        when(batchService.getBatchesForOffering(100L)).thenReturn(List.of());

        SkeletonBuilderResponse response = service.getSkeleton(100L);

        SkeletonSubjectBudget lab = response.budgets().stream()
            .filter(b -> b.sessionType() == ClassSessionType.LAB).findFirst().orElseThrow();
        assertThat(lab.batchId()).isNull();
        assertThat(lab.requiredSessionsPerWeek()).isEqualTo(2);
    }

    @Test
    void shouldThrowWhenCurriculumMappingMissing() {
        offering.setCurriculumSemesterCourse(null);
        when(courseOfferingRepository.findById(100L)).thenReturn(Optional.of(offering));

        assertThatThrownBy(() -> service.getSkeleton(100L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("no resolved curriculum mapping");
    }

    @Test
    void shouldPlaceATheoryCellWithNoFacultyOrRoom() {
        SkeletonCellPlacementRequest request = new SkeletonCellPlacementRequest(100L, ClassSessionType.THEORY, DayOfWeek.MONDAY, 1L, null);
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
        SkeletonCellPlacementRequest request = new SkeletonCellPlacementRequest(100L, ClassSessionType.LAB, DayOfWeek.MONDAY, 1L, null);
        when(courseOfferingRepository.findById(100L)).thenReturn(Optional.of(offering));
        when(periodRepository.findById(1L)).thenReturn(Optional.of(period));

        assertThatThrownBy(() -> service.placeCell(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("batch is required");
    }

    @Test
    void shouldRejectABatchBelongingToADifferentOffering() {
        CourseOffering otherOffering = new CourseOffering();
        otherOffering.setId(999L);
        Batch foreignBatch = new Batch();
        foreignBatch.setId(500L);
        foreignBatch.setCourseOffering(otherOffering);

        SkeletonCellPlacementRequest request = new SkeletonCellPlacementRequest(100L, ClassSessionType.LAB, DayOfWeek.MONDAY, 1L, 500L);
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

        SkeletonCellPlacementRequest request = new SkeletonCellPlacementRequest(100L, ClassSessionType.THEORY, DayOfWeek.MONDAY, 1L, null);
        when(courseOfferingRepository.findById(100L)).thenReturn(Optional.of(offering));
        when(periodRepository.findById(1L)).thenReturn(Optional.of(period));
        when(classScheduleRepository.findByCourseOfferingId(100L)).thenReturn(List.of(existing));

        assertThatThrownBy(() -> service.placeCell(request))
            .isInstanceOf(LifecycleConflictException.class);
    }

    @Test
    void shouldRemoveAnUnstaffedDraftCell() {
        ClassSchedule cs = existingRow(ClassSessionType.THEORY, null, false);
        cs.setId(1L);
        when(classScheduleRepository.findById(1L)).thenReturn(Optional.of(cs));

        service.removeCell(1L);

        org.mockito.Mockito.verify(classScheduleRepository).deleteById(1L);
    }

    @Test
    void shouldRefuseToRemoveAStaffedRow() {
        ClassSchedule cs = existingRow(ClassSessionType.THEORY, null, true);
        cs.setId(1L);
        when(classScheduleRepository.findById(1L)).thenReturn(Optional.of(cs));

        assertThatThrownBy(() -> service.removeCell(1L))
            .isInstanceOf(LifecycleConflictException.class);

        org.mockito.Mockito.verify(classScheduleRepository, org.mockito.Mockito.never()).deleteById(any());
    }

    @Test
    void shouldThrowWhenRemovingNonExistentCell() {
        when(classScheduleRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.removeCell(999L))
            .isInstanceOf(ResourceNotFoundException.class);
    }
}
