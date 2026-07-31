package com.cms.controller;

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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cms.dto.ActiveStatusUpdateRequest;
import com.cms.dto.ActiveStatusUpdateResponse;
import com.cms.dto.ClinicalVenueRequest;
import com.cms.dto.ClinicalVenueResponse;
import com.cms.service.ClinicalVenueService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/clinical-venues")
public class ClinicalVenueController {

    private final ClinicalVenueService clinicalVenueService;

    public ClinicalVenueController(ClinicalVenueService clinicalVenueService) {
        this.clinicalVenueService = clinicalVenueService;
    }

    @PostMapping
    @PreAuthorize("@perm.has('CLINICAL_VENUE_MANAGE')")
    public ResponseEntity<ClinicalVenueResponse> create(@Valid @RequestBody ClinicalVenueRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clinicalVenueService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<ClinicalVenueResponse>> findAll(
            @RequestParam(required = false, defaultValue = "false") boolean activeOnly) {
        List<ClinicalVenueResponse> result = activeOnly
            ? clinicalVenueService.findActive()
            : clinicalVenueService.findAll();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClinicalVenueResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(clinicalVenueService.findById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('CLINICAL_VENUE_MANAGE')")
    public ResponseEntity<ClinicalVenueResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ClinicalVenueRequest request) {
        return ResponseEntity.ok(clinicalVenueService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.has('CLINICAL_VENUE_MANAGE')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        clinicalVenueService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("@perm.has('CLINICAL_VENUE_MANAGE')")
    public ResponseEntity<ActiveStatusUpdateResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody ActiveStatusUpdateRequest request) {
        return ResponseEntity.ok(clinicalVenueService.updateStatus(id, request));
    }

    @GetMapping("/page")
    public ResponseEntity<Page<ClinicalVenueResponse>> findPage(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 25, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(clinicalVenueService.findPage(search, pageable));
    }

    @GetMapping("/name-exists")
    @PreAuthorize("@perm.has('CLINICAL_VENUE_MANAGE')")
    public ResponseEntity<Boolean> nameExists(
            @RequestParam String value,
            @RequestParam(required = false) Long excludeId) {
        return ResponseEntity.ok(clinicalVenueService.nameExists(value, excludeId));
    }
}
