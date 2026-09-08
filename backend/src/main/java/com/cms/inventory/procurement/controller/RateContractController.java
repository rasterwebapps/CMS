package com.cms.inventory.procurement.controller;

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
import com.cms.inventory.procurement.dto.RateContractRequest;
import com.cms.inventory.procurement.dto.RateContractResponse;
import com.cms.inventory.procurement.service.RateContractService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/inventory/procurement/rate-contracts")
public class RateContractController {

    private final RateContractService rateContractService;

    public RateContractController(RateContractService rateContractService) {
        this.rateContractService = rateContractService;
    }

    @PostMapping
    @PreAuthorize("@perm.has('INVENTORY_RATE_CONTRACT_MANAGE')")
    public ResponseEntity<RateContractResponse> create(@Valid @RequestBody RateContractRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(rateContractService.create(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@perm.hasAny('INVENTORY_RATE_CONTRACT_VIEW', 'INVENTORY_RATE_CONTRACT_MANAGE')")
    public ResponseEntity<RateContractResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(rateContractService.findById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('INVENTORY_RATE_CONTRACT_MANAGE')")
    public ResponseEntity<RateContractResponse> update(@PathVariable Long id, @Valid @RequestBody RateContractRequest request) {
        return ResponseEntity.ok(rateContractService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.has('INVENTORY_RATE_CONTRACT_MANAGE')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        rateContractService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("@perm.has('INVENTORY_RATE_CONTRACT_MANAGE')")
    public ResponseEntity<ActiveStatusUpdateResponse> updateStatus(
            @PathVariable Long id, @Valid @RequestBody ActiveStatusUpdateRequest request) {
        return ResponseEntity.ok(rateContractService.updateStatus(id, request));
    }

    @GetMapping("/page")
    @PreAuthorize("@perm.hasAny('INVENTORY_RATE_CONTRACT_VIEW', 'INVENTORY_RATE_CONTRACT_MANAGE')")
    public ResponseEntity<Page<RateContractResponse>> findPage(
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) Boolean activeOnly,
            @PageableDefault(size = 25, sort = "startDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(rateContractService.findPage(supplierId, activeOnly, pageable));
    }
}
