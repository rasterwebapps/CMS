package com.cms.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.CourseResponse;
import com.cms.dto.SpecialityResponse;
import com.cms.dto.ProgramResponse;
import com.cms.dto.SubjectRequest;
import com.cms.dto.SubjectResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Course;
import com.cms.model.Speciality;
import com.cms.model.Program;
import com.cms.model.Subject;
import com.cms.repository.CourseRepository;
import com.cms.repository.SpecialityRepository;
import com.cms.repository.SubjectRepository;

@Service
@Transactional(readOnly = true)
public class SubjectService {

    private final SubjectRepository subjectRepository;
    private final CourseRepository courseRepository;
    private final SpecialityRepository specialityRepository;
    private final ProgramService programService;

    public SubjectService(SubjectRepository subjectRepository, CourseRepository courseRepository,
                          SpecialityRepository specialityRepository,
                          ProgramService programService) {
        this.subjectRepository = subjectRepository;
        this.courseRepository = courseRepository;
        this.specialityRepository = specialityRepository;
        this.programService = programService;
    }

    @Transactional
    public SubjectResponse create(SubjectRequest request) {
        Course course = courseRepository.findById(request.courseId())
            .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + request.courseId()));

        Speciality speciality = null;
        if (request.specialityId() != null) {
            speciality = specialityRepository.findById(request.specialityId())
                .orElseThrow(() -> new ResourceNotFoundException("Speciality not found with id: " + request.specialityId()));
        }

        Subject subject = new Subject(
            request.name(),
            request.code(),
            request.credits(),
            request.theoryCredits(),
            request.labCredits(),
            course,
            speciality,
            request.termNumber()
        );
        Subject saved = subjectRepository.save(subject);
        return toResponse(saved);
    }

    public List<SubjectResponse> findAll() {
        return subjectRepository.findAll().stream()
            .map(this::toResponse)
            .toList();
    }

    public SubjectResponse findById(Long id) {
        Subject subject = subjectRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Subject not found with id: " + id));
        return toResponse(subject);
    }

    public List<SubjectResponse> findByCourseId(Long courseId) {
        if (!courseRepository.existsById(courseId)) {
            throw new ResourceNotFoundException("Course not found with id: " + courseId);
        }
        return subjectRepository.findByCourseId(courseId).stream()
            .map(this::toResponse)
            .toList();
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

        Course course = courseRepository.findById(request.courseId())
            .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + request.courseId()));

        Speciality speciality = null;
        if (request.specialityId() != null) {
            speciality = specialityRepository.findById(request.specialityId())
                .orElseThrow(() -> new ResourceNotFoundException("Speciality not found with id: " + request.specialityId()));
        }

        if (subjectRepository.existsByNameAndIdNot(request.name(), id)) {
            throw new IllegalArgumentException(
                "A subject with the name '" + request.name() + "' already exists");
        }
        if (subjectRepository.existsByCodeAndIdNot(request.code(), id)) {
            throw new IllegalArgumentException(
                "A subject with the code '" + request.code() + "' already exists");
        }

        subject.setName(request.name());
        subject.setCode(request.code());
        subject.setCredits(request.credits());
        subject.setTheoryCredits(request.theoryCredits());
        subject.setLabCredits(request.labCredits());
        subject.setCourse(course);
        subject.setSpeciality(speciality);
        subject.setSemester(request.termNumber());

        Subject updated = subjectRepository.save(subject);
        return toResponse(updated);
    }

    @Transactional
    public void delete(Long id) {
        if (!subjectRepository.existsById(id)) {
            throw new ResourceNotFoundException("Subject not found with id: " + id);
        }
        subjectRepository.deleteById(id);
    }

    private SubjectResponse toResponse(Subject subject) {
        Course course = subject.getCourse();
        Program program = course.getProgram();

        ProgramResponse programResponse = programService.toResponse(program);

        CourseResponse courseResponse = new CourseResponse(
            course.getId(), course.getName(), course.getCode(),
            course.getSpecialization(),
            course.getRollNumberCode(),
            course.getIsActive(),
            programResponse,
            course.getCreatedAt(), course.getUpdatedAt()
        );

        SpecialityResponse specialityResponse = null;
        Speciality speciality = subject.getSpeciality();
        if (speciality != null) {
            specialityResponse = new SpecialityResponse(
                speciality.getId(), speciality.getName(), speciality.getCode(),
                speciality.getDescription(), speciality.getHodFacultyId(), speciality.getHodName(),
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
            courseResponse,
            specialityResponse,
            subject.getTermNumber(),
            subject.getCreatedAt(),
            subject.getUpdatedAt()
        );
    }
}
