package com.cms.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import com.cms.model.AppRole;
import com.cms.model.AppUser;
import com.cms.model.UserDashboardWidgetConfig;
import com.cms.repository.AppUserRepository;
import com.cms.repository.UserDashboardWidgetConfigRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = UserDashboardConfigController.class)
class UserDashboardConfigControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AppUserRepository appUserRepository;

    @MockitoBean
    private UserDashboardWidgetConfigRepository userWidgetConfigRepo;

    @MockitoBean(name = "perm")
    private PermSecurityBean perm;

    private AppUser testUser;

    @BeforeEach
    void setUp() {
        when(perm.has(anyString())).thenReturn(true);
        when(perm.hasAny(org.mockito.ArgumentMatchers.<String>any())).thenReturn(true);

        AppRole role = new AppRole();
        role.setName("ROLE_ADMIN");
        role.setDisplayName("Administrator");

        testUser = new AppUser();
        testUser.setId(1L);
        testUser.setKeycloakUsername("testuser");
        testUser.setAppRole(role);

        when(appUserRepository.findByKeycloakUsername("testuser"))
            .thenReturn(Optional.of(testUser));
    }

    @Test
    void getConfigReturnsPersonalConfigWhenPresent() throws Exception {
        UserDashboardWidgetConfig cfg = new UserDashboardWidgetConfig(
            testUser, "hero", 0, 2, 1);
        cfg.setConfigJson(null);

        when(userWidgetConfigRepo.findByUserIdOrderByWidgetOrderAsc(anyLong()))
            .thenReturn(List.of(cfg));

        mockMvc.perform(get("/dashboard/config")
                .with(jwt().jwt(j -> j.claim("preferred_username", "testuser"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0].key").value("hero"))
            .andExpect(jsonPath("$[0].colSpan").value(2));
    }

    @Test
    void getConfigReturnsRoleDefaultWhenNoPersonalConfig() throws Exception {
        when(userWidgetConfigRepo.findByUserIdOrderByWidgetOrderAsc(anyLong()))
            .thenReturn(List.of());

        // Role has no widget configs either — returns empty list
        mockMvc.perform(get("/dashboard/config")
                .with(jwt().jwt(j -> j.claim("preferred_username", "testuser"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getConfigReturns404WhenUserNotFound() throws Exception {
        when(appUserRepository.findByKeycloakUsername("ghost"))
            .thenReturn(Optional.empty());

        mockMvc.perform(get("/dashboard/config")
                .with(jwt().jwt(j -> j.claim("preferred_username", "ghost"))))
            .andExpect(status().isNotFound());
    }

    @Test
    void saveConfigPersistsWidgets() throws Exception {
        doNothing().when(userWidgetConfigRepo).deleteAllByUserId(anyLong());
        when(userWidgetConfigRepo.save(any(UserDashboardWidgetConfig.class)))
            .thenAnswer(inv -> inv.getArgument(0));
        when(userWidgetConfigRepo.findByUserIdOrderByWidgetOrderAsc(anyLong()))
            .thenReturn(List.of());

        String body = """
            [
              {"key": "hero", "order": 0, "colSpan": 2, "rowSpan": 1, "configJson": null},
              {"key": "stat-students", "order": 1, "colSpan": 1, "rowSpan": 1, "configJson": null}
            ]
            """;

        mockMvc.perform(put("/dashboard/config")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .with(jwt().jwt(j -> j.claim("preferred_username", "testuser"))))
            .andExpect(status().isOk());

        verify(userWidgetConfigRepo).deleteAllByUserId(anyLong());
    }

    @Test
    void saveConfigHandlesEmptyList() throws Exception {
        doNothing().when(userWidgetConfigRepo).deleteAllByUserId(anyLong());
        when(userWidgetConfigRepo.findByUserIdOrderByWidgetOrderAsc(anyLong()))
            .thenReturn(List.of());

        mockMvc.perform(put("/dashboard/config")
                .contentType(MediaType.APPLICATION_JSON)
                .content("[]")
                .with(jwt().jwt(j -> j.claim("preferred_username", "testuser"))))
            .andExpect(status().isOk());
    }

    @Test
    void resetConfigDeletesPersonalConfig() throws Exception {
        doNothing().when(userWidgetConfigRepo).deleteAllByUserId(anyLong());

        mockMvc.perform(delete("/dashboard/config")
                .with(jwt().jwt(j -> j.claim("preferred_username", "testuser"))))
            .andExpect(status().isNoContent());

        verify(userWidgetConfigRepo).deleteAllByUserId(anyLong());
    }

    @Test
    void resetConfigReturns404WhenUserNotFound() throws Exception {
        when(appUserRepository.findByKeycloakUsername("ghost"))
            .thenReturn(Optional.empty());

        mockMvc.perform(delete("/dashboard/config")
                .with(jwt().jwt(j -> j.claim("preferred_username", "ghost"))))
            .andExpect(status().isNotFound());
    }
}

