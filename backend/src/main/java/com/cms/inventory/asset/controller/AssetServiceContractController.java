package com.cms.inventory.asset.controller;

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

import com.cms.inventory.asset.dto.AssetServiceContractRequest;
import com.cms.inventory.asset.dto.AssetServiceContractResponse;
import com.cms.inventory.asset.service.AssetServiceContractService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/inventory/asset/service-contracts")
public class AssetServiceContractController {

    private static final String VIEW_ANY = "@perm.hasAny('INVENTORY_ASSET_MAINTENANCE_VIEW', 'INVENTORY_ASSET_MAINTENANCE_MANAGE')";

    private final AssetServiceContractService contractService;

    public AssetServiceContractController(AssetServiceContractService contractService) {
        this.contractService = contractService;
    }

    @PostMapping
    @PreAuthorize("@perm.has('INVENTORY_ASSET_MAINTENANCE_MANAGE')")
    public ResponseEntity<AssetServiceContractResponse> create(@Valid @RequestBody AssetServiceContractRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(contractService.create(request));
    }

    @GetMapping("/page")
    @PreAuthorize(VIEW_ANY)
    public ResponseEntity<Page<AssetServiceContractResponse>> findPage(
            @RequestParam(required = false) Long assetId,
            @RequestParam(required = false) Boolean activeOnly,
            @PageableDefault(size = 25, sort = "startDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(contractService.findPage(assetId, activeOnly, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize(VIEW_ANY)
    public ResponseEntity<AssetServiceContractResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(contractService.findById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('INVENTORY_ASSET_MAINTENANCE_MANAGE')")
    public ResponseEntity<AssetServiceContractResponse> update(@PathVariable Long id, @Valid @RequestBody AssetServiceContractRequest request) {
        return ResponseEntity.ok(contractService.update(id, request));
    }
}
