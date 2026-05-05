package com.cms.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cms.dto.AppUserResponse;
import com.cms.dto.CreateUserRequest;
import com.cms.dto.UpdateUserRequest;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.AppUser;
import com.cms.repository.AppUserRepository;
import com.cms.service.AppUserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/user-management")
@PreAuthorize("@perm.has('USER_VIEW')")
public class UserManagementController {

    private final AppUserService appUserService;
    private final AppUserRepository appUserRepository;

    public UserManagementController(AppUserService appUserService,
                                    AppUserRepository appUserRepository) {
        this.appUserService = appUserService;
        this.appUserRepository = appUserRepository;
    }

    /** Lists all users whose role level is strictly greater than the current user's level. */
    @GetMapping
    public ResponseEntity<List<AppUserResponse>> listManageableUsers(
            @AuthenticationPrincipal Jwt jwt) {
        int requesterLevel = resolveHierarchyLevel(jwt);
        return ResponseEntity.ok(appUserService.findManageable(requesterLevel));
    }

    /** Creates a new user. The assigned role must be below the requester's level. */
    @PostMapping
    public ResponseEntity<AppUserResponse> createUser(
            @Valid @RequestBody CreateUserRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String createdBy = jwt.getClaimAsString("preferred_username");
        int requesterLevel = resolveHierarchyLevel(jwt);
        AppUserResponse created = appUserService.create(request, createdBy, requesterLevel);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /** Updates user details. Only works on users below the requester's level. */
    @PutMapping("/{id}")
    public ResponseEntity<AppUserResponse> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String actor = jwt.getClaimAsString("preferred_username");
        int requesterLevel = resolveHierarchyLevel(jwt);
        AppUserResponse updated = appUserService.update(id, request, requesterLevel, actor);
        return ResponseEntity.ok(updated);
    }

    /** Deactivates a user. Only works on users below the requester's level. */
    @PutMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivateUser(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {
        String actor = jwt.getClaimAsString("preferred_username");
        int requesterLevel = resolveHierarchyLevel(jwt);
        appUserService.deactivate(id, requesterLevel, actor);
        return ResponseEntity.noContent().build();
    }

    /** Reactivates a previously deactivated user. */
    @PutMapping("/{id}/reactivate")
    public ResponseEntity<Void> reactivateUser(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {
        String actor = jwt.getClaimAsString("preferred_username");
        int requesterLevel = resolveHierarchyLevel(jwt);
        appUserService.reactivate(id, requesterLevel, actor);
        return ResponseEntity.noContent().build();
    }

    // -------------------------------------------------------------------------

    private int resolveHierarchyLevel(Jwt jwt) {
        String username = jwt.getClaimAsString("preferred_username");
        AppUser user = appUserRepository.findByKeycloakUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException(
                "No app user record found for username: " + username));
        return user.getAppRole() != null ? user.getAppRole().getHierarchyLevel() : Integer.MAX_VALUE;
    }
}
