package com.cms.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.ScholarshipEligibilityRequest;
import com.cms.dto.ScholarshipEligibilityResponse;
import com.cms.dto.ScholarshipTypeResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Student;
import com.cms.model.StudentScholarshipEligibility;
import com.cms.repository.StudentRepository;
import com.cms.repository.StudentScholarshipEligibilityRepository;

@Service
@Transactional(readOnly = true)
public class StudentScholarshipEligibilityService {

    static final BigDecimal EWS_INCOME_LIMIT = new BigDecimal("300000.00");

    private final StudentRepository studentRepository;
    private final StudentScholarshipEligibilityRepository eligibilityRepository;
    private final StudentScholarshipService studentScholarshipService;

    public StudentScholarshipEligibilityService(StudentRepository studentRepository,
                                                StudentScholarshipEligibilityRepository eligibilityRepository,
                                                StudentScholarshipService studentScholarshipService) {
        this.studentRepository = studentRepository;
        this.eligibilityRepository = eligibilityRepository;
        this.studentScholarshipService = studentScholarshipService;
    }

    @Transactional
    public ScholarshipEligibilityResponse getEligibility(Long studentId) {
        StudentScholarshipEligibility eligibility = getOrCreate(studentId);
        return toResponse(eligibility, studentScholarshipService.getEligibleScholarships(studentId));
    }

    @Transactional
    public ScholarshipEligibilityResponse updateEligibility(Long studentId, ScholarshipEligibilityRequest request, String actor) {
        StudentScholarshipEligibility eligibility = getOrCreate(studentId);
        Student student = eligibility.getStudent();

        eligibility.setFirstGraduate(Boolean.TRUE.equals(request.isFirstGraduate()));
        eligibility.setMeritBased(Boolean.TRUE.equals(request.isMeritBased()));
        eligibility.setSportsQuota(Boolean.TRUE.equals(request.isSportsQuota()));
        eligibility.setAnnualFamilyIncome(request.annualFamilyIncome());
        eligibility.setEconomicallyWeaker(Boolean.TRUE.equals(request.isEconomicallyWeaker())
            || (request.annualFamilyIncome() != null && request.annualFamilyIncome().compareTo(EWS_INCOME_LIMIT) < 0));
        eligibility.setIncomeCertificateNumber(blankToNull(request.incomeCertificateNumber()));
        eligibility.setIncomeCertIssuingAuthority(blankToNull(request.incomeCertIssuingAuthority()));
        eligibility.setIncomeCertIssueDate(request.incomeCertIssueDate());
        eligibility.setCommunityCertificateNumber(blankToNull(request.communityCertificateNumber()));
        eligibility.setCommCertIssuingAuthority(blankToNull(request.commCertIssuingAuthority()));
        eligibility.setCommCertIssueDate(request.commCertIssueDate());
        eligibility.setFirstGraduateCertificateNumber(blankToNull(request.firstGraduateCertificateNumber()));
        eligibility.setFirstGradCertIssuingAuthority(blankToNull(request.firstGradCertIssuingAuthority()));
        eligibility.setFirstGradCertIssueDate(request.firstGradCertIssueDate());
        eligibility.setFatherEducation(blankToNull(request.fatherEducation()));
        eligibility.setMotherEducation(blankToNull(request.motherEducation()));

        // ── DBT fields ─────────────────────────────────────────────────────
        eligibility.setAadhaarNumber(blankToNull(request.aadhaarNumber()));
        eligibility.setBankAccountNumber(blankToNull(request.bankAccountNumber()));
        eligibility.setBankIfsc(blankToNull(request.bankIfsc()));
        eligibility.setBankName(blankToNull(request.bankName()));
        eligibility.setBankBranch(blankToNull(request.bankBranch()));
        eligibility.setDbtLinked(Boolean.TRUE.equals(request.dbtLinked()));

        student.setFirstGraduate(eligibility.isFirstGraduate());
        student.setFatherEducation(eligibility.getFatherEducation());
        student.setMotherEducation(eligibility.getMotherEducation());
        studentRepository.save(student);

        StudentScholarshipEligibility saved = eligibilityRepository.save(eligibility);
        return toResponse(saved, studentScholarshipService.getEligibleScholarships(studentId));
    }

    @Transactional
    public ScholarshipEligibilityResponse verifyEligibility(Long studentId, String verifiedBy, String remarks) {
        StudentScholarshipEligibility eligibility = getOrCreate(studentId);
        eligibility.setVerifiedBy(verifiedBy);
        eligibility.setVerifiedAt(Instant.now());
        eligibility.setVerificationRemarks(blankToNull(remarks));
        StudentScholarshipEligibility saved = eligibilityRepository.save(eligibility);
        return toResponse(saved, studentScholarshipService.getEligibleScholarships(studentId));
    }

    StudentScholarshipEligibility getOrCreate(Long studentId) {
        return eligibilityRepository.findByStudentId(studentId).orElseGet(() -> {
            Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));
            StudentScholarshipEligibility eligibility = new StudentScholarshipEligibility();
            eligibility.setStudent(student);
            eligibility.setFirstGraduate(student.isFirstGraduate());
            eligibility.setFatherEducation(student.getFatherEducation());
            eligibility.setMotherEducation(student.getMotherEducation());
            return eligibilityRepository.save(eligibility);
        });
    }

    ScholarshipEligibilityResponse toResponse(StudentScholarshipEligibility eligibility,
                                              List<ScholarshipTypeResponse> eligibleScholarships) {
        Student student = eligibility.getStudent();
        return new ScholarshipEligibilityResponse(
            eligibility.getId(),
            student.getId(),
            student.getFullName(),
            student.getCommunityCategory(),
            student.getCaste(),
            eligibility.isFirstGraduate(),
            eligibility.isMeritBased(),
            eligibility.isSportsQuota(),
            eligibility.isEconomicallyWeaker(),
            eligibility.getAnnualFamilyIncome(),
            eligibility.getIncomeCertificateNumber(),
            eligibility.getIncomeCertIssuingAuthority(),
            eligibility.getIncomeCertIssueDate(),
            eligibility.getCommunityCertificateNumber(),
            eligibility.getCommCertIssuingAuthority(),
            eligibility.getCommCertIssueDate(),
            eligibility.getFirstGraduateCertificateNumber(),
            eligibility.getFirstGradCertIssuingAuthority(),
            eligibility.getFirstGradCertIssueDate(),
            eligibility.getFatherEducation(),
            eligibility.getMotherEducation(),
            eligibility.getVerifiedBy(),
            eligibility.getVerifiedAt(),
            eligibility.getVerificationRemarks(),
            maskAadhaar(eligibility.getAadhaarNumber()),
            eligibility.getBankAccountNumber(),
            eligibility.getBankIfsc(),
            eligibility.getBankName(),
            eligibility.getBankBranch(),
            eligibility.isDbtLinked(),
            eligibleScholarships,
            eligibility.getCreatedAt(),
            eligibility.getUpdatedAt()
        );
    }

    /**
     * Returns "XXXXXXXX1234" for a 12-digit Aadhaar; null if not present.
     * Backend never exposes the full Aadhaar number — only the last 4 digits are visible.
     */
    static String maskAadhaar(String aadhaar) {
        if (aadhaar == null || aadhaar.length() != 12) {
            return null;
        }
        return "XXXXXXXX" + aadhaar.substring(8);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

