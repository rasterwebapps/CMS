package com.cms.controller;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cms.dto.EnquiryConversionPrefillResponse;
import com.cms.dto.EnquiryConversionRequest;
import com.cms.dto.EnquiryDocumentResponse;
import com.cms.dto.EnquiryPaymentRequest;
import com.cms.dto.EnquiryPaymentResponse;
import com.cms.dto.EnquiryRequest;
import com.cms.dto.EnquiryResponse;
import com.cms.dto.EnquirySummaryResponse;
import com.cms.dto.EnquiryYearWiseFeeStatusResponse;
import com.cms.dto.FeeFinalizationRequest;
import com.cms.dto.FeeFinalizationResponse;
import com.cms.dto.MissingDocumentsResponse;
import com.cms.dto.EnquiryStatusHistoryResponse;
import com.cms.model.enums.EnquiryStatus;
import com.cms.repository.EnquiryPaymentRepository;
import com.cms.service.EnquiryDocumentService;
import com.cms.service.EnquiryPaymentService;
import com.cms.service.EnquiryService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/enquiries")
public class EnquiryController {

    private final EnquiryService enquiryService;
    private final EnquiryDocumentService enquiryDocumentService;
    private final EnquiryPaymentRepository enquiryPaymentRepository;
    private final EnquiryPaymentService enquiryPaymentService;

    public EnquiryController(EnquiryService enquiryService,
                              EnquiryDocumentService enquiryDocumentService,
                              EnquiryPaymentRepository enquiryPaymentRepository,
                              EnquiryPaymentService enquiryPaymentService) {
        this.enquiryService = enquiryService;
        this.enquiryDocumentService = enquiryDocumentService;
        this.enquiryPaymentRepository = enquiryPaymentRepository;
        this.enquiryPaymentService = enquiryPaymentService;
    }

    @GetMapping("/document-pending")
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_FRONT_OFFICE')")
    public ResponseEntity<List<EnquiryResponse>> findDocumentPending() {
        return ResponseEntity.ok(enquiryService.findDocumentPending());
    }

    @PostMapping
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_FRONT_OFFICE')")
    public ResponseEntity<EnquiryResponse> create(@Valid @RequestBody EnquiryRequest request) {
        EnquiryResponse response = enquiryService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<EnquiryResponse>> findAll(
            @RequestParam(required = false) EnquiryStatus status,
            @RequestParam(required = false) Long referralTypeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        List<EnquiryResponse> enquiries;
        if (fromDate != null && toDate != null && status != null) {
            enquiries = enquiryService.findByDateRangeAndStatus(fromDate, toDate, status);
        } else if (fromDate != null && toDate != null) {
            enquiries = enquiryService.findByDateRange(fromDate, toDate);
        } else if (status != null) {
            enquiries = enquiryService.findByStatus(status);
        } else if (referralTypeId != null) {
            enquiries = enquiryService.findByReferralTypeId(referralTypeId);
        } else {
            enquiries = enquiryService.findAll();
        }
        return ResponseEntity.ok(enquiries);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EnquiryResponse> findById(@PathVariable Long id) {
        EnquiryResponse response = enquiryService.findById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/summary")
    public ResponseEntity<EnquirySummaryResponse> getSummary(@PathVariable Long id) {
        EnquiryResponse enquiry = enquiryService.findById(id);
        BigDecimal totalPaid = enquiryPaymentRepository.sumAmountPaidByEnquiryId(id);
        BigDecimal outstanding = null;
        if (enquiry.finalizedNetFee() != null) {
            outstanding = enquiry.finalizedNetFee().subtract(totalPaid);
        }
        List<EnquiryDocumentResponse> docs = enquiryDocumentService.findByEnquiryId(id);
        List<String> docTypes = docs.stream().map(d -> d.documentType().name()).toList();
        return ResponseEntity.ok(new EnquirySummaryResponse(enquiry, totalPaid, outstanding, docs.size(), docTypes));
    }

    @GetMapping("/{id}/year-wise-fee-status")
    public ResponseEntity<EnquiryYearWiseFeeStatusResponse> getYearWiseFeeStatus(@PathVariable Long id) {
        return ResponseEntity.ok(enquiryPaymentService.getYearWiseFeeStatus(id));
    }

    @GetMapping("/{id}/status-history")
    public ResponseEntity<List<EnquiryStatusHistoryResponse>> getStatusHistory(@PathVariable Long id) {
        return ResponseEntity.ok(enquiryService.getStatusHistory(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_FRONT_OFFICE')")
    public ResponseEntity<EnquiryResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody EnquiryRequest request) {
        EnquiryResponse response = enquiryService.update(id, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_FRONT_OFFICE')")
    public ResponseEntity<EnquiryResponse> updateStatus(
            @PathVariable Long id,
            @RequestParam EnquiryStatus status,
            Principal principal) {
        String changedBy = principal != null ? principal.getName() : "system";
        EnquiryResponse response = enquiryService.updateStatus(id, status, changedBy);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/finalize-fees")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<FeeFinalizationResponse> finalizeFees(
            @PathVariable Long id,
            @Valid @RequestBody FeeFinalizationRequest request,
            Principal principal) {
        String adminUsername = principal != null ? principal.getName() : "admin";
        FeeFinalizationResponse response = enquiryService.finalizeFees(id, request, adminUsername);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/submit-documents")
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_FRONT_OFFICE')")
    public ResponseEntity<?> submitDocuments(@PathVariable Long id) {
        MissingDocumentsResponse missingResponse = enquiryDocumentService.allMandatoryDocumentsSubmitted(id);
        if (!missingResponse.allSubmitted()) {
            return ResponseEntity.badRequest().body(missingResponse);
        }
        EnquiryResponse response = enquiryService.submitDocuments(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/convert")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<EnquiryResponse> convertToStudent(
            @PathVariable Long id,
            @RequestParam Long studentId) {
        EnquiryResponse response = enquiryService.convertToStudent(id, studentId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/convert")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<EnquiryResponse> convertToStudentWithData(
            @PathVariable Long id,
            @Valid @RequestBody EnquiryConversionRequest request,
            Principal principal) {
        String performedBy = principal != null ? principal.getName() : "admin";
        EnquiryResponse response = enquiryService.convertToStudentWithData(id, request, performedBy);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}/conversion-prefill")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<EnquiryConversionPrefillResponse> getConversionPrefill(@PathVariable Long id) {
        return ResponseEntity.ok(enquiryService.getConversionPrefill(id));
    }

    @PostMapping("/{id}/payments")
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_FRONT_OFFICE')")
    public ResponseEntity<EnquiryPaymentResponse> collectPayment(
            @PathVariable Long id,
            @Valid @RequestBody EnquiryPaymentRequest request,
            Principal principal) {
        String collectedBy = principal != null ? principal.getName() : "system";
        EnquiryPaymentResponse response = enquiryPaymentService.collectPayment(id, request, collectedBy);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}/payments")
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_FRONT_OFFICE')")
    public ResponseEntity<List<EnquiryPaymentResponse>> getPayments(@PathVariable Long id) {
        return ResponseEntity.ok(enquiryPaymentService.getPaymentsByEnquiryId(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        enquiryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
