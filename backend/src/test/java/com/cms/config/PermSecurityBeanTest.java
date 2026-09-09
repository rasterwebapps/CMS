package com.cms.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import com.cms.exception.ModuleNotEnabledException;
import com.cms.module.ModuleRegistry;
import com.cms.service.UserPermissionService;

class PermSecurityBeanTest {

    private UserPermissionService userPermissionService;
    private ModuleConfig moduleConfig;
    private PermSecurityBean permSecurityBean;

    @BeforeEach
    void setUp() {
        userPermissionService = mock(UserPermissionService.class);
        moduleConfig = mock(ModuleConfig.class);
        permSecurityBean = new PermSecurityBean(userPermissionService, moduleConfig);

        Jwt jwt = Jwt.withTokenValue("token")
            .header("alg", "none")
            .claim("preferred_username", "jane.doe")
            .build();
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(jwt, null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void hasReturnsTrueWhenModuleEnabledAndUserHoldsPermission() {
        when(moduleConfig.isEnabled(ModuleRegistry.INVENTORY)).thenReturn(true);
        when(userPermissionService.getPermissions("jane.doe")).thenReturn(Set.of("INVENTORY_VIEW"));

        assertThat(permSecurityBean.has("INVENTORY_VIEW")).isTrue();
    }

    @Test
    void hasReturnsFalseWhenModuleEnabledButUserLacksPermission() {
        when(moduleConfig.isEnabled(ModuleRegistry.INVENTORY)).thenReturn(true);
        when(userPermissionService.getPermissions("jane.doe")).thenReturn(Set.of());

        assertThat(permSecurityBean.has("INVENTORY_VIEW")).isFalse();
    }

    @Test
    void hasThrowsModuleNotEnabledWhenModuleDisabledEvenIfUserHoldsThePermission() {
        when(moduleConfig.isEnabled(ModuleRegistry.INVENTORY)).thenReturn(false);
        when(userPermissionService.getPermissions("jane.doe")).thenReturn(Set.of("INVENTORY_VIEW"));

        assertThatThrownBy(() -> permSecurityBean.has("INVENTORY_VIEW"))
            .isInstanceOf(ModuleNotEnabledException.class)
            .extracting(ex -> ((ModuleNotEnabledException) ex).getModuleCode())
            .isEqualTo(ModuleRegistry.INVENTORY);
    }

    @Test
    void coreCodeWithNoModuleMappingIsUnaffectedByModuleConfig() {
        when(userPermissionService.getPermissions("jane.doe")).thenReturn(Set.of("USER_VIEW"));

        assertThat(permSecurityBean.has("USER_VIEW")).isTrue();
    }

    @Test
    void hasAnyReturnsTrueWhenOneCodeIsFromAnEnabledModuleEvenIfAnotherCodesModuleIsDisabled() {
        when(moduleConfig.isEnabled(ModuleRegistry.INVENTORY)).thenReturn(false);
        when(moduleConfig.isEnabled(ModuleRegistry.LIBRARY)).thenReturn(true);
        when(userPermissionService.getPermissions("jane.doe")).thenReturn(Set.of("LIBRARY_ISSUE_VIEW"));

        assertThat(permSecurityBean.hasAny("INVENTORY_VIEW", "LIBRARY_ISSUE_VIEW")).isTrue();
    }

    @Test
    void hasAnyThrowsModuleNotEnabledOnlyWhenEveryOfferedCodeIsModuleGatedOff() {
        when(moduleConfig.isEnabled(ModuleRegistry.INVENTORY)).thenReturn(false);
        when(moduleConfig.isEnabled(ModuleRegistry.LIBRARY)).thenReturn(false);
        when(userPermissionService.getPermissions("jane.doe")).thenReturn(Set.of("INVENTORY_VIEW", "LIBRARY_ISSUE_VIEW"));

        assertThatThrownBy(() -> permSecurityBean.hasAny("INVENTORY_VIEW", "LIBRARY_ISSUE_VIEW"))
            .isInstanceOf(ModuleNotEnabledException.class);
    }

    @Test
    void hasReturnsFalseWhenNotAuthenticated() {
        SecurityContextHolder.clearContext();
        when(moduleConfig.isEnabled(ModuleRegistry.INVENTORY)).thenReturn(true);

        assertThat(permSecurityBean.has("INVENTORY_VIEW")).isFalse();
    }

    @Test
    void systemRoleUserBypassesModuleGateEvenWhenModuleDisabled() {
        when(moduleConfig.isEnabled(ModuleRegistry.INVENTORY)).thenReturn(false);
        when(userPermissionService.isSystemRole("jane.doe")).thenReturn(true);
        when(userPermissionService.getPermissions("jane.doe")).thenReturn(Set.of("INVENTORY_VIEW"));

        assertThat(permSecurityBean.has("INVENTORY_VIEW")).isTrue();
    }

    @Test
    void systemRoleUserStillNeedsThePermissionItselfEvenIfExemptFromModuleGating() {
        when(moduleConfig.isEnabled(ModuleRegistry.INVENTORY)).thenReturn(false);
        when(userPermissionService.isSystemRole("jane.doe")).thenReturn(true);
        when(userPermissionService.getPermissions("jane.doe")).thenReturn(Set.of());

        // Exempt from the module gate, but DEV_ADMIN/SUPPORT_ADMIN's catch-all sync (V129) is a
        // DB-level guarantee, not something this bean fabricates -- a role that genuinely lacks
        // the permission (e.g. in a test fixture) is still denied on that basis alone.
        assertThat(permSecurityBean.has("INVENTORY_VIEW")).isFalse();
    }
}
