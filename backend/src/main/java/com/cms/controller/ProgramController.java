package com.cms.controller;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.cms.model.enums.DocumentType;
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

import com.cms.dto.ProgramRequest;
import com.cms.dto.ProgramResponse;
import com.cms.service.ProgramService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/programs")
public class ProgramController {

    private final ProgramService programService;

    public ProgramController(ProgramService programService) {
        this.programService = programService;
    }

    @PostMapping
    @PreAuthorize("@perm.has('PROGRAM_MANAGE')")
    public ResponseEntity<ProgramResponse> create(@Valid @RequestBody ProgramRequest request) {
        ProgramResponse response = programService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ProgramResponse>> findAll() {
        List<ProgramResponse> programs = programService.findAll();
        return ResponseEntity.ok(programs);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProgramResponse> findById(@PathVariable Long id) {
        ProgramResponse response = programService.findById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('PROGRAM_MANAGE')")
    public ResponseEntity<ProgramResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ProgramRequest request) {
        ProgramResponse response = programService.update(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.has('PROGRAM_MANAGE')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        programService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ── Document Types Management ────────────────────────────────────────

    /**
     * Get required document types for a specific program.
     */
    @GetMapping("/{id}/document-types")
    public ResponseEntity<Set<String>> getRequiredDocumentTypes(@PathVariable Long id) {
        Set<String> types = programService.getRequiredDocumentTypes(id).stream()
            .map(Enum::name)
            .collect(Collectors.toSet());
        return ResponseEntity.ok(types);
    }

    /**
     * Set required document types for a specific program.
     */
    @PutMapping("/{id}/document-types")
    @PreAuthorize("@perm.has('PROGRAM_MANAGE')")
    public ResponseEntity<Set<String>> setRequiredDocumentTypes(
            @PathVariable Long id,
            @RequestBody Set<String> documentTypes) {
        Set<DocumentType> parsed = (documentTypes == null ? Set.<String>of() : documentTypes).stream()
            .map(this::parseDocumentType)
            .collect(Collectors.toSet());
        Set<String> updated = programService.setRequiredDocumentTypes(id, parsed).stream()
            .map(Enum::name)
            .collect(Collectors.toSet());
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/name-exists")
    @PreAuthorize("@perm.has('PROGRAM_MANAGE')")
    public ResponseEntity<Boolean> nameExists(
            @RequestParam String value,
            @RequestParam(required = false) Long excludeId) {
        return ResponseEntity.ok(programService.nameExists(value, excludeId));
    }

    @GetMapping("/code-exists")
    @PreAuthorize("@perm.has('PROGRAM_MANAGE')")
    public ResponseEntity<Boolean> codeExists(
            @RequestParam String value,
            @RequestParam(required = false) Long excludeId) {
        return ResponseEntity.ok(programService.codeExists(value, excludeId));
    }

    private DocumentType parseDocumentType(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Document type code must not be blank");
        }

        // Most common case: enum name, e.g. "TENTH_MARKSHEET"
        try {
            return DocumentType.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            // Fallback: display name, e.g. "10th Marksheet"
            String normalized = raw.trim().toLowerCase();
            for (DocumentType t : DocumentType.values()) {
                if (t.getDisplayName().equalsIgnoreCase(raw.trim())
                    || t.getDisplayName().toLowerCase().equals(normalized)) {
                    return t;
                }
            }
            throw new IllegalArgumentException("Unknown document type: " + raw);
        }
    }
}
