package com.cms.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.cms.dto.AdmissionConfirmationDto;
import com.cms.dto.AdmissionRequest;
import com.cms.dto.AdmissionResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Admission;
import com.cms.model.AcademicYear;
import com.cms.model.Cohort;
import com.cms.model.Course;
import com.cms.model.IntakeRule;
import com.cms.model.Program;
import com.cms.model.Student;
import com.cms.model.TermInstance;
import com.cms.model.enums.CohortStatus;
import com.cms.model.enums.TermType;
import com.cms.repository.AcademicYearRepository;
import com.cms.repository.AdmissionRepository;
import com.cms.repository.CohortRepository;
import com.cms.repository.IntakeRuleRepository;
import com.cms.repository.StudentRepository;
import com.cms.repository.TermInstanceRepository;

@Service
@Transactional(readOnly = true)
public class AdmissionService {

    private final AdmissionRepository admissionRepository;
    private final StudentRepository studentRepository;
    private final CohortRepository cohortRepository;
    private final IntakeRuleRepository intakeRuleRepository;
    private final AcademicYearRepository academicYearRepository;
    private final TermInstanceRepository termInstanceRepository;
    private final ApplicationNumberSequenceService numberSequenceService;

    public AdmissionService(AdmissionRepository admissionRepository,
                            StudentRepository studentRepository,
                            CohortRepository cohortRepository,
                            IntakeRuleRepository intakeRuleRepository,
                            AcademicYearRepository academicYearRepository,
                            TermInstanceRepository termInstanceRepository,
                            ApplicationNumberSequenceService numberSequenceService) {
        this.admissionRepository = admissionRepository;
        this.studentRepository = studentRepository;
        this.cohortRepository = cohortRepository;
        this.intakeRuleRepository = intakeRuleRepository;
        this.academicYearRepository = academicYearRepository;
        this.termInstanceRepository = termInstanceRepository;
        this.numberSequenceService = numberSequenceService;
    }

