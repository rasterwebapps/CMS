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

import com.cms.inventory.receiving.dto.ReturnableGoodsReceiptLineResponse;
import com.cms.inventory.receiving.dto.SupplierReturnAddLineRequest;
import com.cms.inventory.receiving.dto.SupplierReturnCreateRequest;
import com.cms.inventory.receiving.dto.SupplierReturnLineResponse;
import com.cms.inventory.receiving.dto.SupplierReturnResponse;
import com.cms.inventory.receiving.service.SupplierReturnService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/inventory/receiving/supplier-returns")
public class SupplierReturnController {

    private static final String VIEW_ANY = "@perm.hasAny('INVENTORY_SUPPLIER_RETURN_VIEW', 'INVENTORY_SUPPLIER_RETURN_MANAGE')";

    private final SupplierReturnService returnService;

    public SupplierReturnController(SupplierReturnService returnService) {
        this.returnService = returnService;
    }

    @PostMapping
    @PreAuthorize("@perm.has('INVENTORY_SUPPLIER_RETURN_MANAGE')")
    public ResponseEntity<SupplierReturnResponse> create(@Valid @RequestBody SupplierReturnCreateRequest request, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.status(HttpStatus.CREATED).body(returnService.create(request, username(jwt)));
    }

    @GetMapping("/page")
    @PreAuthorize(VIEW_ANY)
    public ResponseEntity<Page<SupplierReturnResponse>> findPage(
            @RequestParam(required = false) Long goodsReceiptId,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 25, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(returnService.findPage(goodsReceiptId, status, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize(VIEW_ANY)
    public ResponseEntity<SupplierReturnResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(returnService.findById(id));
    }

    @GetMapping("/returnable-lines")
    @PreAuthorize("@perm.has('INVENTORY_SUPPLIER_RETURN_MANAGE')")
    public ResponseEntity<List<ReturnableGoodsReceiptLineResponse>> findReturnableLines(@RequestParam Long goodsReceiptId) {
        return ResponseEntity.ok(returnService.findReturnableLines(goodsReceiptId));
    }

    @PostMapping("/{id}/lines")
    @PreAuthorize("@perm.has('INVENTORY_SUPPLIER_RETURN_MANAGE')")
    public ResponseEntity<SupplierReturnLineResponse> addLine(@PathVariable Long id, @Valid @RequestBody SupplierReturnAddLineRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(returnService.addLine(id, request));
    }

    @DeleteMapping("/{id}/lines/{lineId}")
    @PreAuthorize("@perm.has('INVENTORY_SUPPLIER_RETURN_MANAGE')")
    public ResponseEntity<Void> removeLine(@PathVariable Long id, @PathVariable Long lineId) {
        returnService.removeLine(id, lineId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("@perm.has('INVENTORY_SUPPLIER_RETURN_MANAGE')")
    public ResponseEntity<SupplierReturnResponse> complete(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(returnService.complete(id, username(jwt)));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("@perm.has('INVENTORY_SUPPLIER_RETURN_MANAGE')")
    public ResponseEntity<Void> cancel(@PathVariable Long id) {
        returnService.cancel(id);
        return ResponseEntity.noContent().build();
    }

    private String username(Jwt jwt) {
        return jwt != null ? jwt.getClaimAsString("preferred_username") : "system";
    }
}
