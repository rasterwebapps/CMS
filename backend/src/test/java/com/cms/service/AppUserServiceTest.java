package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.cms.dto.AppUserResponse;
import com.cms.dto.CreateUserRequest;
import com.cms.dto.UpdateUserRequest;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.AppRole;
import com.cms.model.AppUser;
import com.cms.repository.AppRoleRepository;
import com.cms.repository.AppUserRepository;

@ExtendWith(MockitoExtension.class)
class AppUserServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private AppRoleRepository appRoleRepository;

    @Mock
    private UserPermissionService userPermissionService;

    @Mock
    private AuditLogService auditLogService;

    private AppUserService appUserService;

    @BeforeEach
    void setUp() {
        appUserService = new AppUserService(appUserRepository, appRoleRepository, userPermissionService, auditLogService);
    }

    // -------------------------------------------------------------------------
    // findByUsername
    // -------------------------------------------------------------------------

    @Test
    void shouldFindUserByUsername() {
        AppRole role = createRole(1L, "ADMIN", "Admin", 3);
        AppUser user = createUser(1L, "admin", "admin@test.com", "Admin User", role);
        when(appUserRepository.findByKeycloakUsername("admin")).thenReturn(Optional.of(user));

        AppUserResponse response = appUserService.findByUsername("admin");

        assertThat(response.keycloakUsername()).isEqualTo("admin");
        assertThat(response.email()).isEqualTo("admin@test.com");
        assertThat(response.roleName()).isEqualTo("ADMIN");
    }

    @Test
    void shouldThrowWhenUserNotFoundByUsername() {
        when(appUserRepository.findByKeycloakUsername("nobody")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> appUserService.findByUsername("nobody"))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("nobody");
    }

    // -------------------------------------------------------------------------
    // findManageable
    // -------------------------------------------------------------------------

    @Test
    void shouldReturnUsersWithHigherHierarchyLevel() {
        AppRole role = createRole(2L, "FACULTY", "Faculty", 5);
        AppUser user = createUser(1L, "faculty1", "f1@test.com", "Faculty One", role);
        when(appUserRepository.findByAppRoleHierarchyLevelGreaterThan(3)).thenReturn(List.of(user));

        List<AppUserResponse> result = appUserService.findManageable(3);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).keycloakUsername()).isEqualTo("faculty1");
    }

    // -------------------------------------------------------------------------
    // create
    // -------------------------------------------------------------------------

    @Test
    void shouldCreateUser() {
        CreateUserRequest request = new CreateUserRequest("new@test.com", "New User", "newuser", "FACULTY");

        AppRole targetRole = createRole(2L, "FACULTY", "Faculty", 5);
        when(appRoleRepository.findByName("FACULTY")).thenReturn(Optional.of(targetRole));
        when(appUserRepository.existsByKeycloakUsername("newuser")).thenReturn(false);
        when(appUserRepository.existsByEmail("new@test.com")).thenReturn(false);

        AppUser saved = createUser(1L, "newuser", "new@test.com", "New User", targetRole);
        when(appUserRepository.save(any(AppUser.class))).thenReturn(saved);

        AppUserResponse response = appUserService.create(request, "admin", 3);

        assertThat(response.keycloakUsername()).isEqualTo("newuser");
        assertThat(response.roleName()).isEqualTo("FACULTY");

        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(appUserRepository).save(captor.capture());
        assertThat(captor.getValue().getCreatedBy()).isEqualTo("admin");
    }

    @Test
    void shouldThrowForbiddenWhenCreatingUserWithEqualOrHigherLevel() {
        CreateUserRequest request = new CreateUserRequest("peer@test.com", "Peer", "peer", "ADMIN");

        AppRole targetRole = createRole(1L, "ADMIN", "Admin", 3); // same as requester level
        when(appRoleRepository.findByName("ADMIN")).thenReturn(Optional.of(targetRole));

        assertThatThrownBy(() -> appUserService.create(request, "admin", 3))
            .isInstanceOf(ResponseStatusException.class);

        verify(appUserRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenUsernameAlreadyExists() {
        CreateUserRequest request = new CreateUserRequest("dup@test.com", "Dup", "existinguser", "FACULTY");

        AppRole role = createRole(2L, "FACULTY", "Faculty", 5);
        when(appRoleRepository.findByName("FACULTY")).thenReturn(Optional.of(role));
        when(appUserRepository.existsByKeycloakUsername("existinguser")).thenReturn(true);

        assertThatThrownBy(() -> appUserService.create(request, "admin", 3))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("existinguser");
    }

    @Test
    void shouldThrowWhenEmailAlreadyExists() {
        CreateUserRequest request = new CreateUserRequest("dup@test.com", "Dup", "newuser2", "FACULTY");

        AppRole role = createRole(2L, "FACULTY", "Faculty", 5);
        when(appRoleRepository.findByName("FACULTY")).thenReturn(Optional.of(role));
        when(appUserRepository.existsByKeycloakUsername("newuser2")).thenReturn(false);
        when(appUserRepository.existsByEmail("dup@test.com")).thenReturn(true);

        assertThatThrownBy(() -> appUserService.create(request, "admin", 3))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("dup@test.com");
    }

    @Test
    void shouldThrowWhenRoleNotFoundOnCreate() {
        CreateUserRequest request = new CreateUserRequest("x@test.com", "X", "xuser", "NONEXISTENT");
        when(appRoleRepository.findByName("NONEXISTENT")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> appUserService.create(request, "admin", 2))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    // -------------------------------------------------------------------------
    // update
    // -------------------------------------------------------------------------

    @Test
    void shouldUpdateUserFullNameAndEmail() {
        AppRole role = createRole(2L, "FACULTY", "Faculty", 5);
        AppUser user = createUser(1L, "faculty1", "old@test.com", "Old Name", role);
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(user));

        AppUser updated = createUser(1L, "faculty1", "new@test.com", "New Name", role);
        when(appUserRepository.save(any(AppUser.class))).thenReturn(updated);

        UpdateUserRequest request = new UpdateUserRequest("New Name", "new@test.com", null, null);
        AppUserResponse response = appUserService.update(1L, request, 3);

        assertThat(response.email()).isEqualTo("new@test.com");
        assertThat(response.fullName()).isEqualTo("New Name");
    }

    @Test
    void shouldUpdateUserRole() {
        AppRole oldRole = createRole(2L, "FACULTY", "Faculty", 5);
        AppUser user = createUser(1L, "faculty1", "f@test.com", "Faculty", oldRole);
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(user));

        AppRole newRole = createRole(3L, "STUDENT", "Student", 6);
        when(appRoleRepository.findByName("STUDENT")).thenReturn(Optional.of(newRole));

        AppUser updated = createUser(1L, "faculty1", "f@test.com", "Faculty", newRole);
        when(appUserRepository.save(any(AppUser.class))).thenReturn(updated);

        UpdateUserRequest request = new UpdateUserRequest(null, null, "STUDENT", null);
        appUserService.update(1L, request, 3);

        verify(userPermissionService).evict("faculty1");
    }

    @Test
    void shouldThrowForbiddenWhenUpdatingUserWithHigherOrEqualLevel() {
        AppRole role = createRole(1L, "ADMIN", "Admin", 3); // same as requester
        AppUser user = createUser(1L, "peer", "peer@test.com", "Peer", role);
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> appUserService.update(1L, new UpdateUserRequest("X", null, null, null), 3))
            .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void shouldThrowWhenUserNotFoundOnUpdate() {
        when(appUserRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> appUserService.update(99L, new UpdateUserRequest(null, null, null, null), 3))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    // -------------------------------------------------------------------------
    // deactivate
    // -------------------------------------------------------------------------

    @Test
    void shouldDeactivateUser() {
        AppRole role = createRole(2L, "FACULTY", "Faculty", 5);
        AppUser user = createUser(1L, "faculty1", "f@test.com", "Faculty", role);
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(user));
        when(appUserRepository.save(any(AppUser.class))).thenReturn(user);

        appUserService.deactivate(1L, 3);

        assertThat(user.isActive()).isFalse();
        verify(userPermissionService).evict("faculty1");
    }

    @Test
    void shouldThrowWhenDeactivatingUserWithHigherOrEqualLevel() {
        AppRole role = createRole(1L, "ADMIN", "Admin", 3);
        AppUser user = createUser(1L, "peer", "peer@test.com", "Peer", role);
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> appUserService.deactivate(1L, 3))
            .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void shouldThrowWhenUserNotFoundOnDeactivate() {
        when(appUserRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> appUserService.deactivate(99L, 3))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    // -------------------------------------------------------------------------
    // reactivate
    // -------------------------------------------------------------------------

    @Test
    void shouldReactivateUser() {
        AppRole role = createRole(2L, "FACULTY", "Faculty", 5);
        AppUser user = createUser(1L, "faculty1", "f@test.com", "Faculty", role);
        user.setActive(false);
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(user));
        when(appUserRepository.save(any(AppUser.class))).thenReturn(user);

        appUserService.reactivate(1L, 3);

        assertThat(user.isActive()).isTrue();
    }

    @Test
    void shouldThrowWhenReactivatingUserWithHigherOrEqualLevel() {
        AppRole role = createRole(1L, "ADMIN", "Admin", 3);
        AppUser user = createUser(1L, "peer", "peer@test.com", "Peer", role);
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> appUserService.reactivate(1L, 3))
            .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void shouldThrowWhenUserNotFoundOnReactivate() {
        when(appUserRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> appUserService.reactivate(99L, 3))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private AppRole createRole(Long id, String name, String displayName, int level) {
        AppRole role = new AppRole(name, displayName, level, false, null);
        role.setId(id);
        role.setPermissions(new HashSet<>());
        return role;
    }

    private AppUser createUser(Long id, String username, String email, String fullName, AppRole role) {
        AppUser user = new AppUser(username, email, fullName, role, true, "system");
        user.setId(id);
        user.setCreatedAt(Instant.now());
        return user;
    }
}

