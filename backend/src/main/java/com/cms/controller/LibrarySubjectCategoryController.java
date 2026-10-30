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
import com.cms.dto.LibrarySubjectCategoryRequest;
import com.cms.dto.LibrarySubjectCategoryResponse;
import com.cms.service.LibrarySubjectCategoryService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/library/subject-categories")
public class LibrarySubjectCategoryController {

    private final LibrarySubjectCategoryService categoryService;

    public LibrarySubjectCategoryController(LibrarySubjectCategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @PostMapping
    @PreAuthorize("@perm.hasAny('LIBRARY_SUBJECT_CATEGORY_MANAGE', 'LIBRARY_CATALOGUE_MANAGE')")
    public ResponseEntity<LibrarySubjectCategoryResponse> create(@Valid @RequestBody LibrarySubjectCategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.create(request));
    }

    // Unpaginated — used to populate Subject Category dropdowns in book form, book filters, my-issues
    @GetMapping
    @PreAuthorize("@perm.hasAny('LIBRARY_CATALOGUE_VIEW', 'LIBRARY_CATALOGUE_MANAGE', 'LIBRARY_ISSUE_VIEW', 'LIBRARY_ISSUE_MANAGE', 'LIBRARY_SUBJECT_CATEGORY_VIEW', 'LIBRARY_SUBJECT_CATEGORY_MANAGE')")
    public ResponseEntity<List<LibrarySubjectCategoryResponse>> findAll(
            @RequestParam(required = false, defaultValue = "false") boolean activeOnly) {
        return ResponseEntity.ok(categoryService.findAll(activeOnly));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@perm.hasAny('LIBRARY_SUBJECT_CATEGORY_VIEW', 'LIBRARY_SUBJECT_CATEGORY_MANAGE', 'LIBRARY_CATALOGUE_MANAGE')")
    public ResponseEntity<LibrarySubjectCategoryResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(categoryService.findById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.hasAny('LIBRARY_SUBJECT_CATEGORY_MANAGE', 'LIBRARY_CATALOGUE_MANAGE')")
    public ResponseEntity<LibrarySubjectCategoryResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody LibrarySubjectCategoryRequest request) {
        return ResponseEntity.ok(categoryService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.hasAny('LIBRARY_SUBJECT_CATEGORY_MANAGE', 'LIBRARY_CATALOGUE_MANAGE')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("@perm.hasAny('LIBRARY_SUBJECT_CATEGORY_MANAGE', 'LIBRARY_CATALOGUE_MANAGE')")
    public ResponseEntity<ActiveStatusUpdateResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody ActiveStatusUpdateRequest request) {
        return ResponseEntity.ok(categoryService.updateStatus(id, request));
    }

    @GetMapping("/page")
    @PreAuthorize("@perm.hasAny('LIBRARY_SUBJECT_CATEGORY_VIEW', 'LIBRARY_SUBJECT_CATEGORY_MANAGE', 'LIBRARY_CATALOGUE_MANAGE')")
    public ResponseEntity<Page<LibrarySubjectCategoryResponse>> findPage(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 25, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(categoryService.findPage(search, pageable));
    }

    @GetMapping("/name-exists")
    @PreAuthorize("@perm.hasAny('LIBRARY_SUBJECT_CATEGORY_MANAGE', 'LIBRARY_CATALOGUE_MANAGE')")
    public ResponseEntity<Boolean> nameExists(
            @RequestParam String value,
            @RequestParam(required = false) Long excludeId) {
        return ResponseEntity.ok(categoryService.nameExists(value, excludeId));
    }

    @GetMapping("/code-exists")
    @PreAuthorize("@perm.hasAny('LIBRARY_SUBJECT_CATEGORY_MANAGE', 'LIBRARY_CATALOGUE_MANAGE')")
    public ResponseEntity<Boolean> codeExists(
            @RequestParam String value,
            @RequestParam(required = false) Long excludeId) {
        return ResponseEntity.ok(categoryService.codeExists(value, excludeId));
    }
}
