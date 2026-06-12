package com.cms.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
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
import com.cms.dto.AppUserResponse;
import com.cms.dto.CreateUserRequest;
import com.cms.dto.UpdateUserRequest;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.AppRole;
import com.cms.model.AppUser;
import com.cms.repository.AppUserRepository;
import com.cms.service.AppUserService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = UserManagementController.class)
class UserManagementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AppUserService appUserService;

    @MockitoBean
    private AppUserRepository appUserRepository;

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

    private AppUserResponse buildUserResponse(Long id, String username, String roleName) {
        return new AppUserResponse(id, username, username + "@test.com", "Full Name",
            roleName, roleName + " Display", 5, true, "admin", Instant.now());
    }

    @Test
    void shouldListManageableUsers() throws Exception {
        when(appUserRepository.findByKeycloakUsername("admin")).thenReturn(Optional.of(buildAdminUser()));
        when(appUserService.findManageable(3)).thenReturn(
            List.of(buildUserResponse(2L, "faculty1", "FACULTY"),
                    buildUserResponse(3L, "student1", "STUDENT")));

        mockMvc.perform(get("/user-management")
                .with(jwt().jwt(j -> j.claim("preferred_username", "admin"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].keycloakUsername").value("faculty1"))
            .andExpect(jsonPath("$[1].keycloakUsername").value("student1"));
    }

    @Test
    void shouldReturn404WhenRequestingUserNotFound() throws Exception {
        when(appUserRepository.findByKeycloakUsername("ghost"))
            .thenThrow(new ResourceNotFoundException("No app user record found for username: ghost"));

        mockMvc.perform(get("/user-management")
                .with(jwt().jwt(j -> j.claim("preferred_username", "ghost"))))
            .andExpect(status().isNotFound());
    }

    @Test
    void shouldCreateUser() throws Exception {
        when(appUserRepository.findByKeycloakUsername("admin")).thenReturn(Optional.of(buildAdminUser()));

        CreateUserRequest request = new CreateUserRequest("new@test.com", "New Faculty", "newfaculty", null, "FACULTY", null, null);

        AppUserResponse created = buildUserResponse(10L, "newfaculty", "FACULTY");
        when(appUserService.create(any(CreateUserRequest.class), eq("admin"), eq(3))).thenReturn(created);

        mockMvc.perform(post("/user-management")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(jwt().jwt(j -> j.claim("preferred_username", "admin"))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.keycloakUsername").value("newfaculty"))
            .andExpect(jsonPath("$.roleName").value("FACULTY"));
    }

    @Test
    void shouldReturnBadRequestWhenCreateUserMissingFields() throws Exception {
        String invalidJson = """
            { "email": "" }
            """;

        mockMvc.perform(post("/user-management")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson)
                .with(jwt().jwt(j -> j.claim("preferred_username", "admin"))))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldUpdateUser() throws Exception {
        when(appUserRepository.findByKeycloakUsername("admin")).thenReturn(Optional.of(buildAdminUser()));

        UpdateUserRequest request = new UpdateUserRequest("Updated Name", "updated@test.com", null, null);
        AppUserResponse updated = buildUserResponse(2L, "faculty1", "FACULTY");
        when(appUserService.update(eq(2L), any(UpdateUserRequest.class), eq(3), anyString())).thenReturn(updated);

        mockMvc.perform(put("/user-management/2")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(jwt().jwt(j -> j.claim("preferred_username", "admin"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.keycloakUsername").value("faculty1"));
    }

    @Test
    void shouldReturnBadRequestWhenUpdateEmailInvalid() throws Exception {
        String invalidJson = """
            { "email": "not-an-email" }
            """;

        mockMvc.perform(put("/user-management/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson)
                .with(jwt().jwt(j -> j.claim("preferred_username", "admin"))))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldDeactivateUser() throws Exception {
        when(appUserRepository.findByKeycloakUsername("admin")).thenReturn(Optional.of(buildAdminUser()));
        doNothing().when(appUserService).deactivate(anyLong(), anyInt(), anyString());

        mockMvc.perform(put("/user-management/2/deactivate")
                .with(jwt().jwt(j -> j.claim("preferred_username", "admin"))))
            .andExpect(status().isNoContent());
    }

    @Test
    void shouldReactivateUser() throws Exception {
        when(appUserRepository.findByKeycloakUsername("admin")).thenReturn(Optional.of(buildAdminUser()));
        doNothing().when(appUserService).reactivate(anyLong(), anyInt(), anyString());

        mockMvc.perform(put("/user-management/2/reactivate")
                .with(jwt().jwt(j -> j.claim("preferred_username", "admin"))))
            .andExpect(status().isNoContent());
    }
}

