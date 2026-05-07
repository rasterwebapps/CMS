package com.cms.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cms.dto.ScholarshipTypeRequest;
import com.cms.dto.ScholarshipTypeResponse;
import com.cms.service.ScholarshipTypeService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/scholarships")
public class ScholarshipController {

    private final ScholarshipTypeService scholarshipTypeService;

    public ScholarshipController(ScholarshipTypeService scholarshipTypeService) {
        this.scholarshipTypeService = scholarshipTypeService;
    }

    @GetMapping
    @PreAuthorize("@perm.has('SCHOLARSHIP_VIEW')")
    public ResponseEntity<List<ScholarshipTypeResponse>> list() {
        return ResponseEntity.ok(scholarshipTypeService.getAllActive());
    }

    @GetMapping("/{id}")
    @PreAuthorize("@perm.has('SCHOLARSHIP_VIEW')")
    public ResponseEntity<ScholarshipTypeResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(scholarshipTypeService.getById(id));
    }

    @PostMapping
    @PreAuthorize("@perm.has('SCHOLARSHIP_MANAGE')")
    public ResponseEntity<ScholarshipTypeResponse> create(
            @Valid @RequestBody ScholarshipTypeRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        ScholarshipTypeResponse response = scholarshipTypeService.create(request, username(jwt));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('SCHOLARSHIP_MANAGE')")
    public ResponseEntity<ScholarshipTypeResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ScholarshipTypeRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(scholarshipTypeService.update(id, request, username(jwt)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.has('SCHOLARSHIP_MANAGE')")
    public ResponseEntity<Void> deactivate(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        scholarshipTypeService.deactivate(id, username(jwt));
        return ResponseEntity.noContent().build();
    }

    private static String username(Jwt jwt) {
        return jwt != null && jwt.getClaimAsString("preferred_username") != null
            ? jwt.getClaimAsString("preferred_username")
            : "system";
    }
}

