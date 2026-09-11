package com.cms.inventory.catalog.controller;

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
import com.cms.inventory.catalog.dto.BrandRequest;
import com.cms.inventory.catalog.dto.BrandResponse;
import com.cms.inventory.catalog.service.BrandService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/inventory/brands")
public class BrandController {

    private final BrandService brandService;

    public BrandController(BrandService brandService) {
        this.brandService = brandService;
    }

    @PostMapping
    @PreAuthorize("@perm.has('INVENTORY_BRAND_MANAGE')")
    public ResponseEntity<BrandResponse> create(@Valid @RequestBody BrandRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(brandService.create(request));
    }

    // Unpaginated — used to populate the brand picker on the Product form.
    @GetMapping
    @PreAuthorize("@perm.hasAny('INVENTORY_BRAND_VIEW', 'INVENTORY_BRAND_MANAGE')")
    public ResponseEntity<List<BrandResponse>> findAll(
            @RequestParam(required = false, defaultValue = "false") boolean activeOnly) {
        return ResponseEntity.ok(brandService.findAll(activeOnly));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@perm.hasAny('INVENTORY_BRAND_VIEW', 'INVENTORY_BRAND_MANAGE')")
    public ResponseEntity<BrandResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(brandService.findById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('INVENTORY_BRAND_MANAGE')")
    public ResponseEntity<BrandResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody BrandRequest request) {
        return ResponseEntity.ok(brandService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.has('INVENTORY_BRAND_MANAGE')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        brandService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("@perm.has('INVENTORY_BRAND_MANAGE')")
    public ResponseEntity<ActiveStatusUpdateResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody ActiveStatusUpdateRequest request) {
        return ResponseEntity.ok(brandService.updateStatus(id, request));
    }

    @GetMapping("/page")
    @PreAuthorize("@perm.hasAny('INVENTORY_BRAND_VIEW', 'INVENTORY_BRAND_MANAGE')")
    public ResponseEntity<Page<BrandResponse>> findPage(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 25, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(brandService.findPage(search, pageable));
    }

    @GetMapping("/name-exists")
    @PreAuthorize("@perm.has('INVENTORY_BRAND_MANAGE')")
    public ResponseEntity<Boolean> nameExists(
            @RequestParam String value,
            @RequestParam(required = false) Long excludeId) {
        return ResponseEntity.ok(brandService.nameExists(value, excludeId));
    }
}
