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

import com.cms.dto.IndiaDistrictRequest;
import com.cms.dto.IndiaDistrictResponse;
import com.cms.dto.IndiaStateRequest;
import com.cms.dto.IndiaStateResponse;
import com.cms.service.IndiaLocationService;

import jakarta.validation.Valid;

/**
 * REST controller for India States and Districts master data.
 * Base path: /api/v1/india
 */
@RestController
@RequestMapping("/india")
public class IndiaLocationController {

    private final IndiaLocationService service;

    public IndiaLocationController(IndiaLocationService service) {
        this.service = service;
    }

    // ─── States ──────────────────────────────────────────────────────────────

    @GetMapping("/states")
    public ResponseEntity<List<IndiaStateResponse>> getStates(
            @RequestParam(required = false, defaultValue = "true") boolean activeOnly) {
        List<IndiaStateResponse> result = activeOnly
            ? service.findActiveStates()
            : service.findAllStates();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/states/{id}")
    public ResponseEntity<IndiaStateResponse> getState(@PathVariable Long id) {
        return ResponseEntity.ok(service.findStateById(id));
    }

    @PostMapping("/states")
    @PreAuthorize("@perm.has('INDIA_LOCATION_MANAGE')")
    public ResponseEntity<IndiaStateResponse> createState(@Valid @RequestBody IndiaStateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createState(request));
    }

    @PutMapping("/states/{id}")
    @PreAuthorize("@perm.has('INDIA_LOCATION_MANAGE')")
    public ResponseEntity<IndiaStateResponse> updateState(
            @PathVariable Long id,
            @Valid @RequestBody IndiaStateRequest request) {
        return ResponseEntity.ok(service.updateState(id, request));
    }

    @DeleteMapping("/states/{id}")
    @PreAuthorize("@perm.has('INDIA_LOCATION_MANAGE')")
    public ResponseEntity<Void> deleteState(@PathVariable Long id) {
        service.deleteState(id);
        return ResponseEntity.noContent().build();
    }

    // ─── Districts ───────────────────────────────────────────────────────────

    @GetMapping("/states/{stateId}/districts")
    public ResponseEntity<List<IndiaDistrictResponse>> getDistricts(
            @PathVariable Long stateId,
            @RequestParam(required = false, defaultValue = "true") boolean activeOnly) {
        List<IndiaDistrictResponse> result = activeOnly
            ? service.findActiveDistrictsByState(stateId)
            : service.findDistrictsByState(stateId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/districts/{id}")
    public ResponseEntity<IndiaDistrictResponse> getDistrict(@PathVariable Long id) {
        return ResponseEntity.ok(service.findDistrictById(id));
    }

    @PostMapping("/states/{stateId}/districts")
    @PreAuthorize("@perm.has('INDIA_LOCATION_MANAGE')")
    public ResponseEntity<IndiaDistrictResponse> createDistrict(
            @PathVariable Long stateId,
            @Valid @RequestBody IndiaDistrictRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createDistrict(stateId, request));
    }

    @PutMapping("/districts/{id}")
    @PreAuthorize("@perm.has('INDIA_LOCATION_MANAGE')")
    public ResponseEntity<IndiaDistrictResponse> updateDistrict(
            @PathVariable Long id,
            @Valid @RequestBody IndiaDistrictRequest request) {
        return ResponseEntity.ok(service.updateDistrict(id, request));
    }

    @DeleteMapping("/districts/{id}")
    @PreAuthorize("@perm.has('INDIA_LOCATION_MANAGE')")
    public ResponseEntity<Void> deleteDistrict(@PathVariable Long id) {
        service.deleteDistrict(id);
        return ResponseEntity.noContent().build();
    }
}

