package com.cms.inventory.approval.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cms.inventory.approval.dto.ApprovalWorkflowRequest;
import com.cms.inventory.approval.dto.ApprovalWorkflowResponse;
import com.cms.inventory.approval.service.ApprovalWorkflowService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/inventory/approval/workflows")
public class ApprovalWorkflowController {

    private static final String VIEW_ANY = "@perm.hasAny('INVENTORY_APPROVAL_WORKFLOW_VIEW', 'INVENTORY_APPROVAL_WORKFLOW_MANAGE')";

    private final ApprovalWorkflowService workflowService;

    public ApprovalWorkflowController(ApprovalWorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @PostMapping
    @PreAuthorize("@perm.has('INVENTORY_APPROVAL_WORKFLOW_MANAGE')")
    public ResponseEntity<ApprovalWorkflowResponse> create(@Valid @RequestBody ApprovalWorkflowRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(workflowService.create(request));
    }

    @GetMapping("/page")
    @PreAuthorize(VIEW_ANY)
    public ResponseEntity<Page<ApprovalWorkflowResponse>> findPage(
            @RequestParam(required = false) String documentType,
            @RequestParam(required = false) Boolean activeOnly,
            @PageableDefault(size = 25, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(workflowService.findPage(documentType, activeOnly, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize(VIEW_ANY)
    public ResponseEntity<ApprovalWorkflowResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(workflowService.findById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('INVENTORY_APPROVAL_WORKFLOW_MANAGE')")
    public ResponseEntity<ApprovalWorkflowResponse> update(@PathVariable Long id, @Valid @RequestBody ApprovalWorkflowRequest request) {
        return ResponseEntity.ok(workflowService.update(id, request));
    }

    @GetMapping("/eligible")
    @PreAuthorize("@perm.hasAny('INVENTORY_PURCHASE_REQUISITION_MANAGE', 'INVENTORY_PURCHASE_ORDER_MANAGE')")
    public ResponseEntity<List<ApprovalWorkflowResponse>> findEligible(
            @RequestParam String documentType,
            @RequestParam Long locationId,
            @RequestParam(required = false) BigDecimal amount) {
        return ResponseEntity.ok(workflowService.findEligible(documentType, locationId, amount));
    }
}
