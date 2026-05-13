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

import com.cms.dto.AppRoleRequest;
import com.cms.dto.AppRoleResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.AppUser;
import com.cms.repository.AppUserRepository;
import com.cms.service.AppRoleService;
import com.cms.service.UserPermissionService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/role-management")
@PreAuthorize("@perm.has('ROLE_VIEW')")
public class RoleManagementController {

    private final AppRoleService appRoleService;
    private final AppUserRepository appUserRepository;
    private final UserPermissionService userPermissionService;

    public RoleManagementController(AppRoleService appRoleService,
                                    AppUserRepository appUserRepository,
                                    UserPermissionService userPermissionService) {
        this.appRoleService = appRoleService;
        this.appUserRepository = appUserRepository;
        this.userPermissionService = userPermissionService;
    }

    /** Lists roles that the current user is permitted to assign (level > own level). */
    @GetMapping
    public ResponseEntity<List<AppRoleResponse>> listAssignableRoles(
            @AuthenticationPrincipal Jwt jwt) {
        int requesterLevel = resolveHierarchyLevel(jwt);
        return ResponseEntity.ok(appRoleService.findAssignableRoles(requesterLevel));
    }

    /** Returns full detail (including permissions) for a single role. */
    @GetMapping("/{id}")
    public ResponseEntity<AppRoleResponse> getRole(@PathVariable Long id) {
        return ResponseEntity.ok(appRoleService.findById(id));
    }

    /** Creates a new custom role. */
    @PostMapping
    public ResponseEntity<AppRoleResponse> createRole(
            @Valid @RequestBody AppRoleRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String actor = jwt.getClaimAsString("preferred_username");
        int requesterLevel = resolveHierarchyLevel(jwt);
        AppRoleResponse created = appRoleService.create(request, requesterLevel, actor);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /** Replaces the ordered dashboard widget list for a role. */
    @PutMapping("/{id}/dashboard-widgets")
    public ResponseEntity<AppRoleResponse> updateDashboardWidgets(
            @PathVariable Long id,
            @RequestBody List<String> widgetKeys,
            @AuthenticationPrincipal Jwt jwt) {
        String actor = jwt.getClaimAsString("preferred_username");
        int requesterLevel = resolveHierarchyLevel(jwt);
        return ResponseEntity.ok(appRoleService.updateDashboardWidgets(id, widgetKeys, actor, requesterLevel));
    }

    /** Replaces the permission set of an existing role. */
    @PutMapping("/{id}/permissions")
    public ResponseEntity<AppRoleResponse> updatePermissions(
            @PathVariable Long id,
            @RequestBody List<String> permissionCodes,
            @AuthenticationPrincipal Jwt jwt) {
        String username = jwt.getClaimAsString("preferred_username");
        int requesterLevel = resolveHierarchyLevel(jwt);
        var requesterPermissions = userPermissionService.getPermissions(username);
        AppRoleResponse updated = appRoleService.updatePermissions(id, permissionCodes, requesterPermissions,
            username, requesterLevel);
        return ResponseEntity.ok(updated);
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
