package com.cms.controller;

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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cms.dto.UnifiedReceiptResponse;
import com.cms.service.ReceiptExportService;
import com.cms.service.UnifiedReceiptService;

/**
 * Unified receipts endpoint — covers both student fee payments and
 * enquiry pre-enrollment payments in a single list.
 */
@RestController
@RequestMapping("/receipts")
public class ReceiptController {

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
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        List<UnifiedReceiptResponse> data = unifiedReceiptService.getPaymentsAll(
            search, paymentMode, payerType, fromDate, toDate);
        try {
            if ("pdf".equalsIgnoreCase(format)) {
                byte[] bytes = receiptExportService.toPdf(data);
                String filename = "receipts-" + LocalDate.now() + ".pdf";
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_PDF);
                headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
                return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
            } else {
                byte[] bytes = receiptExportService.toExcel(data);
                String filename = "receipts-" + LocalDate.now() + ".xlsx";
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

    @GetMapping("/{receiptNumber}")
    @PreAuthorize("@perm.has('RECEIPT_VIEW')")
    public ResponseEntity<UnifiedReceiptResponse> getReceipt(
            @PathVariable String receiptNumber) {
        return ResponseEntity.ok(unifiedReceiptService.getReceiptByNumber(receiptNumber));
    }
}

