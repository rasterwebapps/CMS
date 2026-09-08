package com.cms.inventory.approval.controller;

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

import com.cms.inventory.approval.dto.ApprovalActionResolutionRequest;
import com.cms.inventory.approval.dto.ApprovalInstanceResponse;
import com.cms.inventory.approval.dto.ApprovalInstanceStartRequest;
import com.cms.inventory.approval.service.ApprovalInstanceService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/inventory/approval/instances")
public class ApprovalInstanceController {

    private static final String VIEW_ANY = "@perm.hasAny('INVENTORY_APPROVAL_VIEW', 'INVENTORY_APPROVAL_ACT')";

    private final ApprovalInstanceService instanceService;

    public ApprovalInstanceController(ApprovalInstanceService instanceService) {
        this.instanceService = instanceService;
    }

    @PostMapping("/start")
    @PreAuthorize("@perm.hasAny('INVENTORY_PURCHASE_REQUISITION_MANAGE', 'INVENTORY_PURCHASE_ORDER_MANAGE')")
    public ResponseEntity<ApprovalInstanceResponse> start(@Valid @RequestBody ApprovalInstanceStartRequest request, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.status(HttpStatus.CREATED).body(instanceService.start(request, username(jwt)));
    }

    @GetMapping("/page")
    @PreAuthorize(VIEW_ANY)
    public ResponseEntity<Page<ApprovalInstanceResponse>> findPage(
            @RequestParam(required = false) String documentType,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 25, sort = "initiatedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(instanceService.findPage(documentType, status, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize(VIEW_ANY)
    public ResponseEntity<ApprovalInstanceResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(instanceService.findById(id));
    }

    @PostMapping("/{id}/actions/{actionId}/approve")
    @PreAuthorize("@perm.has('INVENTORY_APPROVAL_ACT')")
    public ResponseEntity<ApprovalInstanceResponse> approveAction(
            @PathVariable Long id, @PathVariable Long actionId,
            @Valid @RequestBody(required = false) ApprovalActionResolutionRequest request, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(instanceService.approveAction(id, actionId, request, username(jwt)));
    }

    @PostMapping("/{id}/actions/{actionId}/reject")
    @PreAuthorize("@perm.has('INVENTORY_APPROVAL_ACT')")
    public ResponseEntity<ApprovalInstanceResponse> rejectAction(
            @PathVariable Long id, @PathVariable Long actionId,
            @Valid @RequestBody(required = false) ApprovalActionResolutionRequest request, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(instanceService.rejectAction(id, actionId, request, username(jwt)));
    }

    private String username(Jwt jwt) {
        return jwt != null ? jwt.getClaimAsString("preferred_username") : "system";
    }
}
