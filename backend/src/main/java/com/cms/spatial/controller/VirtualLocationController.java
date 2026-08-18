package com.cms.spatial.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cms.spatial.dto.VirtualLocationRequest;
import com.cms.spatial.dto.VirtualLocationResponse;
import com.cms.spatial.service.VirtualLocationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/spatial/virtual-locations")
public class VirtualLocationController {

    private final VirtualLocationService virtualLocationService;

    public VirtualLocationController(VirtualLocationService virtualLocationService) {
        this.virtualLocationService = virtualLocationService;
    }

    @GetMapping
    @PreAuthorize("@perm.has('SPATIAL_VIRTUAL_LOCATION_VIEW')")
    public ResponseEntity<List<VirtualLocationResponse>> find(
            @RequestParam(required = false) Long floorPlanId,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) Long entityId) {
        if (floorPlanId != null) {
            return ResponseEntity.ok(virtualLocationService.findByFloorPlan(floorPlanId));
        }
        if (entityType != null && entityId != null) {
            return ResponseEntity.ok(virtualLocationService.findByEntity(entityType, entityId));
        }
        throw new IllegalArgumentException("Either floorPlanId, or both entityType and entityId, is required");
    }

    @GetMapping("/{id}")
    @PreAuthorize("@perm.has('SPATIAL_VIRTUAL_LOCATION_VIEW')")
    public ResponseEntity<VirtualLocationResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(virtualLocationService.findById(id));
    }

    @PostMapping
    @PreAuthorize("@perm.has('SPATIAL_VIRTUAL_LOCATION_MANAGE')")
    public ResponseEntity<VirtualLocationResponse> create(@Valid @RequestBody VirtualLocationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(virtualLocationService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('SPATIAL_VIRTUAL_LOCATION_MANAGE')")
    public ResponseEntity<VirtualLocationResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody VirtualLocationRequest request) {
        return ResponseEntity.ok(virtualLocationService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.has('SPATIAL_VIRTUAL_LOCATION_MANAGE')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        virtualLocationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
