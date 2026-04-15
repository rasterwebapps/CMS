package com.cms.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.ExperimentRequest;
import com.cms.dto.ExperimentResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Subject;
import com.cms.model.Experiment;
import com.cms.repository.SubjectRepository;
import com.cms.repository.ExperimentRepository;

@Service
@Transactional(readOnly = true)
public class ExperimentService {

    private final ExperimentRepository experimentRepository;
    private final SubjectRepository subjectRepository;

    public ExperimentService(ExperimentRepository experimentRepository, SubjectRepository subjectRepository) {
        this.experimentRepository = experimentRepository;
        this.subjectRepository = subjectRepository;
    }

    @Transactional
    public ExperimentResponse create(ExperimentRequest request) {
        Subject subject = subjectRepository.findById(request.subjectId())
            .orElseThrow(() -> new ResourceNotFoundException("Subject not found with id: " + request.subjectId()));

        Boolean isActive = request.isActive() != null ? request.isActive() : true;

        Experiment experiment = new Experiment(
            subject,
            request.experimentNumber(),
            request.name(),
            request.description(),
            request.aim(),
            request.apparatus(),
            request.procedure(),
            request.expectedOutcome(),
            request.learningOutcomes(),
            request.estimatedDurationMinutes(),
            isActive
        );

        Experiment saved = experimentRepository.save(experiment);
        return toResponse(saved);
    }

    public List<ExperimentResponse> findAll() {
        return experimentRepository.findAll().stream()
            .map(this::toResponse)
            .toList();
    }

    public ExperimentResponse findById(Long id) {
        Experiment experiment = experimentRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Experiment not found with id: " + id));
        return toResponse(experiment);
    }

    public List<ExperimentResponse> findBySubjectId(Long subjectId) {
        if (!subjectRepository.existsById(subjectId)) {
            throw new ResourceNotFoundException("Subject not found with id: " + subjectId);
        }
        return experimentRepository.findBySubjectIdOrderByExperimentNumberAsc(subjectId).stream()
            .map(this::toResponse)
            .toList();
    }

    public List<ExperimentResponse> findActiveBySubjectId(Long subjectId) {
        if (!subjectRepository.existsById(subjectId)) {
            throw new ResourceNotFoundException("Subject not found with id: " + subjectId);
        }
        return experimentRepository.findBySubjectIdAndIsActiveTrue(subjectId).stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public ExperimentResponse update(Long id, ExperimentRequest request) {
        Experiment experiment = experimentRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Experiment not found with id: " + id));

        Subject subject = subjectRepository.findById(request.subjectId())
            .orElseThrow(() -> new ResourceNotFoundException("Subject not found with id: " + request.subjectId()));

        experiment.setSubject(subject);
        experiment.setExperimentNumber(request.experimentNumber());
        experiment.setName(request.name());
        experiment.setDescription(request.description());
        experiment.setAim(request.aim());
        experiment.setApparatus(request.apparatus());
        experiment.setProcedure(request.procedure());
        experiment.setExpectedOutcome(request.expectedOutcome());
        experiment.setLearningOutcomes(request.learningOutcomes());
        experiment.setEstimatedDurationMinutes(request.estimatedDurationMinutes());

        if (request.isActive() != null) {
            experiment.setIsActive(request.isActive());
        }

        Experiment updated = experimentRepository.save(experiment);
        return toResponse(updated);
    }

    @Transactional
    public void delete(Long id) {
        if (!experimentRepository.existsById(id)) {
            throw new ResourceNotFoundException("Experiment not found with id: " + id);
        }
        experimentRepository.deleteById(id);
    }

    private ExperimentResponse toResponse(Experiment experiment) {
        return new ExperimentResponse(
            experiment.getId(),
            experiment.getSubject().getId(),
            experiment.getSubject().getName(),
            experiment.getSubject().getCode(),
            experiment.getExperimentNumber(),
            experiment.getName(),
            experiment.getDescription(),
            experiment.getAim(),
            experiment.getApparatus(),
            experiment.getProcedure(),
            experiment.getExpectedOutcome(),
            experiment.getLearningOutcomes(),
            experiment.getEstimatedDurationMinutes(),
            experiment.getIsActive(),
            experiment.getCreatedAt(),
            experiment.getUpdatedAt()
        );
    }
}