    @Transactional
    public AdmissionResponse create(AdmissionRequest request) {
        Student student = studentRepository.findById(request.studentId())
            .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + request.studentId()));
        AcademicYear joiningYear = academicYearRepository.findById(request.joiningAcademicYearId())
            .orElseThrow(() -> new ResourceNotFoundException("Academic year not found: " + request.joiningAcademicYearId()));
        Admission admission = new Admission(student, joiningYear, request.applicationDate());
        admission.setDeclarationPlace(request.declarationPlace());
        admission.setDeclarationDate(request.declarationDate());
        admission.setParentConsentGiven(request.parentConsentGiven());
        admission.setApplicantConsentGiven(request.applicantConsentGiven());
        Admission saved = admissionRepository.save(admission);
        return toResponse(saved);
    }

    public List<AdmissionResponse> findAll() {
        return admissionRepository.findAll().stream()
            .map(this::toResponse)
            .toList();
    }

    public AdmissionResponse findById(Long id) {
        Admission admission = admissionRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Admission not found with id: " + id));
        return toResponse(admission);
    }

    public AdmissionResponse findByStudentId(Long studentId) {
        Admission admission = admissionRepository.findByStudentId(studentId)
            .orElseThrow(() -> new ResourceNotFoundException("Admission not found for student id: " + studentId));
        return toResponse(admission);
    }

    @Transactional
    public AdmissionResponse update(Long id, AdmissionRequest request) {
        Admission admission = admissionRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Admission not found with id: " + id));
        Student student = studentRepository.findById(request.studentId())
            .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + request.studentId()));
        AcademicYear joiningYear = academicYearRepository.findById(request.joiningAcademicYearId())
            .orElseThrow(() -> new ResourceNotFoundException("Academic year not found: " + request.joiningAcademicYearId()));
        admission.setStudent(student);
        admission.setJoiningAcademicYear(joiningYear);
        admission.setApplicationDate(request.applicationDate());
        admission.setDeclarationPlace(request.declarationPlace());
        admission.setDeclarationDate(request.declarationDate());
        admission.setParentConsentGiven(request.parentConsentGiven());
        admission.setApplicantConsentGiven(request.applicantConsentGiven());
        Admission updated = admissionRepository.save(admission);
        return toResponse(updated);
    }

    @Transactional
    public void delete(Long id) {
        if (!admissionRepository.existsById(id)) {
            throw new ResourceNotFoundException("Admission not found with id: " + id);
        }
        admissionRepository.deleteById(id);
    }

    @Transactional
    public AdmissionConfirmationDto confirm(Long admissionId, LocalDate admissionDate) {
        Admission admission = admissionRepository.findById(admissionId)
            .orElseThrow(() -> new ResourceNotFoundException("Admission not found with id: " + admissionId));

        Student student = admission.getStudent();
        Program program = student.getProgram();

        List<IntakeRule> activeRules = intakeRuleRepository.findByProgramIdAndIsActiveTrue(program.getId());
        IntakeRule rule = activeRules.stream()
            .filter(r -> !admissionDate.isBefore(r.getAdmissionWindowStartDate())
                && !admissionDate.isAfter(r.getAdmissionWindowEndDate()))
            .findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                "No active intake rule found for program " + program.getCode()
                    + " and admission date " + admissionDate
                    + ". Please configure an intake rule first."));

        com.cms.model.Course studentCourse = student.getCourse();
        if (studentCourse == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                "Student does not have a course assigned. Please assign a course before confirming admission.");
        }
        Cohort cohort = cohortRepository
            .findByCourseIdAndAdmissionAcademicYearId(studentCourse.getId(), rule.getMappedAcademicYear().getId())
            .orElseGet(() -> {
                int startYear = rule.getMappedAcademicYear().getStartYear();
                int durationYears = program.getDurationYears();
                String cohortCode = studentCourse.getCode() + "-" + startYear + "-" + (startYear + durationYears);
                String displayName = studentCourse.getName() + " (" + startYear + "-" + (startYear + durationYears) + ")";
                AcademicYear expectedGradAY = academicYearRepository
                    .findByName((startYear + durationYears) + "-" + (startYear + durationYears + 1))
                    .orElse(null);
                Cohort newCohort = new Cohort();
                newCohort.setCourse(studentCourse);
                newCohort.setAdmissionAcademicYear(rule.getMappedAcademicYear());
                newCohort.setExpectedGraduationAcademicYear(expectedGradAY);
                newCohort.setCohortCode(cohortCode);
                newCohort.setDisplayName(displayName);
                newCohort.setStatus(CohortStatus.ACTIVE);
                return cohortRepository.save(newCohort);
            });

        student.setCohort(cohort);

        Long firstTermInstanceId = termInstanceRepository
            .findByAcademicYearIdAndTermType(rule.getMappedAcademicYear().getId(), TermType.ODD)
            .map(TermInstance::getId)
            .orElse(null);

        TermInstance expectedGraduationTermInstance = null;
        if (cohort.getExpectedGraduationAcademicYear() != null) {
            expectedGraduationTermInstance = termInstanceRepository
                .findByAcademicYearIdAndTermType(cohort.getExpectedGraduationAcademicYear().getId(), TermType.EVEN)
                .orElse(null);
        }
        student.setExpectedGraduationTermInstance(expectedGraduationTermInstance);
        ensureAdmissionNumber(student, admission.getJoiningAcademicYear(), student.getCourse());
        studentRepository.save(student);

        Long expectedGraduationTermInstanceId = expectedGraduationTermInstance != null
            ? expectedGraduationTermInstance.getId() : null;

        return new AdmissionConfirmationDto(
            student.getId(),
            student.getFullName(),
            student.getAdmissionNumber(),
            cohort.getCohortCode(),
            cohort.getDisplayName(),
            rule.getStartingSemesterNumber(),
            firstTermInstanceId,
            expectedGraduationTermInstanceId
        );
    }

    private AdmissionResponse toResponse(Admission admission) {
        AcademicYear ay = admission.getJoiningAcademicYear();
        Student student = admission.getStudent();
        Integer durationYears = student.getProgram() != null
            ? student.getProgram().getDurationYears() : null;
        Integer expectedCompletion = (ay != null && durationYears != null)
            ? ay.getStartYear() + durationYears : null;
        return new AdmissionResponse(
            admission.getId(),
            student.getId(),
            student.getFullName(),
            student.getAdmissionNumber(),
            student.getRollNumber(),
            student.getProgram() != null ? student.getProgram().getName() : null,
            student.getCourse() != null ? student.getCourse().getName() : null,
            student.getSemester(),
            student.getStatus() != null ? student.getStatus().name() : null,
            ay != null ? ay.getId() : null,
            ay != null ? ay.getName() : null,
            expectedCompletion,
            admission.getApplicationDate(),
            admission.getDeclarationPlace(),
            admission.getDeclarationDate(),
            admission.getParentConsentGiven(),
            admission.getApplicantConsentGiven(),
            admission.getCreatedAt(),
            admission.getUpdatedAt()
        );
    }

    private void ensureAdmissionNumber(Student student, AcademicYear academicYear, Course course) {
        if (student.getAdmissionNumber() == null || student.getAdmissionNumber().isBlank()) {
            student.setAdmissionNumber(numberSequenceService.nextAdmissionNumber(academicYear, course));
        }
    }
}
