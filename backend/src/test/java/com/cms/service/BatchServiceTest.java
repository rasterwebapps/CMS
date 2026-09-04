package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.dto.BatchDto;
import com.cms.dto.BatchRequest;
import com.cms.model.Batch;
import com.cms.model.CohortSection;
import com.cms.model.CourseOffering;
import com.cms.model.Faculty;
import com.cms.model.Student;
import com.cms.model.TermInstance;
import com.cms.repository.BatchRepository;
import com.cms.repository.ClassScheduleRepository;
import com.cms.repository.CourseOfferingRepository;
import com.cms.repository.EscortRotationAssignmentRepository;
import com.cms.repository.FacultyRepository;
import com.cms.repository.RotationMemberAssignmentRepository;
import com.cms.repository.SessionOccurrenceRepository;
import com.cms.repository.StudentRepository;

@ExtendWith(MockitoExtension.class)
class BatchServiceTest {

    @Mock
    private BatchRepository batchRepository;
    @Mock
    private CourseOfferingRepository courseOfferingRepository;
    @Mock
    private FacultyRepository facultyRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private ClassScheduleRepository classScheduleRepository;
    @Mock
    private RotationMemberAssignmentRepository rotationMemberAssignmentRepository;
    @Mock
    private EscortRotationAssignmentRepository escortRotationAssignmentRepository;
    @Mock
    private SessionOccurrenceRepository sessionOccurrenceRepository;

    private BatchService service;

    private CourseOffering testOffering;

    @BeforeEach
    void setUp() {
        service = new BatchService(batchRepository, courseOfferingRepository, facultyRepository, studentRepository,
            classScheduleRepository, rotationMemberAssignmentRepository, escortRotationAssignmentRepository,
            sessionOccurrenceRepository);

        TermInstance termInstance = new TermInstance();
        termInstance.setId(1L);

        testOffering = new CourseOffering();
        testOffering.setId(1L);
        testOffering.setSubject(new com.cms.model.Subject());
        testOffering.setTermInstance(termInstance);
    }

    @Test
    void shouldRejectAddingStudentPastCapacity() {
        Batch batch = new Batch(testOffering, "Batch A", 2, testOffering.getTermInstance());
        batch.setId(1L);
        Student student = new Student();
        student.setId(5L);

        when(batchRepository.findById(1L)).thenReturn(Optional.of(batch));
        when(studentRepository.findById(5L)).thenReturn(Optional.of(student));
        when(batchRepository.existsStudentInBatch(1L, 5L)).thenReturn(false);
        when(batchRepository.countStudents(1L)).thenReturn(2L);

        assertThatThrownBy(() -> service.addStudent(1L, 5L))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("capacity");

        verify(batchRepository, never()).save(any());
    }

    @Test
    void shouldAddStudentWhenBelowCapacity() {
        Batch batch = new Batch(testOffering, "Batch A", 2, testOffering.getTermInstance());
        batch.setId(1L);
        Student student = new Student();
        student.setId(5L);

        when(batchRepository.findById(1L)).thenReturn(Optional.of(batch));
        when(studentRepository.findById(5L)).thenReturn(Optional.of(student));
        when(batchRepository.existsStudentInBatch(1L, 5L)).thenReturn(false);
        when(batchRepository.countStudents(1L)).thenReturn(1L);
        when(batchRepository.save(any(Batch.class))).thenAnswer(inv -> inv.getArgument(0));

        service.addStudent(1L, 5L);

        assertThat(batch.getStudents()).contains(student);
        verify(batchRepository).save(batch);
    }

    @Test
    void shouldNoOpWhenStudentAlreadyInBatch() {
        when(batchRepository.findById(1L)).thenReturn(Optional.of(
            new Batch(testOffering, "Batch A", 2, testOffering.getTermInstance())));
        when(studentRepository.findById(5L)).thenReturn(Optional.of(new Student()));
        when(batchRepository.existsStudentInBatch(1L, 5L)).thenReturn(true);

        service.addStudent(1L, 5L);

        verify(batchRepository, never()).save(any());
    }

