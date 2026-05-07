package com.cms.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.ScholarshipApplicationRequest;
import com.cms.dto.ScholarshipApplicationResponse;
import com.cms.dto.ScholarshipApprovalRequest;
import com.cms.dto.ScholarshipRejectionRequest;
import com.cms.dto.ScholarshipSanctionRequest;
import com.cms.dto.ScholarshipTypeResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.AcademicYear;
import com.cms.model.Program;
import com.cms.model.ScholarshipType;
import com.cms.model.Student;
import com.cms.model.StudentScholarship;
import com.cms.model.StudentScholarshipEligibility;
import com.cms.model.enums.DiscountType;
import com.cms.model.enums.DisbursementFrequency;
import com.cms.model.enums.ScholarshipApplicationMode;
import com.cms.model.enums.ScholarshipStatus;
import com.cms.repository.AcademicYearRepository;
import com.cms.repository.ScholarshipTypeRepository;
import com.cms.repository.StudentRepository;
import com.cms.repository.StudentScholarshipEligibilityRepository;
import com.cms.repository.StudentScholarshipRepository;

@Service
@Transactional(readOnly = true)
public class StudentScholarshipService {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100.00");

    private final StudentRepository studentRepository;
    private final ScholarshipTypeRepository scholarshipTypeRepository;
    private final StudentScholarshipRepository studentScholarshipRepository;
    private final StudentScholarshipEligibilityRepository eligibilityRepository;
    private final AcademicYearRepository academicYearRepository;
    private final ScholarshipTypeService scholarshipTypeService;

    public StudentScholarshipService(StudentRepository studentRepository,
                                     ScholarshipTypeRepository scholarshipTypeRepository,
                                     StudentScholarshipRepository studentScholarshipRepository,
                                     StudentScholarshipEligibilityRepository eligibilityRepository,
                                     AcademicYearRepository academicYearRepository,
                                     ScholarshipTypeService scholarshipTypeService) {
        this.studentRepository = studentRepository;
        this.scholarshipTypeRepository = scholarshipTypeRepository;
        this.studentScholarshipRepository = studentScholarshipRepository;
        this.eligibilityRepository = eligibilityRepository;
        this.academicYearRepository = academicYearRepository;
        this.scholarshipTypeService = scholarshipTypeService;
    }

    public List<ScholarshipTypeResponse> getEligibleScholarships(Long studentId) {
        Student student = findStudent(studentId);
        Optional<StudentScholarshipEligibility> eligibility = eligibilityRepository.findByStudentId(studentId);
        return scholarshipTypeRepository.findByActiveTrueOrderByNameAsc().stream()
            .filter(type -> isEligible(student, eligibility.orElse(null), type))
            .filter(type -> isWithinYearOfStudy(student, type))
            .map(scholarshipTypeService::toResponse)
            .toList();
    }

    public List<ScholarshipApplicationResponse> getStudentScholarships(Long studentId) {
        if (!studentRepository.existsById(studentId)) {
            throw new ResourceNotFoundException("Student not found with id: " + studentId);
        }
        return studentScholarshipRepository.findByStudentIdOrderByAcademicYearStartDateDesc(studentId).stream()
            .map(this::toResponse)
            .toList();
    }

