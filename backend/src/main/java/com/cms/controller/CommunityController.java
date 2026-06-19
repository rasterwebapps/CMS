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

import com.cms.dto.CommunityRequest;
import com.cms.dto.CommunityResponse;
import com.cms.dto.ActiveStatusUpdateRequest;
import com.cms.dto.ActiveStatusUpdateResponse;
import com.cms.service.CommunityService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/communities")
public class CommunityController {

    private final CommunityService communityService;

    public CommunityController(CommunityService communityService) {
        this.communityService = communityService;
    }

    @PostMapping
    @PreAuthorize("@perm.has('COMMUNITY_MANAGE')")
    public ResponseEntity<CommunityResponse> create(@Valid @RequestBody CommunityRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(communityService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<CommunityResponse>> findAll(
            @RequestParam(required = false, defaultValue = "false") boolean activeOnly) {
        List<CommunityResponse> result = activeOnly
            ? communityService.findActive()
            : communityService.findAll();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommunityResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(communityService.findById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('COMMUNITY_MANAGE')")
    public ResponseEntity<CommunityResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody CommunityRequest request) {
        return ResponseEntity.ok(communityService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.has('COMMUNITY_MANAGE')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        communityService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("@perm.has('COMMUNITY_MANAGE')")
    public ResponseEntity<ActiveStatusUpdateResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody ActiveStatusUpdateRequest request) {
        return ResponseEntity.ok(communityService.updateStatus(id, request));
    }

    @PutMapping("/{id}/deactivate")
    @PreAuthorize("@perm.has('COMMUNITY_MANAGE')")
    public ResponseEntity<CommunityResponse> deactivate(@PathVariable Long id) {
        return ResponseEntity.ok(communityService.deactivate(id));
    }

    @PutMapping("/{id}/reactivate")
    @PreAuthorize("@perm.has('COMMUNITY_MANAGE')")
    public ResponseEntity<CommunityResponse> reactivate(@PathVariable Long id) {
        return ResponseEntity.ok(communityService.reactivate(id));
    }

    @GetMapping("/name-exists")
    @PreAuthorize("@perm.has('COMMUNITY_MANAGE')")
    public ResponseEntity<Boolean> nameExists(
            @RequestParam String value,
            @RequestParam(required = false) Long excludeId) {
        return ResponseEntity.ok(communityService.nameExists(value, excludeId));
    }

    @GetMapping("/code-exists")
    @PreAuthorize("@perm.has('COMMUNITY_MANAGE')")
    public ResponseEntity<Boolean> codeExists(
            @RequestParam String value,
            @RequestParam(required = false) Long excludeId) {
        return ResponseEntity.ok(communityService.codeExists(value, excludeId));
    }
}
