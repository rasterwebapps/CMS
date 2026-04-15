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

import com.cms.dto.LabCurriculumMappingRequest;
import com.cms.dto.LabCurriculumMappingResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Subject;
import com.cms.model.Experiment;
import com.cms.model.LabCurriculumMapping;
import com.cms.model.Program;
import com.cms.model.enums.MappingLevel;
import com.cms.model.enums.OutcomeType;
import com.cms.repository.ExperimentRepository;
import com.cms.repository.LabCurriculumMappingRepository;

@ExtendWith(MockitoExtension.class)
class LabCurriculumMappingServiceTest {

    @Mock
    private LabCurriculumMappingRepository mappingRepository;

    @Mock
    private ExperimentRepository experimentRepository;

    private LabCurriculumMappingService mappingService;

    private Experiment testExperiment;
    private Subject testCourse;

    @BeforeEach
    void setUp() {
        mappingService = new LabCurriculumMappingService(mappingRepository, experimentRepository);
        testCourse = createSubject(1L, "Data Structures Lab", "CS201L");
        testExperiment = createExperiment(1L, testCourse, 1, "Stack Implementation");
    }

    @Test
    void shouldCreateMapping() {
        LabCurriculumMappingRequest request = new LabCurriculumMappingRequest(
            1L, OutcomeType.COURSE_OUTCOME, "CO1",
            "Understand stack operations", MappingLevel.HIGH,
            "Direct correlation with stack concepts"
        );

        LabCurriculumMapping savedMapping = createMapping(1L, testExperiment, OutcomeType.COURSE_OUTCOME, "CO1", MappingLevel.HIGH);

        when(experimentRepository.findById(1L)).thenReturn(Optional.of(testExperiment));
        when(mappingRepository.save(any(LabCurriculumMapping.class))).thenReturn(savedMapping);

        LabCurriculumMappingResponse response = mappingService.create(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.outcomeType()).isEqualTo(OutcomeType.COURSE_OUTCOME);
        assertThat(response.outcomeCode()).isEqualTo("CO1");
        assertThat(response.mappingLevel()).isEqualTo(MappingLevel.HIGH);

        ArgumentCaptor<LabCurriculumMapping> captor = ArgumentCaptor.forClass(LabCurriculumMapping.class);
        verify(mappingRepository).save(captor.capture());
        assertThat(captor.getValue().getOutcomeCode()).isEqualTo("CO1");
    }

    @Test
    void shouldThrowExceptionWhenCreatingMappingWithNonExistentExperiment() {
        LabCurriculumMappingRequest request = new LabCurriculumMappingRequest(
            999L, OutcomeType.COURSE_OUTCOME, "CO1", "Desc", MappingLevel.MEDIUM, "Just"
        );

        when(experimentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mappingService.create(request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Experiment not found with id: 999");

        verify(mappingRepository, never()).save(any(LabCurriculumMapping.class));
    }

    @Test
    void shouldFindAllMappings() {
        LabCurriculumMapping mapping1 = createMapping(1L, testExperiment, OutcomeType.COURSE_OUTCOME, "CO1", MappingLevel.HIGH);
        LabCurriculumMapping mapping2 = createMapping(2L, testExperiment, OutcomeType.PROGRAM_OUTCOME, "PO1", MappingLevel.MEDIUM);

        when(mappingRepository.findAll()).thenReturn(List.of(mapping1, mapping2));

        List<LabCurriculumMappingResponse> responses = mappingService.findAll();

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).outcomeType()).isEqualTo(OutcomeType.COURSE_OUTCOME);
        assertThat(responses.get(1).outcomeType()).isEqualTo(OutcomeType.PROGRAM_OUTCOME);
    }

    @Test
    void shouldFindMappingById() {
        LabCurriculumMapping mapping = createMapping(1L, testExperiment, OutcomeType.COURSE_OUTCOME, "CO1", MappingLevel.HIGH);

        when(mappingRepository.findById(1L)).thenReturn(Optional.of(mapping));

        LabCurriculumMappingResponse response = mappingService.findById(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.experimentId()).isEqualTo(1L);
    }

