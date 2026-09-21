package com.cms.inventory.stock.controller;

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
import com.cms.inventory.stock.dto.ProductLocationReorderConfigRequest;
import com.cms.inventory.stock.dto.ProductLocationReorderConfigResponse;
import com.cms.inventory.stock.service.ProductLocationReorderConfigService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/inventory/stock/reorder-configs")
public class ProductLocationReorderConfigController {

    private static final String VIEW_ANY = "@perm.hasAny('INVENTORY_REORDER_CONFIG_VIEW', 'INVENTORY_REORDER_CONFIG_MANAGE')";

    private final ProductLocationReorderConfigService configService;

    public ProductLocationReorderConfigController(ProductLocationReorderConfigService configService) {
        this.configService = configService;
    }

    @PostMapping
    @PreAuthorize("@perm.has('INVENTORY_REORDER_CONFIG_MANAGE')")
    public ResponseEntity<ProductLocationReorderConfigResponse> create(@Valid @RequestBody ProductLocationReorderConfigRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(configService.create(request));
    }

    @GetMapping("/page")
    @PreAuthorize(VIEW_ANY)
    public ResponseEntity<Page<ProductLocationReorderConfigResponse>> findPage(
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Long locationId,
            @RequestParam(required = false) Boolean activeOnly,
            @PageableDefault(size = 25, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(configService.findPage(productId, locationId, activeOnly, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize(VIEW_ANY)
    public ResponseEntity<ProductLocationReorderConfigResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(configService.findById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('INVENTORY_REORDER_CONFIG_MANAGE')")
    public ResponseEntity<ProductLocationReorderConfigResponse> update(
            @PathVariable Long id, @Valid @RequestBody ProductLocationReorderConfigRequest request) {
        return ResponseEntity.ok(configService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.has('INVENTORY_REORDER_CONFIG_MANAGE')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        configService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("@perm.has('INVENTORY_REORDER_CONFIG_MANAGE')")
    public ResponseEntity<ActiveStatusUpdateResponse> updateStatus(
            @PathVariable Long id, @Valid @RequestBody ActiveStatusUpdateRequest request) {
        return ResponseEntity.ok(configService.updateStatus(id, request));
    }

    @GetMapping("/pair-exists")
    @PreAuthorize(VIEW_ANY)
    public ResponseEntity<Boolean> pairExists(
            @RequestParam Long value, @RequestParam Long locationId, @RequestParam(required = false) Long excludeId) {
        return ResponseEntity.ok(configService.pairExists(value, locationId, excludeId));
    }
}
