package com.cms.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.ExaminationRequest;
import com.cms.dto.ExaminationResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Examination;
import com.cms.model.Subject;
import com.cms.repository.ExaminationRepository;
import com.cms.repository.SubjectRepository;

@Service
@Transactional(readOnly = true)
public class ExaminationService {

    private final ExaminationRepository examinationRepository;
    private final SubjectRepository subjectRepository;

    public ExaminationService(ExaminationRepository examinationRepository,
                               SubjectRepository subjectRepository) {
        this.examinationRepository = examinationRepository;
        this.subjectRepository = subjectRepository;
    }

    @Transactional
    public ExaminationResponse create(ExaminationRequest request) {
        Subject subject = subjectRepository.findById(request.subjectId())
            .orElseThrow(() -> new ResourceNotFoundException("Subject not found with id: " + request.subjectId()));
        Examination examination = new Examination(
            request.name(), subject, request.examType(),
            request.date(), request.duration(), request.maxMarks()
        );
        return toResponse(examinationRepository.save(examination));
    }

    public List<ExaminationResponse> findAll() {
        return examinationRepository.findAll().stream().map(this::toResponse).toList();
    }

    public ExaminationResponse findById(Long id) {
        return toResponse(examinationRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Examination not found with id: " + id)));
    }

    public List<ExaminationResponse> findBySubjectId(Long subjectId) {
        return examinationRepository.findBySubjectId(subjectId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public ExaminationResponse update(Long id, ExaminationRequest request) {
        Examination examination = examinationRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Examination not found with id: " + id));
        Subject subject = subjectRepository.findById(request.subjectId())
            .orElseThrow(() -> new ResourceNotFoundException("Subject not found with id: " + request.subjectId()));
        examination.setName(request.name());
        examination.setSubject(subject);
        examination.setExamType(request.examType());
        examination.setDate(request.date());
        examination.setDuration(request.duration());
        examination.setMaxMarks(request.maxMarks());
        return toResponse(examinationRepository.save(examination));
    }

    @Transactional
    public void delete(Long id) {
        if (!examinationRepository.existsById(id)) {
            throw new ResourceNotFoundException("Examination not found with id: " + id);
        }
        examinationRepository.deleteById(id);
    }

    private ExaminationResponse toResponse(Examination examination) {
        return new ExaminationResponse(
            examination.getId(),
            examination.getName(),
            examination.getSubject().getId(),
            examination.getSubject().getName(),
            examination.getExamType(),
            examination.getDate(),
            examination.getDuration(),
            examination.getMaxMarks(),
            examination.getCreatedAt(),
            examination.getUpdatedAt()
        );
    }
}
