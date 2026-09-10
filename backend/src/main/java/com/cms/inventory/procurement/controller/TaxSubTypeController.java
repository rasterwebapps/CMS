package com.cms.inventory.procurement.controller;

import java.util.List;

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
import com.cms.inventory.procurement.dto.TaxSubTypeRequest;
import com.cms.inventory.procurement.dto.TaxSubTypeResponse;
import com.cms.inventory.procurement.service.TaxSubTypeService;

import jakarta.validation.Valid;

/**
 * Rides on {@code INVENTORY_TAX_RULE_VIEW}/{@code _MANAGE} rather than its own permission pair —
 * see {@link TaxSubTypeService}'s Javadoc for why.
 */
@RestController
@RequestMapping("/inventory/procurement/tax-sub-types")
public class TaxSubTypeController {

    private final TaxSubTypeService taxSubTypeService;

    public TaxSubTypeController(TaxSubTypeService taxSubTypeService) {
        this.taxSubTypeService = taxSubTypeService;
    }

    @PostMapping
    @PreAuthorize("@perm.has('INVENTORY_TAX_RULE_MANAGE')")
    public ResponseEntity<TaxSubTypeResponse> create(@Valid @RequestBody TaxSubTypeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taxSubTypeService.create(request));
    }

    @GetMapping
    @PreAuthorize("@perm.hasAny('INVENTORY_TAX_RULE_VIEW', 'INVENTORY_TAX_RULE_MANAGE')")
    public ResponseEntity<List<TaxSubTypeResponse>> findByTaxRule(@RequestParam Long taxRuleId) {
        return ResponseEntity.ok(taxSubTypeService.findByTaxRule(taxRuleId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@perm.hasAny('INVENTORY_TAX_RULE_VIEW', 'INVENTORY_TAX_RULE_MANAGE')")
    public ResponseEntity<TaxSubTypeResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(taxSubTypeService.findById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('INVENTORY_TAX_RULE_MANAGE')")
    public ResponseEntity<TaxSubTypeResponse> update(@PathVariable Long id, @Valid @RequestBody TaxSubTypeRequest request) {
        return ResponseEntity.ok(taxSubTypeService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.has('INVENTORY_TAX_RULE_MANAGE')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        taxSubTypeService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("@perm.has('INVENTORY_TAX_RULE_MANAGE')")
    public ResponseEntity<ActiveStatusUpdateResponse> updateStatus(
            @PathVariable Long id, @Valid @RequestBody ActiveStatusUpdateRequest request) {
        return ResponseEntity.ok(taxSubTypeService.updateStatus(id, request));
    }
}
