package com.cms.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.cms.config.PermSecurityBean;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.AppRole;
import com.cms.model.AppUser;
import com.cms.model.Permission;
import com.cms.model.RoleDashboardWidgetConfig;
import com.cms.model.UserDashboardWidgetConfig;
import com.cms.repository.AppRoleRepository;
import com.cms.repository.AppUserRepository;
import com.cms.repository.PermissionRepository;
import com.cms.repository.UserDashboardWidgetConfigRepository;
import com.cms.service.AuditLogService;
import com.cms.service.UserPermissionService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = PermissionController.class)
class PermissionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper json = new ObjectMapper();

    @MockitoBean
    private AppUserRepository appUserRepository;

    @MockitoBean
    private AppRoleRepository appRoleRepository;

    @MockitoBean
    private PermissionRepository permissionRepository;

    @MockitoBean
    private UserPermissionService userPermissionService;

    @MockitoBean
    private UserDashboardWidgetConfigRepository userWidgetConfigRepo;

    @MockitoBean
    private AuditLogService auditLogService;

    @MockitoBean(name = "perm")
    private PermSecurityBean perm;

    @BeforeEach
    void grantAll() {
        when(perm.has(anyString())).thenReturn(true);
    }

    @Test
    void shouldReturnMyPermissions() throws Exception {
        AppRole role = new AppRole("ADMIN", "Admin", 3, false, null);
        role.setId(1L);
        AppUser user = new AppUser("admin", "admin@test.com", "Admin User", role, true, "system");
        user.setId(1L);

        when(appUserRepository.findByKeycloakUsernameWithRole("admin")).thenReturn(Optional.of(user));
        when(userPermissionService.getPermissions("admin")).thenReturn(Set.of("USER_VIEW", "ROLE_VIEW"));

        mockMvc.perform(get("/permissions/my")
                .with(jwt().jwt(j -> j.claim("preferred_username", "admin"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("admin"))
            .andExpect(jsonPath("$.roleName").value("ADMIN"))
            .andExpect(jsonPath("$.roleDisplayName").value("Admin"))
            .andExpect(jsonPath("$.hierarchyLevel").value(3))
            .andExpect(jsonPath("$.permissions").isArray());
    }

    @Test
    void shouldReturnMyPermissionsWithNullRole() throws Exception {
        AppUser user = new AppUser("admin", "admin@test.com", "Admin User", null, true, "system");
        user.setId(1L);

        when(appUserRepository.findByKeycloakUsernameWithRole("admin")).thenReturn(Optional.of(user));
        when(userPermissionService.getPermissions("admin")).thenReturn(Set.of());

        mockMvc.perform(get("/permissions/my")
                .with(jwt().jwt(j -> j.claim("preferred_username", "admin"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("admin"))
            .andExpect(jsonPath("$.hierarchyLevel").value(0));
    }

    @Test
    void shouldReturn404WhenUserNotFoundForMyPermissions() throws Exception {
        when(appUserRepository.findByKeycloakUsernameWithRole(anyString()))
            .thenThrow(new ResourceNotFoundException("No app user record found for username: ghost"));

        mockMvc.perform(get("/permissions/my")
                .with(jwt().jwt(j -> j.claim("preferred_username", "ghost"))))
            .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnMyPermissionsWithUserWidgetConfigs() throws Exception {
        AppRole role = new AppRole("ADMIN", "Admin", 3, false, null);
        role.setId(1L);
        AppUser user = new AppUser("admin", "admin@test.com", "Admin User", role, true, "system");
        user.setId(1L);

        UserDashboardWidgetConfig cfg = new UserDashboardWidgetConfig(user, "hero", 0, 2, 1);
        when(appUserRepository.findByKeycloakUsernameWithRole("admin")).thenReturn(Optional.of(user));
        when(userPermissionService.getPermissions("admin")).thenReturn(Set.of("USER_VIEW"));
        when(userWidgetConfigRepo.findByUserIdOrderByWidgetOrderAsc(1L)).thenReturn(List.of(cfg));

        mockMvc.perform(get("/permissions/my")
                .with(jwt().jwt(j -> j.claim("preferred_username", "admin"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.dashboardWidgets[0].key").value("hero"));
    }

    @Test
    void shouldReturnMyPermissionsWithRoleWidgetConfigs() throws Exception {
        AppRole role = new AppRole("COLLEGE_ADMIN", "College Admin", 4, false, null);
        role.setId(2L);
        RoleDashboardWidgetConfig roleCfg = new RoleDashboardWidgetConfig(role, "stat-students", 0, 1, 1);
        role.setWidgetConfigs(List.of(roleCfg));

        AppUser user = new AppUser("college_admin", "cadmin@test.com", "College Admin", role, true, "system");
        user.setId(2L);

        when(appUserRepository.findByKeycloakUsernameWithRole("college_admin")).thenReturn(Optional.of(user));
        when(userPermissionService.getPermissions("college_admin")).thenReturn(Set.of());
        when(userWidgetConfigRepo.findByUserIdOrderByWidgetOrderAsc(2L)).thenReturn(List.of());

        mockMvc.perform(get("/permissions/my")
                .with(jwt().jwt(j -> j.claim("preferred_username", "college_admin"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.dashboardWidgets[0].key").value("stat-students"));
    }

    @Test
    void shouldReturnAllPermissions() throws Exception {
        Permission p1 = new Permission("USER_VIEW", "View Users", "USER", "desc");
        p1.setId(1L);
        Permission p2 = new Permission("COURSE_VIEW", "View Courses", "COURSE", "desc");
        p2.setId(2L);

        when(permissionRepository.findAll()).thenReturn(List.of(p1, p2));

        mockMvc.perform(get("/permissions/all")
                .with(jwt().jwt(j -> j.claim("preferred_username", "admin"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].code").value("COURSE_VIEW"))  // sorted by category then code
            .andExpect(jsonPath("$[1].code").value("USER_VIEW"));
    }

    @Test
    void shouldReturnEmptyListWhenNoPermissions() throws Exception {
        when(permissionRepository.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/permissions/all")
                .with(jwt().jwt(j -> j.claim("preferred_username", "admin"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
    }

    // ── Tier-impact preview & auto-revoke on tier change ────────────────────

    @Test
    void tierImpactPreviewShouldListRolesThatWouldLoseThePermission() throws Exception {
        Permission perm = new Permission("ADMISSION_CREATE", "Create Admissions", "ADMISSION", "desc");
        perm.setId(44L);
        perm.setTier(4);

        AppRole collegeAdmin = new AppRole("COLLEGEADMIN", "College Admin", 4, false, null);
        collegeAdmin.setId(12L);
        AppRole devAdmin = new AppRole("DEVADMIN", "Developer Admin", 1, true, null);
        devAdmin.setId(1L);

        when(permissionRepository.findById(44L)).thenReturn(Optional.of(perm));
        when(appRoleRepository.findByPermissionCode("ADMISSION_CREATE"))
            .thenReturn(List.of(devAdmin, collegeAdmin));
        when(appUserRepository.countByAppRoleId(12L)).thenReturn(7L);

        String body = json.writeValueAsString(List.of(java.util.Map.of("id", 44, "tier", 1)));

        mockMvc.perform(post("/permissions/tier-impact")
                .with(jwt().jwt(j -> j.claim("preferred_username", "dev")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].code").value("ADMISSION_CREATE"))
            .andExpect(jsonPath("$[0].revokedFrom.length()").value(1))
            .andExpect(jsonPath("$[0].revokedFrom[0].roleName").value("COLLEGEADMIN"))
            .andExpect(jsonPath("$[0].revokedFrom[0].userCount").value(7));
    }

    @Test
    void tierImpactPreviewShouldOmitPermissionsWithNoImpact() throws Exception {
        Permission perm = new Permission("ADMISSION_CREATE", "Create Admissions", "ADMISSION", "desc");
        perm.setId(44L);
        perm.setTier(4);

        AppRole devAdmin = new AppRole("DEVADMIN", "Developer Admin", 1, true, null);
        devAdmin.setId(1L);

        when(permissionRepository.findById(44L)).thenReturn(Optional.of(perm));
        when(appRoleRepository.findByPermissionCode("ADMISSION_CREATE")).thenReturn(List.of(devAdmin));

        String body = json.writeValueAsString(List.of(java.util.Map.of("id", 44, "tier", 1)));

        mockMvc.perform(post("/permissions/tier-impact")
                .with(jwt().jwt(j -> j.claim("preferred_username", "dev")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void updateTierShouldRevokePermissionFromRolesThatNoLongerQualify() throws Exception {
        Permission perm = new Permission("ADMISSION_CREATE", "Create Admissions", "ADMISSION", "desc");
        perm.setId(44L);
        perm.setTier(4);

        AppRole collegeAdmin = new AppRole("COLLEGEADMIN", "College Admin", 4, false, null);
        collegeAdmin.setId(12L);
        collegeAdmin.getPermissions().add(perm);

        AppRole devAdmin = new AppRole("DEVADMIN", "Developer Admin", 1, true, null);
        devAdmin.setId(1L);
        devAdmin.getPermissions().add(perm);

        when(permissionRepository.findById(44L)).thenReturn(Optional.of(perm));
        when(appRoleRepository.findByPermissionCode("ADMISSION_CREATE"))
            .thenReturn(List.of(devAdmin, collegeAdmin));

        mockMvc.perform(put("/permissions/44/tier")
                .with(jwt().jwt(j -> j.claim("preferred_username", "dev")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tier\":1}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tier").value(1));

        // College Admin (level 4) no longer qualifies for tier 1 — revoked and saved.
        assertThat(collegeAdmin.getPermissions()).doesNotContain(perm);
        verify(appRoleRepository).save(collegeAdmin);
        // DEV_ADMIN (level 1) still qualifies for every tier — untouched, never saved.
        assertThat(devAdmin.getPermissions()).contains(perm);
        verify(appRoleRepository, never()).save(devAdmin);
    }

    @Test
    void updateTierShouldNotSweepRolesWhenTierIsUnchanged() throws Exception {
        Permission perm = new Permission("ADMISSION_CREATE", "Create Admissions", "ADMISSION", "desc");
        perm.setId(44L);
        perm.setTier(4);

        when(permissionRepository.findById(44L)).thenReturn(Optional.of(perm));

        mockMvc.perform(put("/permissions/44/tier")
                .with(jwt().jwt(j -> j.claim("preferred_username", "dev")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tier\":4}"))
            .andExpect(status().isOk());

        verify(appRoleRepository, never()).findByPermissionCode(anyString());
    }
}

