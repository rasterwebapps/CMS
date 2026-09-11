package com.cms.inventory.catalog.controller;

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
import com.cms.inventory.catalog.dto.ProductVariantRequest;
import com.cms.inventory.catalog.dto.ProductVariantResponse;
import com.cms.inventory.catalog.service.ProductVariantService;

import jakarta.validation.Valid;

/** CRUD, always in the context of the owning Product — a variant is only ever created/edited from
 *  its parent product's own screen, same posture as {@code ProductImageController}. Barcode
 *  lookup/label-printing live in {@link ProductVariantLookupController} instead, since a scan
 *  workflow doesn't know the parent productId upfront. */
@RestController
@RequestMapping("/inventory/products/{productId}/variants")
public class ProductVariantController {

    private final ProductVariantService variantService;

    public ProductVariantController(ProductVariantService variantService) {
        this.variantService = variantService;
    }

    @PostMapping
    @PreAuthorize("@perm.has('INVENTORY_PRODUCT_VARIANT_MANAGE')")
    public ResponseEntity<ProductVariantResponse> create(
            @PathVariable Long productId,
            @Valid @RequestBody ProductVariantRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(variantService.create(productId, request));
    }

    @GetMapping
    @PreAuthorize("@perm.hasAny('INVENTORY_PRODUCT_VARIANT_VIEW', 'INVENTORY_PRODUCT_VARIANT_MANAGE')")
    public ResponseEntity<List<ProductVariantResponse>> findByProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(variantService.findByProduct(productId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@perm.hasAny('INVENTORY_PRODUCT_VARIANT_VIEW', 'INVENTORY_PRODUCT_VARIANT_MANAGE')")
    public ResponseEntity<ProductVariantResponse> findById(@PathVariable Long productId, @PathVariable Long id) {
        return ResponseEntity.ok(variantService.findById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('INVENTORY_PRODUCT_VARIANT_MANAGE')")
    public ResponseEntity<ProductVariantResponse> update(
            @PathVariable Long productId,
            @PathVariable Long id,
            @Valid @RequestBody ProductVariantRequest request) {
        return ResponseEntity.ok(variantService.update(productId, id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.has('INVENTORY_PRODUCT_VARIANT_MANAGE')")
    public ResponseEntity<Void> delete(@PathVariable Long productId, @PathVariable Long id) {
        variantService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("@perm.has('INVENTORY_PRODUCT_VARIANT_MANAGE')")
    public ResponseEntity<ActiveStatusUpdateResponse> updateStatus(
            @PathVariable Long productId,
            @PathVariable Long id,
            @Valid @RequestBody ActiveStatusUpdateRequest request) {
        return ResponseEntity.ok(variantService.updateStatus(id, request));
    }

    @GetMapping("/code-exists")
    @PreAuthorize("@perm.has('INVENTORY_PRODUCT_VARIANT_MANAGE')")
    public ResponseEntity<Boolean> codeExists(
            @PathVariable Long productId,
            @RequestParam String value,
            @RequestParam(required = false) Long excludeId) {
        return ResponseEntity.ok(variantService.codeExists(value, excludeId));
    }

    @GetMapping("/barcode-exists")
    @PreAuthorize("@perm.has('INVENTORY_PRODUCT_VARIANT_MANAGE')")
    public ResponseEntity<Boolean> barcodeExists(
            @PathVariable Long productId,
            @RequestParam String value,
            @RequestParam(required = false) Long excludeId) {
        return ResponseEntity.ok(variantService.barcodeExists(value, excludeId));
    }
}
