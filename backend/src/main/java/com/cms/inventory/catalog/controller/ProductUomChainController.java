package com.cms.inventory.catalog.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cms.inventory.catalog.dto.ProductUomChainSaveRequest;
import com.cms.inventory.catalog.dto.ProductUomChainVersionResponse;
import com.cms.inventory.catalog.service.ProductUomChainService;

import jakarta.validation.Valid;

/** Same permission tier as {@link ProductController} — the chain is an embedded section of the
 * Product edit form, not a separate screen, so it uses the existing Product-manage permission
 * rather than a new dedicated one. */
@RestController
@RequestMapping("/inventory/products/{productId}/uom-chain")
public class ProductUomChainController {

    private final ProductUomChainService chainService;

    public ProductUomChainController(ProductUomChainService chainService) {
        this.chainService = chainService;
    }

    @GetMapping("/active")
    @PreAuthorize("@perm.hasAny('INVENTORY_PRODUCT_VIEW', 'INVENTORY_PRODUCT_MANAGE')")
    public ResponseEntity<ProductUomChainVersionResponse> getActiveVersion(@PathVariable Long productId) {
        return ResponseEntity.ok(chainService.getActiveVersion(productId));
    }

    @GetMapping("/versions")
    @PreAuthorize("@perm.hasAny('INVENTORY_PRODUCT_VIEW', 'INVENTORY_PRODUCT_MANAGE')")
    public ResponseEntity<List<ProductUomChainVersionResponse>> listVersions(@PathVariable Long productId) {
        return ResponseEntity.ok(chainService.listVersions(productId));
    }

    @PostMapping("/versions")
    @PreAuthorize("@perm.has('INVENTORY_PRODUCT_MANAGE')")
    public ResponseEntity<ProductUomChainVersionResponse> saveVersion(
            @PathVariable Long productId,
            @Valid @RequestBody ProductUomChainSaveRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(chainService.saveVersion(productId, request, username(jwt)));
    }

    private String username(Jwt jwt) {
        return jwt != null ? jwt.getClaimAsString("preferred_username") : "system";
    }
}
