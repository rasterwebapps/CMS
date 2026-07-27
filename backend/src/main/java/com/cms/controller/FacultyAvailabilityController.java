package com.cms.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cms.dto.FacultyAvailabilityRequest;
import com.cms.dto.FacultyAvailabilityResponse;
import com.cms.service.FacultyAvailabilityService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/faculty-availability")
public class FacultyAvailabilityController {

    private final FacultyAvailabilityService facultyAvailabilityService;

    public FacultyAvailabilityController(FacultyAvailabilityService facultyAvailabilityService) {
        this.facultyAvailabilityService = facultyAvailabilityService;
    }

    @GetMapping
    @PreAuthorize("@perm.has('FACULTY_AVAILABILITY_VIEW') or @perm.has('FACULTY_AVAILABILITY_MANAGE')")
    public ResponseEntity<List<FacultyAvailabilityResponse>> listForFaculty(@RequestParam Long facultyId) {
        return ResponseEntity.ok(facultyAvailabilityService.listForFaculty(facultyId));
    }

    @PostMapping
    @PreAuthorize("@perm.has('FACULTY_AVAILABILITY_MANAGE')")
    public ResponseEntity<FacultyAvailabilityResponse> addBlock(@Valid @RequestBody FacultyAvailabilityRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(facultyAvailabilityService.addBlock(request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.has('FACULTY_AVAILABILITY_MANAGE')")
    public ResponseEntity<Void> removeBlock(@PathVariable Long id) {
        facultyAvailabilityService.removeBlock(id);
        return ResponseEntity.noContent().build();
    }
}
