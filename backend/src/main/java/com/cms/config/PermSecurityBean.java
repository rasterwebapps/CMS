package com.cms.config;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import com.cms.service.UserPermissionService;

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
 */
@Component("perm")
public class PermSecurityBean {

    private final UserPermissionService userPermissionService;

    public PermSecurityBean(UserPermissionService userPermissionService) {
        this.userPermissionService = userPermissionService;
    }

    /**
     * Returns {@code true} if the currently authenticated user holds the given
     * permission {@code code} (as defined in the {@code permissions} DB table).
     *
     * @param permissionCode the permission code to check (e.g. {@code "DEPT_MANAGE"})
     * @return {@code true} if the user has the permission, {@code false} otherwise
     */
    public boolean has(String permissionCode) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        Object principal = auth.getPrincipal();
        if (!(principal instanceof Jwt jwt)) {
            return false;
        }
        String username = jwt.getClaimAsString("preferred_username");
        if (username == null || username.isBlank()) {
            return false;
        }
        return userPermissionService.getPermissions(username).contains(permissionCode);
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
     */
    public boolean hasAny(String... permissionCodes) {
        for (String code : permissionCodes) {
            if (has(code)) {
                return true;
            }
        }
        return false;
    }
}

