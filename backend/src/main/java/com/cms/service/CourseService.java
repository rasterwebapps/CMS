package com.cms.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.CourseRequest;
import com.cms.dto.CourseResponse;
import com.cms.dto.DepartmentResponse;
import com.cms.dto.ProgramResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Course;
import com.cms.model.Program;
import com.cms.repository.CourseRepository;
import com.cms.repository.ProgramRepository;

@Service
@Transactional(readOnly = true)
public class CourseService {

    private final CourseRepository courseRepository;
    private final ProgramRepository programRepository;
    private final ProgramService programService;

    public CourseService(CourseRepository courseRepository,
                         ProgramRepository programRepository,
                         ProgramService programService) {
        this.courseRepository = courseRepository;
        this.programRepository = programRepository;
        this.programService = programService;
    }

    @Transactional
    public CourseResponse create(CourseRequest request) {
        Program program = programRepository.findById(request.programId())
            .orElseThrow(() -> new ResourceNotFoundException("Program not found with id: " + request.programId()));

        Course course = new Course(
            request.name(),
            request.code(),
            request.specialization(),
            program
        );
        Course saved = courseRepository.save(course);
        return toResponse(saved);
    }

    public List<CourseResponse> findAll() {
        return courseRepository.findAll().stream()
            .map(this::toResponse)
            .toList();
    }

    public CourseResponse findById(Long id) {
        Course course = courseRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + id));
        return toResponse(course);
    }

    public List<CourseResponse> findByProgramId(Long programId) {
        if (!programRepository.existsById(programId)) {
            throw new ResourceNotFoundException("Program not found with id: " + programId);
        }
        return courseRepository.findByProgramId(programId).stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public CourseResponse update(Long id, CourseRequest request) {
        Course course = courseRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + id));

        Program program = programRepository.findById(request.programId())
            .orElseThrow(() -> new ResourceNotFoundException("Program not found with id: " + request.programId()));

        if (courseRepository.existsByNameAndIdNot(request.name(), id)) {
            throw new IllegalArgumentException(
                "A course with the name '" + request.name() + "' already exists");
        }
        if (courseRepository.existsByCodeAndIdNot(request.code(), id)) {
            throw new IllegalArgumentException(
                "A course with the code '" + request.code() + "' already exists");
        }

        course.setName(request.name());
        course.setCode(request.code());
        course.setSpecialization(request.specialization());
        course.setProgram(program);

        Course updated = courseRepository.save(course);
        return toResponse(updated);
    }

    @Transactional
    public void delete(Long id) {
        if (!courseRepository.existsById(id)) {
            throw new ResourceNotFoundException("Course not found with id: " + id);
        }
        courseRepository.deleteById(id);
    }

    private CourseResponse toResponse(Course course) {
        Program program = course.getProgram();
        ProgramResponse programResponse = programService.toResponse(program);

        return new CourseResponse(
            course.getId(),
            course.getName(),
            course.getCode(),
            course.getSpecialization(),
            programResponse,
            course.getCreatedAt(),
            course.getUpdatedAt()
        );
    }
}
