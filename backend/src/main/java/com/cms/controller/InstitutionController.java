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

import com.cms.dto.InstitutionRequest;
import com.cms.dto.InstitutionResponse;
import com.cms.dto.ActiveStatusUpdateRequest;
import com.cms.dto.ActiveStatusUpdateResponse;
import com.cms.service.InstitutionService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/institutions")
public class InstitutionController {

    private final InstitutionService institutionService;

    public InstitutionController(InstitutionService institutionService) {
        this.institutionService = institutionService;
    }

    @PostMapping
    @PreAuthorize("@perm.has('INSTITUTION_MANAGE')")
    public ResponseEntity<InstitutionResponse> create(@Valid @RequestBody InstitutionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(institutionService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<InstitutionResponse>> findAll(
            @RequestParam(required = false, defaultValue = "false") boolean activeOnly) {
        List<InstitutionResponse> result = activeOnly
            ? institutionService.findActive()
            : institutionService.findAll();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<InstitutionResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(institutionService.findById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('INSTITUTION_MANAGE')")
    public ResponseEntity<InstitutionResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody InstitutionRequest request) {
        return ResponseEntity.ok(institutionService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.has('INSTITUTION_MANAGE')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        institutionService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("@perm.has('INSTITUTION_MANAGE')")
    public ResponseEntity<ActiveStatusUpdateResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody ActiveStatusUpdateRequest request) {
        return ResponseEntity.ok(institutionService.updateStatus(id, request));
    }

    @GetMapping("/name-exists")
    @PreAuthorize("@perm.has('INSTITUTION_MANAGE')")
    public ResponseEntity<Boolean> nameExists(
            @RequestParam String value,
            @RequestParam(required = false) Long excludeId) {
        return ResponseEntity.ok(institutionService.nameExists(value, excludeId));
    }

    @GetMapping("/code-exists")
    @PreAuthorize("@perm.has('INSTITUTION_MANAGE')")
    public ResponseEntity<Boolean> codeExists(
            @RequestParam String value,
            @RequestParam(required = false) Long excludeId) {
        return ResponseEntity.ok(institutionService.codeExists(value, excludeId));
    }
}
