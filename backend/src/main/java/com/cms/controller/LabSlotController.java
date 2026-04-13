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

import com.cms.dto.LabSlotRequest;
import com.cms.dto.LabSlotResponse;
import com.cms.service.LabSlotService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/lab-slots")
public class LabSlotController {

    private final LabSlotService labSlotService;

    public LabSlotController(LabSlotService labSlotService) {
        this.labSlotService = labSlotService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<LabSlotResponse> create(@Valid @RequestBody LabSlotRequest request) {
        LabSlotResponse response = labSlotService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<LabSlotResponse>> findAll(
            @RequestParam(required = false) Boolean activeOnly) {
        List<LabSlotResponse> labSlots;
        if (Boolean.TRUE.equals(activeOnly)) {
            labSlots = labSlotService.findAllActive();
        } else {
            labSlots = labSlotService.findAll();
        }
        return ResponseEntity.ok(labSlots);
    }

    @GetMapping("/{id}")
    public ResponseEntity<LabSlotResponse> findById(@PathVariable Long id) {
        LabSlotResponse response = labSlotService.findById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<LabSlotResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody LabSlotRequest request) {
        LabSlotResponse response = labSlotService.update(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        labSlotService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
