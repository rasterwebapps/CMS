package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.dto.LabContinuousEvaluationRequest;
import com.cms.dto.LabContinuousEvaluationResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Course;
import com.cms.model.Experiment;
import com.cms.model.LabContinuousEvaluation;
import com.cms.model.Student;
import com.cms.model.enums.StudentStatus;
import com.cms.repository.ExperimentRepository;
import com.cms.repository.LabContinuousEvaluationRepository;
import com.cms.repository.StudentRepository;

@ExtendWith(MockitoExtension.class)
class LabContinuousEvaluationServiceTest {

    @Mock
    private LabContinuousEvaluationRepository labContinuousEvaluationRepository;

    @Mock
    private ExperimentRepository experimentRepository;

    @Mock
    private StudentRepository studentRepository;

    private LabContinuousEvaluationService labContinuousEvaluationService;

    @BeforeEach
    void setUp() {
        labContinuousEvaluationService = new LabContinuousEvaluationService(
            labContinuousEvaluationRepository, experimentRepository, studentRepository
        );
    }

    @Test
    void shouldCreateEvaluation() {
        Experiment experiment = createExperiment();
        Student student = createStudent();
        LabContinuousEvaluationRequest request = new LabContinuousEvaluationRequest(
            1L, 1L, 8, 7, 9, 24, LocalDate.of(2024, 6, 1), "Dr. Smith"
        );
        LabContinuousEvaluation saved = createEvaluation(experiment, student);

        when(experimentRepository.findById(1L)).thenReturn(Optional.of(experiment));
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(labContinuousEvaluationRepository.save(any(LabContinuousEvaluation.class))).thenReturn(saved);

        LabContinuousEvaluationResponse response = labContinuousEvaluationService.create(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.experimentId()).isEqualTo(1L);
        assertThat(response.totalMarks()).isEqualTo(24);

        ArgumentCaptor<LabContinuousEvaluation> captor = ArgumentCaptor.forClass(LabContinuousEvaluation.class);
        verify(labContinuousEvaluationRepository).save(captor.capture());
        assertThat(captor.getValue().getTotalMarks()).isEqualTo(24);
    }

