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

import com.cms.dto.StaffReferrerRequest;
import com.cms.dto.StaffReferrerResponse;
import com.cms.service.StaffReferrerService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/staff-referrers")
public class StaffReferrerController {

    private final StaffReferrerService service;

    public StaffReferrerController(StaffReferrerService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("@perm.has('STAFF_REFERRER_MANAGE')")
    public ResponseEntity<StaffReferrerResponse> create(@Valid @RequestBody StaffReferrerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @GetMapping
    public ResponseEntity<List<StaffReferrerResponse>> findAll(
            @RequestParam(required = false) Boolean active) {
        List<StaffReferrerResponse> list = Boolean.TRUE.equals(active)
            ? service.findActive()
            : service.findAll();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    public ResponseEntity<StaffReferrerResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('STAFF_REFERRER_MANAGE')")
    public ResponseEntity<StaffReferrerResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody StaffReferrerRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.has('STAFF_REFERRER_MANAGE')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/name-exists")
    @PreAuthorize("@perm.has('STAFF_REFERRER_MANAGE')")
    public ResponseEntity<Boolean> nameExists(
            @RequestParam String value,
            @RequestParam(required = false) Long excludeId) {
        return ResponseEntity.ok(service.nameExists(value, excludeId));
    }
}