    public List<ScholarshipApplicationResponse> getPendingApplications() {
        return studentScholarshipRepository.findByStatusOrderByApplicationDateAsc(ScholarshipStatus.PENDING).stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public ScholarshipApplicationResponse applyForScholarship(Long studentId, ScholarshipApplicationRequest request, String actor) {
        Student student = findStudent(studentId);
        AcademicYear academicYear = resolveAcademicYear(request.academicYearId());
        if (studentScholarshipRepository.existsByStudentIdAndAcademicYearId(studentId, academicYear.getId())) {
            throw new IllegalArgumentException("Student already has a scholarship application for academic year " + academicYear.getName());
        }
        ScholarshipType type = scholarshipTypeRepository.findById(request.scholarshipTypeId())
            .orElseThrow(() -> new ResourceNotFoundException("Scholarship type not found with id: " + request.scholarshipTypeId()));
        if (!type.isActive()) {
            throw new IllegalArgumentException("Scholarship type is inactive: " + type.getCode());
        }
        if (!isWithinYearOfStudy(student, type)) {
            throw new IllegalArgumentException(String.format(
                "Scholarship %s is restricted to year(s) %s–%s — student is currently in semester %s",
                type.getCode(),
                type.getEligibleFromYear() != null ? type.getEligibleFromYear() : "any",
                type.getEligibleToYear() != null ? type.getEligibleToYear() : "any",
                student.getSemester()));
        }

        StudentScholarship application = new StudentScholarship();
        application.setStudent(student);
        application.setScholarshipType(type);
        application.setAcademicYear(academicYear);
        application.setApplicationDate(LocalDate.now());
        application.setApplicationRemarks(blankToNull(request.applicationRemarks()));
        application.setStatus(ScholarshipStatus.PENDING);
        application.setCreatedBy(actor);
        return toResponse(studentScholarshipRepository.save(application));
    }

    @Transactional
    public ScholarshipApplicationResponse approveScholarship(Long scholarshipApplicationId,
                                                             ScholarshipApprovalRequest request,
                                                             String actor) {
        StudentScholarship application = findApplication(scholarshipApplicationId);
        if (application.getStatus() != ScholarshipStatus.PENDING && application.getStatus() != ScholarshipStatus.ON_HOLD) {
            throw new IllegalStateException("Only pending/on-hold scholarship applications can be approved");
        }
        application.setStatus(ScholarshipStatus.APPROVED);
        application.setApprovedBy(actor);
        application.setApprovedAt(Instant.now());
        application.setRejectionReason(null);
        application.setApprovedAmount(capAmount(request.approvedAmount(), application.getScholarshipType()));
        application.setDisbursementFrequency(request.disbursementFrequency() != null
            ? request.disbursementFrequency() : DisbursementFrequency.ANNUAL);
        application.setValidFrom(request.validFrom() != null ? request.validFrom() : application.getAcademicYear().getStartDate());
        application.setValidTill(request.validTill() != null ? request.validTill() : application.getAcademicYear().getEndDate());
        return toResponse(studentScholarshipRepository.save(application));
    }

    @Transactional
    public ScholarshipApplicationResponse rejectScholarship(Long scholarshipApplicationId,
                                                            ScholarshipRejectionRequest request,
                                                            String actor) {
        StudentScholarship application = findApplication(scholarshipApplicationId);
        if (application.getStatus() == ScholarshipStatus.APPROVED) {
            throw new IllegalStateException("Approved scholarship applications cannot be rejected");
        }
        application.setStatus(ScholarshipStatus.REJECTED);
        application.setRejectionReason(request.reason().trim());
        application.setApprovedAmount(null);
        application.setApprovedBy(null);
        application.setApprovedAt(null);
        return toResponse(studentScholarshipRepository.save(application));
    }

    @Transactional
    public ScholarshipApplicationResponse cancelScholarship(Long scholarshipApplicationId, String actor) {
        StudentScholarship application = findApplication(scholarshipApplicationId);
        if (application.getStatus() == ScholarshipStatus.APPROVED) {
            throw new IllegalStateException("Approved scholarship applications cannot be cancelled");
        }
        application.setStatus(ScholarshipStatus.CANCELLED);
        return toResponse(studentScholarshipRepository.save(application));
    }

    /**
     * Records a govt sanction for an APPROVED govt-portal scholarship.
     * Status moves APPROVED → SANCTIONED.
     */
    @Transactional
    public ScholarshipApplicationResponse sanctionScholarship(Long scholarshipApplicationId,
                                                              ScholarshipSanctionRequest request,
                                                              String actor) {
        StudentScholarship application = findApplication(scholarshipApplicationId);
        if (application.getStatus() != ScholarshipStatus.APPROVED) {
            throw new IllegalStateException("Only APPROVED applications can be sanctioned");
        }
        if (application.getScholarshipType().getApplicationMode() != ScholarshipApplicationMode.GOVT_PORTAL) {
            throw new IllegalStateException(
                "Sanction is only applicable to GOVT_PORTAL scholarship types — "
                + application.getScholarshipType().getCode() + " is INSTITUTION-mode");
        }
        application.setStatus(ScholarshipStatus.SANCTIONED);
        application.setGovtSanctionNumber(request.govtSanctionNumber().trim());
        application.setSanctionDate(request.sanctionDate());
        application.setSanctionedBy(actor);
        if (request.remarks() != null && !request.remarks().isBlank()) {
            String existing = application.getApplicationRemarks();
            String appended = (existing == null ? "" : existing + "\n") + "Sanction note: " + request.remarks().trim();
            application.setApplicationRemarks(appended);
        }
        return toResponse(studentScholarshipRepository.save(application));
    }

    @Transactional
    public ScholarshipApplicationResponse renewScholarship(Long scholarshipApplicationId, String actor) {
        StudentScholarship source = findApplication(scholarshipApplicationId);
        if (source.getStatus() != ScholarshipStatus.APPROVED) {
            throw new IllegalStateException("Only approved scholarship applications can be renewed");
        }
        if (!source.getScholarshipType().isRenewalRequired()) {
            throw new IllegalArgumentException("Scholarship type does not require renewal: " + source.getScholarshipType().getCode());
        }
        AcademicYear nextYear = findNextAcademicYear(source.getAcademicYear());
        if (studentScholarshipRepository.existsByStudentIdAndAcademicYearId(source.getStudent().getId(), nextYear.getId())) {
            throw new IllegalArgumentException("Student already has a scholarship application for academic year " + nextYear.getName());
        }
        StudentScholarship renewed = new StudentScholarship();
        renewed.setStudent(source.getStudent());
        renewed.setScholarshipType(source.getScholarshipType());
        renewed.setAcademicYear(nextYear);
        renewed.setApplicationDate(LocalDate.now());
        renewed.setApplicationRemarks("Renewal from " + source.getAcademicYear().getName());
        renewed.setStatus(ScholarshipStatus.PENDING);
        renewed.setRenewedFrom(source);
        renewed.setCreatedBy(actor);
        return toResponse(studentScholarshipRepository.save(renewed));
    }

    public Optional<StudentScholarship> findApprovedForStudentInCurrentYear(Long studentId) {
        AcademicYear current = currentAcademicYear();
        return studentScholarshipRepository.findByStudentIdAndAcademicYearId(studentId, current.getId())
            .filter(application -> application.getStatus() == ScholarshipStatus.APPROVED);
    }

    public ScholarshipApplicationResponse toResponse(StudentScholarship application) {
        Student student = application.getStudent();
        ScholarshipType type = application.getScholarshipType();
        AcademicYear academicYear = application.getAcademicYear();
        Program program = student.getProgram();
        return new ScholarshipApplicationResponse(
            application.getId(),
            student.getId(),
            student.getFullName(),
            student.getRollNumber(),
            program != null ? program.getId() : null,
            program != null ? program.getName() : null,
            program != null ? program.getCode() : null,
            student.getSemester(),
            type.getId(),
            type.getCode(),
            type.getName(),
            type.getApplicationMode(),
            type.getPortalName(),
            academicYear.getId(),
            academicYear.getName(),
            application.getApplicationDate(),
            application.getApplicationRemarks(),
            application.getStatus(),
            application.getApprovedBy(),
            application.getApprovedAt(),
            application.getRejectionReason(),
            application.getApprovedAmount(),
            application.getDisbursementFrequency(),
            application.getValidFrom(),
            application.getValidTill(),
            application.getGovtSanctionNumber(),
            application.getSanctionDate(),
            application.getSanctionedBy(),
            application.getRenewedFrom() != null ? application.getRenewedFrom().getId() : null,
            type.isRenewalRequired(),
            application.getCreatedBy(),
            application.getCreatedAt(),
            application.getUpdatedAt()
        );
    }

    public BigDecimal calculateScholarshipAmount(BigDecimal totalFee, ScholarshipType type) {
        if (type.getDiscountType() == DiscountType.FULL_WAIVER) {
            return capAmount(totalFee, type);
        }
        if (type.getDiscountType() == DiscountType.PERCENTAGE) {
            BigDecimal percentage = type.getDiscountValue() != null ? type.getDiscountValue() : BigDecimal.ZERO;
            return capAmount(totalFee.multiply(percentage).divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP), type);
        }
        return capAmount(type.getDiscountValue() != null ? type.getDiscountValue() : BigDecimal.ZERO, type);
    }

