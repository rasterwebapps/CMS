package com.cms.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cms.dto.FacultyDocumentTypeRequirementRequest;
import com.cms.dto.FacultyDocumentTypeRequirementResponse;
import com.cms.service.FacultyDocumentTypeRequirementService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/faculty-document-type-requirements")
public class FacultyDocumentTypeRequirementController {

    private final FacultyDocumentTypeRequirementService service;

    public FacultyDocumentTypeRequirementController(FacultyDocumentTypeRequirementService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<FacultyDocumentTypeRequirementResponse>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @PostMapping
    @PreAuthorize("@perm.has('FACULTY_DOC_CONFIG_MANAGE')")
    public ResponseEntity<FacultyDocumentTypeRequirementResponse> create(
            @Valid @RequestBody FacultyDocumentTypeRequirementRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.has('FACULTY_DOC_CONFIG_MANAGE')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
