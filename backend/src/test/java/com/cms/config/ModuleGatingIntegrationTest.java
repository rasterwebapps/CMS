package com.cms.config;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.cms.dto.OrganizationResponse;
import com.cms.inventory.catalog.dto.CategoryResponse;
import com.cms.inventory.catalog.service.CategoryService;
import com.cms.service.CampusInfrastructureService;
import com.cms.service.UserPermissionService;

/**
 * Full-context, end-to-end proof that an exception thrown from inside a
 * {@code @PreAuthorize("@perm...")} SpEL evaluation — here,
 * {@link com.cms.exception.ModuleNotEnabledException}, thrown by {@link PermSecurityBean} when
 * a permission's module is disabled — actually reaches {@code GlobalExceptionHandler} as itself,
 * rather than being swallowed into a generic {@code AccessDeniedException} by Spring Security's
 * method-security machinery. This is the one technical assumption the whole module-gating design
 * rests on, per the module-architecture decision log, so it's verified against the real
 * {@code SecurityConfig} (full context, not a {@code @WebMvcTest} slice) and a real controller
 * endpoint rather than only unit-tested against {@link PermSecurityBean} directly.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(ModuleGatingIntegrationTest.TestConfig.class)
@TestPropertySource(properties = "app.modules.enabled=CORE_INFRA")
class ModuleGatingIntegrationTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        JwtDecoder jwtDecoder() {
            return mock(JwtDecoder.class);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserPermissionService userPermissionService;

    @MockBean
    private CategoryService categoryService;

    @MockBean
    private CampusInfrastructureService campusInfrastructureService;

    @Test
    void moduleDisabledEndpointReturns403WithDistinctModuleNotEnabledCode() throws Exception {
        // app.modules.enabled=CORE_INFRA (above) does not include INVENTORY, even though this
        // user genuinely holds the permission the endpoint asks for -- the module gate must win.
        when(userPermissionService.getPermissions("jane.doe"))
            .thenReturn(Set.of("INVENTORY_CATEGORY_VIEW"));
        when(categoryService.findAll(false)).thenReturn(List.<CategoryResponse>of());

        mockMvc.perform(get("/inventory/categories")
                .with(jwt().jwt(j -> j.claim("preferred_username", "jane.doe"))))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("MODULE_NOT_ENABLED"))
            .andExpect(jsonPath("$.moduleCode").value("INVENTORY"));
    }

    @Test
    void systemRoleUserReachesDisabledModuleEndpointSuccessfully() throws Exception {
        // Same deployment (app.modules.enabled=CORE_INFRA, INVENTORY disabled) as the test above,
        // but this user's role is a platform system role (DEV_ADMIN/SUPPORT_ADMIN) -- exempt from
        // module gating entirely, per the mandatory DEV_ADMIN/SUPPORT_ADMIN catch-all guarantee.
        when(userPermissionService.isSystemRole("devadmin")).thenReturn(true);
        when(userPermissionService.getPermissions("devadmin"))
            .thenReturn(Set.of("INVENTORY_CATEGORY_VIEW"));
        when(categoryService.findAll(false)).thenReturn(List.<CategoryResponse>of());

        mockMvc.perform(get("/inventory/categories")
                .with(jwt().jwt(j -> j.claim("preferred_username", "devadmin"))))
            .andExpect(status().isOk());
    }

    @Test
    void enabledModuleWithoutPermissionReturnsPlainForbidden() throws Exception {
        // CORE_INFRA is enabled but this user holds no CAMPUS_INFRASTRUCTURE_* permission --
        // a plain permission denial, not a module-gate error.
        when(userPermissionService.getPermissions("no.access")).thenReturn(Set.of());
        when(campusInfrastructureService.findAllOrganizations()).thenReturn(List.<OrganizationResponse>of());

        mockMvc.perform(get("/campus-infrastructure/organizations")
                .with(jwt().jwt(j -> j.claim("preferred_username", "no.access"))))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value("Access denied"));
    }
}
