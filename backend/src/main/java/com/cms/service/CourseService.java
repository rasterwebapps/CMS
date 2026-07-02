package com.cms.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.CourseRequest;
import com.cms.dto.CourseResponse;
import com.cms.dto.CourseStatusUpdateRequest;
import com.cms.dto.CourseStatusUpdateResponse;
import com.cms.dto.ProgramResponse;
import com.cms.exception.LifecycleConflictException;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Course;
import com.cms.model.Program;
import com.cms.model.enums.ProgramStatus;
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
        assertProgramActiveForCourse(program, null, true);
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
        course.setIsActive(request.isActive() != null ? request.isActive() : true);
        Course saved = courseRepository.save(course);
        return toResponse(saved);
    }

    public List<CourseResponse> findAll() {
        return courseRepository.findAll().stream()
            .map(this::toResponse)
            .toList();
    }

    public Page<CourseResponse> findPage(String search, Long programId, Pageable pageable) {
        Specification<Course> spec = Specification.where(null);
        if (search != null && !search.isBlank()) {
            String pattern = "%" + search.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("name")), pattern),
                cb.like(cb.lower(root.get("code")), pattern),
                cb.like(cb.lower(root.get("specialization")), pattern)
            ));
        }
        if (programId != null) {
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("program").get("id"), programId));
        }
        return courseRepository.findAll(spec, pageable).map(this::toResponse);
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
        boolean programChanged = course.getProgram() == null || !course.getProgram().getId().equals(program.getId());
        assertProgramActiveForCourse(program, course.getId(), programChanged);
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
        if (request.isActive() != null) {
            if (Boolean.TRUE.equals(request.isActive()) && program.getStatus() == ProgramStatus.INACTIVE) {
                throw new LifecycleConflictException(
                    "Cannot activate Course while parent Program is inactive.",
                    "ANCESTOR_INACTIVE",
                    "Course",
                    id,
                    null
                );
            }
            course.setIsActive(request.isActive());
        }
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

    @Transactional
    public CourseStatusUpdateResponse updateStatus(Long id, CourseStatusUpdateRequest request) {
        Course course = courseRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + id));

        if (Boolean.FALSE.equals(request.isActive())
            && feeStructureGroupRepository.existsByCourseIdAndIsActiveTrue(id)) {
            throw new LifecycleConflictException(
                "Cannot deactivate Course: active Fee Structures exist.",
                "ACTIVE_REFERENCE_EXISTS",
                "Course",
                id,
                null
            );
        }

        if (Boolean.TRUE.equals(request.isActive())
            && course.getProgram() != null
            && course.getProgram().getStatus() == ProgramStatus.INACTIVE) {
            throw new LifecycleConflictException(
                "Cannot activate Course while parent Program is inactive.",
                "ANCESTOR_INACTIVE",
                "Course",
                id,
                null
            );
        }

        course.setIsActive(request.isActive());
        Course saved = courseRepository.save(course);
        return new CourseStatusUpdateResponse(saved.getId(), saved.getIsActive(), saved.getUpdatedAt());
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
            course.getIsActive(),
            programResponse,
            course.getCreatedAt(),
            course.getUpdatedAt()
        );
    }

    private void assertProgramActiveForCourse(Program program, Long courseId, boolean failWhenInactive) {
        if (!failWhenInactive || program.getStatus() == ProgramStatus.ACTIVE) {
            return;
        }
        throw new LifecycleConflictException(
            "Cannot assign Course to an inactive Program.",
            "ANCESTOR_INACTIVE",
            "Course",
            courseId,
            null
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
