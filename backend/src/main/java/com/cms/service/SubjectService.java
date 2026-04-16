package com.cms.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.CourseResponse;
import com.cms.dto.DepartmentResponse;
import com.cms.dto.ProgramResponse;
import com.cms.dto.SubjectRequest;
import com.cms.dto.SubjectResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Course;
import com.cms.model.Department;
import com.cms.model.Program;
import com.cms.model.Subject;
import com.cms.repository.CourseRepository;
import com.cms.repository.DepartmentRepository;
import com.cms.repository.SubjectRepository;

@Service
@Transactional(readOnly = true)
public class SubjectService {

    private final SubjectRepository subjectRepository;
    private final CourseRepository courseRepository;
    private final DepartmentRepository departmentRepository;

    public SubjectService(SubjectRepository subjectRepository, CourseRepository courseRepository,
                          DepartmentRepository departmentRepository) {
        this.subjectRepository = subjectRepository;
        this.courseRepository = courseRepository;
        this.departmentRepository = departmentRepository;
    }

    @Transactional
    public SubjectResponse create(SubjectRequest request) {
        Course course = courseRepository.findById(request.courseId())
            .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + request.courseId()));

        Department department = null;
        if (request.departmentId() != null) {
            department = departmentRepository.findById(request.departmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + request.departmentId()));
        }

        Subject subject = new Subject(
            request.name(),
            request.code(),
            request.credits(),
            request.theoryCredits(),
            request.labCredits(),
            course,
            department,
            request.semester()
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

    public List<SubjectResponse> findByDepartmentId(Long departmentId) {
        if (!departmentRepository.existsById(departmentId)) {
            throw new ResourceNotFoundException("Department not found with id: " + departmentId);
        }
        return subjectRepository.findByDepartmentId(departmentId).stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public SubjectResponse update(Long id, SubjectRequest request) {
        Subject subject = subjectRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Subject not found with id: " + id));

        Course course = courseRepository.findById(request.courseId())
            .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + request.courseId()));

        Department department = null;
        if (request.departmentId() != null) {
            department = departmentRepository.findById(request.departmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + request.departmentId()));
        }

        subject.setName(request.name());
        subject.setCode(request.code());
        subject.setCredits(request.credits());
        subject.setTheoryCredits(request.theoryCredits());
        subject.setLabCredits(request.labCredits());
        subject.setCourse(course);
        subject.setDepartment(department);
        subject.setSemester(request.semester());

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

        List<DepartmentResponse> programDeptResponses = program.getDepartments().stream()
            .map(dept -> new DepartmentResponse(
                dept.getId(), dept.getName(), dept.getCode(),
                dept.getDescription(), dept.getHodName(),
                dept.getCreatedAt(), dept.getUpdatedAt()
            ))
            .toList();

        ProgramResponse programResponse = new ProgramResponse(
            program.getId(), program.getName(), program.getCode(),
            program.getProgramLevel(), program.getDurationYears(), programDeptResponses,
            program.getCreatedAt(), program.getUpdatedAt()
        );

        CourseResponse courseResponse = new CourseResponse(
            course.getId(), course.getName(), course.getCode(),
            course.getSpecialization(),
            programResponse,
            course.getCreatedAt(), course.getUpdatedAt()
        );

        DepartmentResponse departmentResponse = null;
        Department dept = subject.getDepartment();
        if (dept != null) {
            departmentResponse = new DepartmentResponse(
                dept.getId(), dept.getName(), dept.getCode(),
                dept.getDescription(), dept.getHodName(),
                dept.getCreatedAt(), dept.getUpdatedAt()
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
            departmentResponse,
            subject.getSemester(),
            subject.getCreatedAt(),
            subject.getUpdatedAt()
        );
    }
}
