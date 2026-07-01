package com.cms.service;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.model.AppUser;
import com.cms.repository.AppUserRepository;

@Service
@Transactional(readOnly = true)
public class UserPermissionService {

    private static final List<String> GRANULAR_SUFFIXES =
        List.of("_VIEW", "_CREATE", "_EDIT", "_DELETE", "_EXPORT");

    private final AppUserRepository appUserRepository;

    public UserPermissionService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    /**
     * Returns the set of permission codes for the given Keycloak username.
     * Results are loaded from the repository on each call so profile-based
     * deployments always reflect current PostgreSQL data without stale cache reads.
     *
     * Any X_MANAGE code in the stored set is expanded at runtime to also include
     * X_VIEW, X_CREATE, X_EDIT, X_DELETE and X_EXPORT, providing backward
     * compatibility while granular backend annotations are rolled out incrementally.
     */
    public Set<String> getPermissions(String keycloakUsername) {
        return expandManage(loadPermissions(keycloakUsername));
    }

    private Set<String> loadPermissions(String keycloakUsername) {
        return appUserRepository.findByKeycloakUsername(keycloakUsername)
            .map(AppUser::getAppRole)
            .map(role -> role.getPermissions().stream()
                .map(p -> p.getCode())
                .collect(Collectors.<String>toSet()))
            .orElse(new HashSet<>());
    }

    private static Set<String> expandManage(Set<String> stored) {
        Set<String> expanded = new HashSet<>(stored);
        for (String code : stored) {
            if (code.endsWith("_MANAGE")) {
                String prefix = code.substring(0, code.length() - "_MANAGE".length());
                for (String suffix : GRANULAR_SUFFIXES) {
                    expanded.add(prefix + suffix);
                }
            }
        }
        return Collections.unmodifiableSet(expanded);
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
