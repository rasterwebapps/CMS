package com.cms.controller;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cms.dto.UnifiedReceiptResponse;
import com.cms.service.ReceiptExportService;
import com.cms.service.UnifiedReceiptService;
import com.cms.util.ExportSortUtils;
import com.cms.util.export.ExportMetadata;
import com.cms.util.export.ExportResponseFactory;

/**
 * Unified receipts endpoint — covers both student fee payments and
 * enquiry pre-enrollment payments in a single list.
 */
@RestController
@RequestMapping("/receipts")
public class ReceiptController {

    private static final Map<String, String> EXPORT_SORT_FIELDS = new LinkedHashMap<>();
    static {
        EXPORT_SORT_FIELDS.put("receiptNumber", "Receipt No.");
        EXPORT_SORT_FIELDS.put("payerName", "Payer");
        EXPORT_SORT_FIELDS.put("payerType", "Type");
        EXPORT_SORT_FIELDS.put("amountPaid", "Amount");
        EXPORT_SORT_FIELDS.put("paymentMode", "Mode");
        EXPORT_SORT_FIELDS.put("paymentDate", "Date");
        EXPORT_SORT_FIELDS.put("transactionReference", "Txn Ref");
        EXPORT_SORT_FIELDS.put("installmentsCovered", "Towards");
    }

    private final UnifiedReceiptService unifiedReceiptService;
    private final ReceiptExportService  receiptExportService;

    public ReceiptController(UnifiedReceiptService unifiedReceiptService,
                             ReceiptExportService receiptExportService) {
        this.unifiedReceiptService = unifiedReceiptService;
        this.receiptExportService  = receiptExportService;
    }

    @GetMapping
    @PreAuthorize("@perm.has('RECEIPT_VIEW')")
    public ResponseEntity<Page<UnifiedReceiptResponse>> getAllReceipts(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String paymentMode,
            @RequestParam(required = false) String payerType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @PageableDefault(size = 25, sort = "paymentDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(unifiedReceiptService.getPaymentsPage(search, paymentMode, payerType, fromDate, toDate, pageable));
    }

    @GetMapping("/export")
    @PreAuthorize("@perm.has('RECEIPT_EXPORT')")
    public ResponseEntity<byte[]> export(
            @RequestParam(defaultValue = "excel") String format,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String paymentMode,
            @RequestParam(required = false) String payerType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String direction) {

        Sort exportSort = ExportSortUtils.resolve(
            sort, direction, EXPORT_SORT_FIELDS.keySet(), "paymentDate", Sort.Direction.DESC);
        List<UnifiedReceiptResponse> data = unifiedReceiptService.getPaymentsAll(
            search, paymentMode, payerType, fromDate, toDate, exportSort);

        Sort.Order order = ExportSortUtils.firstOrder(exportSort, "paymentDate", Sort.Direction.DESC);
        ExportMetadata meta = ExportMetadata.of("Fee Receipts Export")
            .filter("Search", search)
            .filter("Payment Mode", paymentMode)
            .filter("Payer Type", payerType)
            .filter("Date Range", (fromDate != null && toDate != null) ? fromDate + " to " + toDate : null)
            .sort(EXPORT_SORT_FIELDS.get(order.getProperty()), order.getDirection());

        return ExportResponseFactory.respond(format, "receipts",
            () -> receiptExportService.toExcel(data, meta),
            () -> receiptExportService.toPdf(data, meta));
    }

    @GetMapping("/{receiptNumber}")
    @PreAuthorize("@perm.has('RECEIPT_VIEW')")
    public ResponseEntity<UnifiedReceiptResponse> getReceipt(
            @PathVariable String receiptNumber) {
        return ResponseEntity.ok(unifiedReceiptService.getReceiptByNumber(receiptNumber));
    }
}

