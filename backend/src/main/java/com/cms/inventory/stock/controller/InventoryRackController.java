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
import com.cms.inventory.stock.dto.InventoryRackRequest;
import com.cms.inventory.stock.dto.InventoryRackResponse;
import com.cms.inventory.stock.service.InventoryRackService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/inventory/racks")
public class InventoryRackController {

    private final InventoryRackService rackService;

    public InventoryRackController(InventoryRackService rackService) {
        this.rackService = rackService;
    }

    @PostMapping
    @PreAuthorize("@perm.has('INVENTORY_RACK_MANAGE')")
    public ResponseEntity<InventoryRackResponse> create(@Valid @RequestBody InventoryRackRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(rackService.create(request));
    }

    // Unpaginated — used to populate Rack pickers (Bin form, future stock-movement bin selection).
    @GetMapping
    @PreAuthorize("@perm.hasAny('INVENTORY_RACK_VIEW', 'INVENTORY_RACK_MANAGE', 'INVENTORY_BIN_VIEW', 'INVENTORY_BIN_MANAGE')")
    public ResponseEntity<List<InventoryRackResponse>> findAll(
            @RequestParam(required = false) Long locationId,
            @RequestParam(required = false, defaultValue = "false") boolean activeOnly) {
        return ResponseEntity.ok(rackService.findAll(locationId, activeOnly));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@perm.hasAny('INVENTORY_RACK_VIEW', 'INVENTORY_RACK_MANAGE')")
    public ResponseEntity<InventoryRackResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(rackService.findById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('INVENTORY_RACK_MANAGE')")
    public ResponseEntity<InventoryRackResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody InventoryRackRequest request) {
        return ResponseEntity.ok(rackService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.has('INVENTORY_RACK_MANAGE')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        rackService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("@perm.has('INVENTORY_RACK_MANAGE')")
    public ResponseEntity<ActiveStatusUpdateResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody ActiveStatusUpdateRequest request) {
        return ResponseEntity.ok(rackService.updateStatus(id, request));
    }

    @GetMapping("/page")
    @PreAuthorize("@perm.hasAny('INVENTORY_RACK_VIEW', 'INVENTORY_RACK_MANAGE')")
    public ResponseEntity<Page<InventoryRackResponse>> findPage(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long locationId,
            @PageableDefault(size = 25, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(rackService.findPage(search, locationId, pageable));
    }

    @GetMapping("/name-exists")
    @PreAuthorize("@perm.has('INVENTORY_RACK_MANAGE')")
    public ResponseEntity<Boolean> nameExists(
            @RequestParam String value,
            @RequestParam Long locationId,
            @RequestParam(required = false) Long excludeId) {
        return ResponseEntity.ok(rackService.nameExists(value, locationId, excludeId));
    }

    @GetMapping("/code-exists")
    @PreAuthorize("@perm.has('INVENTORY_RACK_MANAGE')")
    public ResponseEntity<Boolean> codeExists(
            @RequestParam String value,
            @RequestParam Long locationId,
            @RequestParam(required = false) Long excludeId) {
        return ResponseEntity.ok(rackService.codeExists(value, locationId, excludeId));
    }
}
