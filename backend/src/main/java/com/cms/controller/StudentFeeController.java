package com.cms.controller;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cms.dto.CollectPaymentRequest;
import com.cms.dto.CollectPaymentResponse;
import com.cms.dto.EnquiryCreditApplicationDto;
import com.cms.dto.FeeExplorerResponse;
import com.cms.dto.FeeRefundApprovalRequest;
import com.cms.dto.FeeRefundRejectionRequest;
import com.cms.dto.FeeRefundRequest;
import com.cms.dto.FeeRefundResponse;
import com.cms.dto.FeeRefundSummaryResponse;
import com.cms.dto.PenaltyResponse;
import com.cms.dto.ReceiptResponse;
import com.cms.dto.ReceiptSummaryResponse;
import com.cms.dto.StudentFeeAllocationRequest;
import com.cms.dto.StudentFeeAllocationResponse;
import com.cms.dto.YearFeeFromEnquiry;
import com.cms.model.OneBookPaymentRequest;
import com.cms.service.FeeExplorerService;
import com.cms.service.FeeExportService;
import com.cms.service.FeeRefundExportService;
import com.cms.service.FeeFinalizationService;
import com.cms.service.FeeRefundService;
import com.cms.service.OneBookIntegrationService;
import com.cms.service.PaymentCollectionService;
import com.cms.service.PenaltyCalculationService;
import com.cms.util.ExportSortUtils;
import com.cms.util.export.ExportMetadata;
import com.cms.util.export.ExportResponseFactory;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/student-fees")
public class StudentFeeController {

    private static final Map<String, String> EXPLORER_SORT_FIELDS = new LinkedHashMap<>();
    static {
        EXPLORER_SORT_FIELDS.put("rollNumber", "Roll No.");
        EXPLORER_SORT_FIELDS.put("firstName", "Student Name");
        EXPLORER_SORT_FIELDS.put("program.name", "Program");
    }

    private static final Map<String, String> REFUND_SORT_FIELDS = new LinkedHashMap<>();
    static {
        REFUND_SORT_FIELDS.put("requestedAt", "Requested At");
        REFUND_SORT_FIELDS.put("refundAmount", "Refund Amount");
        REFUND_SORT_FIELDS.put("status", "Status");
        REFUND_SORT_FIELDS.put("originalReceiptNumber", "Original Receipt");
        REFUND_SORT_FIELDS.put("programName", "Program");
    }

    private final FeeFinalizationService feeFinalizationService;
    private final PaymentCollectionService paymentCollectionService;
    private final PenaltyCalculationService penaltyCalculationService;
    private final FeeExplorerService feeExplorerService;
    private final FeeExportService feeExportService;
    private final FeeRefundService feeRefundService;
    private final FeeRefundExportService feeRefundExportService;
    private final OneBookIntegrationService oneBookService;

    public StudentFeeController(FeeFinalizationService feeFinalizationService,
                                 PaymentCollectionService paymentCollectionService,
                                 PenaltyCalculationService penaltyCalculationService,
                                 FeeExplorerService feeExplorerService,
                                 FeeExportService feeExportService,
                                 FeeRefundService feeRefundService,
                                 FeeRefundExportService feeRefundExportService,
                                 OneBookIntegrationService oneBookService) {
        this.feeFinalizationService = feeFinalizationService;
        this.paymentCollectionService = paymentCollectionService;
        this.penaltyCalculationService = penaltyCalculationService;
        this.feeExplorerService = feeExplorerService;
        this.feeExportService = feeExportService;
        this.feeRefundService = feeRefundService;
        this.feeRefundExportService = feeRefundExportService;
        this.oneBookService = oneBookService;
    }

