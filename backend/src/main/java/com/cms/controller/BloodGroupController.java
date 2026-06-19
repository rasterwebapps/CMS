package com.cms.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cms.dto.BloodGroupRequest;
import com.cms.dto.BloodGroupResponse;
import com.cms.dto.ActiveStatusUpdateRequest;
import com.cms.dto.ActiveStatusUpdateResponse;
import com.cms.service.BloodGroupService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/blood-groups")
public class BloodGroupController {

    private final BloodGroupService bloodGroupService;

    public BloodGroupController(BloodGroupService bloodGroupService) {
        this.bloodGroupService = bloodGroupService;
    }

    @PostMapping
    @PreAuthorize("@perm.has('BLOOD_GROUP_MANAGE')")
    public ResponseEntity<BloodGroupResponse> create(@Valid @RequestBody BloodGroupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bloodGroupService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<BloodGroupResponse>> findAll(
            @RequestParam(required = false, defaultValue = "false") boolean activeOnly) {
        List<BloodGroupResponse> result = activeOnly
            ? bloodGroupService.findActive()
            : bloodGroupService.findAll();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BloodGroupResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(bloodGroupService.findById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('BLOOD_GROUP_MANAGE')")
    public ResponseEntity<BloodGroupResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody BloodGroupRequest request) {
        return ResponseEntity.ok(bloodGroupService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.has('BLOOD_GROUP_MANAGE')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        bloodGroupService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("@perm.has('BLOOD_GROUP_MANAGE')")
    public ResponseEntity<ActiveStatusUpdateResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody ActiveStatusUpdateRequest request) {
        return ResponseEntity.ok(bloodGroupService.updateStatus(id, request));
    }

    @PutMapping("/{id}/deactivate")
    @PreAuthorize("@perm.has('BLOOD_GROUP_MANAGE')")
    public ResponseEntity<BloodGroupResponse> deactivate(@PathVariable Long id) {
        return ResponseEntity.ok(bloodGroupService.deactivate(id));
    }

    @PutMapping("/{id}/reactivate")
    @PreAuthorize("@perm.has('BLOOD_GROUP_MANAGE')")
    public ResponseEntity<BloodGroupResponse> reactivate(@PathVariable Long id) {
        return ResponseEntity.ok(bloodGroupService.reactivate(id));
    }

    @GetMapping("/name-exists")
    @PreAuthorize("@perm.has('BLOOD_GROUP_MANAGE')")
    public ResponseEntity<Boolean> nameExists(
            @RequestParam String value,
            @RequestParam(required = false) Long excludeId) {
        return ResponseEntity.ok(bloodGroupService.nameExists(value, excludeId));
    }

    @GetMapping("/code-exists")
    @PreAuthorize("@perm.has('BLOOD_GROUP_MANAGE')")
    public ResponseEntity<Boolean> codeExists(
            @RequestParam String value,
            @RequestParam(required = false) Long excludeId) {
        return ResponseEntity.ok(bloodGroupService.codeExists(value, excludeId));
    }
}
