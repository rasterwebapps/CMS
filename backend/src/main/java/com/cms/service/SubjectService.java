package com.cms.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.ActiveStatusUpdateRequest;
import com.cms.dto.ActiveStatusUpdateResponse;
import com.cms.dto.SpecialityResponse;
import com.cms.dto.SubjectRequest;
import com.cms.dto.SubjectResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Course;
import com.cms.model.Speciality;
import com.cms.model.Subject;
import com.cms.repository.CourseOfferingRepository;
import com.cms.repository.CourseRepository;
import com.cms.repository.CurriculumSemesterCourseRepository;
import com.cms.repository.SpecialityRepository;
import com.cms.repository.SubjectRepository;

@Service
@Transactional(readOnly = true)
public class SubjectService {

    private final SubjectRepository subjectRepository;
    private final CourseRepository courseRepository;
    private final SpecialityRepository specialityRepository;
    private final CurriculumSemesterCourseRepository curriculumSemesterCourseRepository;
    private final CourseOfferingRepository courseOfferingRepository;

    public SubjectService(SubjectRepository subjectRepository, CourseRepository courseRepository,
                          SpecialityRepository specialityRepository,
                          CurriculumSemesterCourseRepository curriculumSemesterCourseRepository,
                          CourseOfferingRepository courseOfferingRepository) {
        this.subjectRepository = subjectRepository;
        this.courseRepository = courseRepository;
        this.specialityRepository = specialityRepository;
        this.curriculumSemesterCourseRepository = curriculumSemesterCourseRepository;
        this.courseOfferingRepository = courseOfferingRepository;
    }

    @Transactional
    public SubjectResponse create(SubjectRequest request) {
        Speciality speciality = null;
        if (request.specialityId() != null) {
            speciality = specialityRepository.findById(request.specialityId())
                .orElseThrow(() -> new ResourceNotFoundException("Speciality not found with id: " + request.specialityId()));
        }

        if (subjectRepository.existsByNameIgnoreCase(request.name())) {
            throw new IllegalArgumentException(
                "A subject with the name '" + request.name() + "' already exists");
        }
        if (subjectRepository.existsByCodeIgnoreCase(request.code())) {
            throw new IllegalArgumentException(
                "A subject with the code '" + request.code() + "' already exists");
        }

        Subject subject = new Subject(
            request.name(),
            request.code(),
            request.credits(),
            request.theoryCredits(),
            request.labCredits(),
            speciality,
            request.termNumber()
        );
        if (request.isActive() != null) {
            subject.setIsActive(request.isActive());
        }
        Subject saved = subjectRepository.save(subject);
        return toResponse(saved);
    }

    public List<SubjectResponse> findAll(boolean activeOnly) {
        List<Subject> subjects = activeOnly
            ? subjectRepository.findByIsActiveTrueOrderByNameAsc()
            : subjectRepository.findAllByOrderByNameAsc();
        return subjects.stream()
            .map(this::toResponse)
            .toList();
    }