    @PostMapping("/finalize")
    @PreAuthorize("@perm.has('STUDENT_FEE_MANAGE')")
    public ResponseEntity<StudentFeeAllocationResponse> finalize(
            @Valid @RequestBody StudentFeeAllocationRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String username = jwt != null ? jwt.getClaimAsString("preferred_username") : "admin";
        StudentFeeAllocationResponse response = feeFinalizationService.finalize(request, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{studentId}/allocation-exists")
    public ResponseEntity<Boolean> allocationExists(@PathVariable Long studentId) {
        return ResponseEntity.ok(feeFinalizationService.allocationExists(studentId));
    }

    @GetMapping("/{studentId}/enquiry-year-fees")
    public ResponseEntity<List<YearFeeFromEnquiry>> getEnquiryYearFees(@PathVariable Long studentId) {
        return ResponseEntity.ok(feeFinalizationService.getEnquiryYearFees(studentId));
    }

    @GetMapping("/{studentId}/semester-breakdown")
    public ResponseEntity<StudentFeeAllocationResponse> getSemesterBreakdown(
            @PathVariable Long studentId) {
        return ResponseEntity.ok(feeFinalizationService.getByStudentId(studentId));
    }

    @GetMapping("/{studentId}/semester-status")
    public ResponseEntity<StudentFeeAllocationResponse> getSemesterStatus(
            @PathVariable Long studentId) {
        return ResponseEntity.ok(feeFinalizationService.getByStudentId(studentId));
    }

    @PostMapping("/{studentId}/collect")
    @PreAuthorize("@perm.has('FEE_COLLECT')")
    public ResponseEntity<CollectPaymentResponse> collectPayment(
            @PathVariable Long studentId,
            @Valid @RequestBody CollectPaymentRequest request) {
        CollectPaymentResponse response = paymentCollectionService.collectPayment(studentId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{studentId}/collect-advance")
    @PreAuthorize("@perm.has('FEE_COLLECT')")
    public ResponseEntity<CollectPaymentResponse> collectAdvancePayment(
            @PathVariable Long studentId,
            @Valid @RequestBody CollectPaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
            paymentCollectionService.collectAdvancePayment(studentId, request));
    }

    @GetMapping("/{studentId}/penalties")
    public ResponseEntity<PenaltyResponse> getPenalties(@PathVariable Long studentId) {
        PenaltyResponse response = penaltyCalculationService.calculatePenalties(studentId);
        return ResponseEntity.ok(response);
    }

    /**
     * Paginated fee explorer. When called with page/size params, returns a Spring Page of
     * StudentFeeSummary records. Falls back to the legacy unpaged response when the ?legacy=true
     * param is present (used by the old non-paginated client).
     */
    @GetMapping("/explorer")
    public ResponseEntity<?> explorer(
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "false") boolean legacy,
            @PageableDefault(size = 25, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        if (legacy) {
            return ResponseEntity.ok(feeExplorerService.search(search));
        }
        Page<FeeExplorerResponse.StudentFeeSummary> page = feeExplorerService.searchPageable(search, pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/explorer/export")
    @PreAuthorize("@perm.has('STUDENT_FEE_EXPORT')")
    public ResponseEntity<byte[]> exportExplorer(
            @RequestParam(defaultValue = "excel") String format,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String program,
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) Integer yearOfStudy,
            @RequestParam(required = false) String allocationStatus,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String direction) {

        Sort exportSort = ExportSortUtils.resolve(
            sort, direction, EXPLORER_SORT_FIELDS.keySet(), "rollNumber", Sort.Direction.ASC);
        List<FeeExplorerResponse.StudentFeeSummary> data =
            feeExplorerService.searchAll(search, program, academicYear, yearOfStudy, allocationStatus, exportSort);

        Sort.Order order = ExportSortUtils.firstOrder(exportSort, "rollNumber", Sort.Direction.ASC);
        ExportMetadata meta = ExportMetadata.of("Student Fee Explorer Export")
            .filter("Search", search)
            .filter("Program", (program != null && !program.equals("ALL")) ? program : null)
            .filter("Academic Year", (academicYear != null && !academicYear.equals("ALL")) ? academicYear : null)
            .filter("Year of Study", yearOfStudy != null ? String.valueOf(yearOfStudy) : null)
            .filter("Allocation Status", (allocationStatus != null && !allocationStatus.equals("ALL")) ? allocationStatus : null)
            .sort(EXPLORER_SORT_FIELDS.get(order.getProperty()), order.getDirection());

        return ExportResponseFactory.respond(format, "fee-explorer",
            () -> feeExportService.toExcel(data, meta),
            () -> feeExportService.toPdf(data, meta));
    }

    /** Unified refund initiation — auto-detects entity type (STUDENT or ENQUIRY) from the receipt. */
    @PostMapping("/refunds")
    @PreAuthorize("@perm.has('FEE_REFUND')")
    public ResponseEntity<FeeRefundResponse> initiateRefund(
            @Valid @RequestBody FeeRefundRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String username = jwt != null ? jwt.getClaimAsString("preferred_username") : null;
        return ResponseEntity.status(HttpStatus.CREATED).body(
            feeRefundService.initiateRefund(request, username));
    }

    @GetMapping("/refunds")
    @PreAuthorize("@perm.has('FEE_REFUND_APPROVE')")
    public ResponseEntity<Page<FeeRefundSummaryResponse>> getAllRefunds(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @PageableDefault(size = 25, sort = "requestedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(feeRefundService.getAllRefundsPage(search, status, entityType, fromDate, toDate, pageable));
    }

    @GetMapping("/refunds/export")
    @PreAuthorize("@perm.has('FEE_REFUND_EXPORT')")
    public ResponseEntity<byte[]> exportRefunds(
            @RequestParam(defaultValue = "excel") String format,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String direction) {

        Sort exportSort = ExportSortUtils.resolve(
            sort, direction, REFUND_SORT_FIELDS.keySet(), "requestedAt", Sort.Direction.DESC);
        List<FeeRefundSummaryResponse> data = feeRefundService.getAllRefundsAll(
            search, status, entityType, fromDate, toDate, exportSort);

        Sort.Order order = ExportSortUtils.firstOrder(exportSort, "requestedAt", Sort.Direction.DESC);
        ExportMetadata meta = ExportMetadata.of("Fee Refunds Export")
            .filter("Search", search)
            .filter("Status", status)
            .filter("Entity Type", entityType)
            .filter("Date Range", (fromDate != null && toDate != null) ? fromDate + " to " + toDate : null)
            .sort(REFUND_SORT_FIELDS.get(order.getProperty()), order.getDirection());

        return ExportResponseFactory.respond(format, "fee-refunds",
            () -> feeRefundExportService.toExcel(data, meta),
            () -> feeRefundExportService.toPdf(data, meta));
    }

    @GetMapping("/refunds/pending")
    @PreAuthorize("@perm.has('FEE_REFUND_APPROVE')")
    public ResponseEntity<List<FeeRefundSummaryResponse>> getPendingRefunds() {
        return ResponseEntity.ok(feeRefundService.getPendingRefunds());
    }

    @PostMapping("/refunds/{refundId}/approve")
    @PreAuthorize("@perm.has('FEE_REFUND_APPROVE')")
    public ResponseEntity<FeeRefundSummaryResponse> approveRefund(
            @PathVariable Long refundId,
            @Valid @RequestBody FeeRefundApprovalRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String username = jwt != null ? jwt.getClaimAsString("preferred_username") : null;
        return ResponseEntity.ok(feeRefundService.approveRefund(refundId, request, username));
    }

    @PostMapping("/refunds/{refundId}/approve-onebook")
    @PreAuthorize("@perm.has('FEE_REFUND_APPROVE')")
    public ResponseEntity<Map<String, String>> approveRefundViaOneBook(
            @PathVariable Long refundId,
            @AuthenticationPrincipal Jwt jwt) {
        String username = jwt != null ? jwt.getClaimAsString("preferred_username") : null;
        OneBookPaymentRequest obReq = oneBookService.pushRefundPayment(refundId, username);
        return ResponseEntity.ok(Map.of("referenceId", obReq.getReferenceId(), "status", obReq.getStatus()));
    }

    @PostMapping("/refunds/{refundId}/reject")
    @PreAuthorize("@perm.has('FEE_REFUND_APPROVE')")
    public ResponseEntity<FeeRefundSummaryResponse> rejectRefund(
            @PathVariable Long refundId,
            @Valid @RequestBody FeeRefundRejectionRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String username = jwt != null ? jwt.getClaimAsString("preferred_username") : null;
        return ResponseEntity.ok(feeRefundService.rejectRefund(refundId, request, username));
    }

    @GetMapping("/receipts")
    @PreAuthorize("@perm.has('STUDENT_FEE_VIEW')")
    public ResponseEntity<List<ReceiptSummaryResponse>> getAllReceipts() {
        return ResponseEntity.ok(paymentCollectionService.getAllReceiptSummaries());
    }

    @GetMapping("/{studentId}/receipts")
    public ResponseEntity<List<ReceiptResponse>> getReceipts(@PathVariable Long studentId) {
        List<ReceiptResponse> receipts = paymentCollectionService.getReceipts(studentId);
        return ResponseEntity.ok(receipts);
    }

    @GetMapping("/{studentId}/receipts/{receiptId}")
    public ResponseEntity<ReceiptResponse> getReceipt(
            @PathVariable Long studentId,
            @PathVariable Long receiptId) {
        ReceiptResponse receipt = paymentCollectionService.getReceiptById(studentId, receiptId);
        return ResponseEntity.ok(receipt);
    }

    @GetMapping("/{studentId}/credit-applications")
    @PreAuthorize("@perm.has('STUDENT_FEE_VIEW')")
    public ResponseEntity<List<EnquiryCreditApplicationDto>> getCreditApplications(@PathVariable Long studentId) {
        return ResponseEntity.ok(paymentCollectionService.getCreditApplicationsByStudent(studentId));
    }
}
