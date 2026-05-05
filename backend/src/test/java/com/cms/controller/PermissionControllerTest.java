package com.cms.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.cms.config.PermSecurityBean;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.AppRole;
import com.cms.model.AppUser;
import com.cms.model.Permission;
import com.cms.repository.AppUserRepository;
import com.cms.repository.PermissionRepository;
import com.cms.service.UserPermissionService;

@WebMvcTest(controllers = PermissionController.class)
class PermissionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AppUserRepository appUserRepository;

    @MockitoBean
    private PermissionRepository permissionRepository;

    @MockitoBean
    private UserPermissionService userPermissionService;

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

        when(appUserRepository.findByKeycloakUsername("admin")).thenReturn(Optional.of(user));
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

        when(appUserRepository.findByKeycloakUsername("admin")).thenReturn(Optional.of(user));
        when(userPermissionService.getPermissions("admin")).thenReturn(Set.of());

        mockMvc.perform(get("/permissions/my")
                .with(jwt().jwt(j -> j.claim("preferred_username", "admin"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("admin"))
            .andExpect(jsonPath("$.hierarchyLevel").value(0));
    }

    @Test
    void shouldReturn404WhenUserNotFoundForMyPermissions() throws Exception {
        when(appUserRepository.findByKeycloakUsername(anyString()))
            .thenThrow(new ResourceNotFoundException("No app user record found for username: ghost"));

        mockMvc.perform(get("/permissions/my")
                .with(jwt().jwt(j -> j.claim("preferred_username", "ghost"))))
            .andExpect(status().isNotFound());
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
}

