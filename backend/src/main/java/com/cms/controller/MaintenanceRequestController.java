package com.cms.controller;

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

import com.cms.dto.MaintenanceRequestDto;
import com.cms.dto.MaintenanceRequestResponse;
import com.cms.model.enums.MaintenanceStatus;
import com.cms.service.MaintenanceRequestService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/maintenance")
public class MaintenanceRequestController {

    private final MaintenanceRequestService maintenanceRequestService;

    public MaintenanceRequestController(MaintenanceRequestService maintenanceRequestService) {
        this.maintenanceRequestService = maintenanceRequestService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_LAB_INCHARGE') or hasRole('ROLE_TECHNICIAN')")
    public ResponseEntity<MaintenanceRequestResponse> create(@Valid @RequestBody MaintenanceRequestDto request) {
        MaintenanceRequestResponse response = maintenanceRequestService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<MaintenanceRequestResponse>> findAll(
            @RequestParam(required = false) Long equipmentId,
            @RequestParam(required = false) MaintenanceStatus status,
            @RequestParam(required = false) Long assignedToId,
            @RequestParam(required = false) Boolean pendingOnly) {
        List<MaintenanceRequestResponse> requests;
        if (Boolean.TRUE.equals(pendingOnly)) {
            requests = maintenanceRequestService.findPendingRequests();
        } else if (equipmentId != null) {
            requests = maintenanceRequestService.findByEquipmentId(equipmentId);
        } else if (status != null) {
            requests = maintenanceRequestService.findByStatus(status);
        } else if (assignedToId != null) {
            requests = maintenanceRequestService.findByAssignedToId(assignedToId);
        } else {
            requests = maintenanceRequestService.findAll();
        }
        return ResponseEntity.ok(requests);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MaintenanceRequestResponse> findById(@PathVariable Long id) {
        MaintenanceRequestResponse response = maintenanceRequestService.findById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_LAB_INCHARGE') or hasRole('ROLE_TECHNICIAN')")
    public ResponseEntity<MaintenanceRequestResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody MaintenanceRequestDto request) {
        MaintenanceRequestResponse response = maintenanceRequestService.update(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        maintenanceRequestService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
