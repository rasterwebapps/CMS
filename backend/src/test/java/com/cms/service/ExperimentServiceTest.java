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
import com.cms.model.Course;
import com.cms.model.Experiment;
import com.cms.model.Program;
import com.cms.repository.CourseRepository;
import com.cms.repository.ExperimentRepository;

@ExtendWith(MockitoExtension.class)
class ExperimentServiceTest {

    @Mock
    private ExperimentRepository experimentRepository;

    @Mock
    private CourseRepository courseRepository;

    private ExperimentService experimentService;

    private Course testCourse;

    @BeforeEach
    void setUp() {
        experimentService = new ExperimentService(experimentRepository, courseRepository);
        testCourse = createCourse(1L, "Data Structures Lab", "CS201L");
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

        when(courseRepository.findById(1L)).thenReturn(Optional.of(testCourse));
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

        when(courseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> experimentService.create(request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Course not found with id: 999");

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

        when(courseRepository.existsById(1L)).thenReturn(true);
        when(experimentRepository.findByCourseIdOrderByExperimentNumberAsc(1L)).thenReturn(List.of(exp));

        List<ExperimentResponse> responses = experimentService.findByCourseId(1L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).courseId()).isEqualTo(1L);
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
        when(courseRepository.findById(1L)).thenReturn(Optional.of(testCourse));
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

    private Course createCourse(Long id, String name, String code) {
        Program program = new Program();
        program.setId(1L);
        program.setName("Computer Science");

        Course course = new Course(name, code, 3, 2, 1, program, 1);
        course.setId(id);
        return course;
    }

    private Experiment createExperiment(Long id, Course course, Integer expNum, String name) {
        Experiment experiment = new Experiment(
            course, expNum, name, "Description", "Aim", "Apparatus",
            "Procedure", "Expected Outcome", "Learning Outcomes", 120, true
        );
        experiment.setId(id);
        Instant now = Instant.now();
        experiment.setCreatedAt(now);
        experiment.setUpdatedAt(now);
        return experiment;
    }
}
