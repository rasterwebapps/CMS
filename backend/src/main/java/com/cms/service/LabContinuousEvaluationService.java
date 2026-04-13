package com.cms.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.LabContinuousEvaluationRequest;
import com.cms.dto.LabContinuousEvaluationResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Experiment;
import com.cms.model.LabContinuousEvaluation;
import com.cms.model.Student;
import com.cms.repository.ExperimentRepository;
import com.cms.repository.LabContinuousEvaluationRepository;
import com.cms.repository.StudentRepository;

@Service
@Transactional(readOnly = true)
public class LabContinuousEvaluationService {

    private final LabContinuousEvaluationRepository labContinuousEvaluationRepository;
    private final ExperimentRepository experimentRepository;
    private final StudentRepository studentRepository;

    public LabContinuousEvaluationService(LabContinuousEvaluationRepository labContinuousEvaluationRepository,
                                           ExperimentRepository experimentRepository,
                                           StudentRepository studentRepository) {
        this.labContinuousEvaluationRepository = labContinuousEvaluationRepository;
        this.experimentRepository = experimentRepository;
        this.studentRepository = studentRepository;
    }

    @Transactional
    public LabContinuousEvaluationResponse create(LabContinuousEvaluationRequest request) {
        Experiment experiment = experimentRepository.findById(request.experimentId())
            .orElseThrow(() -> new ResourceNotFoundException("Experiment not found with id: " + request.experimentId()));
        Student student = studentRepository.findById(request.studentId())
            .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + request.studentId()));
        LabContinuousEvaluation evaluation = new LabContinuousEvaluation(
            experiment, student, request.recordMarks(), request.vivaMarks(),
            request.performanceMarks(), request.totalMarks(),
            request.evaluationDate(), request.evaluatedBy()
        );
        LabContinuousEvaluation saved = labContinuousEvaluationRepository.save(evaluation);
        return toResponse(saved);
    }

    public List<LabContinuousEvaluationResponse> findByExperimentId(Long experimentId) {
        return labContinuousEvaluationRepository.findByExperimentId(experimentId).stream()
            .map(this::toResponse).toList();
    }

    public List<LabContinuousEvaluationResponse> findByStudentId(Long studentId) {
        return labContinuousEvaluationRepository.findByStudentId(studentId).stream()
            .map(this::toResponse).toList();
    }

    public LabContinuousEvaluationResponse findById(Long id) {
        LabContinuousEvaluation evaluation = labContinuousEvaluationRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Lab evaluation not found with id: " + id));
        return toResponse(evaluation);
    }

    @Transactional
    public LabContinuousEvaluationResponse update(Long id, LabContinuousEvaluationRequest request) {
        LabContinuousEvaluation evaluation = labContinuousEvaluationRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Lab evaluation not found with id: " + id));
        Experiment experiment = experimentRepository.findById(request.experimentId())
            .orElseThrow(() -> new ResourceNotFoundException("Experiment not found with id: " + request.experimentId()));
        Student student = studentRepository.findById(request.studentId())
            .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + request.studentId()));
        evaluation.setExperiment(experiment);
        evaluation.setStudent(student);
        evaluation.setRecordMarks(request.recordMarks());
        evaluation.setVivaMarks(request.vivaMarks());
        evaluation.setPerformanceMarks(request.performanceMarks());
        evaluation.setTotalMarks(request.totalMarks());
        evaluation.setEvaluationDate(request.evaluationDate());
        evaluation.setEvaluatedBy(request.evaluatedBy());
        LabContinuousEvaluation updated = labContinuousEvaluationRepository.save(evaluation);
        return toResponse(updated);
    }

    @Transactional
    public void delete(Long id) {
        if (!labContinuousEvaluationRepository.existsById(id)) {
            throw new ResourceNotFoundException("Lab evaluation not found with id: " + id);
        }
        labContinuousEvaluationRepository.deleteById(id);
    }

    private LabContinuousEvaluationResponse toResponse(LabContinuousEvaluation evaluation) {
        return new LabContinuousEvaluationResponse(
            evaluation.getId(),
            evaluation.getExperiment().getId(),
            evaluation.getExperiment().getName(),
            evaluation.getStudent().getId(),
            evaluation.getStudent().getFullName(),
            evaluation.getStudent().getRollNumber(),
            evaluation.getRecordMarks(),
            evaluation.getVivaMarks(),
            evaluation.getPerformanceMarks(),
            evaluation.getTotalMarks(),
            evaluation.getEvaluationDate(),
            evaluation.getEvaluatedBy(),
            evaluation.getCreatedAt(),
            evaluation.getUpdatedAt()
        );
    }
}
