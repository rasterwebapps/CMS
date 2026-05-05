package com.cms.service;

import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.cms.dto.AppRoleRequest;
import com.cms.dto.AppRoleResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.AppRole;
import com.cms.model.Permission;
import com.cms.repository.AppRoleRepository;
import com.cms.repository.PermissionRepository;

@Service
@Transactional(readOnly = true)
public class AppRoleService {

    private final AppRoleRepository appRoleRepository;
    private final PermissionRepository permissionRepository;
    private final UserPermissionService userPermissionService;
    private final AuditLogService auditLogService;

    public AppRoleService(AppRoleRepository appRoleRepository,
                          PermissionRepository permissionRepository,
                          UserPermissionService userPermissionService,
                          AuditLogService auditLogService) {
        this.appRoleRepository = appRoleRepository;
        this.permissionRepository = permissionRepository;
        this.userPermissionService = userPermissionService;
        this.auditLogService = auditLogService;
    }

    /** Returns roles with hierarchy_level strictly greater than the requester's level. */
    public List<AppRoleResponse> findAssignableRoles(int requesterLevel) {
        return appRoleRepository.findByHierarchyLevelGreaterThan(requesterLevel).stream()
            .map(this::toResponse)
            .toList();
    }

    public AppRoleResponse findById(Long id) {
        AppRole role = appRoleRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + id));
        return toResponse(role);
    }

    public List<AppRoleResponse> findAll() {
        return appRoleRepository.findAllByOrderByHierarchyLevelAsc().stream()
            .map(this::toResponse)
            .toList();
    }

    /**
     * Creates a new (non-system) role. The role is assigned a hierarchy_level one greater
     * than the requester's level, placing it just below the requester.
     */
    @Transactional
    public AppRoleResponse create(AppRoleRequest request, int requesterLevel, String actor) {
        if (appRoleRepository.findByName(request.name()).isPresent()) {
            throw new IllegalArgumentException("A role with name '" + request.name() + "' already exists");
        }

        // New custom roles are placed at the level directly beneath the requester
        int newLevel = requesterLevel + 1;

        AppRole role = new AppRole(request.name(), request.displayName(), newLevel, false, request.description());

        if (request.permissionCodes() != null && !request.permissionCodes().isEmpty()) {
            List<Permission> permissions = permissionRepository.findByCodeIn(request.permissionCodes());
            role.getPermissions().addAll(permissions);
        }

        AppRole saved = appRoleRepository.save(role);
        auditLogService.record(actor, "ROLE_CREATED", "AppRole",
            String.valueOf(saved.getId()),
            "Created role '" + saved.getName() + "' at level " + newLevel);
        return toResponse(saved);
    }

    /**
     * Backwards-compatible overload — actor defaults to "system".
     * @deprecated Prefer {@link #create(AppRoleRequest, int, String)}.
     */
    @Deprecated
    @Transactional
    public AppRoleResponse create(AppRoleRequest request, int requesterLevel) {
        return create(request, requesterLevel, "system");
    }

    /**
     * Replaces the permission set of a role. The requester may only assign permissions
     * that they themselves hold (no privilege escalation).
     */
    @Transactional
    public AppRoleResponse updatePermissions(Long roleId, List<String> permissionCodes,
                                             Set<String> requesterPermissions, String actor) {
        AppRole role = appRoleRepository.findById(roleId)
            .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + roleId));

        if (permissionCodes != null) {
            for (String code : permissionCodes) {
                if (!requesterPermissions.contains(code)) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "You do not hold permission '" + code + "' and cannot assign it");
                }
            }

            List<Permission> permissions = permissionCodes.isEmpty()
                ? List.of()
                : permissionRepository.findByCodeIn(permissionCodes);

            role.getPermissions().clear();
            role.getPermissions().addAll(permissions);
        }

        AppRole updated = appRoleRepository.save(role);
        userPermissionService.evictAll();
        auditLogService.record(actor, "PERMISSIONS_UPDATED", "AppRole",
            String.valueOf(updated.getId()),
            "Permissions set to: " + permissionCodes);
        return toResponse(updated);
    }

    /**
     * Backwards-compatible overload — actor defaults to "system".
     * @deprecated Prefer {@link #updatePermissions(Long, List, Set, String)}.
     */
    @Deprecated
    @Transactional
    public AppRoleResponse updatePermissions(Long roleId, List<String> permissionCodes,
                                             Set<String> requesterPermissions) {
        return updatePermissions(roleId, permissionCodes, requesterPermissions, "system");
    }

    public List<String> getPermissions(Long roleId) {
        AppRole role = appRoleRepository.findById(roleId)
            .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + roleId));
        return role.getPermissions().stream()
            .map(Permission::getCode)
            .sorted()
            .toList();
    }

    private AppRoleResponse toResponse(AppRole role) {
        List<String> codes = role.getPermissions().stream()
            .map(Permission::getCode)
            .sorted()
            .toList();
        return new AppRoleResponse(
            role.getId(),
            role.getName(),
            role.getDisplayName(),
            role.getHierarchyLevel(),
            role.isSystemRole(),
            role.getDescription(),
            codes
        );
    }
}
