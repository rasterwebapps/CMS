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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cms.inventory.asset.dto.AssetDisposalRequest;
import com.cms.inventory.asset.dto.AssetRequest;
import com.cms.inventory.asset.dto.AssetResponse;
import com.cms.inventory.asset.dto.AssetStatusUpdateRequest;
import com.cms.inventory.asset.service.AssetService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/inventory/asset/assets")
public class AssetController {

    private static final String VIEW_ANY = "@perm.hasAny('INVENTORY_ASSET_VIEW', 'INVENTORY_ASSET_MANAGE')";

    private final AssetService assetService;

    public AssetController(AssetService assetService) {
        this.assetService = assetService;
    }

    @PostMapping
    @PreAuthorize("@perm.has('INVENTORY_ASSET_MANAGE')")
    public ResponseEntity<AssetResponse> create(@Valid @RequestBody AssetRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(assetService.create(request));
    }

    @GetMapping("/page")
    @PreAuthorize(VIEW_ANY)
    public ResponseEntity<Page<AssetResponse>> findPage(
            @RequestParam(required = false) Long locationId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 25, sort = "assetTag", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(assetService.findPage(locationId, status, search, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize(VIEW_ANY)
    public ResponseEntity<AssetResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(assetService.findById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('INVENTORY_ASSET_MANAGE')")
    public ResponseEntity<AssetResponse> update(@PathVariable Long id, @Valid @RequestBody AssetRequest request) {
        return ResponseEntity.ok(assetService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("@perm.has('INVENTORY_ASSET_MANAGE')")
    public ResponseEntity<AssetResponse> updateStatus(@PathVariable Long id, @Valid @RequestBody AssetStatusUpdateRequest request) {
        return ResponseEntity.ok(assetService.updateStatus(id, request));
    }

    @GetMapping("/asset-tag-exists")
    @PreAuthorize("@perm.has('INVENTORY_ASSET_MANAGE')")
    public ResponseEntity<Boolean> assetTagExists(@RequestParam String value, @RequestParam(required = false) Long excludeId) {
        return ResponseEntity.ok(assetService.assetTagExists(value, excludeId));
    }

    @PostMapping("/{id}/dispose")
    @PreAuthorize("@perm.has('INVENTORY_ASSET_DISPOSE')")
    public ResponseEntity<AssetResponse> dispose(@PathVariable Long id, @Valid @RequestBody AssetDisposalRequest request, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(assetService.dispose(id, request, username(jwt)));
    }

    private String username(Jwt jwt) {
        return jwt != null ? jwt.getClaimAsString("preferred_username") : "system";
    }
}
