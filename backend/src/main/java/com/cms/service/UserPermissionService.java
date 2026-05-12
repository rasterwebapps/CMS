package com.cms.service;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.model.AppUser;
import com.cms.repository.AppUserRepository;

@Service
@Transactional(readOnly = true)
public class UserPermissionService {

    private final AppUserRepository appUserRepository;

    public UserPermissionService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    /**
     * Returns the set of permission codes for the given Keycloak username.
     * Results are loaded from the repository on each call so profile-based
     * deployments always reflect current PostgreSQL data without stale cache reads.
     */
    public Set<String> getPermissions(String keycloakUsername) {
        return loadPermissions(keycloakUsername);
    }

    private Set<String> loadPermissions(String keycloakUsername) {
        return appUserRepository.findByKeycloakUsername(keycloakUsername)
            .map(AppUser::getAppRole)
            .map(role -> role.getPermissions().stream()
                .map(p -> p.getCode())
                .collect(Collectors.<String>toUnmodifiableSet()))
            .orElse(Collections.emptySet());
    }

    /**
     * Backwards-compatible no-op retained for callers that update user roles.
     * Permission lookups are uncached and therefore need no eviction.
     */
    public void evict(String keycloakUsername) {
    }

    /**
     * Backwards-compatible no-op retained for callers that update role permissions.
     * Permission lookups are uncached and therefore need no eviction.
     */
    public void evictAll() {
    }
}
