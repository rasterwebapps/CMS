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
import com.cms.inventory.stock.dto.InventoryLocationRequest;
import com.cms.inventory.stock.dto.InventoryLocationResponse;
import com.cms.inventory.stock.service.InventoryLocationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/inventory/locations")
public class InventoryLocationController {

    private final InventoryLocationService locationService;

    public InventoryLocationController(InventoryLocationService locationService) {
        this.locationService = locationService;
    }

    @PostMapping
    @PreAuthorize("@perm.has('INVENTORY_LOCATION_MANAGE')")
    public ResponseEntity<InventoryLocationResponse> create(@Valid @RequestBody InventoryLocationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(locationService.create(request));
    }

    // Unpaginated — used to populate the Location picker on the Record Stock Movement form.
    @GetMapping
    @PreAuthorize("@perm.hasAny('INVENTORY_LOCATION_VIEW', 'INVENTORY_LOCATION_MANAGE', 'INVENTORY_STOCK_VIEW', 'INVENTORY_STOCK_MANAGE')")
    public ResponseEntity<List<InventoryLocationResponse>> findAll(
            @RequestParam(required = false, defaultValue = "false") boolean activeOnly) {
        return ResponseEntity.ok(locationService.findAll(activeOnly));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@perm.hasAny('INVENTORY_LOCATION_VIEW', 'INVENTORY_LOCATION_MANAGE')")
    public ResponseEntity<InventoryLocationResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(locationService.findById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('INVENTORY_LOCATION_MANAGE')")
    public ResponseEntity<InventoryLocationResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody InventoryLocationRequest request) {
        return ResponseEntity.ok(locationService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.has('INVENTORY_LOCATION_MANAGE')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        locationService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("@perm.has('INVENTORY_LOCATION_MANAGE')")
    public ResponseEntity<ActiveStatusUpdateResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody ActiveStatusUpdateRequest request) {
        return ResponseEntity.ok(locationService.updateStatus(id, request));
    }

    @GetMapping("/page")
    @PreAuthorize("@perm.hasAny('INVENTORY_LOCATION_VIEW', 'INVENTORY_LOCATION_MANAGE')")
    public ResponseEntity<Page<InventoryLocationResponse>> findPage(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 25, sort = "virtualName", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(locationService.findPage(search, pageable));
    }

    @GetMapping("/name-exists")
    @PreAuthorize("@perm.has('INVENTORY_LOCATION_MANAGE')")
    public ResponseEntity<Boolean> nameExists(
            @RequestParam String value,
            @RequestParam(required = false) Long excludeId) {
        return ResponseEntity.ok(locationService.nameExists(value, excludeId));
    }
}
