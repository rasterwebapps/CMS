package com.cms.controller;

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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cms.dto.CommissionExplorerResponse;
import com.cms.dto.CommissionPayoutRequest;
import com.cms.dto.CommissionRejectionRequest;
import com.cms.service.CommissionExplorerService;
import com.cms.service.CommissionExportService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/commission-explorer")
public class CommissionExplorerController {

    private final CommissionExplorerService service;
    private final CommissionExportService   exportService;

    public CommissionExplorerController(CommissionExplorerService service,
                                         CommissionExportService exportService) {
        this.service       = service;
        this.exportService = exportService;
    }

    @GetMapping
    @PreAuthorize("@perm.hasAny('COMMISSION_VIEW', 'COMMISSION_MANAGE', 'COMMISSION_SETTLE')")
    public ResponseEntity<Page<CommissionExplorerResponse>> findAll(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) Long referralTypeId,
            @RequestParam(required = false) Long agentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 25, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(
                service.findPage(status, source, referralTypeId, agentId, fromDate, toDate, search, pageable));
    }

    @GetMapping("/export")
    @PreAuthorize("@perm.has('COMMISSION_EXPORT')")
    public ResponseEntity<byte[]> export(
            @RequestParam(defaultValue = "excel") String format,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String search) {

        List<CommissionExplorerResponse> data =
            service.findAll(status, source, null, null, fromDate, toDate, search);

        try {
            if ("pdf".equalsIgnoreCase(format)) {
                byte[] bytes = exportService.toPdf(data);
                String filename = "commissions-" + LocalDate.now() + ".pdf";
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_PDF);
                headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
                return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
            } else {
                byte[] bytes = exportService.toExcel(data);
                String filename = "commissions-" + LocalDate.now() + ".xlsx";
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

    @PostMapping("/{enquiryId}/approve")
    @PreAuthorize("@perm.has('COMMISSION_MANAGE')")
    public ResponseEntity<CommissionExplorerResponse> approve(
            @PathVariable Long enquiryId,
            Principal principal) {
        String approvedBy = principal != null ? principal.getName() : "system";
        return ResponseEntity.ok(service.approve(enquiryId, approvedBy));
    }

    @PostMapping("/{enquiryId}/reject")
    @PreAuthorize("@perm.has('COMMISSION_MANAGE')")
    public ResponseEntity<CommissionExplorerResponse> reject(
            @PathVariable Long enquiryId,
            @Valid @RequestBody CommissionRejectionRequest request,
            Principal principal) {
        String rejectedBy = principal != null ? principal.getName() : "system";
        return ResponseEntity.ok(service.reject(enquiryId, request.reason(), rejectedBy));
    }

    @PostMapping("/{enquiryId}/reopen")
    @PreAuthorize("@perm.has('COMMISSION_MANAGE')")
    public ResponseEntity<CommissionExplorerResponse> reopen(@PathVariable Long enquiryId) {
        return ResponseEntity.ok(service.reopen(enquiryId));
    }

    @PostMapping("/{enquiryId}/payouts")
    @PreAuthorize("@perm.hasAny('COMMISSION_SETTLE', 'COMMISSION_MANAGE')")
    public ResponseEntity<CommissionExplorerResponse> recordPayout(
            @PathVariable Long enquiryId,
            @Valid @RequestBody CommissionPayoutRequest request,
            Principal principal) {
        String paidBy = principal != null ? principal.getName() : "system";
        return ResponseEntity.ok(service.recordPayout(enquiryId, request, paidBy));
    }
}
