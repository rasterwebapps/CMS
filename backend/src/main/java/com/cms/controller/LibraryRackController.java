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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cms.dto.ActiveStatusUpdateRequest;
import com.cms.dto.ActiveStatusUpdateResponse;
import com.cms.dto.LibraryRackRequest;
import com.cms.dto.LibraryRackResponse;
import com.cms.service.LibraryRackService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/library/racks")
public class LibraryRackController {

    private final LibraryRackService rackService;

    public LibraryRackController(LibraryRackService rackService) {
        this.rackService = rackService;
    }

    @PostMapping
    @PreAuthorize("@perm.has('LIBRARY_SHELF_MANAGE')")
    public ResponseEntity<LibraryRackResponse> create(@Valid @RequestBody LibraryRackRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(rackService.create(request));
    }

    // Unpaginated — used to populate Rack dropdowns (Book Catalogue / Search Catalogue filters,
    // Book form, Transfer dialog), so it's readable by anyone who can view the library catalogue.
    @GetMapping
    @PreAuthorize("@perm.hasAny('LIBRARY_CATALOGUE_VIEW', 'LIBRARY_CATALOGUE_MANAGE', 'LIBRARY_ISSUE_VIEW', 'LIBRARY_ISSUE_MANAGE', 'LIBRARY_SHELF_VIEW', 'LIBRARY_SHELF_MANAGE')")
    public ResponseEntity<List<LibraryRackResponse>> findAll(
            @RequestParam(required = false) Long libraryId,
            @RequestParam(required = false, defaultValue = "false") boolean activeOnly) {
        return ResponseEntity.ok(rackService.findAll(libraryId, activeOnly));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@perm.hasAny('LIBRARY_SHELF_VIEW', 'LIBRARY_SHELF_MANAGE')")
    public ResponseEntity<LibraryRackResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(rackService.findById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('LIBRARY_SHELF_MANAGE')")
    public ResponseEntity<LibraryRackResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody LibraryRackRequest request) {
        return ResponseEntity.ok(rackService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.has('LIBRARY_SHELF_MANAGE')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        rackService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("@perm.has('LIBRARY_SHELF_MANAGE')")
    public ResponseEntity<ActiveStatusUpdateResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody ActiveStatusUpdateRequest request) {
        return ResponseEntity.ok(rackService.updateStatus(id, request));
    }

    @GetMapping("/page")
    @PreAuthorize("@perm.hasAny('LIBRARY_SHELF_VIEW', 'LIBRARY_SHELF_MANAGE')")
    public ResponseEntity<Page<LibraryRackResponse>> findPage(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long libraryId,
            @PageableDefault(size = 25, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(rackService.findPage(search, libraryId, pageable));
    }

    @GetMapping("/name-exists")
    @PreAuthorize("@perm.has('LIBRARY_SHELF_MANAGE')")
    public ResponseEntity<Boolean> nameExists(
            @RequestParam String value,
            @RequestParam Long libraryId,
            @RequestParam(required = false) Long excludeId) {
        return ResponseEntity.ok(rackService.nameExists(value, libraryId, excludeId));
    }

    @GetMapping("/code-exists")
    @PreAuthorize("@perm.has('LIBRARY_SHELF_MANAGE')")
    public ResponseEntity<Boolean> codeExists(
            @RequestParam String value,
            @RequestParam Long libraryId,
            @RequestParam(required = false) Long excludeId) {
        return ResponseEntity.ok(rackService.codeExists(value, libraryId, excludeId));
    }
}
