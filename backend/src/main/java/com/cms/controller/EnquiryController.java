package com.cms.controller;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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

import com.cms.dto.EnquiryCreditApplicationDto;
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
import com.cms.service.EnquiryDocumentService;
import com.cms.service.EnquiryExportService;
import com.cms.service.EnquiryPaymentService;
import com.cms.service.EnquiryService;
import com.cms.service.PaymentCollectionService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/enquiries")
public class EnquiryController {

    private final EnquiryService enquiryService;
    private final EnquiryDocumentService enquiryDocumentService;
    private final EnquiryPaymentService enquiryPaymentService;
    private final PaymentCollectionService paymentCollectionService;
    private final EnquiryExportService enquiryExportService;

    public EnquiryController(EnquiryService enquiryService,
                              EnquiryDocumentService enquiryDocumentService,
                              EnquiryPaymentService enquiryPaymentService,
                              PaymentCollectionService paymentCollectionService,
                              EnquiryExportService enquiryExportService) {
        this.enquiryService = enquiryService;
        this.enquiryDocumentService = enquiryDocumentService;
        this.enquiryPaymentService = enquiryPaymentService;
        this.paymentCollectionService = paymentCollectionService;
        this.enquiryExportService = enquiryExportService;
    }

    @GetMapping("/document-pending")
    @PreAuthorize("@perm.has('ENQUIRY_VIEW')")
    public ResponseEntity<Page<EnquiryResponse>> findDocumentPending(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long programId,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) String studentType,
            @PageableDefault(size = 25, sort = "enquiryDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(
            enquiryService.findDocumentPendingPage(search, programId, courseId, studentType, pageable));
    }

    @GetMapping("/document-verification-pending")
    @PreAuthorize("@perm.has('DOCUMENT_VERIFICATION_MANAGE')")
    public ResponseEntity<Page<EnquiryResponse>> findDocumentVerificationPending(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long programId,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) String studentType,
            @PageableDefault(size = 25, sort = "enquiryDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(
            enquiryService.findDocumentVerificationPendingPage(search, programId, courseId, studentType, pageable));
    }

    @GetMapping("/admission-pending")
    @PreAuthorize("@perm.has('ENQUIRY_VIEW')")
    public ResponseEntity<Page<EnquiryResponse>> findAdmissionPending(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long programId,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) String studentType,
            @PageableDefault(size = 25, sort = "enquiryDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(
            enquiryService.findAdmissionPendingPage(search, programId, courseId, studentType, pageable));
    }

    @PostMapping
    @PreAuthorize("@perm.has('ENQUIRY_CREATE')")
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
        BigDecimal totalPaid = enquiryPaymentService.getTotalAmountPaid(id);
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
    @PreAuthorize("@perm.has('ENQUIRY_EDIT')")
    public ResponseEntity<EnquiryResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody EnquiryRequest request) {
        EnquiryResponse response = enquiryService.update(id, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("@perm.has('ENQUIRY_EDIT')")
    public ResponseEntity<EnquiryResponse> updateStatus(
            @PathVariable Long id,
            @RequestParam EnquiryStatus status,
            Principal principal) {
        String changedBy = principal != null ? principal.getName() : "system";
        EnquiryResponse response = enquiryService.updateStatus(id, status, changedBy);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/finalize-fees")
    @PreAuthorize("@perm.has('FEE_FINALIZE')")
    public ResponseEntity<FeeFinalizationResponse> finalizeFees(
            @PathVariable Long id,
            @Valid @RequestBody FeeFinalizationRequest request,
            Principal principal) {
        String adminUsername = principal != null ? principal.getName() : "admin";
        FeeFinalizationResponse response = enquiryService.finalizeFees(id, request, adminUsername);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/submit-documents")
    @PreAuthorize("@perm.has('ENQUIRY_EDIT')")
    public ResponseEntity<?> submitDocuments(@PathVariable Long id) {
        MissingDocumentsResponse missingResponse = enquiryDocumentService.allMandatoryDocumentsSubmitted(id);
        if (!missingResponse.allSubmitted()) {
            return ResponseEntity.badRequest().body(missingResponse);
        }
        EnquiryResponse response = enquiryService.submitDocuments(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/convert")
    @PreAuthorize("@perm.has('ENQUIRY_EDIT')")
    public ResponseEntity<EnquiryResponse> convertToStudent(
            @PathVariable Long id,
            @RequestParam Long studentId) {
        EnquiryResponse response = enquiryService.convertToStudent(id, studentId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/convert")
    @PreAuthorize("@perm.has('ENQUIRY_EDIT')")
    public ResponseEntity<EnquiryResponse> convertToStudentWithData(
            @PathVariable Long id,
            @Valid @RequestBody EnquiryConversionRequest request,
            Principal principal) {
        String performedBy = principal != null ? principal.getName() : "admin";
        EnquiryResponse response = enquiryService.convertToStudentWithData(id, request, performedBy);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}/conversion-prefill")
    @PreAuthorize("@perm.has('ENQUIRY_VIEW')")
    public ResponseEntity<EnquiryConversionPrefillResponse> getConversionPrefill(@PathVariable Long id) {
        return ResponseEntity.ok(enquiryService.getConversionPrefill(id));
    }

    @PostMapping("/{id}/payments")
    @PreAuthorize("@perm.has('FEE_COLLECT')")
    public ResponseEntity<EnquiryPaymentResponse> collectPayment(
            @PathVariable Long id,
            @Valid @RequestBody EnquiryPaymentRequest request,
            Principal principal) {
        String collectedBy = principal != null ? principal.getName() : "system";
        EnquiryPaymentResponse response = enquiryPaymentService.collectPayment(id, request, collectedBy);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}/payments")
    @PreAuthorize("@perm.has('FEE_COLLECT')")
    public ResponseEntity<List<EnquiryPaymentResponse>> getPayments(@PathVariable Long id) {
        return ResponseEntity.ok(enquiryPaymentService.getPaymentsByEnquiryId(id));
    }

    @GetMapping("/{id}/credit-applications")
    @PreAuthorize("@perm.has('ENQUIRY_VIEW')")
    public ResponseEntity<List<EnquiryCreditApplicationDto>> getCreditApplications(@PathVariable Long id) {
        return ResponseEntity.ok(paymentCollectionService.getCreditApplicationsByEnquiry(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.has('ENQUIRY_DELETE')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        enquiryService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/export")
    @PreAuthorize("@perm.has('ENQUIRY_EXPORT')")
    public ResponseEntity<byte[]> export(
            @RequestParam(defaultValue = "excel") String format,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {

        List<EnquiryResponse> data = (fromDate != null && toDate != null)
            ? enquiryService.findByDateRange(fromDate, toDate)
            : enquiryService.findAll();

        try {
            if ("pdf".equalsIgnoreCase(format)) {
                byte[] bytes = enquiryExportService.toPdf(data);
                String filename = "enquiries-" + LocalDate.now() + ".pdf";
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_PDF);
                headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
                return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
            } else {
                byte[] bytes = enquiryExportService.toExcel(data);
                String filename = "enquiries-" + LocalDate.now() + ".xlsx";
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
                headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
                return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
