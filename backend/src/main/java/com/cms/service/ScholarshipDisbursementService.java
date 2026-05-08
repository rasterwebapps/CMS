package com.cms.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.DisbursementRequest;
import com.cms.dto.DisbursementResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.AcademicYear;
import com.cms.model.ScholarshipDisbursement;
import com.cms.model.StudentScholarship;
import com.cms.model.enums.ScholarshipStatus;
import com.cms.repository.AcademicYearRepository;
import com.cms.repository.ScholarshipDisbursementRepository;
import com.cms.repository.StudentScholarshipRepository;

@Service
@Transactional(readOnly = true)
public class ScholarshipDisbursementService {

    private final ScholarshipDisbursementRepository disbursementRepository;
    private final StudentScholarshipRepository studentScholarshipRepository;
    private final AcademicYearRepository academicYearRepository;

    public ScholarshipDisbursementService(ScholarshipDisbursementRepository disbursementRepository,
                                          StudentScholarshipRepository studentScholarshipRepository,
                                          AcademicYearRepository academicYearRepository) {
        this.disbursementRepository = disbursementRepository;
        this.studentScholarshipRepository = studentScholarshipRepository;
        this.academicYearRepository = academicYearRepository;
    }

    @Transactional
    public DisbursementResponse disburse(Long studentScholarshipId, DisbursementRequest request, String actor) {
        StudentScholarship application = studentScholarshipRepository.findById(studentScholarshipId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Scholarship application not found with id: " + studentScholarshipId));
        if (application.getStatus() != ScholarshipStatus.APPROVED) {
            throw new IllegalStateException("Only approved scholarship applications can be disbursed");
        }

        AcademicYear academicYear = null;
        if (request.academicYearId() != null) {
            academicYear = academicYearRepository.findById(request.academicYearId())
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Academic year not found with id: " + request.academicYearId()));
        } else {
            academicYear = application.getAcademicYear();
        }

        ScholarshipDisbursement disbursement = new ScholarshipDisbursement();
        disbursement.setStudentScholarship(application);
        disbursement.setAcademicYear(academicYear);
        disbursement.setSemesterNumber(request.termNumber());
        disbursement.setAmount(request.amount());
        disbursement.setDisbursementDate(request.disbursementDate());
        disbursement.setDisbursementMode(request.disbursementMode());
        disbursement.setTransactionReference(blankToNull(request.transactionReference()));
        disbursement.setChequeNumber(blankToNull(request.chequeNumber()));
        disbursement.setBankName(blankToNull(request.bankName()));
        disbursement.setRemarks(blankToNull(request.remarks()));
        disbursement.setDisbursedBy(actor);
        return toResponse(disbursementRepository.save(disbursement));
    }

    public List<DisbursementResponse> getApplicationDisbursements(Long studentScholarshipId) {
        if (!studentScholarshipRepository.existsById(studentScholarshipId)) {
            throw new ResourceNotFoundException("Scholarship application not found with id: " + studentScholarshipId);
        }
        return disbursementRepository.findByStudentScholarshipIdOrderByDisbursementDateDesc(studentScholarshipId)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    public List<DisbursementResponse> getStudentDisbursementHistory(Long studentId) {
        return disbursementRepository.findByStudentScholarshipStudentIdOrderByDisbursementDateDesc(studentId)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    private DisbursementResponse toResponse(ScholarshipDisbursement disbursement) {
        StudentScholarship application = disbursement.getStudentScholarship();
        AcademicYear academicYear = disbursement.getAcademicYear();
        return new DisbursementResponse(
            disbursement.getId(),
            application.getId(),
            application.getStudent().getId(),
            application.getStudent().getFullName(),
            academicYear != null ? academicYear.getId() : null,
            academicYear != null ? academicYear.getName() : null,
            disbursement.getSemesterNumber(),
            disbursement.getAmount(),
            disbursement.getDisbursementDate(),
            disbursement.getDisbursementMode(),
            disbursement.getTransactionReference(),
            disbursement.getChequeNumber(),
            disbursement.getBankName(),
            disbursement.getRemarks(),
            disbursement.getDisbursedBy(),
            disbursement.getCreatedAt()
        );
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

