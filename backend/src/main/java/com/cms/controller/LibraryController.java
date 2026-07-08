package com.cms.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cms.dto.LibraryResponse;
import com.cms.service.LibraryService;

@RestController
@RequestMapping("/libraries")
public class LibraryController {

    private final LibraryService libraryService;

    public LibraryController(LibraryService libraryService) {
        this.libraryService = libraryService;
    }

    @GetMapping
    @PreAuthorize("@perm.hasAny('LIBRARY_CATALOGUE_VIEW', 'LIBRARY_CATALOGUE_MANAGE', 'LIBRARY_ISSUE_VIEW', 'LIBRARY_ISSUE_MANAGE', 'LIBRARY_SHELF_VIEW', 'LIBRARY_SHELF_MANAGE')")
    public ResponseEntity<List<LibraryResponse>> findAll() {
        return ResponseEntity.ok(libraryService.findAll());
    }
}
