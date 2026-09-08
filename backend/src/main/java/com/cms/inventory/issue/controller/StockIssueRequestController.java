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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cms.inventory.issue.dto.StockIssueRequestAddLineRequest;
import com.cms.inventory.issue.dto.StockIssueRequestCreateRequest;
import com.cms.inventory.issue.dto.StockIssueRequestItemResponse;
import com.cms.inventory.issue.dto.StockIssueRequestResolutionRequest;
import com.cms.inventory.issue.dto.StockIssueRequestResponse;
import com.cms.inventory.issue.dto.StockIssueRequestReturnLineRequest;
import com.cms.inventory.issue.service.StockIssueRequestService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/inventory/issue/stock-issue-requests")
public class StockIssueRequestController {

    private static final String VIEW_ANY =
        "@perm.hasAny('INVENTORY_ISSUE_REQUEST_VIEW', 'INVENTORY_ISSUE_REQUEST_MANAGE', 'INVENTORY_ISSUE_REQUEST_APPROVE')";

    private final StockIssueRequestService requestService;

    public StockIssueRequestController(StockIssueRequestService requestService) {
        this.requestService = requestService;
    }

    @PostMapping
    @PreAuthorize("@perm.has('INVENTORY_ISSUE_REQUEST_MANAGE')")
    public ResponseEntity<StockIssueRequestResponse> create(@Valid @RequestBody StockIssueRequestCreateRequest request, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.status(HttpStatus.CREATED).body(requestService.create(request, username(jwt)));
    }

    @GetMapping("/page")
    @PreAuthorize(VIEW_ANY)
    public ResponseEntity<Page<StockIssueRequestResponse>> findPage(
            @RequestParam(required = false) Long locationId,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 25, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(requestService.findPage(locationId, status, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize(VIEW_ANY)
    public ResponseEntity<StockIssueRequestResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(requestService.findById(id));
    }

    @PostMapping("/{id}/lines")
    @PreAuthorize("@perm.has('INVENTORY_ISSUE_REQUEST_MANAGE')")
    public ResponseEntity<StockIssueRequestItemResponse> addLine(@PathVariable Long id, @Valid @RequestBody StockIssueRequestAddLineRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(requestService.addLine(id, request));
    }

    @DeleteMapping("/{id}/lines/{lineId}")
    @PreAuthorize("@perm.has('INVENTORY_ISSUE_REQUEST_MANAGE')")
    public ResponseEntity<Void> removeLine(@PathVariable Long id, @PathVariable Long lineId) {
        requestService.removeLine(id, lineId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("@perm.has('INVENTORY_ISSUE_REQUEST_MANAGE')")
    public ResponseEntity<StockIssueRequestResponse> submit(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(requestService.submit(id, username(jwt)));
    }

    @PostMapping("/{id}/lines/{lineId}/approve")
    @PreAuthorize("@perm.has('INVENTORY_ISSUE_REQUEST_APPROVE')")
    public ResponseEntity<StockIssueRequestItemResponse> approveLine(
            @PathVariable Long id, @PathVariable Long lineId,
            @Valid @RequestBody(required = false) StockIssueRequestResolutionRequest request, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(requestService.approveLine(id, lineId, request != null ? request : new StockIssueRequestResolutionRequest(null), username(jwt)));
    }

    @PostMapping("/{id}/lines/{lineId}/reject")
    @PreAuthorize("@perm.has('INVENTORY_ISSUE_REQUEST_APPROVE')")
    public ResponseEntity<StockIssueRequestItemResponse> rejectLine(
            @PathVariable Long id, @PathVariable Long lineId,
            @Valid @RequestBody(required = false) StockIssueRequestResolutionRequest request, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(requestService.rejectLine(id, lineId, request != null ? request : new StockIssueRequestResolutionRequest(null), username(jwt)));
    }

    @PostMapping("/{id}/lines/{lineId}/return")
    @PreAuthorize("@perm.has('INVENTORY_ISSUE_REQUEST_RETURN')")
    public ResponseEntity<StockIssueRequestItemResponse> returnLine(
            @PathVariable Long id, @PathVariable Long lineId,
            @Valid @RequestBody StockIssueRequestReturnLineRequest request, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(requestService.returnLine(id, lineId, request, username(jwt)));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("@perm.has('INVENTORY_ISSUE_REQUEST_MANAGE')")
    public ResponseEntity<Void> cancel(@PathVariable Long id) {
        requestService.cancel(id);
        return ResponseEntity.noContent().build();
    }

    private String username(Jwt jwt) {
        return jwt != null ? jwt.getClaimAsString("preferred_username") : "system";
    }
}
