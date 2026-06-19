package com.cms.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cms.dto.CountryRequest;
import com.cms.dto.CountryResponse;
import com.cms.dto.ActiveStatusUpdateRequest;
import com.cms.dto.ActiveStatusUpdateResponse;
import com.cms.dto.IndiaDistrictRequest;
import com.cms.dto.IndiaDistrictResponse;
import com.cms.dto.IndiaStateRequest;
import com.cms.dto.IndiaStateResponse;
import com.cms.service.IndiaLocationService;

import jakarta.validation.Valid;

/**
 * REST controller for Location master data (Countries, States, Districts).
 * Base path: /api/v1/india
 *
 * Country endpoints: GET/POST/PUT/DELETE /india/countries
 * States per country: GET /india/countries/{countryId}/states
 * States (India legacy): GET /india/states  (activeOnly=true by default)
 * Districts: GET/POST/PUT/DELETE under /india/states/{stateId}/districts
 */
@RestController
@RequestMapping("/india")
public class IndiaLocationController {

    private final IndiaLocationService service;

    public IndiaLocationController(IndiaLocationService service) {
        this.service = service;
    }

    // ─── Countries ────────────────────────────────────────────────────────────

    @GetMapping("/countries")
    public ResponseEntity<List<CountryResponse>> getCountries(
            @RequestParam(required = false, defaultValue = "false") boolean activeOnly) {
        List<CountryResponse> result = activeOnly
            ? service.findActiveCountries()
            : service.findAllCountries();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/countries/{id}")
    public ResponseEntity<CountryResponse> getCountry(@PathVariable Long id) {
        return ResponseEntity.ok(service.findCountryById(id));
    }

    @PostMapping("/countries")
    @PreAuthorize("@perm.has('INDIA_LOCATION_MANAGE')")
    public ResponseEntity<CountryResponse> createCountry(@Valid @RequestBody CountryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createCountry(request));
    }

    @PutMapping("/countries/{id}")
    @PreAuthorize("@perm.has('INDIA_LOCATION_MANAGE')")
    public ResponseEntity<CountryResponse> updateCountry(
            @PathVariable Long id,
            @Valid @RequestBody CountryRequest request) {
        return ResponseEntity.ok(service.updateCountry(id, request));
    }

    @DeleteMapping("/countries/{id}")
    @PreAuthorize("@perm.has('INDIA_LOCATION_MANAGE')")
    public ResponseEntity<Void> deleteCountry(@PathVariable Long id) {
        service.deleteCountry(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/countries/{id}/status")
    @PreAuthorize("@perm.has('INDIA_LOCATION_MANAGE')")
    public ResponseEntity<ActiveStatusUpdateResponse> updateCountryStatus(
            @PathVariable Long id,
            @Valid @RequestBody ActiveStatusUpdateRequest request) {
        return ResponseEntity.ok(service.updateCountryStatus(id, request));
    }

    /** Get states belonging to a specific country. */
    @GetMapping("/countries/{countryId}/states")
    public ResponseEntity<List<IndiaStateResponse>> getStatesByCountry(
            @PathVariable Long countryId,
            @RequestParam(required = false, defaultValue = "true") boolean activeOnly) {
        List<IndiaStateResponse> result = activeOnly
            ? service.findActiveStatesByCountry(countryId)
            : service.findStatesByCountry(countryId);
        return ResponseEntity.ok(result);
    }

    /** Create a state under a specific country. */
    @PostMapping("/countries/{countryId}/states")
    @PreAuthorize("@perm.has('INDIA_LOCATION_MANAGE')")
    public ResponseEntity<IndiaStateResponse> createStateForCountry(
            @PathVariable Long countryId,
            @Valid @RequestBody IndiaStateRequest request) {
        // Build a request with the countryId baked in
        IndiaStateRequest withCountry = new IndiaStateRequest(
            request.name(), request.code(), request.isActive(), countryId);
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createState(withCountry));
    }

    // ─── States (India-scoped — legacy / backward-compat) ────────────────────

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

    @PatchMapping("/states/{id}/status")
    @PreAuthorize("@perm.has('INDIA_LOCATION_MANAGE')")
    public ResponseEntity<ActiveStatusUpdateResponse> updateStateStatus(
            @PathVariable Long id,
            @Valid @RequestBody ActiveStatusUpdateRequest request) {
        return ResponseEntity.ok(service.updateStateStatus(id, request));
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

    @PatchMapping("/districts/{id}/status")
    @PreAuthorize("@perm.has('INDIA_LOCATION_MANAGE')")
    public ResponseEntity<ActiveStatusUpdateResponse> updateDistrictStatus(
            @PathVariable Long id,
            @Valid @RequestBody ActiveStatusUpdateRequest request) {
        return ResponseEntity.ok(service.updateDistrictStatus(id, request));
    }
}