    @Test
    void shouldThrowExceptionWhenMappingNotFoundById() {
        when(mappingRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mappingService.findById(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Lab curriculum mapping not found with id: 999");
    }

    @Test
    void shouldFindMappingsByExperimentId() {
        LabCurriculumMapping mapping = createMapping(1L, testExperiment, OutcomeType.COURSE_OUTCOME, "CO1", MappingLevel.HIGH);

        when(experimentRepository.existsById(1L)).thenReturn(true);
        when(mappingRepository.findByExperimentId(1L)).thenReturn(List.of(mapping));

        List<LabCurriculumMappingResponse> responses = mappingService.findByExperimentId(1L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).experimentId()).isEqualTo(1L);
    }

    @Test
    void shouldFindMappingsByExperimentIdAndOutcomeType() {
        LabCurriculumMapping mapping = createMapping(1L, testExperiment, OutcomeType.COURSE_OUTCOME, "CO1", MappingLevel.HIGH);

        when(experimentRepository.existsById(1L)).thenReturn(true);
        when(mappingRepository.findByExperimentIdAndOutcomeType(1L, OutcomeType.COURSE_OUTCOME))
            .thenReturn(List.of(mapping));

        List<LabCurriculumMappingResponse> responses =
            mappingService.findByExperimentIdAndOutcomeType(1L, OutcomeType.COURSE_OUTCOME);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).outcomeType()).isEqualTo(OutcomeType.COURSE_OUTCOME);
    }

    @Test
    void shouldUpdateMapping() {
        LabCurriculumMapping existingMapping = createMapping(1L, testExperiment, OutcomeType.COURSE_OUTCOME, "CO1", MappingLevel.MEDIUM);

        LabCurriculumMappingRequest updateRequest = new LabCurriculumMappingRequest(
            1L, OutcomeType.COURSE_OUTCOME, "CO2", "Updated desc", MappingLevel.HIGH, "Updated just"
        );

        LabCurriculumMapping updatedMapping = createMapping(1L, testExperiment, OutcomeType.COURSE_OUTCOME, "CO2", MappingLevel.HIGH);

        when(mappingRepository.findById(1L)).thenReturn(Optional.of(existingMapping));
        when(experimentRepository.findById(1L)).thenReturn(Optional.of(testExperiment));
        when(mappingRepository.save(any(LabCurriculumMapping.class))).thenReturn(updatedMapping);

        LabCurriculumMappingResponse response = mappingService.update(1L, updateRequest);

        assertThat(response.outcomeCode()).isEqualTo("CO2");
        assertThat(response.mappingLevel()).isEqualTo(MappingLevel.HIGH);
        verify(mappingRepository).save(any(LabCurriculumMapping.class));
    }

    @Test
    void shouldDeleteMapping() {
        when(mappingRepository.existsById(1L)).thenReturn(true);

        mappingService.delete(1L);

        verify(mappingRepository).deleteById(1L);
    }

    @Test
    void shouldThrowExceptionWhenDeletingNonExistentMapping() {
        when(mappingRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> mappingService.delete(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Lab curriculum mapping not found with id: 999");

        verify(mappingRepository, never()).deleteById(any());
    }

    private Subject createSubject(Long id, String name, String code) {
        Program program = new Program();
        program.setId(1L);
        program.setName("Computer Science");

        Subject course = new Subject(name, code, 3, 2, 1, null, null, 1);
        course.setId(id);
        return course;
    }

    private Experiment createExperiment(Long id, Subject subject, Integer expNum, String name) {
        Experiment experiment = new Experiment(
            subject, expNum, name, "Description", "Aim", "Apparatus",
            "Procedure", "Expected Outcome", "Learning Outcomes", 120, true
        );
        experiment.setId(id);
        return experiment;
    }

    private LabCurriculumMapping createMapping(Long id, Experiment experiment, OutcomeType type,
                                                String code, MappingLevel level) {
        LabCurriculumMapping mapping = new LabCurriculumMapping(
            experiment, type, code, "Description", level, "Justification"
        );
        mapping.setId(id);
        Instant now = Instant.now();
        mapping.setCreatedAt(now);
        mapping.setUpdatedAt(now);
        return mapping;
    }
}
