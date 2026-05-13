package com.cms.service;

import java.util.List;
import java.util.Locale;
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

    /**
     * Role names that cannot be created manually — they are pre-seeded system roles
     * reserved exclusively for the development / platform support teams.
     */
    private static final Set<String> RESERVED_ROLE_NAMES = Set.of("DEVADMIN", "SUPPORTADMIN");

    /**
     * Role names whose permission set is absolutely immutable — nobody (not even
     * DEV_ADMIN) can alter what DEV_ADMIN can do.
     * SUPPORT_ADMIN is intentionally excluded: a DEV_ADMIN must be able to adjust
     * the support team's access level when needed.
     */
    private static final Set<String> TRULY_IMMUTABLE_ROLE_NAMES = Set.of("DEVADMIN");

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
        String normalizedName = normalizeRoleName(request.name());
        if (RESERVED_ROLE_NAMES.contains(normalizedName)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Role '" + request.name() + "' is reserved and cannot be created manually");
        }

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

        if (request.dashboardWidgets() != null) {
            role.getDashboardWidgets().addAll(request.dashboardWidgets());
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
     * that they themselves hold (no privilege escalation), and may only modify roles
     * that are strictly below their own hierarchy level.
     */
    @Transactional
    public AppRoleResponse updatePermissions(Long roleId, List<String> permissionCodes,
                                             Set<String> requesterPermissions, String actor,
                                             int requesterLevel) {
        AppRole role = appRoleRepository.findById(roleId)
            .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + roleId));

        ensureRoleIsEditable(role, requesterLevel);

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
     * Backwards-compatible overload — requester level defaults to 0 (bypasses hierarchy
     * check; immutability guard for DEV_ADMIN is still enforced).
     * @deprecated Prefer {@link #updatePermissions(Long, List, Set, String, int)}.
     */
    @Deprecated
    @Transactional
    public AppRoleResponse updatePermissions(Long roleId, List<String> permissionCodes,
                                             Set<String> requesterPermissions, String actor) {
        return updatePermissions(roleId, permissionCodes, requesterPermissions, actor, 0);
    }

    /** Replaces the dashboard widget list for a role in the given order. */
    @Transactional
    public AppRoleResponse updateDashboardWidgets(Long roleId, List<String> widgetKeys, String actor,
                                                  int requesterLevel) {
        AppRole role = appRoleRepository.findById(roleId)
            .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + roleId));

        ensureRoleIsEditable(role, requesterLevel);

        role.getDashboardWidgets().clear();
        if (widgetKeys != null) {
            role.getDashboardWidgets().addAll(widgetKeys);
        }

        AppRole updated = appRoleRepository.save(role);
        auditLogService.record(actor, "DASHBOARD_WIDGETS_UPDATED", "AppRole",
            String.valueOf(updated.getId()),
            "Dashboard widgets set to: " + widgetKeys);
        return toResponse(updated);
    }

    /**
     * Backwards-compatible overload — requester level defaults to 0 (bypasses hierarchy
     * check; immutability guard for DEV_ADMIN is still enforced).
     * @deprecated Prefer {@link #updateDashboardWidgets(Long, List, String, int)}.
     */
    @Deprecated
    @Transactional
    public AppRoleResponse updateDashboardWidgets(Long roleId, List<String> widgetKeys, String actor) {
        return updateDashboardWidgets(roleId, widgetKeys, actor, 0);
    }

    public List<String> getPermissions(Long roleId) {
        AppRole role = appRoleRepository.findById(roleId)
            .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + roleId));
        return role.getPermissions().stream()
            .map(Permission::getCode)
            .sorted()
            .toList();
    }

    private void ensureRoleIsEditable(AppRole role, int requesterLevel) {
        // DEV_ADMIN is absolutely immutable — nobody can change its permissions.
        if (TRULY_IMMUTABLE_ROLE_NAMES.contains(normalizeRoleName(role.getName()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Role '" + role.getName() + "' is immutable and cannot be modified");
        }
        // A role can only be modified by someone strictly above it in the hierarchy.
        // This prevents SUPPORT_ADMIN or lower from editing roles at their own level.
        if (role.getHierarchyLevel() <= requesterLevel) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "You cannot modify a role at or above your own hierarchy level");
        }
    }

    private String normalizeRoleName(String roleName) {
        return roleName.replace("_", "").toUpperCase(Locale.ROOT);
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
            codes,
            List.copyOf(role.getDashboardWidgets())
        );
    }
}
