package com.cms.service;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.model.AppUser;
import com.cms.repository.AppUserRepository;

@Service
@Transactional(readOnly = true)
public class UserPermissionService {

    private final AppUserRepository appUserRepository;

    /** Simple in-process permission cache: username -> set of permission codes. */
    private final ConcurrentHashMap<String, Set<String>> cache = new ConcurrentHashMap<>();

    public UserPermissionService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    /**
     * Returns the set of permission codes for the given Keycloak username.
     * Results are cached until evicted (e.g. on role or permission change).
     */
    public Set<String> getPermissions(String keycloakUsername) {
        return cache.computeIfAbsent(keycloakUsername, this::loadPermissions);
    }

    private Set<String> loadPermissions(String keycloakUsername) {
        return appUserRepository.findByKeycloakUsername(keycloakUsername)
            .map(AppUser::getAppRole)
            .map(role -> role.getPermissions().stream()
                .map(p -> p.getCode())
                .collect(Collectors.<String>toUnmodifiableSet()))
            .orElse(Collections.emptySet());
    }

    /** Evicts a single user's cached permissions (call when their role changes). */
    public void evict(String keycloakUsername) {
        cache.remove(keycloakUsername);
    }

    /** Evicts all cached permissions (call when a role's permission set changes). */
    public void evictAll() {
        cache.clear();
    }
}
