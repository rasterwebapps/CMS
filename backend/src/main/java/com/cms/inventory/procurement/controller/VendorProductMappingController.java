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
import com.cms.inventory.procurement.dto.VendorProductMappingRequest;
import com.cms.inventory.procurement.dto.VendorProductMappingResponse;
import com.cms.inventory.procurement.service.VendorProductMappingService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/inventory/procurement/vendor-product-mappings")
public class VendorProductMappingController {

    private final VendorProductMappingService mappingService;

    public VendorProductMappingController(VendorProductMappingService mappingService) {
        this.mappingService = mappingService;
    }

    @PostMapping
    @PreAuthorize("@perm.has('INVENTORY_VENDOR_PRODUCT_MAPPING_MANAGE')")
    public ResponseEntity<VendorProductMappingResponse> create(@Valid @RequestBody VendorProductMappingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(mappingService.create(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@perm.hasAny('INVENTORY_VENDOR_PRODUCT_MAPPING_VIEW', 'INVENTORY_VENDOR_PRODUCT_MAPPING_MANAGE')")
    public ResponseEntity<VendorProductMappingResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(mappingService.findById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('INVENTORY_VENDOR_PRODUCT_MAPPING_MANAGE')")
    public ResponseEntity<VendorProductMappingResponse> update(@PathVariable Long id, @Valid @RequestBody VendorProductMappingRequest request) {
        return ResponseEntity.ok(mappingService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.has('INVENTORY_VENDOR_PRODUCT_MAPPING_MANAGE')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        mappingService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("@perm.has('INVENTORY_VENDOR_PRODUCT_MAPPING_MANAGE')")
    public ResponseEntity<ActiveStatusUpdateResponse> updateStatus(
            @PathVariable Long id, @Valid @RequestBody ActiveStatusUpdateRequest request) {
        return ResponseEntity.ok(mappingService.updateStatus(id, request));
    }

    @GetMapping("/page")
    @PreAuthorize("@perm.hasAny('INVENTORY_VENDOR_PRODUCT_MAPPING_VIEW', 'INVENTORY_VENDOR_PRODUCT_MAPPING_MANAGE')")
    public ResponseEntity<Page<VendorProductMappingResponse>> findPage(
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Boolean activeOnly,
            @PageableDefault(size = 25, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(mappingService.findPage(supplierId, productId, activeOnly, pageable));
    }

    // Query param names (value/excludeId/scoped extra param) match the shared uniqueFieldValidator
    // convention on the frontend (see Product's name-exists), so the form reuses it directly rather
    // than a bespoke async validator: value = productId (the field being checked), supplierId = scope.
    @GetMapping("/pair-exists")
    @PreAuthorize("@perm.hasAny('INVENTORY_VENDOR_PRODUCT_MAPPING_VIEW', 'INVENTORY_VENDOR_PRODUCT_MAPPING_MANAGE')")
    public ResponseEntity<Boolean> pairExists(
            @RequestParam Long value, @RequestParam Long supplierId, @RequestParam(required = false) Long excludeId) {
        return ResponseEntity.ok(mappingService.pairExists(supplierId, value, excludeId));
    }
}