    @Test
    void shouldThrowWhenExperimentNotFound() {
        LabContinuousEvaluationRequest request = new LabContinuousEvaluationRequest(
            999L, 1L, 8, 7, 9, 24, LocalDate.of(2024, 6, 1), "Dr. Smith"
        );

        when(experimentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> labContinuousEvaluationService.create(request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Experiment not found with id: 999");

        verify(labContinuousEvaluationRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenStudentNotFound() {
        Experiment experiment = createExperiment();
        LabContinuousEvaluationRequest request = new LabContinuousEvaluationRequest(
            1L, 999L, 8, 7, 9, 24, LocalDate.of(2024, 6, 1), "Dr. Smith"
        );

        when(experimentRepository.findById(1L)).thenReturn(Optional.of(experiment));
        when(studentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> labContinuousEvaluationService.create(request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Student not found with id: 999");

        verify(labContinuousEvaluationRepository, never()).save(any());
    }

    @Test
    void shouldFindByExperimentId() {
        Experiment experiment = createExperiment();
        Student student = createStudent();
        LabContinuousEvaluation evaluation = createEvaluation(experiment, student);

        when(labContinuousEvaluationRepository.findByExperimentId(1L)).thenReturn(List.of(evaluation));

        List<LabContinuousEvaluationResponse> responses = labContinuousEvaluationService.findByExperimentId(1L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).experimentId()).isEqualTo(1L);
        verify(labContinuousEvaluationRepository).findByExperimentId(1L);
    }

    @Test
    void shouldFindByStudentId() {
        Experiment experiment = createExperiment();
        Student student = createStudent();
        LabContinuousEvaluation evaluation = createEvaluation(experiment, student);

        when(labContinuousEvaluationRepository.findByStudentId(1L)).thenReturn(List.of(evaluation));

        List<LabContinuousEvaluationResponse> responses = labContinuousEvaluationService.findByStudentId(1L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).studentId()).isEqualTo(1L);
        verify(labContinuousEvaluationRepository).findByStudentId(1L);
    }

    @Test
    void shouldFindById() {
        Experiment experiment = createExperiment();
        Student student = createStudent();
        LabContinuousEvaluation evaluation = createEvaluation(experiment, student);

        when(labContinuousEvaluationRepository.findById(1L)).thenReturn(Optional.of(evaluation));

        LabContinuousEvaluationResponse response = labContinuousEvaluationService.findById(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.totalMarks()).isEqualTo(24);
        verify(labContinuousEvaluationRepository).findById(1L);
    }

    @Test
    void shouldThrowWhenNotFoundById() {
        when(labContinuousEvaluationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> labContinuousEvaluationService.findById(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Lab evaluation not found with id: 999");
    }

    @Test
    void shouldUpdateEvaluation() {
        Experiment experiment = createExperiment();
        Student student = createStudent();
        LabContinuousEvaluation existing = createEvaluation(experiment, student);
        LabContinuousEvaluationRequest request = new LabContinuousEvaluationRequest(
            1L, 1L, 9, 8, 10, 27, LocalDate.of(2024, 6, 15), "Dr. Jones"
        );
        LabContinuousEvaluation updated = createEvaluation(experiment, student);
        updated.setTotalMarks(27);
        updated.setEvaluatedBy("Dr. Jones");

        when(labContinuousEvaluationRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(experimentRepository.findById(1L)).thenReturn(Optional.of(experiment));
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(labContinuousEvaluationRepository.save(any(LabContinuousEvaluation.class))).thenReturn(updated);

        LabContinuousEvaluationResponse response = labContinuousEvaluationService.update(1L, request);

        assertThat(response.totalMarks()).isEqualTo(27);
        assertThat(response.evaluatedBy()).isEqualTo("Dr. Jones");
        verify(labContinuousEvaluationRepository).findById(1L);
        verify(labContinuousEvaluationRepository).save(any(LabContinuousEvaluation.class));
    }

    @Test
    void shouldDeleteEvaluation() {
        when(labContinuousEvaluationRepository.existsById(1L)).thenReturn(true);

        labContinuousEvaluationService.delete(1L);

        verify(labContinuousEvaluationRepository).existsById(1L);
        verify(labContinuousEvaluationRepository).deleteById(1L);
    }

    @Test
    void shouldThrowWhenNotFoundOnDelete() {
        when(labContinuousEvaluationRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> labContinuousEvaluationService.delete(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Lab evaluation not found with id: 999");

        verify(labContinuousEvaluationRepository, never()).deleteById(any());
    }

    private Course createCourse() {
        Course course = new Course("Physics", "PHY101", 4, 3, 1, null, 1);
        course.setId(1L);
        return course;
    }

    private Experiment createExperiment() {
        Experiment experiment = new Experiment(createCourse(), 1, "Ohm's Law",
            null, null, null, null, null, null, null, true);
        experiment.setId(1L);
        return experiment;
    }

    private Student createStudent() {
        Student student = new Student("ROLL001", "John", "Doe", "john@example.com",
            null, 1, LocalDate.of(2024, 1, 1), StudentStatus.ACTIVE);
        student.setId(1L);
        return student;
    }

    private LabContinuousEvaluation createEvaluation(Experiment experiment, Student student) {
        LabContinuousEvaluation evaluation = new LabContinuousEvaluation(
            experiment, student, 8, 7, 9, 24, LocalDate.of(2024, 6, 1), "Dr. Smith"
        );
        evaluation.setId(1L);
        evaluation.setCreatedAt(Instant.now());
        evaluation.setUpdatedAt(Instant.now());
        return evaluation;
    }
}
