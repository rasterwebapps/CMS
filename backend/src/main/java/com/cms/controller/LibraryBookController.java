package com.cms.controller;

import java.util.List;
import java.util.Map;

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

import com.cms.dto.LibraryBookRequest;
import com.cms.dto.LibraryBookResponse;
import com.cms.model.enums.BookStatus;
import com.cms.service.LibraryBookService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/library/books")
public class LibraryBookController {

    private final LibraryBookService bookService;

    public LibraryBookController(LibraryBookService bookService) {
        this.bookService = bookService;
    }

    @PostMapping
    @PreAuthorize("@perm.has('LIBRARY_CATALOGUE_MANAGE')")
    public ResponseEntity<LibraryBookResponse> create(@Valid @RequestBody LibraryBookRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bookService.create(request));
    }

    @GetMapping
    @PreAuthorize("@perm.hasAny('LIBRARY_CATALOGUE_VIEW', 'LIBRARY_CATALOGUE_MANAGE')")
    public ResponseEntity<List<LibraryBookResponse>> findAll(
            @RequestParam(required = false) BookStatus status) {
        if (status != null) {
            return ResponseEntity.ok(bookService.findByStatus(status));
        }
        return ResponseEntity.ok(bookService.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("@perm.hasAny('LIBRARY_CATALOGUE_VIEW', 'LIBRARY_CATALOGUE_MANAGE')")
    public ResponseEntity<LibraryBookResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(bookService.findById(id));
    }

    @GetMapping("/accession-number-exists")
    @PreAuthorize("@perm.hasAny('LIBRARY_CATALOGUE_VIEW', 'LIBRARY_CATALOGUE_MANAGE')")
    public ResponseEntity<Map<String, Boolean>> accessionNumberExists(
            @RequestParam String accessionNumber,
            @RequestParam(required = false) Long excludeId) {
        boolean exists = bookService.accessionNumberExists(accessionNumber, excludeId);
        return ResponseEntity.ok(Map.of("exists", exists));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('LIBRARY_CATALOGUE_MANAGE')")
    public ResponseEntity<LibraryBookResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody LibraryBookRequest request) {
        return ResponseEntity.ok(bookService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.has('LIBRARY_CATALOGUE_MANAGE')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        bookService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
