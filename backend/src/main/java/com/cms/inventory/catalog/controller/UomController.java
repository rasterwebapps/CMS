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
import com.cms.inventory.catalog.dto.UomRequest;
import com.cms.inventory.catalog.dto.UomResponse;
import com.cms.inventory.catalog.service.UomService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/inventory/uoms")
public class UomController {

    private final UomService uomService;

    public UomController(UomService uomService) {
        this.uomService = uomService;
    }

    @PostMapping
    @PreAuthorize("@perm.has('INVENTORY_UOM_MANAGE')")
    public ResponseEntity<UomResponse> create(@Valid @RequestBody UomRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(uomService.create(request));
    }

    // Unpaginated — used to populate the UOM picker on the Product form (once built).
    @GetMapping
    @PreAuthorize("@perm.hasAny('INVENTORY_UOM_VIEW', 'INVENTORY_UOM_MANAGE')")
    public ResponseEntity<List<UomResponse>> findAll(
            @RequestParam(required = false, defaultValue = "false") boolean activeOnly) {
        return ResponseEntity.ok(uomService.findAll(activeOnly));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@perm.hasAny('INVENTORY_UOM_VIEW', 'INVENTORY_UOM_MANAGE')")
    public ResponseEntity<UomResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(uomService.findById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('INVENTORY_UOM_MANAGE')")
    public ResponseEntity<UomResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UomRequest request) {
        return ResponseEntity.ok(uomService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.has('INVENTORY_UOM_MANAGE')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        uomService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("@perm.has('INVENTORY_UOM_MANAGE')")
    public ResponseEntity<ActiveStatusUpdateResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody ActiveStatusUpdateRequest request) {
        return ResponseEntity.ok(uomService.updateStatus(id, request));
    }

    @GetMapping("/page")
    @PreAuthorize("@perm.hasAny('INVENTORY_UOM_VIEW', 'INVENTORY_UOM_MANAGE')")
    public ResponseEntity<Page<UomResponse>> findPage(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 25, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(uomService.findPage(search, pageable));
    }

    @GetMapping("/code-exists")
    @PreAuthorize("@perm.has('INVENTORY_UOM_MANAGE')")
    public ResponseEntity<Boolean> codeExists(
            @RequestParam String value,
            @RequestParam(required = false) Long excludeId) {
        return ResponseEntity.ok(uomService.codeExists(value, excludeId));
    }

    @GetMapping("/name-exists")
    @PreAuthorize("@perm.has('INVENTORY_UOM_MANAGE')")
    public ResponseEntity<Boolean> nameExists(
            @RequestParam String value,
            @RequestParam(required = false) Long excludeId) {
        return ResponseEntity.ok(uomService.nameExists(value, excludeId));
    }
}
