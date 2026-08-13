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
import org.springframework.web.bind.annotation.RestController;

import com.cms.dto.DayMappingOverrideRequest;
import com.cms.dto.DayMappingOverrideResponse;
import com.cms.service.DayMappingOverrideService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/day-mappings")
public class DayMappingOverrideController {

    private final DayMappingOverrideService dayMappingOverrideService;

    public DayMappingOverrideController(DayMappingOverrideService dayMappingOverrideService) {
        this.dayMappingOverrideService = dayMappingOverrideService;
    }

    @PostMapping
    @PreAuthorize("@perm.has('TIMETABLE_DAY_MAPPING_MANAGE')")
    public ResponseEntity<DayMappingOverrideResponse> create(@Valid @RequestBody DayMappingOverrideRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(dayMappingOverrideService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<DayMappingOverrideResponse>> findAll() {
        return ResponseEntity.ok(dayMappingOverrideService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DayMappingOverrideResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(dayMappingOverrideService.findById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('TIMETABLE_DAY_MAPPING_MANAGE')")
    public ResponseEntity<DayMappingOverrideResponse> update(@PathVariable Long id,
                                                               @Valid @RequestBody DayMappingOverrideRequest request) {
        return ResponseEntity.ok(dayMappingOverrideService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.has('TIMETABLE_DAY_MAPPING_MANAGE')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        dayMappingOverrideService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
