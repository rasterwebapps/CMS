package com.cms.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.CourseRequest;
import com.cms.dto.CourseResponse;
import com.cms.dto.ProgramResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Course;
import com.cms.model.Program;
import com.cms.repository.CourseRepository;
import com.cms.repository.FeeStructureGroupRepository;
import com.cms.repository.ProgramRepository;

@Service
@Transactional(readOnly = true)
public class CourseService {

    private final CourseRepository courseRepository;
    private final ProgramRepository programRepository;
    private final ProgramService programService;
    private final FeeStructureGroupRepository feeStructureGroupRepository;

    public CourseService(CourseRepository courseRepository,
                         ProgramRepository programRepository,
                         ProgramService programService,
                         FeeStructureGroupRepository feeStructureGroupRepository) {
        this.courseRepository = courseRepository;
        this.programRepository = programRepository;
        this.programService = programService;
        this.feeStructureGroupRepository = feeStructureGroupRepository;
    }

    @Transactional
    public CourseResponse create(CourseRequest request) {
        Program program = programRepository.findById(request.programId())
            .orElseThrow(() -> new ResourceNotFoundException("Program not found with id: " + request.programId()));
        String name = requireTrimmed(request.name(), "Course name is required");
        String code = requireTrimmed(request.code(), "Course code is required");

        if (courseRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException(
                "A course with the name '" + name + "' already exists");
        }
        if (courseRepository.existsByCodeIgnoreCase(code)) {
            throw new IllegalArgumentException(
                "A course with the code '" + code + "' already exists");
        }

        Course course = new Course(
            name,
            code,
            trim(request.specialization()),
            program
        );
        course.setRollNumberCode(trim(request.rollNumberCode()));
        course.setAdmissionNumberCode(trim(request.admissionNumberCode()));
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
        String name = requireTrimmed(request.name(), "Course name is required");
        String code = requireTrimmed(request.code(), "Course code is required");

        if (courseRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new IllegalArgumentException(
                "A course with the name '" + name + "' already exists");
        }
        if (courseRepository.existsByCodeIgnoreCaseAndIdNot(code, id)) {
            throw new IllegalArgumentException(
                "A course with the code '" + code + "' already exists");
        }

        course.setName(name);
        course.setCode(code);
        course.setSpecialization(trim(request.specialization()));
        course.setRollNumberCode(trim(request.rollNumberCode()));
        course.setAdmissionNumberCode(trim(request.admissionNumberCode()));
        course.setProgram(program);

        Course updated = courseRepository.save(course);
        return toResponse(updated);
    }

    @Transactional
    public void delete(Long id) {
        if (!courseRepository.existsById(id)) {
            throw new ResourceNotFoundException("Course not found with id: " + id);
        }
        if (feeStructureGroupRepository.existsByCourseId(id)) {
            throw new IllegalStateException(
                "Cannot delete course because fee structures are associated with it.");
        }
        courseRepository.deleteById(id);
    }

    public boolean nameExists(String name, Long excludeId) {
        String trimmed = name == null ? "" : name.trim();
        if (excludeId != null) {
            return courseRepository.existsByNameIgnoreCaseAndIdNot(trimmed, excludeId);
        }
        return courseRepository.existsByNameIgnoreCase(trimmed);
    }

    public boolean codeExists(String code, Long excludeId) {
        String trimmed = code == null ? "" : code.trim();
        if (excludeId != null) {
            return courseRepository.existsByCodeIgnoreCaseAndIdNot(trimmed, excludeId);
        }
        return courseRepository.existsByCodeIgnoreCase(trimmed);
    }

    private CourseResponse toResponse(Course course) {
        Program program = course.getProgram();
        ProgramResponse programResponse = programService.toResponse(program);

        return new CourseResponse(
            course.getId(),
            course.getName(),
            course.getCode(),
            course.getSpecialization(),
            course.getRollNumberCode(),
            course.getAdmissionNumberCode(),
            programResponse,
            course.getCreatedAt(),
            course.getUpdatedAt()
        );
    }

    private static String trim(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static String requireTrimmed(String s, String message) {
        String t = trim(s);
        if (t == null) {
            throw new IllegalArgumentException(message);
        }
        return t;
    }
}
