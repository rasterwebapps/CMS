package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.model.AppRole;
import com.cms.model.AppUser;
import com.cms.model.Permission;
import com.cms.repository.AppUserRepository;

@ExtendWith(MockitoExtension.class)
class UserPermissionServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    private UserPermissionService userPermissionService;

    @BeforeEach
    void setUp() {
        userPermissionService = new UserPermissionService(appUserRepository);
    }

    @Test
    void shouldReturnPermissionsForUser() {
        Permission p1 = new Permission("USER_VIEW", "View Users", "USER", "");
        Permission p2 = new Permission("ROLE_VIEW", "View Roles", "ROLE", "");

        AppRole role = new AppRole("ADMIN", "Admin", 3, false, null);
        role.setId(1L);
        role.setPermissions(Set.of(p1, p2));

        AppUser user = new AppUser("admin", "admin@test.com", "Admin", role, true, "system");
        user.setId(1L);

        when(appUserRepository.findByKeycloakUsername("admin")).thenReturn(Optional.of(user));

        Set<String> permissions = userPermissionService.getPermissions("admin");

        assertThat(permissions).containsExactlyInAnyOrder("USER_VIEW", "ROLE_VIEW");
    }

    @Test
    void shouldReturnEmptySetWhenUserNotFound() {
        when(appUserRepository.findByKeycloakUsername("ghost")).thenReturn(Optional.empty());

        Set<String> permissions = userPermissionService.getPermissions("ghost");

        assertThat(permissions).isEmpty();
    }

    @Test
    void shouldReturnEmptySetWhenUserHasNoRole() {
        AppUser user = new AppUser("noRole", "norole@test.com", "No Role User", null, true, "system");
        user.setId(2L);
        when(appUserRepository.findByKeycloakUsername("noRole")).thenReturn(Optional.of(user));

        Set<String> permissions = userPermissionService.getPermissions("noRole");

        assertThat(permissions).isEmpty();
    }

    @Test
    void shouldCachePermissionsOnSecondCall() {
        AppRole role = new AppRole("ADMIN", "Admin", 3, false, null);
        role.setId(1L);
        role.setPermissions(Set.of(new Permission("USER_VIEW", "View", "USER", "")));

        AppUser user = new AppUser("admin", "admin@test.com", "Admin", role, true, "system");
        user.setId(1L);
        when(appUserRepository.findByKeycloakUsername("admin")).thenReturn(Optional.of(user));

        userPermissionService.getPermissions("admin");
        userPermissionService.getPermissions("admin"); // second call — should use cache

        // Repository should only be called once due to caching
        org.mockito.Mockito.verify(appUserRepository, org.mockito.Mockito.times(1))
            .findByKeycloakUsername("admin");
    }

    @Test
    void shouldEvictSingleUserCache() {
        AppRole role = new AppRole("ADMIN", "Admin", 3, false, null);
        role.setId(1L);
        role.setPermissions(new HashSet<>());

        AppUser user = new AppUser("admin", "admin@test.com", "Admin", role, true, "system");
        user.setId(1L);
        when(appUserRepository.findByKeycloakUsername("admin")).thenReturn(Optional.of(user));

        userPermissionService.getPermissions("admin"); // loads into cache
        userPermissionService.evict("admin");           // evicts
        userPermissionService.getPermissions("admin"); // reloads from repository

        org.mockito.Mockito.verify(appUserRepository, org.mockito.Mockito.times(2))
            .findByKeycloakUsername("admin");
    }

    @Test
    void shouldEvictAllUserCache() {
        AppRole role = new AppRole("ADMIN", "Admin", 3, false, null);
        role.setId(1L);
        role.setPermissions(new HashSet<>());

        AppUser user1 = new AppUser("user1", "u1@test.com", "User One", role, true, "system");
        AppUser user2 = new AppUser("user2", "u2@test.com", "User Two", role, true, "system");
        when(appUserRepository.findByKeycloakUsername("user1")).thenReturn(Optional.of(user1));
        when(appUserRepository.findByKeycloakUsername("user2")).thenReturn(Optional.of(user2));

        userPermissionService.getPermissions("user1");
        userPermissionService.getPermissions("user2");
        userPermissionService.evictAll();
        userPermissionService.getPermissions("user1"); // re-load
        userPermissionService.getPermissions("user2"); // re-load

        org.mockito.Mockito.verify(appUserRepository, org.mockito.Mockito.times(2))
            .findByKeycloakUsername("user1");
        org.mockito.Mockito.verify(appUserRepository, org.mockito.Mockito.times(2))
            .findByKeycloakUsername("user2");
    }
}

