package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.cms.dto.AppRoleRequest;
import com.cms.dto.AppRoleResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.AppRole;
import com.cms.model.Permission;
import com.cms.repository.AppRoleRepository;
import com.cms.repository.PermissionRepository;

@ExtendWith(MockitoExtension.class)
class AppRoleServiceTest {

    @Mock
    private AppRoleRepository appRoleRepository;

    @Mock
    private PermissionRepository permissionRepository;

    @Mock
    private UserPermissionService userPermissionService;

    @Mock
    private AuditLogService auditLogService;

    private AppRoleService appRoleService;

    @BeforeEach
    void setUp() {
        appRoleService = new AppRoleService(appRoleRepository, permissionRepository, userPermissionService, auditLogService);
    }

    // -------------------------------------------------------------------------
    // findAssignableRoles
    // -------------------------------------------------------------------------

    @Test
    void shouldReturnRolesWithHigherHierarchyLevel() {
        AppRole role1 = createRole(1L, "COLLEGE_ADMIN", "College Admin", 4, false);
        AppRole role2 = createRole(2L, "FACULTY", "Faculty", 5, false);
        when(appRoleRepository.findByHierarchyLevelGreaterThan(3)).thenReturn(List.of(role1, role2));

        List<AppRoleResponse> result = appRoleService.findAssignableRoles(3);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).name()).isEqualTo("COLLEGE_ADMIN");
        assertThat(result.get(1).name()).isEqualTo("FACULTY");
        verify(appRoleRepository).findByHierarchyLevelGreaterThan(3);
    }

    @Test
    void shouldReturnEmptyListWhenNoAssignableRoles() {
        when(appRoleRepository.findByHierarchyLevelGreaterThan(6)).thenReturn(List.of());

        List<AppRoleResponse> result = appRoleService.findAssignableRoles(6);

        assertThat(result).isEmpty();
    }

    // -------------------------------------------------------------------------
    // findById
    // -------------------------------------------------------------------------

    @Test
    void shouldFindRoleById() {
        AppRole role = createRole(1L, "ADMIN", "Admin", 3, true);
        when(appRoleRepository.findById(1L)).thenReturn(Optional.of(role));

        AppRoleResponse response = appRoleService.findById(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("ADMIN");
        assertThat(response.hierarchyLevel()).isEqualTo(3);
        assertThat(response.isSystemRole()).isTrue();
    }

    @Test
    void shouldThrowWhenRoleNotFoundById() {
        when(appRoleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> appRoleService.findById(99L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("99");
    }

    // -------------------------------------------------------------------------
    // findAll
    // -------------------------------------------------------------------------

    @Test
    void shouldReturnAllRolesOrderedByHierarchyLevel() {
        AppRole r1 = createRole(1L, "ADMIN", "Admin", 3, false);
        AppRole r2 = createRole(2L, "STUDENT", "Student", 6, false);
        when(appRoleRepository.findAllByOrderByHierarchyLevelAsc()).thenReturn(List.of(r1, r2));

        List<AppRoleResponse> result = appRoleService.findAll();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).name()).isEqualTo("ADMIN");
        assertThat(result.get(1).name()).isEqualTo("STUDENT");
    }

    // -------------------------------------------------------------------------
    // create
    // -------------------------------------------------------------------------

    @Test
    void shouldCreateNewRole() {
        AppRoleRequest request = new AppRoleRequest("CUSTOM", "Custom Role", "A custom role", List.of(), List.of());
        when(appRoleRepository.findByName("CUSTOM")).thenReturn(Optional.empty());

        AppRole savedRole = createRole(10L, "CUSTOM", "Custom Role", 4, false);
        when(appRoleRepository.save(any(AppRole.class))).thenReturn(savedRole);

        AppRoleResponse response = appRoleService.create(request, 3, "tester");

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.name()).isEqualTo("CUSTOM");

        ArgumentCaptor<AppRole> captor = ArgumentCaptor.forClass(AppRole.class);
        verify(appRoleRepository).save(captor.capture());
        assertThat(captor.getValue().getHierarchyLevel()).isEqualTo(4); // requesterLevel + 1
    }

    @Test
    void shouldCreateRoleWithPermissions() {
        Permission perm = new Permission("USER_VIEW", "View Users", "USER", "");
        perm.setId(1L);

        AppRoleRequest request = new AppRoleRequest("CUSTOM", "Custom Role", "desc", List.of("USER_VIEW"), List.of());
        when(appRoleRepository.findByName("CUSTOM")).thenReturn(Optional.empty());
        when(permissionRepository.findByCodeIn(List.of("USER_VIEW"))).thenReturn(List.of(perm));

        AppRole savedRole = createRole(10L, "CUSTOM", "Custom Role", 4, false);
        when(appRoleRepository.save(any(AppRole.class))).thenReturn(savedRole);

        appRoleService.create(request, 3, "tester");

        verify(permissionRepository).findByCodeIn(List.of("USER_VIEW"));
    }

    @Test
    void shouldThrowWhenCreatingRoleWithDuplicateName() {
        AppRoleRequest request = new AppRoleRequest("ADMIN", "Admin", "desc", null, null);
        when(appRoleRepository.findByName("ADMIN")).thenReturn(Optional.of(createRole(1L, "ADMIN", "Admin", 3, true)));

        assertThatThrownBy(() -> appRoleService.create(request, 2, "tester"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("ADMIN")
            .hasMessageContaining("already exists");

        verify(appRoleRepository, never()).save(any());
    }

    @Test
    void shouldRejectCreatingReservedSupportAdminRoleName() {
        AppRoleRequest request = new AppRoleRequest("supportadmin", "Support Admin", "desc", List.of(), List.of());

        assertThatThrownBy(() -> appRoleService.create(request, 1, "tester"))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("reserved");

        verify(appRoleRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // updatePermissions
    // -------------------------------------------------------------------------

    @Test
    void shouldUpdatePermissionsWhenRequesterHoldsAll() {
        AppRole role = createRole(5L, "FACULTY", "Faculty", 5, false);
        when(appRoleRepository.findById(5L)).thenReturn(Optional.of(role));
        when(permissionRepository.findByCodeIn(List.of("COURSE_VIEW"))).thenReturn(
            List.of(new Permission("COURSE_VIEW", "View Courses", "COURSE", "")));
        when(appRoleRepository.save(any(AppRole.class))).thenReturn(role);

        Set<String> requesterPerms = Set.of("COURSE_VIEW", "USER_VIEW");
        AppRoleResponse response = appRoleService.updatePermissions(5L, List.of("COURSE_VIEW"), requesterPerms, "tester");

        assertThat(response).isNotNull();
        verify(userPermissionService).evictAll();
    }

    @Test
    void shouldThrowForbiddenWhenRequesterLacksPermission() {
        AppRole role = createRole(5L, "FACULTY", "Faculty", 5, false);
        when(appRoleRepository.findById(5L)).thenReturn(Optional.of(role));

        Set<String> requesterPerms = Set.of("COURSE_VIEW");

        assertThatThrownBy(() -> appRoleService.updatePermissions(5L, List.of("ADMIN_VIEW"), requesterPerms, "tester"))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("ADMIN_VIEW");
    }

    @Test
    void shouldClearPermissionsWhenListIsEmpty() {
        AppRole role = createRole(5L, "FACULTY", "Faculty", 5, false);
        when(appRoleRepository.findById(5L)).thenReturn(Optional.of(role));
        when(appRoleRepository.save(any(AppRole.class))).thenReturn(role);

        appRoleService.updatePermissions(5L, List.of(), Set.of("COURSE_VIEW"), "tester");

        verify(userPermissionService).evictAll();
    }

    @Test
    void shouldThrowWhenRoleNotFoundOnUpdate() {
        when(appRoleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> appRoleService.updatePermissions(99L, List.of(), Set.of(), "tester"))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldRejectPermissionUpdatesForImmutableDevAdminRole() {
        AppRole role = createRole(1L, "DEV_ADMIN", "Developer Admin", 1, true);
        when(appRoleRepository.findById(1L)).thenReturn(Optional.of(role));

        assertThatThrownBy(() -> appRoleService.updatePermissions(1L, List.of("USER_VIEW"), Set.of("USER_VIEW"), "tester"))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("immutable");

        verify(appRoleRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // getPermissions
    // -------------------------------------------------------------------------

    @Test
    void shouldReturnSortedPermissionCodesForRole() {
        Permission p1 = new Permission("USER_VIEW", "View Users", "USER", "");
        Permission p2 = new Permission("COURSE_VIEW", "View Courses", "COURSE", "");
        AppRole role = createRole(1L, "ADMIN", "Admin", 3, false);
        role.getPermissions().add(p1);
        role.getPermissions().add(p2);
        when(appRoleRepository.findById(1L)).thenReturn(Optional.of(role));

        List<String> codes = appRoleService.getPermissions(1L);

        assertThat(codes).containsExactly("COURSE_VIEW", "USER_VIEW"); // sorted
    }

    @Test
    void shouldThrowWhenRoleNotFoundForPermissions() {
        when(appRoleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> appRoleService.getPermissions(99L))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private AppRole createRole(Long id, String name, String displayName, int level, boolean systemRole) {
        AppRole role = new AppRole(name, displayName, level, systemRole, null);
        role.setId(id);
        role.setPermissions(new HashSet<>());
        return role;
    }
}

