package com.cms.controller;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
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
import com.cms.service.CommissionExplorerService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/commission-explorer")
public class CommissionExplorerController {

    private final CommissionExplorerService service;

    public CommissionExplorerController(CommissionExplorerService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("@perm.has('COMMISSION_VIEW')")
    public ResponseEntity<List<CommissionExplorerResponse>> findAll(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) Long referralTypeId,
            @RequestParam(required = false) Long agentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(
                service.findAll(status, source, referralTypeId, agentId, fromDate, toDate, search));
    }

    @PostMapping("/{enquiryId}/request-payment")
    @PreAuthorize("@perm.has('COMMISSION_VIEW')")
    public ResponseEntity<CommissionExplorerResponse> requestPayment(
            @PathVariable Long enquiryId,
            Principal principal) {
        String requestedBy = principal != null ? principal.getName() : "system";
        return ResponseEntity.ok(service.requestPayment(enquiryId, requestedBy));
    }

    @PostMapping("/{enquiryId}/approve-onebook")
    @PreAuthorize("@perm.has('COMMISSION_MANAGE')")
    public ResponseEntity<CommissionExplorerResponse> approvePayout(
            @PathVariable Long enquiryId,
            Principal principal) {
        String approvedBy = principal != null ? principal.getName() : "system";
        return ResponseEntity.ok(service.approvePayout(enquiryId, approvedBy));
    }

    @PostMapping("/{enquiryId}/payouts")
    @PreAuthorize("@perm.has('COMMISSION_MANAGE')")
    public ResponseEntity<CommissionExplorerResponse> recordPayout(
            @PathVariable Long enquiryId,
            @Valid @RequestBody CommissionPayoutRequest request,
            Principal principal) {
        String paidBy = principal != null ? principal.getName() : "system";
        return ResponseEntity.ok(service.recordPayout(enquiryId, request, paidBy));
    }
}