    /**
     * courseId filters to subjects actually mapped into a curriculum term row that applies to
     * that course (see {@link CurriculumSemesterCourseRepository#findDistinctSubjectIdsByCourseId}),
     * since a subject is no longer owned by a single course and may be shared across programs.
     */
    public Page<SubjectResponse> findPage(String search, Long courseId, Pageable pageable) {
        Specification<Subject> spec = (root, query, cb) -> cb.conjunction();
        if (search != null && !search.isBlank()) {
            String pattern = "%" + search.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("name")), pattern),
                cb.like(cb.lower(root.get("code")), pattern)
            ));
        }
        if (courseId != null) {
            List<Long> subjectIds = subjectIdsUsedByCourse(courseId);
            spec = spec.and((root, query, cb) -> root.get("id").in(subjectIds));
        }
        return subjectRepository.findAll(spec, pageable).map(this::toResponse);
    }

    public SubjectResponse findById(Long id) {
        Subject subject = subjectRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Subject not found with id: " + id));
        return toResponse(subject);
    }

    /** Subjects mapped into any curriculum term row that applies to the given course. */
    public List<SubjectResponse> findByCourseId(Long courseId) {
        List<Long> subjectIds = subjectIdsUsedByCourse(courseId);
        return subjectRepository.findAllById(subjectIds).stream()
            .map(this::toResponse)
            .toList();
    }

    private List<Long> subjectIdsUsedByCourse(Long courseId) {
        Course course = courseRepository.findById(courseId)
            .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));
        return curriculumSemesterCourseRepository
            .findDistinctSubjectIdsByCourseId(courseId, course.getProgram().getId());
    }

    public List<SubjectResponse> findBySpecialityId(Long specialityId) {
        if (!specialityRepository.existsById(specialityId)) {
            throw new ResourceNotFoundException("Speciality not found with id: " + specialityId);
        }
        return subjectRepository.findBySpecialityId(specialityId).stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public SubjectResponse update(Long id, SubjectRequest request) {
        Subject subject = subjectRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Subject not found with id: " + id));

        Speciality speciality = null;
        if (request.specialityId() != null) {
            speciality = specialityRepository.findById(request.specialityId())
                .orElseThrow(() -> new ResourceNotFoundException("Speciality not found with id: " + request.specialityId()));
        }

        if (subjectRepository.existsByNameIgnoreCaseAndIdNot(request.name(), id)) {
            throw new IllegalArgumentException(
                "A subject with the name '" + request.name() + "' already exists");
        }
        if (subjectRepository.existsByCodeIgnoreCaseAndIdNot(request.code(), id)) {
            throw new IllegalArgumentException(
                "A subject with the code '" + request.code() + "' already exists");
        }

        subject.setName(request.name());
        subject.setCode(request.code());
        subject.setCredits(request.credits());
        subject.setTheoryCredits(request.theoryCredits());
        subject.setLabCredits(request.labCredits());
        subject.setSpeciality(speciality);
        subject.setSemester(request.termNumber());
        if (request.isActive() != null) {
            subject.setIsActive(request.isActive());
        }

        Subject updated = subjectRepository.save(subject);
        return toResponse(updated);
    }

    @Transactional
    public void delete(Long id) {
        if (!subjectRepository.existsById(id)) {
            throw new ResourceNotFoundException("Subject not found with id: " + id);
        }
        if (curriculumSemesterCourseRepository.existsBySubjectId(id)) {
            throw new IllegalStateException(
                "Cannot delete subject because it is mapped into one or more curriculum versions.");
        }
        if (courseOfferingRepository.existsBySubjectId(id)) {
            throw new IllegalStateException(
                "Cannot delete subject because course offerings exist for it.");
        }
        subjectRepository.deleteById(id);
    }

    @Transactional
    public ActiveStatusUpdateResponse updateStatus(Long id, ActiveStatusUpdateRequest request) {
        Subject subject = subjectRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Subject not found with id: " + id));
        subject.setIsActive(Boolean.TRUE.equals(request.isActive()));
        Subject saved = subjectRepository.save(subject);
        return new ActiveStatusUpdateResponse(saved.getId(), saved.getIsActive(), saved.getUpdatedAt());
    }

    public boolean nameExists(String name, Long excludeId) {
        String trimmed = name == null ? "" : name.trim();
        if (excludeId != null) {
            return subjectRepository.existsByNameIgnoreCaseAndIdNot(trimmed, excludeId);
        }
        return subjectRepository.existsByNameIgnoreCase(trimmed);
    }

    public boolean codeExists(String code, Long excludeId) {
        String trimmed = code == null ? "" : code.trim();
        if (excludeId != null) {
            return subjectRepository.existsByCodeIgnoreCaseAndIdNot(trimmed, excludeId);
        }
        return subjectRepository.existsByCodeIgnoreCase(trimmed);
    }

    private SubjectResponse toResponse(Subject subject) {
        SpecialityResponse specialityResponse = null;
        Speciality speciality = subject.getSpeciality();
        if (speciality != null) {
            specialityResponse = new SpecialityResponse(
                speciality.getId(), speciality.getName(), speciality.getCode(),
                speciality.getDescription(), speciality.getHodFacultyId(), speciality.getHodName(),
                speciality.getIsActive(),
                speciality.getCreatedAt(), speciality.getUpdatedAt()
            );
        }

        return new SubjectResponse(
            subject.getId(),
            subject.getName(),
            subject.getCode(),
            subject.getCredits(),
            subject.getTheoryCredits(),
            subject.getLabCredits(),
            specialityResponse,
            subject.getTermNumber(),
            subject.getIsActive(),
            subject.getCreatedAt(),
            subject.getUpdatedAt()
        );
    }
}
