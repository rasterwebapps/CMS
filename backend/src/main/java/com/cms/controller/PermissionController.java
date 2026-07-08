package com.cms.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cms.dto.MyPermissionsResponse;
import com.cms.dto.WidgetConfigDto;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.AppRole;
import com.cms.model.AppUser;
import com.cms.model.Permission;
import com.cms.model.RoleDashboardWidgetConfig;
import com.cms.model.UserDashboardWidgetConfig;
import com.cms.repository.AppUserRepository;
import com.cms.repository.PermissionRepository;
import com.cms.repository.UserDashboardWidgetConfigRepository;
import com.cms.service.AuditLogService;
import com.cms.service.UserPermissionService;

@RestController
@RequestMapping("/permissions")
public class PermissionController {

    private final AppUserRepository appUserRepository;
    private final PermissionRepository permissionRepository;
    private final UserPermissionService userPermissionService;
    private final UserDashboardWidgetConfigRepository userWidgetConfigRepo;
    private final AuditLogService auditLogService;

    public PermissionController(AppUserRepository appUserRepository,
                                PermissionRepository permissionRepository,
                                UserPermissionService userPermissionService,
                                UserDashboardWidgetConfigRepository userWidgetConfigRepo,
                                AuditLogService auditLogService) {
        this.appUserRepository    = appUserRepository;
        this.permissionRepository = permissionRepository;
        this.userPermissionService = userPermissionService;
        this.userWidgetConfigRepo  = userWidgetConfigRepo;
        this.auditLogService       = auditLogService;
    }

    /**
     * Returns the current user's role, permissions, and resolved dashboard widget config.
     * Resolution order: user personal config → role default → empty list.
     */
    @GetMapping("/my")
    public ResponseEntity<MyPermissionsResponse> getMyPermissions(
            @AuthenticationPrincipal Jwt jwt) {
        String username = jwt.getClaimAsString("preferred_username");
        AppUser user = appUserRepository.findByKeycloakUsernameWithRole(username)
            .orElseThrow(() -> new ResourceNotFoundException(
                "No app user record found for username: " + username));

        AppRole role = user.getAppRole();
        List<String> permissionCodes = userPermissionService.getPermissions(username).stream()
            .sorted()
            .toList();

        List<WidgetConfigDto> widgetConfigs;
        List<UserDashboardWidgetConfig> userConfigs =
            userWidgetConfigRepo.findByUserIdOrderByWidgetOrderAsc(user.getId());

        if (!userConfigs.isEmpty()) {
            widgetConfigs = userConfigs.stream()
                .map(c -> new WidgetConfigDto(c.getWidgetKey(), c.getWidgetOrder(),
                                              c.getColSpan(), c.getRowSpan(), c.getConfigJson()))
                .toList();
        } else if (role != null) {
            widgetConfigs = role.getWidgetConfigs().stream()
                .map(c -> new WidgetConfigDto(c.getWidgetKey(), c.getWidgetOrder(),
                                              c.getColSpan(), c.getRowSpan(), c.getConfigJson()))
                .toList();
        } else {
            widgetConfigs = List.of();
        }

        MyPermissionsResponse response = new MyPermissionsResponse(
            username,
            role != null ? role.getName() : null,
            role != null ? role.getDisplayName() : null,
            role != null ? role.getHierarchyLevel() : 0,
            permissionCodes,
            widgetConfigs
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Returns ALL permissions with tier info — for the Permission Tier Management screen.
     * Requires PERMISSION_TIER_MANAGE (DEV_ADMIN only).
     */
    @GetMapping("/all")
    @PreAuthorize("@perm.has('ROLE_VIEW')")
    public ResponseEntity<List<PermissionDetail>> getAllPermissions() {
        List<PermissionDetail> details = permissionRepository.findAll().stream()
            .sorted(java.util.Comparator.comparing(Permission::getCategory)
                .thenComparing(Permission::getCode))
            .map(p -> new PermissionDetail(p.getId(), p.getCode(), p.getDisplayName(), p.getCategory(), p.getTier(), p.getScreenLabel()))
            .toList();
        return ResponseEntity.ok(details);
    }

    /**
     * Returns only the permissions that the current user is permitted to delegate
     * (assign to sub-roles). Used by the Role Management editor picker.
     *
     * Delegation rules by tier:
     *   Tier 1 → only hierarchy_level 1 (DEV_ADMIN)
     *   Tier 2 → hierarchy_level ≤ 2 (DEV_ADMIN, SUPPORT_ADMIN)
     *   Tier 3 → hierarchy_level ≤ 2 (same as tier 2 — senior roles hold but cannot delegate)
     *   Tier 4 → anyone
     */
    @GetMapping("/delegatable")
    @PreAuthorize("@perm.has('ROLE_VIEW')")
    public ResponseEntity<List<PermissionDetail>> getDelegatablePermissions(
            @AuthenticationPrincipal Jwt jwt) {
        int callerLevel = resolveHierarchyLevel(jwt);
        List<PermissionDetail> details = permissionRepository.findAll().stream()
            .filter(p -> canDelegate(p.getTier(), callerLevel))
            .sorted(java.util.Comparator.comparing(Permission::getCategory)
                .thenComparing(Permission::getCode))
            .map(p -> new PermissionDetail(p.getId(), p.getCode(), p.getDisplayName(), p.getCategory(), p.getTier(), p.getScreenLabel()))
            .toList();
        return ResponseEntity.ok(details);
    }

    /**
     * Updates the tier of a single permission.
     * Requires PERMISSION_TIER_MANAGE (DEV_ADMIN only).
     * Changes are audit-logged.
     */
    @PutMapping("/{id}/tier")
    @PreAuthorize("@perm.has('PERMISSION_TIER_MANAGE')")
    public ResponseEntity<PermissionDetail> updateTier(
            @PathVariable Long id,
            @RequestBody TierUpdateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        if (request.tier() < 1 || request.tier() > 4) {
            throw new IllegalArgumentException("Tier must be between 1 and 4");
        }
        Permission perm = permissionRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Permission not found with id: " + id));

        int previousTier = perm.getTier();
        perm.setTier(request.tier());
        permissionRepository.save(perm);

        String actor = jwt.getClaimAsString("preferred_username");
        auditLogService.record(actor, "PERMISSION_TIER_CHANGED", "Permission",
            String.valueOf(id),
            "'" + perm.getCode() + "' tier changed from " + previousTier + " to " + request.tier());

        return ResponseEntity.ok(
            new PermissionDetail(perm.getId(), perm.getCode(), perm.getDisplayName(), perm.getCategory(), perm.getTier(), perm.getScreenLabel()));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private int resolveHierarchyLevel(Jwt jwt) {
        String username = jwt.getClaimAsString("preferred_username");
        AppUser user = appUserRepository.findByKeycloakUsernameWithRole(username)
            .orElseThrow(() -> new ResourceNotFoundException(
                "No app user record found for username: " + username));
        return user.getAppRole() != null ? user.getAppRole().getHierarchyLevel() : Integer.MAX_VALUE;
    }

    static boolean canDelegate(int tier, int callerLevel) {
        return switch (tier) {
            case 1 -> callerLevel <= 1;
            case 2, 3 -> callerLevel <= 2;
            default -> true;
        };
    }

    public record PermissionDetail(Long id, String code, String displayName, String category, int tier, String screenLabel) {}
    public record TierUpdateRequest(int tier) {}
}
