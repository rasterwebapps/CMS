package com.cms.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.DisbursementRequest;
import com.cms.dto.DisbursementResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.AcademicYear;
import com.cms.model.OneBookPaymentRequest;
import com.cms.model.ScholarshipDisbursement;
import com.cms.model.StudentScholarship;
import com.cms.model.enums.DisbursementMode;
import com.cms.model.enums.ScholarshipStatus;
import com.cms.repository.AcademicYearRepository;
import com.cms.repository.ScholarshipDisbursementRepository;
import com.cms.repository.StudentScholarshipRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@Transactional(readOnly = true)
public class ScholarshipDisbursementService {

    private final ScholarshipDisbursementRepository disbursementRepository;
    private final StudentScholarshipRepository studentScholarshipRepository;
    private final AcademicYearRepository academicYearRepository;
    private final ObjectMapper objectMapper;

    public ScholarshipDisbursementService(ScholarshipDisbursementRepository disbursementRepository,
                                          StudentScholarshipRepository studentScholarshipRepository,
                                          AcademicYearRepository academicYearRepository,
                                          ObjectMapper objectMapper) {
        this.disbursementRepository = disbursementRepository;
        this.studentScholarshipRepository = studentScholarshipRepository;
        this.academicYearRepository = academicYearRepository;
        this.objectMapper = objectMapper;
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

    /**
     * Creates a disbursement record when OneBook reports a PAID status for a scholarship payment.
     * Reads academicYearId, termNumber, and remarks from the OB request's stored metadata JSON.
     */
    @Transactional
    public void completeOneBookDisbursement(OneBookPaymentRequest obRequest) {
        StudentScholarship application = studentScholarshipRepository.findById(obRequest.getEntityId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Scholarship application not found: " + obRequest.getEntityId()));

        Long academicYearId = null;
        Integer termNumber = null;
        String remarks = null;

        if (obRequest.getRequestMetadata() != null) {
            try {
                Map<String, Object> meta = objectMapper.readValue(
                        obRequest.getRequestMetadata(), new TypeReference<>() {});
                if (meta.get("academicYearId") instanceof Number n) academicYearId = n.longValue();
                if (meta.get("termNumber") instanceof Number n) termNumber = n.intValue();
                if (meta.get("remarks") instanceof String s) remarks = s;
            } catch (Exception ignored) {
                // fall through to defaults if metadata is corrupt
            }
        }

        AcademicYear academicYear = null;
        if (academicYearId != null) {
            Long ayId = academicYearId;
            academicYear = academicYearRepository.findById(ayId).orElse(null);
        }
        if (academicYear == null) {
            academicYear = application.getAcademicYear();
        }

        LocalDate disbursementDate = obRequest.getOnebookPaidDate() != null
                ? obRequest.getOnebookPaidDate() : LocalDate.now();

        ScholarshipDisbursement disbursement = new ScholarshipDisbursement();
        disbursement.setStudentScholarship(application);
        disbursement.setAcademicYear(academicYear);
        disbursement.setSemesterNumber(termNumber);
        disbursement.setAmount(obRequest.getAmount());
        disbursement.setDisbursementDate(disbursementDate);
        disbursement.setDisbursementMode(DisbursementMode.DIRECT_CREDIT);
        disbursement.setTransactionReference(obRequest.getOnebookTxnId());
        disbursement.setRemarks(remarks);
        disbursement.setDisbursedBy(obRequest.getApprovedBy() != null ? obRequest.getApprovedBy() : "onebook");
        disbursement.setDisbursementNumber(obRequest.getInvoiceNumber());
        disbursementRepository.save(disbursement);
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

