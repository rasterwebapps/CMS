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

import com.cms.dto.SystemConfigurationRequest;
import com.cms.dto.SystemConfigurationResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.service.SystemConfigurationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/system-configurations")
public class SystemConfigurationController {

    private final SystemConfigurationService systemConfigurationService;

    public SystemConfigurationController(SystemConfigurationService systemConfigurationService) {
        this.systemConfigurationService = systemConfigurationService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<SystemConfigurationResponse> create(
            @Valid @RequestBody SystemConfigurationRequest request) {
        SystemConfigurationResponse response = systemConfigurationService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<SystemConfigurationResponse>> findAll(
            @RequestParam(required = false) String category) {
        List<SystemConfigurationResponse> configurations;
        if (category != null) {
            configurations = systemConfigurationService.findByCategory(category);
        } else {
            configurations = systemConfigurationService.findAll();
        }
        return ResponseEntity.ok(configurations);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SystemConfigurationResponse> findById(@PathVariable Long id) {
        SystemConfigurationResponse response = systemConfigurationService.findById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/key/{key}")
    public ResponseEntity<SystemConfigurationResponse> findByKey(@PathVariable String key) {
        SystemConfigurationResponse response = systemConfigurationService.findByKey(key)
            .orElseThrow(() -> new ResourceNotFoundException(
                "System configuration not found with key: " + key));
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<SystemConfigurationResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody SystemConfigurationRequest request) {
        SystemConfigurationResponse response = systemConfigurationService.update(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        systemConfigurationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