    @Test
    void shouldAllowSameFacultyToCoordinateMultipleParallelBatches() {
        // OC-183: one faculty coordinating 2+ batches (even parallel Lab/Clinical batches in
        // different venues) is legitimate as long as they're never actually scheduled at an
        // overlapping day/time -- that real conflict is caught at placement, by
        // TimetableStaffingService#checkFacultyFree, not here. Assignment itself should never be
        // blocked just because the faculty already coordinates a sibling batch.
        CohortSection section = new CohortSection();
        section.setId(50L);

        Faculty sharedFaculty = new Faculty();
        sharedFaculty.setId(28L);

        Batch siblingBatch = new Batch(testOffering, "Clinical - Batch 1", 20, testOffering.getTermInstance());
        siblingBatch.setId(10L);
        siblingBatch.setCohortSection(section);
        siblingBatch.setIsActive(true);
        siblingBatch.setCoordinatorFaculty(sharedFaculty);

        Batch newBatch = new Batch(testOffering, "Clinical - Batch 2", 20, testOffering.getTermInstance());
        newBatch.setId(11L);
        newBatch.setCohortSection(section);

        BatchRequest request = new BatchRequest(1L, "Clinical - Batch 2", 20, 28L, null);

        when(batchRepository.findById(11L)).thenReturn(Optional.of(newBatch));
        when(facultyRepository.findById(28L)).thenReturn(Optional.of(sharedFaculty));
        when(batchRepository.save(any(Batch.class))).thenAnswer(inv -> inv.getArgument(0));
        when(batchRepository.countStudents(11L)).thenReturn(0L);

        BatchDto dto = service.updateBatch(11L, request);

        assertThat(dto.coordinatorFacultyId()).isEqualTo(28L);
    }

    @Test
    void shouldDeleteWhenNothingAttached() {
        Batch batch = new Batch(testOffering, "Batch A", 20, testOffering.getTermInstance());
        batch.setId(1L);

        when(batchRepository.findById(1L)).thenReturn(Optional.of(batch));
        when(batchRepository.countStudents(1L)).thenReturn(0L);
        when(classScheduleRepository.countByBatchIdAndIsActiveTrue(1L)).thenReturn(0L);
        when(rotationMemberAssignmentRepository.countByBatchId(1L)).thenReturn(0L);
        when(escortRotationAssignmentRepository.countByBatchId(1L)).thenReturn(0L);
        when(sessionOccurrenceRepository.countByBatch_IdAndOccurrenceStatusNot(1L, com.cms.model.enums.OccurrenceStatus.CANCELLED))
            .thenReturn(0L);

        service.deleteBatch(1L);

        verify(batchRepository).delete(batch);
    }

    @Test
    void shouldBlockDeleteWhenStudentsEnrolled() {
        Batch batch = new Batch(testOffering, "Batch A", 20, testOffering.getTermInstance());
        batch.setId(1L);

        when(batchRepository.findById(1L)).thenReturn(Optional.of(batch));
        when(batchRepository.countStudents(1L)).thenReturn(3L);
        when(classScheduleRepository.countByBatchIdAndIsActiveTrue(1L)).thenReturn(0L);
        when(rotationMemberAssignmentRepository.countByBatchId(1L)).thenReturn(0L);
        when(escortRotationAssignmentRepository.countByBatchId(1L)).thenReturn(0L);
        when(sessionOccurrenceRepository.countByBatch_IdAndOccurrenceStatusNot(1L, com.cms.model.enums.OccurrenceStatus.CANCELLED))
            .thenReturn(0L);

        assertThatThrownBy(() -> service.deleteBatch(1L))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("enrolled student");

        verify(batchRepository, never()).delete(any());
    }

    @Test
    void shouldRejectUpdateWithStaleVersion() {
        Batch batch = new Batch(testOffering, "Batch A", 20, testOffering.getTermInstance());
        batch.setId(1L);
        batch.setVersion(2L);

        when(batchRepository.findById(1L)).thenReturn(Optional.of(batch));

        BatchRequest request = new BatchRequest(1L, "Batch A", 20, null, 1L);

        assertThatThrownBy(() -> service.updateBatch(1L, request))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("changed by someone else");

        verify(batchRepository, never()).save(any());
    }
}
