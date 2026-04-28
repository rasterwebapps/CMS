package com.cms.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cms.dto.CurriculumVersionDto;
import com.cms.dto.CurriculumVersionRequest;
import com.cms.service.CurriculumVersionService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/curriculum-versions")
public class CurriculumVersionController {

    private final CurriculumVersionService curriculumVersionService;

    public CurriculumVersionController(CurriculumVersionService curriculumVersionService) {
        this.curriculumVersionService = curriculumVersionService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_COLLEGE_ADMIN')")
    public ResponseEntity<CurriculumVersionDto> create(@Valid @RequestBody CurriculumVersionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(curriculumVersionService.createCurriculumVersion(request));
    }

    @GetMapping
    public ResponseEntity<List<CurriculumVersionDto>> getByProgram(@RequestParam Long programId) {
        return ResponseEntity.ok(curriculumVersionService.getCurriculumVersionsByProgram(programId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CurriculumVersionDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(curriculumVersionService.getById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_COLLEGE_ADMIN')")
    public ResponseEntity<CurriculumVersionDto> update(
            @PathVariable Long id,
            @Valid @RequestBody CurriculumVersionRequest request) {
        return ResponseEntity.ok(curriculumVersionService.update(id, request));
    }

    @PostMapping("/{id}/clone")
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_COLLEGE_ADMIN')")
    public ResponseEntity<CurriculumVersionDto> clone(
            @PathVariable Long id,
            @RequestParam String newVersionName,
            @RequestParam Long newEffectiveAcademicYearId) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(curriculumVersionService.cloneCurriculumVersion(id, newVersionName, newEffectiveAcademicYearId));
    }
}