    private boolean isEligible(Student student, StudentScholarshipEligibility eligibility, ScholarshipType type) {
        String code = type.getCode();
        String community = normalize(student.getCommunityCategory());
        return switch (code) {
            case "FIRST_GRAD" -> student.isFirstGraduate() || (eligibility != null && eligibility.isFirstGraduate());
            case "SC_GOVT" -> "SC".equals(community);
            case "ST_GOVT" -> "ST".equals(community);
            case "OBC_GOVT" -> "OBC".equals(community);
            case "BC_STATE" -> "BC".equals(community) || "MBC".equals(community);
            case "EWS" -> eligibility != null && eligibility.getAnnualFamilyIncome() != null
                && eligibility.getAnnualFamilyIncome().compareTo(StudentScholarshipEligibilityService.EWS_INCOME_LIMIT) < 0;
            case "MERIT" -> eligibility != null && eligibility.isMeritBased();
            default -> false;
        };
    }

    /**
     * Returns true if the student's current year of study falls within the scholarship type's
     * eligibleFromYear–eligibleToYear window. Year of study is derived from the student's
     * current semester: years 1=sem 1-2, year 2=sem 3-4, year 3=sem 5-6, year 4=sem 7-8.
     * If the type has no year restriction, returns true.
     */
    boolean isWithinYearOfStudy(Student student, ScholarshipType type) {
        Integer fromYear = type.getEligibleFromYear();
        Integer toYear = type.getEligibleToYear();
        if (fromYear == null && toYear == null) {
            return true; // no restriction
        }
        Integer semester = student.getSemester();
        if (semester == null || semester < 1) {
            return true; // can't determine — let through (admin can override)
        }
        int yearOfStudy = (semester + 1) / 2; // sem 1,2 → year 1; sem 3,4 → year 2; etc.
        if (fromYear != null && yearOfStudy < fromYear) return false;
        if (toYear != null && yearOfStudy > toYear) return false;
        return true;
    }

