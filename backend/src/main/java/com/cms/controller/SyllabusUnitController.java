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

import com.cms.dto.SyllabusUnitDto;
import com.cms.dto.SyllabusUnitRequest;
import com.cms.service.SyllabusUnitService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/syllabus-units")
public class SyllabusUnitController {

    private final SyllabusUnitService service;

    public SyllabusUnitController(SyllabusUnitService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("@perm.has('SYLLABUS_UNIT_MANAGE')")
    public ResponseEntity<SyllabusUnitDto> create(@Valid @RequestBody SyllabusUnitRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @GetMapping
    @PreAuthorize("@perm.has('SYLLABUS_UNIT_VIEW')")
    public ResponseEntity<List<SyllabusUnitDto>> getUnitsForCourse(
            @RequestParam Long curriculumTermCourseId) {
        return ResponseEntity.ok(service.getUnitsForCourse(curriculumTermCourseId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('SYLLABUS_UNIT_MANAGE')")
    public ResponseEntity<SyllabusUnitDto> update(
            @PathVariable Long id, @Valid @RequestBody SyllabusUnitRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.has('SYLLABUS_UNIT_MANAGE')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/unit-number-exists")
    @PreAuthorize("@perm.has('SYLLABUS_UNIT_MANAGE')")
    public ResponseEntity<Boolean> unitNumberExists(
            @RequestParam String value,
            @RequestParam Long curriculumTermCourseId,
            @RequestParam(required = false) Long excludeId) {
        return ResponseEntity.ok(service.unitNumberExists(curriculumTermCourseId, Integer.valueOf(value), excludeId));
    }
}
