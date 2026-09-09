package com.cms.config;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import com.cms.exception.ModuleNotEnabledException;
import com.cms.module.ModuleDefinition;
import com.cms.module.ModuleRegistry;
import com.cms.service.UserPermissionService;

import java.util.Optional;

/**
 * Spring Security SpEL helper bean, callable as {@code @perm.has('CODE')} inside
 * {@link org.springframework.security.access.prepost.PreAuthorize} expressions.
 *
 * <p>Usage example:
 * <pre>{@code @PreAuthorize("@perm.has('DEPT_MANAGE')")}</pre>
 *
 * <p>The check resolves the currently authenticated user's Keycloak {@code preferred_username},
 * looks up their DB-assigned role via {@link UserPermissionService}, and returns {@code true}
 * if the resulting permission set contains the requested code.
 *
 * <p>Also enforces module gating (see the module-architecture decision log): before checking a
 * permission, each code is resolved to its owning module via {@link ModuleRegistry}. If every
 * code offered to {@link #hasAny(String...)} belongs to a module this deployment has disabled
 * (via {@code app.modules.enabled} / {@link ModuleConfig}), the check throws
 * {@link ModuleNotEnabledException} instead of just returning {@code false} — this is deliberate:
 * it lets {@code GlobalExceptionHandler} return a distinct {@code MODULE_NOT_ENABLED} response
 * so the frontend can tell "this feature isn't part of your organization's plan" apart from a
 * plain permission denial. A code whose module IS enabled (or has no module mapping at all — a
 * "core" permission like {@code USER_VIEW}) is evaluated exactly as before. This intentionally
 * reuses the existing 812 call sites of {@code @perm.has(...)}/{@code @perm.hasAny(...)} rather
 * than requiring a new annotation on every controller — see the module-architecture decision log
 * for why a new interceptor/filter was rejected in favor of extending this bean.
 *
 * <p>A platform system-role user (DEV_ADMIN, SUPPORT_ADMIN — see {@link UserPermissionService#isSystemRole}
 * and the mandatory DEV_ADMIN/SUPPORT_ADMIN catch-all sync block every permission-adding migration
 * ends with, e.g. V129) is never blocked by module gating, on any deployment, regardless of
 * {@code app.modules.enabled} — the same "these two roles are guaranteed every capability that
 * exists" contract those migrations already establish for permissions extends here to modules.
 */
@Component("perm")
public class PermSecurityBean {

    private final UserPermissionService userPermissionService;
    private final ModuleConfig moduleConfig;

    public PermSecurityBean(UserPermissionService userPermissionService, ModuleConfig moduleConfig) {
        this.userPermissionService = userPermissionService;
        this.moduleConfig = moduleConfig;
    }

    /**
     * Returns {@code true} if the currently authenticated user holds the given
     * permission {@code code} (as defined in the {@code permissions} DB table).
     *
     * @param permissionCode the permission code to check (e.g. {@code "DEPT_MANAGE"})
     * @return {@code true} if the user has the permission, {@code false} otherwise
     * @throws ModuleNotEnabledException if this code's module is disabled for this deployment
     */
    public boolean has(String permissionCode) {
        return hasAny(permissionCode);
    }

    /**
     * Returns {@code true} if the currently authenticated user holds ANY of the given
     * permission codes.
     *
     * <p>Usage example:
     * <pre>{@code @PreAuthorize("@perm.hasAny('REPORT_VIEW','STUDENT_VIEW')")}</pre>
     *
     * @param permissionCodes one or more permission codes to check
     * @return {@code true} if the user has at least one of the given permissions
     * @throws ModuleNotEnabledException if EVERY code's module is disabled for this deployment —
     *         i.e. there is no enabled module through which this user could ever pass this check.
     *         A code belonging to an enabled module (or with no module mapping) is evaluated
     *         normally even when another code offered alongside it is module-gated off.
     */
    public boolean hasAny(String... permissionCodes) {
        String username = currentUsername();
        boolean exemptFromModuleGating = username != null && userPermissionService.isSystemRole(username);

        boolean anyCodeModuleGated = false;
        String gatedModuleCode = null;

        for (String code : permissionCodes) {
            Optional<ModuleDefinition> module = ModuleRegistry.resolveModuleForPermissionCode(code);
            if (module.isPresent() && !moduleConfig.isEnabled(module.get().code()) && !exemptFromModuleGating) {
                anyCodeModuleGated = true;
                gatedModuleCode = module.get().code();
                continue;
            }
            if (hasPermission(username, code)) {
                return true;
            }
        }

        if (anyCodeModuleGated) {
            throw new ModuleNotEnabledException(
                gatedModuleCode,
                "This feature is not part of your organization's enabled modules."
            );
        }
        return false;
    }

    private String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        Object principal = auth.getPrincipal();
        if (!(principal instanceof Jwt jwt)) {
            return null;
        }
        String username = jwt.getClaimAsString("preferred_username");
        return (username == null || username.isBlank()) ? null : username;
    }

    private boolean hasPermission(String username, String permissionCode) {
        if (username == null) {
            return false;
        }
        return userPermissionService.getPermissions(username).contains(permissionCode);
    }
}

