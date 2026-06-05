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

import com.cms.dto.DesignationRequest;
import com.cms.dto.DesignationResponse;
import com.cms.service.DesignationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/designations")
public class DesignationController {

    private final DesignationService designationService;

    public DesignationController(DesignationService designationService) {
        this.designationService = designationService;
    }

    @PostMapping
    @PreAuthorize("@perm.has('DESIGNATION_MANAGE')")
    public ResponseEntity<DesignationResponse> create(@Valid @RequestBody DesignationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(designationService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<DesignationResponse>> findAll() {
        return ResponseEntity.ok(designationService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DesignationResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(designationService.findById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('DESIGNATION_MANAGE')")
    public ResponseEntity<DesignationResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody DesignationRequest request) {
        return ResponseEntity.ok(designationService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.has('DESIGNATION_MANAGE')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        designationService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/name-exists")
    @PreAuthorize("@perm.has('DESIGNATION_MANAGE')")
    public ResponseEntity<Boolean> nameExists(
            @RequestParam String value,
            @RequestParam(required = false) Long excludeId) {
        return ResponseEntity.ok(designationService.nameExists(value, excludeId));
    }

    @GetMapping("/code-exists")
    @PreAuthorize("@perm.has('DESIGNATION_MANAGE')")
    public ResponseEntity<Boolean> codeExists(
            @RequestParam String value,
            @RequestParam(required = false) Long excludeId) {
        return ResponseEntity.ok(designationService.codeExists(value, excludeId));
    }
}
