package com.cms.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.cms.config.PermSecurityBean;
import com.cms.dto.AppRoleRequest;
import com.cms.dto.AppRoleResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.AppRole;
import com.cms.model.AppUser;
import com.cms.repository.AppUserRepository;
import com.cms.service.AppRoleService;
import com.cms.service.UserPermissionService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = RoleManagementController.class)
class RoleManagementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AppRoleService appRoleService;

    @MockitoBean
    private AppUserRepository appUserRepository;

    @MockitoBean
    private UserPermissionService userPermissionService;

    @MockitoBean(name = "perm")
    private PermSecurityBean perm;

    @BeforeEach
    void grantAll() {
        when(perm.has(anyString())).thenReturn(true);
    }

    private AppUser buildAdminUser() {
        AppRole adminRole = new AppRole("ADMIN", "Admin", 3, false, null);
        adminRole.setId(1L);
        AppUser user = new AppUser("admin", "admin@test.com", "Admin User", adminRole, true, "system");
        user.setId(1L);
        return user;
    }

    private AppRoleResponse buildRoleResponse(Long id, String name, int level) {
        return new AppRoleResponse(id, name, name + " Display", level, false, null, List.of(), List.of());
    }

    @Test
    void shouldListAssignableRoles() throws Exception {
        when(appUserRepository.findByKeycloakUsernameWithRole("admin")).thenReturn(Optional.of(buildAdminUser()));
        when(appRoleService.findAssignableRoles(3)).thenReturn(
            List.of(buildRoleResponse(2L, "FACULTY", 5), buildRoleResponse(3L, "STUDENT", 6)));

        mockMvc.perform(get("/role-management")
                .with(jwt().jwt(j -> j.claim("preferred_username", "admin"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].name").value("FACULTY"))
            .andExpect(jsonPath("$[1].name").value("STUDENT"));
    }

    @Test
    void shouldReturn404WhenUserNotFoundOnList() throws Exception {
        when(appUserRepository.findByKeycloakUsernameWithRole("ghost"))
            .thenThrow(new ResourceNotFoundException("No app user record found for username: ghost"));

        mockMvc.perform(get("/role-management")
                .with(jwt().jwt(j -> j.claim("preferred_username", "ghost"))))
            .andExpect(status().isNotFound());
    }

    @Test
    void shouldGetRoleById() throws Exception {
        when(appRoleService.findById(5L)).thenReturn(buildRoleResponse(5L, "FACULTY", 5));

        mockMvc.perform(get("/role-management/5")
                .with(jwt().jwt(j -> j.claim("preferred_username", "admin"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(5))
            .andExpect(jsonPath("$.name").value("FACULTY"));
    }

    @Test
    void shouldReturn404WhenRoleNotFound() throws Exception {
        when(appRoleService.findById(99L))
            .thenThrow(new ResourceNotFoundException("Role not found with id: 99"));

        mockMvc.perform(get("/role-management/99")
                .with(jwt().jwt(j -> j.claim("preferred_username", "admin"))))
            .andExpect(status().isNotFound());
    }

    @Test
    void shouldCreateRole() throws Exception {
        when(appUserRepository.findByKeycloakUsernameWithRole("admin")).thenReturn(Optional.of(buildAdminUser()));

        AppRoleRequest request = new AppRoleRequest("CUSTOM", "Custom Role", "A custom role", List.of(), List.of());
        AppRoleResponse created = buildRoleResponse(10L, "CUSTOM", 4);
        when(appRoleService.create(any(AppRoleRequest.class), eq(3), anyString())).thenReturn(created);

        mockMvc.perform(post("/role-management")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(jwt().jwt(j -> j.claim("preferred_username", "admin"))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(10))
            .andExpect(jsonPath("$.name").value("CUSTOM"));
    }

    @Test
    void shouldReturnBadRequestWhenRoleNameBlank() throws Exception {
        String json = """
            { "name": "", "displayName": "Custom Role" }
            """;

        mockMvc.perform(post("/role-management")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .with(jwt().jwt(j -> j.claim("preferred_username", "admin"))))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldUpdatePermissions() throws Exception {
        when(appUserRepository.findByKeycloakUsernameWithRole("admin")).thenReturn(Optional.of(buildAdminUser()));
        when(userPermissionService.getPermissions("admin")).thenReturn(java.util.Set.of("USER_VIEW"));
        AppRoleResponse updated = buildRoleResponse(5L, "FACULTY", 5);
        when(appRoleService.updatePermissions(anyLong(), anyList(), anySet(), anyString(), anyInt()))
            .thenReturn(updated);

        mockMvc.perform(put("/role-management/5/permissions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of("USER_VIEW")))
                .with(jwt().jwt(j -> j.claim("preferred_username", "admin"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(5));
    }

    @Test
    void shouldUpdateDashboardWidgets() throws Exception {
        when(appUserRepository.findByKeycloakUsernameWithRole("admin")).thenReturn(Optional.of(buildAdminUser()));
        AppRoleResponse updated = buildRoleResponse(5L, "FACULTY", 5);
        when(appRoleService.updateDashboardWidgetConfigs(anyLong(), anyList(), anyString(), anyInt()))
            .thenReturn(updated);

        List<com.cms.dto.WidgetConfigDto> configs = List.of(
            new com.cms.dto.WidgetConfigDto("kpi_students", 0, 1, 1, null),
            new com.cms.dto.WidgetConfigDto("kpi_faculty", 1, 1, 1, null));

        mockMvc.perform(put("/role-management/5/dashboard-widgets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(configs))
                .with(jwt().jwt(j -> j.claim("preferred_username", "admin"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(5));
    }
}

