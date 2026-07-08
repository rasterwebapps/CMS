package com.cms.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cms.dto.WidgetConfigDto;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.AppRole;
import com.cms.model.AppUser;
import com.cms.model.RoleDashboardWidgetConfig;
import com.cms.model.UserDashboardWidgetConfig;
import com.cms.repository.AppUserRepository;
import com.cms.repository.UserDashboardWidgetConfigRepository;

/**
 * Manages a user's personal dashboard widget layout.
 *
 * Resolution order: user config → role default → empty list.
 *
 * GET    /dashboard/config   — returns the resolved config for the caller
 * PUT    /dashboard/config   — saves a personal layout (requires DASHBOARD_CUSTOMIZE)
 * DELETE /dashboard/config   — clears personal overrides; resets to role default
 */
@RestController
@RequestMapping("/dashboard/config")
@PreAuthorize("isAuthenticated()")
public class UserDashboardConfigController {

    private final AppUserRepository               appUserRepository;
    private final UserDashboardWidgetConfigRepository userWidgetConfigRepo;

    public UserDashboardConfigController(
            AppUserRepository appUserRepository,
            UserDashboardWidgetConfigRepository userWidgetConfigRepo) {
        this.appUserRepository   = appUserRepository;
        this.userWidgetConfigRepo = userWidgetConfigRepo;
    }

    // ── GET ─────────────────────────────────────────────────────────────────

    /**
     * Returns the caller's resolved widget config.
     * If the user has a personal override, that is returned; otherwise the role default.
     * Used by the widget picker to show the current layout before editing.
     */
    @GetMapping
    public ResponseEntity<List<WidgetConfigDto>> getConfig(@AuthenticationPrincipal Jwt jwt) {
        AppUser user = resolveUser(jwt);
        return ResponseEntity.ok(resolveWidgetConfigs(user));
    }

    // ── PUT ─────────────────────────────────────────────────────────────────

    /**
     * Saves a personal dashboard layout for the caller.
     * The {@code order} field in each DTO is overridden by the position in the
     * list, so the caller does not need to manage order numbers.
     * Requires the {@code DASHBOARD_CUSTOMIZE} permission.
     */
    @PutMapping
    @Transactional
    @PreAuthorize("@perm.has('DASHBOARD_CUSTOMIZE')")
    public ResponseEntity<List<WidgetConfigDto>> saveConfig(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody List<WidgetConfigDto> configs) {

        AppUser user = resolveUser(jwt);

        // Clear existing personal config and replace atomically
        userWidgetConfigRepo.deleteAllByUserId(user.getId());

        if (configs != null) {
            int order = 0;
            for (WidgetConfigDto dto : configs) {
                UserDashboardWidgetConfig cfg = new UserDashboardWidgetConfig(
                    user,
                    dto.key(),
                    order++,
                    clampSpan(dto.colSpan(), 1, 4),
                    clampSpan(dto.rowSpan(), 1, 2)
                );
                cfg.setConfigJson(dto.configJson());
                userWidgetConfigRepo.save(cfg);
            }
        }

        // Return the freshly persisted config so the caller can confirm
        return ResponseEntity.ok(resolveWidgetConfigs(user));
    }

    // ── DELETE ──────────────────────────────────────────────────────────────

    /**
     * Clears the caller's personal layout, restoring the role default.
     * Returns 204 No Content on success.
     */
    @DeleteMapping
    @Transactional
    public ResponseEntity<Void> resetConfig(@AuthenticationPrincipal Jwt jwt) {
        AppUser user = resolveUser(jwt);
        userWidgetConfigRepo.deleteAllByUserId(user.getId());
        return ResponseEntity.noContent().build();
    }

    // ── Private helpers ─────────────────────────────────────────────────────

    private AppUser resolveUser(Jwt jwt) {
        String username = jwt.getClaimAsString("preferred_username");
        return appUserRepository.findByKeycloakUsernameWithRole(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }

    /**
     * Resolves the widget config for a user applying the standard priority:
     * personal override → role default → empty list.
     */
    private List<WidgetConfigDto> resolveWidgetConfigs(AppUser user) {
        List<UserDashboardWidgetConfig> personal =
            userWidgetConfigRepo.findByUserIdOrderByWidgetOrderAsc(user.getId());

        if (!personal.isEmpty()) {
            return personal.stream()
                .map(c -> new WidgetConfigDto(c.getWidgetKey(), c.getWidgetOrder(),
                                              c.getColSpan(), c.getRowSpan(), c.getConfigJson()))
                .toList();
        }

        AppRole role = user.getAppRole();
        if (role != null && !role.getWidgetConfigs().isEmpty()) {
            return role.getWidgetConfigs().stream()
                .map(c -> new WidgetConfigDto(c.getWidgetKey(), c.getWidgetOrder(),
                                              c.getColSpan(), c.getRowSpan(), c.getConfigJson()))
                .toList();
        }

        return List.of();
    }

    private static int clampSpan(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
