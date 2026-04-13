package com.cms.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.SyllabusRequest;
import com.cms.dto.SyllabusResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Course;
import com.cms.model.Syllabus;
import com.cms.repository.CourseRepository;
import com.cms.repository.SyllabusRepository;

@Service
@Transactional(readOnly = true)
public class SyllabusService {

    private final SyllabusRepository syllabusRepository;
    private final CourseRepository courseRepository;

    public SyllabusService(SyllabusRepository syllabusRepository, CourseRepository courseRepository) {
        this.syllabusRepository = syllabusRepository;
        this.courseRepository = courseRepository;
    }

    @Transactional
    public SyllabusResponse create(SyllabusRequest request) {
        Course course = courseRepository.findById(request.courseId())
            .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + request.courseId()));

        Boolean isActive = request.isActive() != null ? request.isActive() : false;

        Syllabus syllabus = new Syllabus(
            course,
            request.version(),
            request.theoryHours(),
            request.labHours(),
            request.tutorialHours(),
            request.objectives(),
            request.content(),
            request.textBooks(),
            request.referenceBooks(),
            request.courseOutcomes(),
            isActive
        );

        Syllabus saved = syllabusRepository.save(syllabus);
        return toResponse(saved);
    }

    public List<SyllabusResponse> findAll() {
        return syllabusRepository.findAll().stream()
            .map(this::toResponse)
            .toList();
    }

    public SyllabusResponse findById(Long id) {
        Syllabus syllabus = syllabusRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Syllabus not found with id: " + id));
        return toResponse(syllabus);
    }

    public List<SyllabusResponse> findByCourseId(Long courseId) {
        if (!courseRepository.existsById(courseId)) {
            throw new ResourceNotFoundException("Course not found with id: " + courseId);
        }
        return syllabusRepository.findByCourseId(courseId).stream()
            .map(this::toResponse)
            .toList();
    }

    public SyllabusResponse findActiveByCourseId(Long courseId) {
        return syllabusRepository.findByCourseIdAndIsActiveTrue(courseId)
            .map(this::toResponse)
            .orElseThrow(() -> new ResourceNotFoundException("No active syllabus found for course id: " + courseId));
    }

    @Transactional
    public SyllabusResponse update(Long id, SyllabusRequest request) {
        Syllabus syllabus = syllabusRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Syllabus not found with id: " + id));

        Course course = courseRepository.findById(request.courseId())
            .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + request.courseId()));

        syllabus.setCourse(course);
        syllabus.setVersion(request.version());
        syllabus.setTheoryHours(request.theoryHours());
        syllabus.setLabHours(request.labHours());
        syllabus.setTutorialHours(request.tutorialHours());
        syllabus.setObjectives(request.objectives());
        syllabus.setContent(request.content());
        syllabus.setTextBooks(request.textBooks());
        syllabus.setReferenceBooks(request.referenceBooks());
        syllabus.setCourseOutcomes(request.courseOutcomes());

        if (request.isActive() != null) {
            syllabus.setIsActive(request.isActive());
        }

        Syllabus updated = syllabusRepository.save(syllabus);
        return toResponse(updated);
    }

    @Transactional
    public void delete(Long id) {
        if (!syllabusRepository.existsById(id)) {
            throw new ResourceNotFoundException("Syllabus not found with id: " + id);
        }
        syllabusRepository.deleteById(id);
    }

    private SyllabusResponse toResponse(Syllabus syllabus) {
        return new SyllabusResponse(
            syllabus.getId(),
            syllabus.getCourse().getId(),
            syllabus.getCourse().getName(),
            syllabus.getCourse().getCode(),
            syllabus.getVersion(),
            syllabus.getTheoryHours(),
            syllabus.getLabHours(),
            syllabus.getTutorialHours(),
            syllabus.getObjectives(),
            syllabus.getContent(),
            syllabus.getTextBooks(),
            syllabus.getReferenceBooks(),
            syllabus.getCourseOutcomes(),
            syllabus.getIsActive(),
            syllabus.getCreatedAt(),
            syllabus.getUpdatedAt()
        );
    }
}
