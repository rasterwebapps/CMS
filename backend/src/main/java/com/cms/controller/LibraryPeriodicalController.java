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

import com.cms.dto.LibraryPeriodicalRequest;
import com.cms.dto.LibraryPeriodicalResponse;
import com.cms.model.enums.JournalType;
import com.cms.model.enums.SubscriptionStatus;
import com.cms.service.LibraryPeriodicalService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/library/periodicals")
public class LibraryPeriodicalController {

    private final LibraryPeriodicalService periodicalService;

    public LibraryPeriodicalController(LibraryPeriodicalService periodicalService) {
        this.periodicalService = periodicalService;
    }

    @PostMapping
    @PreAuthorize("@perm.has('LIBRARY_PERIODICAL_MANAGE')")
    public ResponseEntity<LibraryPeriodicalResponse> create(
            @Valid @RequestBody LibraryPeriodicalRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(periodicalService.create(request));
    }

    @GetMapping
    @PreAuthorize("@perm.hasAny('LIBRARY_PERIODICAL_VIEW', 'LIBRARY_PERIODICAL_MANAGE')")
    public ResponseEntity<List<LibraryPeriodicalResponse>> findAll(
            @RequestParam(required = false) SubscriptionStatus status,
            @RequestParam(required = false) JournalType journalType) {
        if (status != null) {
            return ResponseEntity.ok(periodicalService.findByStatus(status));
        }
        if (journalType != null) {
            return ResponseEntity.ok(periodicalService.findByType(journalType));
        }
        return ResponseEntity.ok(periodicalService.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("@perm.hasAny('LIBRARY_PERIODICAL_VIEW', 'LIBRARY_PERIODICAL_MANAGE')")
    public ResponseEntity<LibraryPeriodicalResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(periodicalService.findById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('LIBRARY_PERIODICAL_MANAGE')")
    public ResponseEntity<LibraryPeriodicalResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody LibraryPeriodicalRequest request) {
        return ResponseEntity.ok(periodicalService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.has('LIBRARY_PERIODICAL_MANAGE')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        periodicalService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
