package com.cms.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.StudentMarkDto;
import com.cms.dto.StudentMarkRequest;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.CourseRegistration;
import com.cms.model.ExamEvent;
import com.cms.model.StudentMark;
import com.cms.model.enums.ExamSessionStatus;
import com.cms.model.enums.MarkStatus;
import com.cms.repository.CourseRegistrationRepository;
import com.cms.repository.ExamEventRepository;
import com.cms.repository.StudentMarkRepository;

@Service
@Transactional(readOnly = true)
public class StudentMarkServiceImpl implements StudentMarkService {

    private final StudentMarkRepository studentMarkRepository;
    private final ExamEventRepository examEventRepository;
    private final CourseRegistrationRepository courseRegistrationRepository;

    public StudentMarkServiceImpl(StudentMarkRepository studentMarkRepository,
                                   ExamEventRepository examEventRepository,
                                   CourseRegistrationRepository courseRegistrationRepository) {
        this.studentMarkRepository = studentMarkRepository;
        this.examEventRepository = examEventRepository;
        this.courseRegistrationRepository = courseRegistrationRepository;
    }

    @Override
    @Transactional
    public StudentMarkDto upsert(StudentMarkRequest request) {
        ExamEvent event = examEventRepository.findById(request.examEventId())
            .orElseThrow(() -> new ResourceNotFoundException("ExamEvent not found: " + request.examEventId()));

        if (event.getExamSession().getStatus() == ExamSessionStatus.LOCKED) {
            throw new IllegalStateException("Cannot modify marks in a LOCKED exam session");
        }

        CourseRegistration registration = courseRegistrationRepository.findById(request.courseRegistrationId())
            .orElseThrow(() -> new ResourceNotFoundException("CourseRegistration not found: " + request.courseRegistrationId()));

        StudentMark mark = studentMarkRepository
            .findByExamEvent_IdAndCourseRegistration_Id(request.examEventId(), request.courseRegistrationId())
            .orElse(new StudentMark());

        BigDecimal marksObtained = resolveMarks(request.markStatus(), request.marksObtained(), event.getMaxMarks());

        mark.setExamEvent(event);
        mark.setCourseRegistration(registration);
        mark.setMarkStatus(request.markStatus());
        mark.setMarksObtained(marksObtained);
        mark.setRemarks(request.remarks());

        return toDto(studentMarkRepository.save(mark));
    }

    @Override
    public StudentMarkDto getById(Long id) {
        return toDto(findById(id));
    }

    @Override
    public List<StudentMarkDto> getByExamEvent(Long examEventId) {
        return studentMarkRepository.findByExamEvent_Id(examEventId).stream()
            .map(this::toDto).toList();
    }

    @Override
    public List<StudentMarkDto> getByEnrollment(Long enrollmentId) {
        return studentMarkRepository.findByCourseRegistration_StudentTermEnrollment_Id(enrollmentId).stream()
            .map(this::toDto).toList();
    }

    private BigDecimal resolveMarks(MarkStatus status, BigDecimal marksObtained, BigDecimal maxMarks) {
        if (status == MarkStatus.ABSENT || status == MarkStatus.MALPRACTICE) {
            return BigDecimal.ZERO;
        }
        if (marksObtained == null) {
            throw new IllegalArgumentException("marksObtained is required for PRESENT status");
        }
        if (marksObtained.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("marksObtained cannot be negative");
        }
        if (marksObtained.compareTo(maxMarks) > 0) {
            throw new IllegalArgumentException("marksObtained cannot exceed maxMarks: " + maxMarks);
        }
        return marksObtained;
    }

    private StudentMark findById(Long id) {
        return studentMarkRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("StudentMark not found: " + id));
    }

    private StudentMarkDto toDto(StudentMark m) {
        var student = m.getCourseRegistration().getStudentTermEnrollment().getStudent();
        var subject = m.getExamEvent().getCourseOffering().getSubject();
        return new StudentMarkDto(m.getId(), m.getExamEvent().getId(), subject.getName(),
            m.getCourseRegistration().getId(), student.getId(), student.getFullName(),
            m.getMarkStatus(), m.getMarksObtained(), m.getExamEvent().getMaxMarks(), m.getRemarks());
    }
}
