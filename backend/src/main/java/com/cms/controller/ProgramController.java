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

import com.cms.dto.ProgramDocumentRequirementsRequest;
import com.cms.dto.ProgramDocumentRequirementsResponse;
import com.cms.dto.ProgramRequest;
import com.cms.dto.ProgramResponse;
import com.cms.dto.ProgramStatusUpdateRequest;
import com.cms.dto.ProgramStatusUpdateResponse;
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

    @PatchMapping("/{id}/status")
    @PreAuthorize("@perm.has('PROGRAM_MANAGE')")
    public ResponseEntity<ProgramStatusUpdateResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody ProgramStatusUpdateRequest request) {
        ProgramStatusUpdateResponse response = programService.updateStatus(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.has('PROGRAM_MANAGE')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        programService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ── Document Requirements Management ────────────────────────────────────────

    @GetMapping("/{id}/document-types")
    public ResponseEntity<ProgramDocumentRequirementsResponse> getDocumentRequirements(@PathVariable Long id) {
        return ResponseEntity.ok(programService.getDocumentRequirements(id));
    }

    @PutMapping("/{id}/document-types")
    @PreAuthorize("@perm.has('PROGRAM_MANAGE')")
    public ResponseEntity<ProgramDocumentRequirementsResponse> setDocumentRequirements(
            @PathVariable Long id,
            @RequestBody ProgramDocumentRequirementsRequest request) {
        return ResponseEntity.ok(programService.setDocumentRequirements(id, request));
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
}
