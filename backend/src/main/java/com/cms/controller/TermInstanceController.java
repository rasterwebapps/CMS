package com.cms.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cms.dto.TermInstanceDto;
import com.cms.dto.TermInstanceUpdateRequest;
import com.cms.service.TermInstanceService;

@RestController
@RequestMapping("/term-instances")
public class TermInstanceController {

    private final TermInstanceService termInstanceService;

    public TermInstanceController(TermInstanceService termInstanceService) {
        this.termInstanceService = termInstanceService;
    }

    @GetMapping
    public ResponseEntity<List<TermInstanceDto>> getByAcademicYear(
            @RequestParam Long academicYearId) {
        return ResponseEntity.ok(termInstanceService.getTermInstancesByAcademicYear(academicYearId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TermInstanceDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(termInstanceService.getById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_COLLEGE_ADMIN')")
    public ResponseEntity<TermInstanceDto> update(
            @PathVariable Long id,
            @RequestBody TermInstanceUpdateRequest request) {
        return ResponseEntity.ok(termInstanceService.updateTermInstance(id, request));
    }
}
