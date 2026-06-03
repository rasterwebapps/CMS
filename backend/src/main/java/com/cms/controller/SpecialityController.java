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

import com.cms.dto.SpecialityRequest;
import com.cms.dto.SpecialityResponse;
import com.cms.service.SpecialityService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/specialities")
public class SpecialityController {

    private final SpecialityService specialityService;

    public SpecialityController(SpecialityService specialityService) {
        this.specialityService = specialityService;
    }

    @PostMapping
    @PreAuthorize("@perm.has('DEPT_MANAGE')")
    public ResponseEntity<SpecialityResponse> create(@Valid @RequestBody SpecialityRequest request) {
        SpecialityResponse response = specialityService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<SpecialityResponse>> findAll() {
        List<SpecialityResponse> specialities = specialityService.findAll();
        return ResponseEntity.ok(specialities);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SpecialityResponse> findById(@PathVariable Long id) {
        SpecialityResponse response = specialityService.findById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('DEPT_MANAGE')")
    public ResponseEntity<SpecialityResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody SpecialityRequest request) {
        SpecialityResponse response = specialityService.update(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.has('DEPT_MANAGE')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        specialityService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/name-exists")
    @PreAuthorize("@perm.has('DEPT_MANAGE')")
    public ResponseEntity<Boolean> nameExists(
            @RequestParam String value,
            @RequestParam(required = false) Long excludeId) {
        return ResponseEntity.ok(specialityService.nameExists(value, excludeId));
    }

    @GetMapping("/code-exists")
    @PreAuthorize("@perm.has('DEPT_MANAGE')")
    public ResponseEntity<Boolean> codeExists(
            @RequestParam String value,
            @RequestParam(required = false) Long excludeId) {
        return ResponseEntity.ok(specialityService.codeExists(value, excludeId));
    }
}
