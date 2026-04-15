package com.cms.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.LabCurriculumMappingRequest;
import com.cms.dto.LabCurriculumMappingResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Experiment;
import com.cms.model.LabCurriculumMapping;
import com.cms.model.enums.OutcomeType;
import com.cms.repository.ExperimentRepository;
import com.cms.repository.LabCurriculumMappingRepository;

@Service
@Transactional(readOnly = true)
public class LabCurriculumMappingService {

    private final LabCurriculumMappingRepository mappingRepository;
    private final ExperimentRepository experimentRepository;

    public LabCurriculumMappingService(LabCurriculumMappingRepository mappingRepository,
                                        ExperimentRepository experimentRepository) {
        this.mappingRepository = mappingRepository;
        this.experimentRepository = experimentRepository;
    }

    @Transactional
    public LabCurriculumMappingResponse create(LabCurriculumMappingRequest request) {
        Experiment experiment = experimentRepository.findById(request.experimentId())
            .orElseThrow(() -> new ResourceNotFoundException("Experiment not found with id: " + request.experimentId()));

        LabCurriculumMapping mapping = new LabCurriculumMapping(
            experiment,
            request.outcomeType(),
            request.outcomeCode(),
            request.outcomeDescription(),
            request.mappingLevel(),
            request.justification()
        );

        LabCurriculumMapping saved = mappingRepository.save(mapping);
        return toResponse(saved);
    }

    public List<LabCurriculumMappingResponse> findAll() {
        return mappingRepository.findAll().stream()
            .map(this::toResponse)
            .toList();
    }

    public LabCurriculumMappingResponse findById(Long id) {
        LabCurriculumMapping mapping = mappingRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Lab curriculum mapping not found with id: " + id));
        return toResponse(mapping);
    }

    public List<LabCurriculumMappingResponse> findByExperimentId(Long experimentId) {
        if (!experimentRepository.existsById(experimentId)) {
            throw new ResourceNotFoundException("Experiment not found with id: " + experimentId);
        }
        return mappingRepository.findByExperimentId(experimentId).stream()
            .map(this::toResponse)
            .toList();
    }

    public List<LabCurriculumMappingResponse> findByExperimentIdAndOutcomeType(Long experimentId, OutcomeType outcomeType) {
        if (!experimentRepository.existsById(experimentId)) {
            throw new ResourceNotFoundException("Experiment not found with id: " + experimentId);
        }
        return mappingRepository.findByExperimentIdAndOutcomeType(experimentId, outcomeType).stream()
            .map(this::toResponse)
            .toList();
    }

    public List<LabCurriculumMappingResponse> findByOutcomeCode(String outcomeCode) {
        return mappingRepository.findByOutcomeCode(outcomeCode).stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public LabCurriculumMappingResponse update(Long id, LabCurriculumMappingRequest request) {
        LabCurriculumMapping mapping = mappingRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Lab curriculum mapping not found with id: " + id));

        Experiment experiment = experimentRepository.findById(request.experimentId())
            .orElseThrow(() -> new ResourceNotFoundException("Experiment not found with id: " + request.experimentId()));

        mapping.setExperiment(experiment);
        mapping.setOutcomeType(request.outcomeType());
        mapping.setOutcomeCode(request.outcomeCode());
        mapping.setOutcomeDescription(request.outcomeDescription());
        mapping.setMappingLevel(request.mappingLevel());
        mapping.setJustification(request.justification());

        LabCurriculumMapping updated = mappingRepository.save(mapping);
        return toResponse(updated);
    }

    @Transactional
    public void delete(Long id) {
        if (!mappingRepository.existsById(id)) {
            throw new ResourceNotFoundException("Lab curriculum mapping not found with id: " + id);
        }
        mappingRepository.deleteById(id);
    }

    private LabCurriculumMappingResponse toResponse(LabCurriculumMapping mapping) {
        return new LabCurriculumMappingResponse(
            mapping.getId(),
            mapping.getExperiment().getId(),
            mapping.getExperiment().getName(),
            mapping.getExperiment().getExperimentNumber(),
            mapping.getExperiment().getSubject().getId(),
            mapping.getExperiment().getSubject().getName(),
            mapping.getOutcomeType(),
            mapping.getOutcomeCode(),
            mapping.getOutcomeDescription(),
            mapping.getMappingLevel(),
            mapping.getJustification(),
            mapping.getCreatedAt(),
            mapping.getUpdatedAt()
        );
    }
}
