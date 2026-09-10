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
import com.cms.inventory.procurement.dto.TaxTypeRequest;
import com.cms.inventory.procurement.dto.TaxTypeResponse;
import com.cms.inventory.procurement.service.TaxTypeService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/inventory/procurement/tax-types")
public class TaxTypeController {

    private final TaxTypeService taxTypeService;

    public TaxTypeController(TaxTypeService taxTypeService) {
        this.taxTypeService = taxTypeService;
    }

    @PostMapping
    @PreAuthorize("@perm.has('INVENTORY_TAX_TYPE_MANAGE')")
    public ResponseEntity<TaxTypeResponse> create(@Valid @RequestBody TaxTypeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taxTypeService.create(request));
    }

    // Unpaginated — used to populate the Tax Type picker on the Tax form.
    @GetMapping
    @PreAuthorize("@perm.hasAny('INVENTORY_TAX_TYPE_VIEW', 'INVENTORY_TAX_TYPE_MANAGE')")
    public ResponseEntity<List<TaxTypeResponse>> findAll(
            @RequestParam(required = false, defaultValue = "false") boolean activeOnly) {
        return ResponseEntity.ok(taxTypeService.findAll(activeOnly));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@perm.hasAny('INVENTORY_TAX_TYPE_VIEW', 'INVENTORY_TAX_TYPE_MANAGE')")
    public ResponseEntity<TaxTypeResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(taxTypeService.findById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('INVENTORY_TAX_TYPE_MANAGE')")
    public ResponseEntity<TaxTypeResponse> update(@PathVariable Long id, @Valid @RequestBody TaxTypeRequest request) {
        return ResponseEntity.ok(taxTypeService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.has('INVENTORY_TAX_TYPE_MANAGE')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        taxTypeService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("@perm.has('INVENTORY_TAX_TYPE_MANAGE')")
    public ResponseEntity<ActiveStatusUpdateResponse> updateStatus(
            @PathVariable Long id, @Valid @RequestBody ActiveStatusUpdateRequest request) {
        return ResponseEntity.ok(taxTypeService.updateStatus(id, request));
    }

    @GetMapping("/page")
    @PreAuthorize("@perm.hasAny('INVENTORY_TAX_TYPE_VIEW', 'INVENTORY_TAX_TYPE_MANAGE')")
    public ResponseEntity<Page<TaxTypeResponse>> findPage(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 25, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(taxTypeService.findPage(search, pageable));
    }

    @GetMapping("/name-exists")
    @PreAuthorize("@perm.has('INVENTORY_TAX_TYPE_MANAGE')")
    public ResponseEntity<Boolean> nameExists(@RequestParam String value, @RequestParam(required = false) Long excludeId) {
        return ResponseEntity.ok(taxTypeService.nameExists(value, excludeId));
    }
}
