package com.cms.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.StudentTermEnrollmentDto;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Cohort;
import com.cms.model.Student;
import com.cms.model.StudentTermEnrollment;
import com.cms.model.TermInstance;
import com.cms.model.enums.CohortStatus;
import com.cms.model.enums.EnrollmentStatus;
import com.cms.model.enums.StudentStatus;
import com.cms.model.enums.TermType;
import com.cms.repository.CohortRepository;
import com.cms.repository.StudentRepository;
import com.cms.repository.StudentTermEnrollmentRepository;
import com.cms.repository.TermInstanceRepository;

@Service
@Transactional(readOnly = true)
public class StudentTermEnrollmentServiceImpl implements StudentTermEnrollmentService {

    private final StudentTermEnrollmentRepository enrollmentRepository;
    private final TermInstanceRepository termInstanceRepository;
    private final CohortRepository cohortRepository;
    private final StudentRepository studentRepository;

    public StudentTermEnrollmentServiceImpl(StudentTermEnrollmentRepository enrollmentRepository,
                                             TermInstanceRepository termInstanceRepository,
                                             CohortRepository cohortRepository,
                                             StudentRepository studentRepository) {
        this.enrollmentRepository = enrollmentRepository;
        this.termInstanceRepository = termInstanceRepository;
        this.cohortRepository = cohortRepository;
        this.studentRepository = studentRepository;
    }

    @Override
    @Transactional
    public int generateEnrollmentsForTermInstance(Long termInstanceId) {
        TermInstance termInstance = termInstanceRepository.findById(termInstanceId)
            .orElseThrow(() -> new ResourceNotFoundException("Term instance not found with id: " + termInstanceId));

        List<Cohort> activeCohorts = cohortRepository.findByStatus(CohortStatus.ACTIVE);
        int count = 0;

        for (Cohort cohort : activeCohorts) {
            Integer semesterNumber = computeSemesterNumber(cohort, termInstance);
            if (semesterNumber == null) {
                continue;
            }
            int yearOfStudy = (int) Math.ceil(semesterNumber / 2.0);

            List<Student> students = studentRepository.findByCohortIdAndStatus(cohort.getId(), StudentStatus.ACTIVE);
            for (Student student : students) {
                Optional<StudentTermEnrollment> existing =
                    enrollmentRepository.findByStudentIdAndTermInstanceId(student.getId(), termInstanceId);
                if (existing.isEmpty()) {
                    StudentTermEnrollment enrollment = new StudentTermEnrollment();
                    enrollment.setStudent(student);
                    enrollment.setTermInstance(termInstance);
                    enrollment.setCohort(cohort);
                    enrollment.setSemesterNumber(semesterNumber);
                    enrollment.setYearOfStudy(yearOfStudy);
                    enrollment.setStatus(EnrollmentStatus.ENROLLED);
                    enrollmentRepository.save(enrollment);
                    count++;
                }
            }
        }
        return count;
    }

    @Override
    public Integer computeSemesterNumber(Cohort cohort, TermInstance termInstance) {
        int admissionStartYear = cohort.getAdmissionAcademicYear().getStartYear();
        int currentStartYear = termInstance.getAcademicYear().getStartYear();
        int k = currentStartYear - admissionStartYear;

        int semesterNumber;
        if (termInstance.getTermType() == TermType.ODD) {
            semesterNumber = (2 * k) + 1;
        } else {
            semesterNumber = (2 * k) + 2;
        }

        Integer totalSemesters = cohort.getProgram().getTotalSemesters();
        if (semesterNumber < 1 || totalSemesters == null || semesterNumber > totalSemesters) {
            return null;
        }
        return semesterNumber;
    }

    @Override
    public List<StudentTermEnrollmentDto> getEnrollmentsByTermInstance(Long termInstanceId) {
        return enrollmentRepository.findByTermInstanceId(termInstanceId)
            .stream()
            .map(this::toDto)
            .toList();
    }

    @Override
    public List<StudentTermEnrollmentDto> getEnrollmentsByStudent(Long studentId) {
        return enrollmentRepository.findByStudentId(studentId)
            .stream()
            .map(this::toDto)
            .toList();
    }

    @Override
    public List<StudentTermEnrollmentDto> getEnrollmentsByTermInstanceAndSemester(Long termInstanceId,
                                                                                   Integer semesterNumber) {
        return enrollmentRepository.findByTermInstanceIdAndSemesterNumber(termInstanceId, semesterNumber)
            .stream()
            .map(this::toDto)
            .toList();
    }

    @Override
    public StudentTermEnrollmentDto getById(Long id) {
        StudentTermEnrollment enrollment = enrollmentRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found with id: " + id));
        return toDto(enrollment);
    }

    private StudentTermEnrollmentDto toDto(StudentTermEnrollment e) {
        String termInstanceLabel = e.getTermInstance().getAcademicYear().getName()
            + " " + e.getTermInstance().getTermType();
        return new StudentTermEnrollmentDto(
            e.getId(),
            e.getStudent().getId(),
            e.getStudent().getFullName(),
            e.getCohort().getId(),
            e.getCohort().getCohortCode(),
            e.getTermInstance().getId(),
            termInstanceLabel,
            e.getSemesterNumber(),
            e.getYearOfStudy(),
            e.getStatus()
        );
    }
}
