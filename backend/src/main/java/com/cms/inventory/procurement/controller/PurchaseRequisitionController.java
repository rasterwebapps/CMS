package com.cms.inventory.procurement.controller;

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

import com.cms.inventory.procurement.dto.PurchaseRequisitionAddLineRequest;
import com.cms.inventory.procurement.dto.PurchaseRequisitionCreateRequest;
import com.cms.inventory.procurement.dto.PurchaseRequisitionItemResponse;
import com.cms.inventory.procurement.dto.PurchaseRequisitionResolutionRequest;
import com.cms.inventory.procurement.dto.PurchaseRequisitionResponse;
import com.cms.inventory.procurement.service.PurchaseRequisitionService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/inventory/procurement/purchase-requisitions")
public class PurchaseRequisitionController {

    private static final String VIEW_ANY =
        "@perm.hasAny('INVENTORY_PURCHASE_REQUISITION_VIEW', 'INVENTORY_PURCHASE_REQUISITION_MANAGE', 'INVENTORY_PURCHASE_REQUISITION_APPROVE')";

    private final PurchaseRequisitionService requisitionService;

    public PurchaseRequisitionController(PurchaseRequisitionService requisitionService) {
        this.requisitionService = requisitionService;
    }

    @PostMapping
    @PreAuthorize("@perm.has('INVENTORY_PURCHASE_REQUISITION_MANAGE')")
    public ResponseEntity<PurchaseRequisitionResponse> create(@Valid @RequestBody PurchaseRequisitionCreateRequest request, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.status(HttpStatus.CREATED).body(requisitionService.create(request, username(jwt)));
    }

    @GetMapping("/page")
    @PreAuthorize(VIEW_ANY)
    public ResponseEntity<Page<PurchaseRequisitionResponse>> findPage(
            @RequestParam(required = false) Long locationId,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 25, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(requisitionService.findPage(locationId, status, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize(VIEW_ANY)
    public ResponseEntity<PurchaseRequisitionResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(requisitionService.findById(id));
    }

    @PostMapping("/{id}/lines")
    @PreAuthorize("@perm.has('INVENTORY_PURCHASE_REQUISITION_MANAGE')")
    public ResponseEntity<PurchaseRequisitionItemResponse> addLine(@PathVariable Long id, @Valid @RequestBody PurchaseRequisitionAddLineRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(requisitionService.addLine(id, request));
    }

    @DeleteMapping("/{id}/lines/{lineId}")
    @PreAuthorize("@perm.has('INVENTORY_PURCHASE_REQUISITION_MANAGE')")
    public ResponseEntity<Void> removeLine(@PathVariable Long id, @PathVariable Long lineId) {
        requisitionService.removeLine(id, lineId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("@perm.has('INVENTORY_PURCHASE_REQUISITION_MANAGE')")
    public ResponseEntity<PurchaseRequisitionResponse> submit(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(requisitionService.submit(id, username(jwt)));
    }

    @PostMapping("/{id}/lines/{lineId}/approve")
    @PreAuthorize("@perm.has('INVENTORY_PURCHASE_REQUISITION_APPROVE')")
    public ResponseEntity<PurchaseRequisitionItemResponse> approveLine(
            @PathVariable Long id, @PathVariable Long lineId,
            @Valid @RequestBody(required = false) PurchaseRequisitionResolutionRequest request, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(requisitionService.approveLine(id, lineId, request != null ? request : new PurchaseRequisitionResolutionRequest(null), username(jwt)));
    }

    @PostMapping("/{id}/lines/{lineId}/reject")
    @PreAuthorize("@perm.has('INVENTORY_PURCHASE_REQUISITION_APPROVE')")
    public ResponseEntity<PurchaseRequisitionItemResponse> rejectLine(
            @PathVariable Long id, @PathVariable Long lineId,
            @Valid @RequestBody(required = false) PurchaseRequisitionResolutionRequest request, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(requisitionService.rejectLine(id, lineId, request != null ? request : new PurchaseRequisitionResolutionRequest(null), username(jwt)));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("@perm.has('INVENTORY_PURCHASE_REQUISITION_MANAGE')")
    public ResponseEntity<Void> cancel(@PathVariable Long id) {
        requisitionService.cancel(id);
        return ResponseEntity.noContent().build();
    }

    private String username(Jwt jwt) {
        return jwt != null ? jwt.getClaimAsString("preferred_username") : "system";
    }
}
