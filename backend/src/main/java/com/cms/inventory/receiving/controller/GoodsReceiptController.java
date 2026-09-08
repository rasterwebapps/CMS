package com.cms.inventory.receiving.controller;

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

import com.cms.inventory.receiving.dto.GoodsReceiptAddLineRequest;
import com.cms.inventory.receiving.dto.GoodsReceiptCreateRequest;
import com.cms.inventory.receiving.dto.GoodsReceiptLineResponse;
import com.cms.inventory.receiving.dto.GoodsReceiptResponse;
import com.cms.inventory.receiving.dto.ReceivablePurchaseOrderLineResponse;
import com.cms.inventory.receiving.service.GoodsReceiptService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/inventory/receiving/goods-receipts")
public class GoodsReceiptController {

    private static final String VIEW_ANY =
        "@perm.hasAny('INVENTORY_GRN_VIEW', 'INVENTORY_GRN_MANAGE', 'INVENTORY_GRN_CONFIRM')";

    private final GoodsReceiptService receiptService;

    public GoodsReceiptController(GoodsReceiptService receiptService) {
        this.receiptService = receiptService;
    }

    @PostMapping
    @PreAuthorize("@perm.has('INVENTORY_GRN_MANAGE')")
    public ResponseEntity<GoodsReceiptResponse> create(@Valid @RequestBody GoodsReceiptCreateRequest request, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.status(HttpStatus.CREATED).body(receiptService.create(request, username(jwt)));
    }

    @GetMapping("/page")
    @PreAuthorize(VIEW_ANY)
    public ResponseEntity<Page<GoodsReceiptResponse>> findPage(
            @RequestParam(required = false) Long purchaseOrderId,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 25, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(receiptService.findPage(purchaseOrderId, status, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize(VIEW_ANY)
    public ResponseEntity<GoodsReceiptResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(receiptService.findById(id));
    }

    @GetMapping("/receivable-lines")
    @PreAuthorize("@perm.has('INVENTORY_GRN_MANAGE')")
    public ResponseEntity<List<ReceivablePurchaseOrderLineResponse>> findReceivableLines(@RequestParam Long purchaseOrderId) {
        return ResponseEntity.ok(receiptService.findReceivableLines(purchaseOrderId));
    }

    @PostMapping("/{id}/lines")
    @PreAuthorize("@perm.has('INVENTORY_GRN_MANAGE')")
    public ResponseEntity<GoodsReceiptLineResponse> addLine(@PathVariable Long id, @Valid @RequestBody GoodsReceiptAddLineRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(receiptService.addLine(id, request));
    }

    @DeleteMapping("/{id}/lines/{lineId}")
    @PreAuthorize("@perm.has('INVENTORY_GRN_MANAGE')")
    public ResponseEntity<Void> removeLine(@PathVariable Long id, @PathVariable Long lineId) {
        receiptService.removeLine(id, lineId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("@perm.has('INVENTORY_GRN_CONFIRM')")
    public ResponseEntity<GoodsReceiptResponse> confirm(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(receiptService.confirm(id, username(jwt)));
    }

    private String username(Jwt jwt) {
        return jwt != null ? jwt.getClaimAsString("preferred_username") : "system";
    }
}
