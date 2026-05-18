package com.cms.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import com.cms.service.UserPermissionService;

@ExtendWith(MockitoExtension.class)
class PermSecurityBeanTest {

    @Mock
    private UserPermissionService userPermissionService;

    @InjectMocks
    private PermSecurityBean permSecurityBean;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private Jwt buildJwt(String preferredUsername) {
        return Jwt.withTokenValue("token")
            .header("alg", "RS256")
            .claim("preferred_username", preferredUsername)
            .build();
    }

    @Test
    void has_returnsFalse_whenNoAuthentication() {
        SecurityContextHolder.clearContext();
        assertThat(permSecurityBean.has("DEPT_MANAGE")).isFalse();
    }

    @Test
    void has_returnsFalse_whenAuthenticationIsNull() {
        SecurityContextHolder.getContext().setAuthentication(null);
        assertThat(permSecurityBean.has("DEPT_MANAGE")).isFalse();
    }

    @Test
    void has_returnsFalse_whenPrincipalIsNotJwt() {
        UsernamePasswordAuthenticationToken auth = UsernamePasswordAuthenticationToken
            .authenticated("user", "pass", Set.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(auth);
        assertThat(permSecurityBean.has("DEPT_MANAGE")).isFalse();
    }

    @Test
    void has_returnsFalse_whenPreferredUsernameIsMissing() {
        Jwt jwt = Jwt.withTokenValue("token")
            .header("alg", "RS256")
            .claim("sub", "some-subject")
            .build();
        JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt, Set.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
        assertThat(permSecurityBean.has("DEPT_MANAGE")).isFalse();
    }

    @Test
    void has_returnsFalse_whenUserDoesNotHavePermission() {
        Jwt jwt = buildJwt("faculty1");
        JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt, Set.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(userPermissionService.getPermissions("faculty1"))
            .thenReturn(Set.of("COURSE_VIEW", "ATTENDANCE_MANAGE"));

        assertThat(permSecurityBean.has("DEPT_MANAGE")).isFalse();
    }

    @Test
    void has_returnsTrue_whenUserHasPermission() {
        Jwt jwt = buildJwt("admin");
        JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt, Set.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(userPermissionService.getPermissions("admin"))
            .thenReturn(Set.of("DEPT_MANAGE", "USER_VIEW", "ROLE_VIEW"));

        assertThat(permSecurityBean.has("DEPT_MANAGE")).isTrue();
        assertThat(permSecurityBean.has("USER_VIEW")).isTrue();
    }

    @Test
    void has_returnsFalse_whenPreferredUsernameIsBlank() {
        Jwt jwt = Jwt.withTokenValue("token")
            .header("alg", "RS256")
            .claim("preferred_username", "  ")
            .build();
        JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt, Set.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
        assertThat(permSecurityBean.has("DEPT_MANAGE")).isFalse();
    }

    @Test
    void hasAny_returnsTrueWhenUserHoldsOneOfThePermissions() {
        Jwt jwt = Jwt.withTokenValue("token")
            .header("alg", "RS256")
            .claim("preferred_username", "admin")
            .build();
        JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt, Set.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(userPermissionService.getPermissions("admin"))
            .thenReturn(Set.of("STUDENT_VIEW", "DEPT_MANAGE"));

        assertThat(permSecurityBean.hasAny("REPORT_VIEW", "STUDENT_VIEW")).isTrue();
    }

    @Test
    void hasAny_returnsFalseWhenUserHoldsNoneOfThePermissions() {
        Jwt jwt = Jwt.withTokenValue("token")
            .header("alg", "RS256")
            .claim("preferred_username", "user1")
            .build();
        JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt, Set.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(userPermissionService.getPermissions("user1"))
            .thenReturn(Set.of("PROFILE_VIEW"));

        assertThat(permSecurityBean.hasAny("REPORT_VIEW", "STUDENT_VIEW")).isFalse();
    }

    @Test
    void hasAny_returnsFalseWhenNotAuthenticated() {
        SecurityContextHolder.clearContext();
        assertThat(permSecurityBean.hasAny("REPORT_VIEW")).isFalse();
    }
}

