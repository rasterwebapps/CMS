package com.cms.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
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
import com.cms.service.UserPermissionService;

@RestController
@RequestMapping("/permissions")
public class PermissionController {

    private final AppUserRepository appUserRepository;
    private final PermissionRepository permissionRepository;
    private final UserPermissionService userPermissionService;
    private final UserDashboardWidgetConfigRepository userWidgetConfigRepo;

    public PermissionController(AppUserRepository appUserRepository,
                                PermissionRepository permissionRepository,
                                UserPermissionService userPermissionService,
                                UserDashboardWidgetConfigRepository userWidgetConfigRepo) {
        this.appUserRepository    = appUserRepository;
        this.permissionRepository = permissionRepository;
        this.userPermissionService = userPermissionService;
        this.userWidgetConfigRepo  = userWidgetConfigRepo;
    }

    /**
     * Returns the current user's role, permissions, and resolved dashboard widget config.
     * Resolution order: user personal config → role default → empty list.
     */
    @GetMapping("/my")
    public ResponseEntity<MyPermissionsResponse> getMyPermissions(
            @AuthenticationPrincipal Jwt jwt) {
        String username = jwt.getClaimAsString("preferred_username");
        AppUser user = appUserRepository.findByKeycloakUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException(
                "No app user record found for username: " + username));

        AppRole role = user.getAppRole();
        List<String> permissionCodes = userPermissionService.getPermissions(username).stream()
            .sorted()
            .toList();

        // User-level override takes priority; fall back to role default
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

    /** Returns all defined permissions with code, displayName and category for the editor UI. */
    @GetMapping("/all")
    @PreAuthorize("@perm.has('ROLE_VIEW')")
    public ResponseEntity<List<PermissionDetail>> getAllPermissions() {
        List<PermissionDetail> details = permissionRepository.findAll().stream()
            .sorted(java.util.Comparator.comparing(Permission::getCategory)
                .thenComparing(Permission::getCode))
            .map(p -> new PermissionDetail(p.getCode(), p.getDisplayName(), p.getCategory()))
            .toList();
        return ResponseEntity.ok(details);
    }

    public record PermissionDetail(String code, String displayName, String category) {}
}
