package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.dto.ExperimentRequest;
import com.cms.dto.ExperimentResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Subject;
import com.cms.model.Experiment;
import com.cms.repository.SubjectRepository;
import com.cms.repository.ExperimentRepository;

@ExtendWith(MockitoExtension.class)
class ExperimentServiceTest {

    @Mock
    private ExperimentRepository experimentRepository;

    @Mock
    private SubjectRepository subjectRepository;

    private ExperimentService experimentService;

    private Subject testCourse;

    @BeforeEach
    void setUp() {
        experimentService = new ExperimentService(experimentRepository, subjectRepository);
        testCourse = createSubject(1L, "Data Structures Lab", "CS201L");
    }

    @Test
    void shouldCreateExperiment() {
        ExperimentRequest request = new ExperimentRequest(
            1L, 1, "Stack Implementation",
            "Implement stack using arrays",
            "To understand stack operations",
            "Computer with IDE",
            "1. Create stack class...",
            "Working stack implementation",
            "CO1, CO2",
            120, true
        );

        Experiment savedExperiment = createExperiment(1L, testCourse, 1, "Stack Implementation");

        when(subjectRepository.findById(1L)).thenReturn(Optional.of(testCourse));
        when(experimentRepository.save(any(Experiment.class))).thenReturn(savedExperiment);

        ExperimentResponse response = experimentService.create(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.experimentNumber()).isEqualTo(1);
        assertThat(response.name()).isEqualTo("Stack Implementation");

        ArgumentCaptor<Experiment> captor = ArgumentCaptor.forClass(Experiment.class);
        verify(experimentRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Stack Implementation");
    }

    @Test
    void shouldThrowExceptionWhenCreatingExperimentWithNonExistentCourse() {
        ExperimentRequest request = new ExperimentRequest(
            999L, 1, "Test", "Desc", "Aim", "App", "Proc", "Out", "LO", 60, true
        );

        when(subjectRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> experimentService.create(request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Subject not found with id: 999");

        verify(experimentRepository, never()).save(any(Experiment.class));
    }

    @Test
    void shouldFindAllExperiments() {
        Experiment exp1 = createExperiment(1L, testCourse, 1, "Exp 1");
        Experiment exp2 = createExperiment(2L, testCourse, 2, "Exp 2");

        when(experimentRepository.findAll()).thenReturn(List.of(exp1, exp2));

        List<ExperimentResponse> responses = experimentService.findAll();

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).experimentNumber()).isEqualTo(1);
        assertThat(responses.get(1).experimentNumber()).isEqualTo(2);
    }

    @Test
    void shouldFindExperimentById() {
        Experiment experiment = createExperiment(1L, testCourse, 1, "Stack Implementation");

        when(experimentRepository.findById(1L)).thenReturn(Optional.of(experiment));

        ExperimentResponse response = experimentService.findById(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Stack Implementation");
    }

    @Test
    void shouldThrowExceptionWhenExperimentNotFoundById() {
        when(experimentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> experimentService.findById(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Experiment not found with id: 999");
    }

    @Test
    void shouldFindExperimentsByCourseId() {
        Experiment exp = createExperiment(1L, testCourse, 1, "Test");

        when(subjectRepository.existsById(1L)).thenReturn(true);
        when(experimentRepository.findBySubjectIdOrderByExperimentNumberAsc(1L)).thenReturn(List.of(exp));

        List<ExperimentResponse> responses = experimentService.findBySubjectId(1L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).subjectId()).isEqualTo(1L);
    }

    @Test
    void shouldFindActiveExperimentsByCourseId() {
        Experiment exp = createExperiment(1L, testCourse, 1, "Test");

        when(subjectRepository.existsById(1L)).thenReturn(true);
        when(experimentRepository.findBySubjectIdAndIsActiveTrue(1L)).thenReturn(List.of(exp));

        List<ExperimentResponse> responses = experimentService.findActiveBySubjectId(1L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).isActive()).isTrue();
    }

    @Test
    void shouldThrowExceptionWhenFindByCourseIdWithNonExistentCourse() {
        when(subjectRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> experimentService.findBySubjectId(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Subject not found with id: 999");
    }

    @Test
    void shouldThrowExceptionWhenFindActiveByCourseIdWithNonExistentCourse() {
        when(subjectRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> experimentService.findActiveBySubjectId(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Subject not found with id: 999");
    }

    @Test
    void shouldUpdateExperiment() {
        Experiment existingExperiment = createExperiment(1L, testCourse, 1, "Old Name");

        ExperimentRequest updateRequest = new ExperimentRequest(
            1L, 1, "Updated Name", "Updated desc", "Updated aim",
            "Updated app", "Updated proc", "Updated out", "Updated LO", 180, true
        );

        Experiment updatedExperiment = createExperiment(1L, testCourse, 1, "Updated Name");

        when(experimentRepository.findById(1L)).thenReturn(Optional.of(existingExperiment));
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(testCourse));
        when(experimentRepository.save(any(Experiment.class))).thenReturn(updatedExperiment);

        ExperimentResponse response = experimentService.update(1L, updateRequest);

        assertThat(response.name()).isEqualTo("Updated Name");
        verify(experimentRepository).save(any(Experiment.class));
    }

    @Test
    void shouldDeleteExperiment() {
        when(experimentRepository.existsById(1L)).thenReturn(true);

        experimentService.delete(1L);

        verify(experimentRepository).deleteById(1L);
    }

    @Test
    void shouldThrowExceptionWhenDeletingNonExistentExperiment() {
        when(experimentRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> experimentService.delete(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Experiment not found with id: 999");

        verify(experimentRepository, never()).deleteById(any());
    }

    private Subject createSubject(Long id, String name, String code) {
        Subject subject = new Subject(name, code, 3, 2, 1, null, null, 1);
        subject.setId(id);
        return subject;
    }

    private Experiment createExperiment(Long id, Subject subject, Integer expNum, String name) {
        Experiment experiment = new Experiment(
            subject, expNum, name, "Description", "Aim", "Apparatus",
            "Procedure", "Expected Outcome", "Learning Outcomes", 120, true
        );
        experiment.setId(id);
        Instant now = Instant.now();
        experiment.setCreatedAt(now);
        experiment.setUpdatedAt(now);
        return experiment;
    }
}
