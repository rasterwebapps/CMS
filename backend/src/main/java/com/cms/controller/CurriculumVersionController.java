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
    @PreAuthorize("@perm.has('CURRICULUM_CREATE')")
    public ResponseEntity<CurriculumVersionDto> create(@Valid @RequestBody CurriculumVersionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(curriculumVersionService.createCurriculumVersion(request));
    }

    @GetMapping
    public ResponseEntity<List<CurriculumVersionDto>> getByProgram(@RequestParam Long programId) {
        return ResponseEntity.ok(curriculumVersionService.getCurriculumVersionsByProgram(programId));
    }

    @GetMapping("/page")
    public ResponseEntity<Page<CurriculumVersionDto>> findPage(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long programId,
            @RequestParam(required = false) Boolean isActive,
            @PageableDefault(size = 25, sort = "versionName", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(curriculumVersionService.findPage(search, programId, isActive, pageable));
    }

    @GetMapping("/name-exists")
    public ResponseEntity<Boolean> nameExists(
            @RequestParam Long programId,
            @RequestParam(required = false) Long courseId,
            @RequestParam String value,
            @RequestParam(required = false) Long excludeId) {
        return ResponseEntity.ok(curriculumVersionService.nameExists(programId, courseId, value, excludeId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CurriculumVersionDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(curriculumVersionService.getById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('CURRICULUM_EDIT')")
    public ResponseEntity<CurriculumVersionDto> update(
            @PathVariable Long id,
            @Valid @RequestBody CurriculumVersionRequest request) {
        return ResponseEntity.ok(curriculumVersionService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.has('CURRICULUM_DELETE')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        curriculumVersionService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/clone")
    @PreAuthorize("@perm.has('CURRICULUM_CREATE')")
    public ResponseEntity<CurriculumVersionDto> clone(
            @PathVariable Long id,
            @RequestParam String newVersionName,
            @RequestParam Long newEffectiveAcademicYearId) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(curriculumVersionService.cloneCurriculumVersion(id, newVersionName, newEffectiveAcademicYearId));
    }
}
