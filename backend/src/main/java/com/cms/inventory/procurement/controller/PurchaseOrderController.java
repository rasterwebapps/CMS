package com.cms.inventory.procurement.controller;

import java.util.List;

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

import com.cms.inventory.procurement.dto.PurchaseOrderAddLineRequest;
import com.cms.inventory.procurement.dto.PurchaseOrderCreateRequest;
import com.cms.inventory.procurement.dto.PurchaseOrderForceCloseRequest;
import com.cms.inventory.procurement.dto.PurchaseOrderItemResponse;
import com.cms.inventory.procurement.dto.PurchaseOrderResponse;
import com.cms.inventory.procurement.dto.PurchaseRequisitionItemResponse;
import com.cms.inventory.procurement.service.PurchaseOrderService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/inventory/procurement/purchase-orders")
public class PurchaseOrderController {

    private static final String VIEW_ANY =
        "@perm.hasAny('INVENTORY_PURCHASE_ORDER_VIEW', 'INVENTORY_PURCHASE_ORDER_MANAGE', 'INVENTORY_PURCHASE_ORDER_FORCE_CLOSE')";

    private final PurchaseOrderService orderService;

    public PurchaseOrderController(PurchaseOrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @PreAuthorize("@perm.has('INVENTORY_PURCHASE_ORDER_MANAGE')")
    public ResponseEntity<PurchaseOrderResponse> create(@Valid @RequestBody PurchaseOrderCreateRequest request, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.create(request, username(jwt)));
    }

    @GetMapping("/page")
    @PreAuthorize(VIEW_ANY)
    public ResponseEntity<Page<PurchaseOrderResponse>> findPage(
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) Long locationId,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 25, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(orderService.findPage(supplierId, locationId, status, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize(VIEW_ANY)
    public ResponseEntity<PurchaseOrderResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.findById(id));
    }

    @GetMapping("/available-requisition-lines")
    @PreAuthorize("@perm.has('INVENTORY_PURCHASE_ORDER_MANAGE')")
    public ResponseEntity<List<PurchaseRequisitionItemResponse>> findAvailableRequisitionLines(@RequestParam Long locationId) {
        return ResponseEntity.ok(orderService.findAvailableRequisitionLines(locationId));
    }

    @PostMapping("/{id}/lines")
    @PreAuthorize("@perm.has('INVENTORY_PURCHASE_ORDER_MANAGE')")
    public ResponseEntity<PurchaseOrderItemResponse> addLine(@PathVariable Long id, @Valid @RequestBody PurchaseOrderAddLineRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.addLine(id, request));
    }

    @DeleteMapping("/{id}/lines/{lineId}")
    @PreAuthorize("@perm.has('INVENTORY_PURCHASE_ORDER_MANAGE')")
    public ResponseEntity<Void> removeLine(@PathVariable Long id, @PathVariable Long lineId) {
        orderService.removeLine(id, lineId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/order")
    @PreAuthorize("@perm.has('INVENTORY_PURCHASE_ORDER_MANAGE')")
    public ResponseEntity<PurchaseOrderResponse> order(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(orderService.order(id, username(jwt)));
    }

    @PostMapping("/{id}/force-close")
    @PreAuthorize("@perm.has('INVENTORY_PURCHASE_ORDER_FORCE_CLOSE')")
    public ResponseEntity<PurchaseOrderResponse> forceClose(
            @PathVariable Long id, @Valid @RequestBody PurchaseOrderForceCloseRequest request, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(orderService.forceClose(id, request, username(jwt)));
    }

    private String username(Jwt jwt) {
        return jwt != null ? jwt.getClaimAsString("preferred_username") : "system";
    }
}
