package com.cms.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.cms.dto.AppRoleRequest;
import com.cms.dto.AppRoleResponse;
import com.cms.dto.WidgetConfigDto;
import com.cms.model.AppRole;
import com.cms.model.AppUser;
import com.cms.repository.AppUserRepository;
import com.cms.service.AppRoleService;
import com.cms.service.UserPermissionService;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Full-context proof (real {@code SecurityConfig}, real {@code @PreAuthorize} enforcement) that
 *  {@code ROLE_CREATE}, {@code ROLE_EDIT} and {@code PERMISSION_ASSIGN} stay properly separated on
 *  {@code RoleManagementController} -- per this project's operation-wise permission mapping hard
 *  gate, a caller holding only the class-level {@code ROLE_VIEW} must never be able to create a
 *  role, reassign a role's dashboard widgets, or change a role's permission set. Same pattern as
 *  {@code AnnouncementSecurityTest}/{@code GuardianFeeSecurityTest}: a
 *  {@code @WebMvcTest(addFilters=false)} slice never actually exercises {@code @PreAuthorize}, so
 *  only a real {@code @SpringBootTest} can catch a regression here. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(RoleManagementSecurityTest.TestConfig.class)
class RoleManagementSecurityTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        JwtDecoder jwtDecoder() {
            return mock(JwtDecoder.class);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserPermissionService userPermissionService;

    @MockBean
    private AppRoleService appRoleService;

    @MockBean
    private AppUserRepository appUserRepository;

    private AppUser buildUser(String username) {
        AppRole role = new AppRole("VIEWER", "Viewer", 4, false, null);
        role.setId(1L);
        AppUser user = new AppUser(username, username + "@test.com", "Test User", role, true, "system");
        user.setId(1L);
        return user;
    }

    private AppRoleResponse buildRoleResponse(Long id, String name) {
        return new AppRoleResponse(id, name, name + " Display", 5, false, null, List.of(), List.of());
    }