    private BigDecimal capAmount(BigDecimal amount, ScholarshipType type) {
        BigDecimal safeAmount = amount != null ? amount : BigDecimal.ZERO;
        BigDecimal max = type.getMaxAmountPerYear();
        if (max != null && safeAmount.compareTo(max) > 0) {
            return max;
        }
        return safeAmount;
    }

    private Student findStudent(Long studentId) {
        return studentRepository.findById(studentId)
            .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));
    }

    private StudentScholarship findApplication(Long id) {
        return studentScholarshipRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Scholarship application not found with id: " + id));
    }

    private AcademicYear resolveAcademicYear(Long academicYearId) {
        if (academicYearId != null) {
            return academicYearRepository.findById(academicYearId)
                .orElseThrow(() -> new ResourceNotFoundException("Academic year not found with id: " + academicYearId));
        }
        return currentAcademicYear();
    }

    private AcademicYear currentAcademicYear() {
        return academicYearRepository.findByIsCurrentTrue()
            .orElseThrow(() -> new ResourceNotFoundException("No current academic year found"));
    }

    private AcademicYear findNextAcademicYear(AcademicYear source) {
        String nextName = (source.getStartYear() + 1) + "-" + (source.getStartYear() + 2);
        return academicYearRepository.findByName(nextName)
            .orElseThrow(() -> new ResourceNotFoundException("Next academic year not found: " + nextName));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

