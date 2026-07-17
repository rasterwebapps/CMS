package com.cms.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import com.cms.dto.ExamResultRequest;
import com.cms.dto.ExamResultResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.ExamResult;
import com.cms.model.Examination;
import com.cms.model.Student;
import com.cms.model.enums.ExamOutcome;
import com.cms.model.enums.ExamResultStatus;
import com.cms.repository.ExamResultRepository;
import com.cms.repository.ExaminationRepository;
import com.cms.repository.StudentRepository;

@Service
@Transactional(readOnly = true)
public class ExamResultService {

    private final ExamResultRepository examResultRepository;
    private final ExaminationRepository examinationRepository;
    private final StudentRepository studentRepository;

    public ExamResultService(ExamResultRepository examResultRepository,
                              ExaminationRepository examinationRepository,
                              StudentRepository studentRepository) {
        this.examResultRepository = examResultRepository;
        this.examinationRepository = examinationRepository;
        this.studentRepository = studentRepository;
    }

    @Transactional
    public ExamResultResponse create(ExamResultRequest request) {
        Examination examination = examinationRepository.findById(request.examinationId())
            .orElseThrow(() -> new ResourceNotFoundException("Examination not found with id: " + request.examinationId()));
        Student student = studentRepository.findById(request.studentId())
            .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + request.studentId()));
        ExamResult examResult = new ExamResult(
            examination, student, request.marksObtained(), request.grade(), request.status()
        );
        examResult.setOutcome(deriveOutcome(examination, request.marksObtained(), request.status()));
        ExamResult saved = examResultRepository.save(examResult);
        return toResponse(saved);
    }

    public List<ExamResultResponse> findByExaminationId(Long examinationId) {
        return examResultRepository.findByExaminationId(examinationId).stream().map(this::toResponse).toList();
    }

    public List<ExamResultResponse> findByStudentId(Long studentId) {
        return examResultRepository.findByStudentId(studentId).stream().map(this::toResponse).toList();
    }

    public ExamResultResponse findById(Long id) {
        ExamResult examResult = examResultRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Exam result not found with id: " + id));
        return toResponse(examResult);
    }

    @Transactional
    public ExamResultResponse update(Long id, ExamResultRequest request) {
        ExamResult examResult = examResultRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Exam result not found with id: " + id));
        Examination examination = examinationRepository.findById(request.examinationId())
            .orElseThrow(() -> new ResourceNotFoundException("Examination not found with id: " + request.examinationId()));
        Student student = studentRepository.findById(request.studentId())
            .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + request.studentId()));
        examResult.setExamination(examination);
        examResult.setStudent(student);
        examResult.setMarksObtained(request.marksObtained());
        examResult.setGrade(request.grade());
        examResult.setStatus(request.status());
        examResult.setOutcome(deriveOutcome(examination, request.marksObtained(), request.status()));
        ExamResult updated = examResultRepository.save(examResult);
        return toResponse(updated);
    }

    /**
     * PASS/FAIL, derived from marksObtained vs. 50% of the examination's maxMarks, only once a
     * result is PUBLISHED — pending/withheld results don't count toward Student Promotion arrear
     * detection. External/university marks only for v1; internal/CIA marks don't exist yet.
     */
    private ExamOutcome deriveOutcome(Examination examination, BigDecimal marksObtained, ExamResultStatus status) {
        if (status != ExamResultStatus.PUBLISHED || marksObtained == null || examination.getMaxMarks() == null) {
            return null;
        }
        BigDecimal maxMarks = BigDecimal.valueOf(examination.getMaxMarks());
        boolean passed = marksObtained.multiply(BigDecimal.valueOf(100))
            .compareTo(maxMarks.multiply(BigDecimal.valueOf(50))) >= 0;
        return passed ? ExamOutcome.PASS : ExamOutcome.FAIL;
    }

    @Transactional
    public void delete(Long id) {
        if (!examResultRepository.existsById(id)) {
            throw new ResourceNotFoundException("Exam result not found with id: " + id);
        }
        examResultRepository.deleteById(id);
    }

    private ExamResultResponse toResponse(ExamResult examResult) {
        return new ExamResultResponse(
            examResult.getId(),
            examResult.getExamination().getId(),
            examResult.getExamination().getName(),
            examResult.getStudent().getId(),
            examResult.getStudent().getFullName(),
            examResult.getStudent().getRollNumber(),
            examResult.getMarksObtained(),
            examResult.getGrade(),
            examResult.getStatus(),
            examResult.getOutcome(),
            examResult.getCreatedAt(),
            examResult.getUpdatedAt()
        );
    }
}
