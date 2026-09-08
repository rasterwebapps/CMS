package com.cms.inventory.procurement.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cms.dto.ActiveStatusUpdateRequest;
import com.cms.dto.ActiveStatusUpdateResponse;
import com.cms.inventory.procurement.dto.SupplierRequest;
import com.cms.inventory.procurement.dto.SupplierResponse;
import com.cms.inventory.procurement.service.SupplierService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/inventory/procurement/suppliers")
public class SupplierController {

    private final SupplierService supplierService;

    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @PostMapping
    @PreAuthorize("@perm.has('INVENTORY_SUPPLIER_MANAGE')")
    public ResponseEntity<SupplierResponse> create(@Valid @RequestBody SupplierRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(supplierService.create(request));
    }

    // Unpaginated — used to populate the Supplier picker on the Rate Contract form (and the
    // future Requisition/PO forms).
    @GetMapping
    @PreAuthorize("@perm.hasAny('INVENTORY_SUPPLIER_VIEW', 'INVENTORY_SUPPLIER_MANAGE')")
    public ResponseEntity<List<SupplierResponse>> findAll(
            @RequestParam(required = false, defaultValue = "false") boolean activeOnly) {
        return ResponseEntity.ok(supplierService.findAll(activeOnly));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@perm.hasAny('INVENTORY_SUPPLIER_VIEW', 'INVENTORY_SUPPLIER_MANAGE')")
    public ResponseEntity<SupplierResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(supplierService.findById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('INVENTORY_SUPPLIER_MANAGE')")
    public ResponseEntity<SupplierResponse> update(@PathVariable Long id, @Valid @RequestBody SupplierRequest request) {
        return ResponseEntity.ok(supplierService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.has('INVENTORY_SUPPLIER_MANAGE')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        supplierService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("@perm.has('INVENTORY_SUPPLIER_MANAGE')")
    public ResponseEntity<ActiveStatusUpdateResponse> updateStatus(
            @PathVariable Long id, @Valid @RequestBody ActiveStatusUpdateRequest request) {
        return ResponseEntity.ok(supplierService.updateStatus(id, request));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("@perm.has('INVENTORY_SUPPLIER_APPROVE')")
    public ResponseEntity<SupplierResponse> approve(@PathVariable Long id) {
        return ResponseEntity.ok(supplierService.approve(id));
    }

    @GetMapping("/page")
    @PreAuthorize("@perm.hasAny('INVENTORY_SUPPLIER_VIEW', 'INVENTORY_SUPPLIER_MANAGE')")
    public ResponseEntity<Page<SupplierResponse>> findPage(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 25, sort = "supplierName", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(supplierService.findPage(search, pageable));
    }

    @GetMapping("/code-exists")
    @PreAuthorize("@perm.has('INVENTORY_SUPPLIER_MANAGE')")
    public ResponseEntity<Boolean> codeExists(@RequestParam String value, @RequestParam(required = false) Long excludeId) {
        return ResponseEntity.ok(supplierService.codeExists(value, excludeId));
    }
}
