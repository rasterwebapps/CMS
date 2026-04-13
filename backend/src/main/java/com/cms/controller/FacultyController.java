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

import com.cms.dto.FacultyRequest;
import com.cms.dto.FacultyResponse;
import com.cms.model.enums.FacultyStatus;
import com.cms.service.FacultyService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/faculty")
public class FacultyController {

    private final FacultyService facultyService;

    public FacultyController(FacultyService facultyService) {
        this.facultyService = facultyService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<FacultyResponse> create(@Valid @RequestBody FacultyRequest request) {
        FacultyResponse response = facultyService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<FacultyResponse>> findAll(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) FacultyStatus status) {
        List<FacultyResponse> facultyList;
        if (departmentId != null) {
            facultyList = facultyService.findByDepartmentId(departmentId);
        } else if (status != null) {
            facultyList = facultyService.findByStatus(status);
        } else {
            facultyList = facultyService.findAll();
        }
        return ResponseEntity.ok(facultyList);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FacultyResponse> findById(@PathVariable Long id) {
        FacultyResponse response = facultyService.findById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<FacultyResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody FacultyRequest request) {
        FacultyResponse response = facultyService.update(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        facultyService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
