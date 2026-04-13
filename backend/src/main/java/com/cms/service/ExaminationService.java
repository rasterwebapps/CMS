package com.cms.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.ExaminationRequest;
import com.cms.dto.ExaminationResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Course;
import com.cms.model.Examination;
import com.cms.model.Semester;
import com.cms.repository.CourseRepository;
import com.cms.repository.ExaminationRepository;
import com.cms.repository.SemesterRepository;

@Service
@Transactional(readOnly = true)
public class ExaminationService {

    private final ExaminationRepository examinationRepository;
    private final CourseRepository courseRepository;
    private final SemesterRepository semesterRepository;

    public ExaminationService(ExaminationRepository examinationRepository,
                               CourseRepository courseRepository,
                               SemesterRepository semesterRepository) {
        this.examinationRepository = examinationRepository;
        this.courseRepository = courseRepository;
        this.semesterRepository = semesterRepository;
    }

    @Transactional
    public ExaminationResponse create(ExaminationRequest request) {
        Course course = courseRepository.findById(request.courseId())
            .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + request.courseId()));
        Semester semester = null;
        if (request.semesterId() != null) {
            semester = semesterRepository.findById(request.semesterId())
                .orElseThrow(() -> new ResourceNotFoundException("Semester not found with id: " + request.semesterId()));
        }
        Examination examination = new Examination(
            request.name(), course, request.examType(),
            request.date(), request.duration(), request.maxMarks(), semester
        );
        Examination saved = examinationRepository.save(examination);
        return toResponse(saved);
    }

    public List<ExaminationResponse> findAll() {
        return examinationRepository.findAll().stream().map(this::toResponse).toList();
    }

    public ExaminationResponse findById(Long id) {
        Examination examination = examinationRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Examination not found with id: " + id));
        return toResponse(examination);
    }

    public List<ExaminationResponse> findByCourseId(Long courseId) {
        return examinationRepository.findByCourseId(courseId).stream().map(this::toResponse).toList();
    }

    public List<ExaminationResponse> findBySemesterId(Long semesterId) {
        return examinationRepository.findBySemesterId(semesterId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public ExaminationResponse update(Long id, ExaminationRequest request) {
        Examination examination = examinationRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Examination not found with id: " + id));
        Course course = courseRepository.findById(request.courseId())
            .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + request.courseId()));
        examination.setName(request.name());
        examination.setCourse(course);
        examination.setExamType(request.examType());
        examination.setDate(request.date());
        examination.setDuration(request.duration());
        examination.setMaxMarks(request.maxMarks());
        if (request.semesterId() != null) {
            Semester semester = semesterRepository.findById(request.semesterId())
                .orElseThrow(() -> new ResourceNotFoundException("Semester not found with id: " + request.semesterId()));
            examination.setSemester(semester);
        }
        Examination updated = examinationRepository.save(examination);
        return toResponse(updated);
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
            examination.getCourse().getId(),
            examination.getCourse().getName(),
            examination.getExamType(),
            examination.getDate(),
            examination.getDuration(),
            examination.getMaxMarks(),
            examination.getSemester() != null ? examination.getSemester().getId() : null,
            examination.getSemester() != null ? examination.getSemester().getName() : null,
            examination.getCreatedAt(),
            examination.getUpdatedAt()
        );
    }
}
