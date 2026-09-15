package com.cms.inventory.stock.controller;

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
import com.cms.inventory.stock.dto.InventoryBinRequest;
import com.cms.inventory.stock.dto.InventoryBinResponse;
import com.cms.inventory.stock.service.InventoryBinService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/inventory/bins")
public class InventoryBinController {

    private final InventoryBinService binService;

    public InventoryBinController(InventoryBinService binService) {
        this.binService = binService;
    }

    @PostMapping
    @PreAuthorize("@perm.has('INVENTORY_BIN_MANAGE')")
    public ResponseEntity<InventoryBinResponse> create(@Valid @RequestBody InventoryBinRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(binService.create(request));
    }

    // Unpaginated — used to populate Bin pickers (future stock-movement bin selection).
    @GetMapping
    @PreAuthorize("@perm.hasAny('INVENTORY_BIN_VIEW', 'INVENTORY_BIN_MANAGE')")
    public ResponseEntity<List<InventoryBinResponse>> findAll(
            @RequestParam(required = false) Long rackId,
            @RequestParam(required = false) Long locationId,
            @RequestParam(required = false, defaultValue = "false") boolean activeOnly) {
        return ResponseEntity.ok(binService.findAll(rackId, locationId, activeOnly));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@perm.hasAny('INVENTORY_BIN_VIEW', 'INVENTORY_BIN_MANAGE')")
    public ResponseEntity<InventoryBinResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(binService.findById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('INVENTORY_BIN_MANAGE')")
    public ResponseEntity<InventoryBinResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody InventoryBinRequest request) {
        return ResponseEntity.ok(binService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.has('INVENTORY_BIN_MANAGE')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        binService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("@perm.has('INVENTORY_BIN_MANAGE')")
    public ResponseEntity<ActiveStatusUpdateResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody ActiveStatusUpdateRequest request) {
        return ResponseEntity.ok(binService.updateStatus(id, request));
    }

    @GetMapping("/page")
    @PreAuthorize("@perm.hasAny('INVENTORY_BIN_VIEW', 'INVENTORY_BIN_MANAGE')")
    public ResponseEntity<Page<InventoryBinResponse>> findPage(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long rackId,
            @PageableDefault(size = 25, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(binService.findPage(search, rackId, pageable));
    }

    @GetMapping("/name-exists")
    @PreAuthorize("@perm.has('INVENTORY_BIN_MANAGE')")
    public ResponseEntity<Boolean> nameExists(
            @RequestParam String value,
            @RequestParam Long rackId,
            @RequestParam(required = false) Long excludeId) {
        return ResponseEntity.ok(binService.nameExists(value, rackId, excludeId));
    }

    @GetMapping("/code-exists")
    @PreAuthorize("@perm.has('INVENTORY_BIN_MANAGE')")
    public ResponseEntity<Boolean> codeExists(
            @RequestParam String value,
            @RequestParam Long rackId,
            @RequestParam(required = false) Long excludeId) {
        return ResponseEntity.ok(binService.codeExists(value, rackId, excludeId));
    }
}
