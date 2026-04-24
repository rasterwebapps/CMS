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

import com.cms.dto.ReferralTypeRequest;
import com.cms.dto.ReferralTypeResponse;
import com.cms.service.ReferralTypeService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/referral-types")
public class ReferralTypeController {

    private final ReferralTypeService referralTypeService;

    public ReferralTypeController(ReferralTypeService referralTypeService) {
        this.referralTypeService = referralTypeService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_COLLEGE_ADMIN')")
    public ResponseEntity<ReferralTypeResponse> create(@Valid @RequestBody ReferralTypeRequest request) {
        ReferralTypeResponse response = referralTypeService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ReferralTypeResponse>> findAll(
            @RequestParam(required = false) Boolean activeOnly) {
        List<ReferralTypeResponse> types;
        if (Boolean.TRUE.equals(activeOnly)) {
            types = referralTypeService.findActive();
        } else {
            types = referralTypeService.findAll();
        }
        return ResponseEntity.ok(types);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReferralTypeResponse> findById(@PathVariable Long id) {
        ReferralTypeResponse response = referralTypeService.findById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_COLLEGE_ADMIN')")
    public ResponseEntity<ReferralTypeResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ReferralTypeRequest request) {
        ReferralTypeResponse response = referralTypeService.update(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_COLLEGE_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        referralTypeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