    @Test
    void viewOnlyCannotCreateAssignOrEditWidgets() throws Exception {
        when(userPermissionService.getPermissions("viewer1")).thenReturn(Set.of("ROLE_VIEW"));
        when(appUserRepository.findByKeycloakUsernameWithRole("viewer1")).thenReturn(Optional.of(buildUser("viewer1")));
        when(appRoleService.findAssignableRoles(4)).thenReturn(List.of());

        mockMvc.perform(get("/role-management")
                .with(jwt().jwt(j -> j.claim("preferred_username", "viewer1"))))
            .andExpect(status().isOk());

        AppRoleRequest createRequest = new AppRoleRequest("CUSTOM", "Custom Role", "desc", List.of(), List.of());
        mockMvc.perform(post("/role-management")
                .with(jwt().jwt(j -> j.claim("preferred_username", "viewer1")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isForbidden());

        mockMvc.perform(put("/role-management/5/permissions")
                .with(jwt().jwt(j -> j.claim("preferred_username", "viewer1")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of("USER_VIEW"))))
            .andExpect(status().isForbidden());

        mockMvc.perform(put("/role-management/5/dashboard-widgets")
                .with(jwt().jwt(j -> j.claim("preferred_username", "viewer1")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(new WidgetConfigDto("kpi_students", 0, 1, 1, null)))))
            .andExpect(status().isForbidden());
    }

    @Test
    void createOnlyCanCreateRoleButNotAssignPermissionsOrEditWidgets() throws Exception {
        when(userPermissionService.getPermissions("creator1")).thenReturn(Set.of("ROLE_VIEW", "ROLE_CREATE"));
        when(appUserRepository.findByKeycloakUsernameWithRole("creator1")).thenReturn(Optional.of(buildUser("creator1")));
        when(appRoleService.create(org.mockito.ArgumentMatchers.any(AppRoleRequest.class), org.mockito.ArgumentMatchers.eq(4), org.mockito.ArgumentMatchers.anyString()))
            .thenReturn(buildRoleResponse(10L, "CUSTOM"));

        AppRoleRequest createRequest = new AppRoleRequest("CUSTOM", "Custom Role", "desc", List.of(), List.of());
        mockMvc.perform(post("/role-management")
                .with(jwt().jwt(j -> j.claim("preferred_username", "creator1")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isCreated());

        mockMvc.perform(put("/role-management/5/permissions")
                .with(jwt().jwt(j -> j.claim("preferred_username", "creator1")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of("USER_VIEW"))))
            .andExpect(status().isForbidden());

        mockMvc.perform(put("/role-management/5/dashboard-widgets")
                .with(jwt().jwt(j -> j.claim("preferred_username", "creator1")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(new WidgetConfigDto("kpi_students", 0, 1, 1, null)))))
            .andExpect(status().isForbidden());
    }

    @Test
    void permissionAssignOnlyCanAssignPermissionsButNotCreateOrEditWidgets() throws Exception {
        when(userPermissionService.getPermissions("assigner1")).thenReturn(Set.of("ROLE_VIEW", "PERMISSION_ASSIGN"));
        when(appUserRepository.findByKeycloakUsernameWithRole("assigner1")).thenReturn(Optional.of(buildUser("assigner1")));
        when(appRoleService.updatePermissions(org.mockito.ArgumentMatchers.eq(5L), org.mockito.ArgumentMatchers.anyList(),
            org.mockito.ArgumentMatchers.anySet(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.eq(4)))
            .thenReturn(buildRoleResponse(5L, "FACULTY"));

        mockMvc.perform(put("/role-management/5/permissions")
                .with(jwt().jwt(j -> j.claim("preferred_username", "assigner1")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of("USER_VIEW"))))
            .andExpect(status().isOk());

        AppRoleRequest createRequest = new AppRoleRequest("CUSTOM", "Custom Role", "desc", List.of(), List.of());
        mockMvc.perform(post("/role-management")
                .with(jwt().jwt(j -> j.claim("preferred_username", "assigner1")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isForbidden());

        mockMvc.perform(put("/role-management/5/dashboard-widgets")
                .with(jwt().jwt(j -> j.claim("preferred_username", "assigner1")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(new WidgetConfigDto("kpi_students", 0, 1, 1, null)))))
            .andExpect(status().isForbidden());
    }

    @Test
    void roleEditOnlyCanEditWidgetsButNotCreateOrAssignPermissions() throws Exception {
        when(userPermissionService.getPermissions("editor1")).thenReturn(Set.of("ROLE_VIEW", "ROLE_EDIT"));
        when(appUserRepository.findByKeycloakUsernameWithRole("editor1")).thenReturn(Optional.of(buildUser("editor1")));
        when(appRoleService.updateDashboardWidgetConfigs(org.mockito.ArgumentMatchers.eq(5L), org.mockito.ArgumentMatchers.anyList(),
            org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.eq(4)))
            .thenReturn(buildRoleResponse(5L, "FACULTY"));

        mockMvc.perform(put("/role-management/5/dashboard-widgets")
                .with(jwt().jwt(j -> j.claim("preferred_username", "editor1")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(new WidgetConfigDto("kpi_students", 0, 1, 1, null)))))
            .andExpect(status().isOk());

        AppRoleRequest createRequest = new AppRoleRequest("CUSTOM", "Custom Role", "desc", List.of(), List.of());
        mockMvc.perform(post("/role-management")
                .with(jwt().jwt(j -> j.claim("preferred_username", "editor1")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isForbidden());

        mockMvc.perform(put("/role-management/5/permissions")
                .with(jwt().jwt(j -> j.claim("preferred_username", "editor1")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of("USER_VIEW"))))
            .andExpect(status().isForbidden());
    }

    @Test
    void noPermissionAtAllIsForbiddenEverywhere() throws Exception {
        when(userPermissionService.getPermissions("nobody")).thenReturn(Set.of());

        mockMvc.perform(get("/role-management")
                .with(jwt().jwt(j -> j.claim("preferred_username", "nobody"))))
            .andExpect(status().isForbidden());

        AppRoleRequest createRequest = new AppRoleRequest("CUSTOM", "Custom Role", "desc", List.of(), List.of());
        mockMvc.perform(post("/role-management")
                .with(jwt().jwt(j -> j.claim("preferred_username", "nobody")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isForbidden());
    }
}
