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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cms.inventory.asset.dto.AssetMaintenanceMarkPerformedRequest;
import com.cms.inventory.asset.dto.AssetMaintenanceScheduleRequest;
import com.cms.inventory.asset.dto.AssetMaintenanceScheduleResponse;
import com.cms.inventory.asset.service.AssetMaintenanceScheduleService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/inventory/asset/maintenance-schedules")
public class AssetMaintenanceScheduleController {

    private static final String VIEW_ANY = "@perm.hasAny('INVENTORY_ASSET_MAINTENANCE_VIEW', 'INVENTORY_ASSET_MAINTENANCE_MANAGE')";

    private final AssetMaintenanceScheduleService scheduleService;

    public AssetMaintenanceScheduleController(AssetMaintenanceScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @PostMapping
    @PreAuthorize("@perm.has('INVENTORY_ASSET_MAINTENANCE_MANAGE')")
    public ResponseEntity<AssetMaintenanceScheduleResponse> create(@Valid @RequestBody AssetMaintenanceScheduleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(scheduleService.create(request));
    }

    @GetMapping("/page")
    @PreAuthorize(VIEW_ANY)
    public ResponseEntity<Page<AssetMaintenanceScheduleResponse>> findPage(
            @RequestParam(required = false) Long assetId,
            @RequestParam(required = false) Boolean overdueOnly,
            @RequestParam(required = false) Boolean activeOnly,
            @PageableDefault(size = 25, sort = "nextDueDate", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(scheduleService.findPage(assetId, overdueOnly, activeOnly, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize(VIEW_ANY)
    public ResponseEntity<AssetMaintenanceScheduleResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(scheduleService.findById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('INVENTORY_ASSET_MAINTENANCE_MANAGE')")
    public ResponseEntity<AssetMaintenanceScheduleResponse> update(@PathVariable Long id, @Valid @RequestBody AssetMaintenanceScheduleRequest request) {
        return ResponseEntity.ok(scheduleService.update(id, request));
    }

    @PostMapping("/{id}/mark-performed")
    @PreAuthorize("@perm.has('INVENTORY_ASSET_MAINTENANCE_MANAGE')")
    public ResponseEntity<AssetMaintenanceScheduleResponse> markPerformed(
            @PathVariable Long id, @Valid @RequestBody(required = false) AssetMaintenanceMarkPerformedRequest request) {
        return ResponseEntity.ok(scheduleService.markPerformed(id, request));
    }
}
