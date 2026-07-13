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
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Batch;
import com.cms.model.CourseOffering;
import com.cms.model.Student;
import com.cms.model.TermInstance;
import com.cms.repository.BatchRepository;
import com.cms.repository.CourseOfferingRepository;
import com.cms.repository.FacultyRepository;
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

    private BatchService service;

    private CourseOffering testOffering;

    @BeforeEach
    void setUp() {
        service = new BatchService(batchRepository, courseOfferingRepository, facultyRepository, studentRepository);

        TermInstance termInstance = new TermInstance();
        termInstance.setId(1L);

        testOffering = new CourseOffering();
        testOffering.setId(1L);
        testOffering.setTermInstance(termInstance);
    }

    @Test
    void shouldCreateBatch() {
        BatchRequest request = new BatchRequest(1L, "Batch A", 20, null);

        when(courseOfferingRepository.findById(1L)).thenReturn(Optional.of(testOffering));
        when(batchRepository.existsByCourseOfferingIdAndName(1L, "Batch A")).thenReturn(false);
        when(batchRepository.save(any(Batch.class))).thenAnswer(inv -> {
            Batch b = inv.getArgument(0);
            b.setId(1L);
            return b;
        });
        when(batchRepository.countStudents(1L)).thenReturn(0L);

        BatchDto dto = service.createBatch(request);

        assertThat(dto.name()).isEqualTo("Batch A");
        assertThat(dto.capacity()).isEqualTo(20);
        assertThat(dto.enrolledCount()).isEqualTo(0);
    }

    @Test
    void shouldRejectDuplicateBatchNameWithinSameOffering() {
        BatchRequest request = new BatchRequest(1L, "Batch A", 20, null);

        when(courseOfferingRepository.findById(1L)).thenReturn(Optional.of(testOffering));
        when(batchRepository.existsByCourseOfferingIdAndName(1L, "Batch A")).thenReturn(true);

        assertThatThrownBy(() -> service.createBatch(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("already exists");

        verify(batchRepository, never()).save(any());
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
    void shouldThrowWhenCourseOfferingNotFoundOnCreate() {
        BatchRequest request = new BatchRequest(999L, "Batch A", 20, null);

        when(courseOfferingRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createBatch(request))
            .isInstanceOf(ResourceNotFoundException.class);
    }
}
