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

import com.cms.dto.EnquiryRequest;
import com.cms.dto.EnquiryResponse;
import com.cms.model.enums.EnquirySource;
import com.cms.model.enums.EnquiryStatus;
import com.cms.service.EnquiryService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/enquiries")
public class EnquiryController {

    private final EnquiryService enquiryService;

    public EnquiryController(EnquiryService enquiryService) {
        this.enquiryService = enquiryService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<EnquiryResponse> create(@Valid @RequestBody EnquiryRequest request) {
        EnquiryResponse response = enquiryService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<EnquiryResponse>> findAll(
            @RequestParam(required = false) EnquiryStatus status,
            @RequestParam(required = false) EnquirySource source) {
        List<EnquiryResponse> enquiries;
        if (status != null) {
            enquiries = enquiryService.findByStatus(status);
        } else if (source != null) {
            enquiries = enquiryService.findBySource(source);
        } else {
            enquiries = enquiryService.findAll();
        }
        return ResponseEntity.ok(enquiries);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EnquiryResponse> findById(@PathVariable Long id) {
        EnquiryResponse response = enquiryService.findById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<EnquiryResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody EnquiryRequest request) {
        EnquiryResponse response = enquiryService.update(id, request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/convert")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<EnquiryResponse> convertToStudent(
            @PathVariable Long id,
            @RequestParam Long studentId) {
        EnquiryResponse response = enquiryService.convertToStudent(id, studentId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        enquiryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
