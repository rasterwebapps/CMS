package com.cms.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import com.cms.model.AppUser;
import com.cms.repository.AppUserRepository;

@Component
public class CurrentUserResolver {

    private final AppUserRepository appUserRepository;

    public CurrentUserResolver(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    public String resolve() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof Jwt jwt) {
            String username = jwt.getClaimAsString("preferred_username");
            return (username != null && !username.isBlank()) ? username : auth.getName();
        }
        return auth.getName();
    }

    /**
     * Resolves the authenticated user's real display name (local {@code app_users.full_name}),
     * for display on actor-attribution fields such as a receipt's "collected by" or a refund's
     * "approved by" — never the bare Keycloak username. Falls back to the username itself when no
     * matching app_user row exists (e.g. account deactivated/removed), and to "system" when there
     * is no authenticated principal at all.
     */
    public String resolveFullName() {
        String username = resolve();
        if (username == null || username.isBlank()) {
            return "system";
        }
        return appUserRepository.findByKeycloakUsernameWithRole(username)
            .map(AppUser::getFullName)
            .filter(name -> name != null && !name.isBlank())
            .orElse(username);
    }
}
