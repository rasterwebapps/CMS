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

import com.cms.dto.DesignationRequest;
import com.cms.dto.DesignationResponse;
import com.cms.dto.ActiveStatusUpdateRequest;
import com.cms.dto.ActiveStatusUpdateResponse;
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
    public ResponseEntity<List<DesignationResponse>> findAll(
            @RequestParam(required = false, defaultValue = "false") boolean activeOnly) {
        List<DesignationResponse> result = activeOnly
            ? designationService.findActive()
            : designationService.findAll();
        return ResponseEntity.ok(result);
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

    @PatchMapping("/{id}/status")
    @PreAuthorize("@perm.has('DESIGNATION_MANAGE')")
    public ResponseEntity<ActiveStatusUpdateResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody ActiveStatusUpdateRequest request) {
        return ResponseEntity.ok(designationService.updateStatus(id, request));
    }

    @GetMapping("/page")
    public ResponseEntity<Page<DesignationResponse>> findPage(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 25, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(designationService.findPage(search, pageable));
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
