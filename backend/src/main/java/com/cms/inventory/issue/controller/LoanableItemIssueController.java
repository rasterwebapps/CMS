package com.cms.inventory.issue.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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

import com.cms.inventory.issue.dto.LoanableItemIssueCreateRequest;
import com.cms.inventory.issue.dto.LoanableItemIssueResponse;
import com.cms.inventory.issue.dto.LoanableItemIssueReturnRequest;
import com.cms.inventory.issue.service.LoanableItemIssueService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/inventory/issue/loanable-item-issues")
public class LoanableItemIssueController {

    private static final String VIEW_ANY =
        "@perm.hasAny('INVENTORY_LOAN_ISSUE_VIEW', 'INVENTORY_LOAN_ISSUE_MANAGE', 'INVENTORY_LOAN_ISSUE_RETURN')";

    private final LoanableItemIssueService issueService;

    public LoanableItemIssueController(LoanableItemIssueService issueService) {
        this.issueService = issueService;
    }

    @PostMapping
    @PreAuthorize("@perm.has('INVENTORY_LOAN_ISSUE_MANAGE')")
    public ResponseEntity<LoanableItemIssueResponse> create(@Valid @RequestBody LoanableItemIssueCreateRequest request, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.status(HttpStatus.CREATED).body(issueService.create(request, username(jwt)));
    }

    @GetMapping("/page")
    @PreAuthorize(VIEW_ANY)
    public ResponseEntity<Page<LoanableItemIssueResponse>> findPage(
            @RequestParam(required = false) Long locationId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean overdueOnly,
            @PageableDefault(size = 25, sort = "issuedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(issueService.findPage(locationId, status, overdueOnly, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize(VIEW_ANY)
    public ResponseEntity<LoanableItemIssueResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(issueService.findById(id));
    }

    @PostMapping("/{id}/return")
    @PreAuthorize("@perm.has('INVENTORY_LOAN_ISSUE_RETURN')")
    public ResponseEntity<LoanableItemIssueResponse> markReturned(
            @PathVariable Long id, @Valid @RequestBody(required = false) LoanableItemIssueReturnRequest request, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(issueService.markReturned(id, request, username(jwt)));
    }

    private String username(Jwt jwt) {
        return jwt != null ? jwt.getClaimAsString("preferred_username") : "system";
    }
}
