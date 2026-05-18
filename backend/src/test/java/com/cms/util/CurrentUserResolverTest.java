package com.cms.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

class CurrentUserResolverTest {

    private CurrentUserResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new CurrentUserResolver();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void resolveReturnsUsernameFromJwtPreferredUsernameClaimWhenPresent() {
        Jwt jwt = Jwt.withTokenValue("token")
            .header("alg", "none")
            .claim("preferred_username", "john.doe")
            .build();
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(jwt, null, List.of()));

        assertThat(resolver.resolve()).isEqualTo("john.doe");
    }

    @Test
    void resolveReturnsAuthNameWhenJwtHasNoPreferredUsername() {
        Jwt jwt = Jwt.withTokenValue("token")
            .header("alg", "none")
            .claim("sub", "user-uuid-123")
            .build();
        UsernamePasswordAuthenticationToken auth =
            new UsernamePasswordAuthenticationToken(jwt, null, List.of()) {
                @Override
                public String getName() { return "user-uuid-123"; }
            };
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThat(resolver.resolve()).isEqualTo("user-uuid-123");
    }

    @Test
    void resolveReturnsAuthNameWhenPrincipalIsNotJwt() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn("plain-string-principal");
        when(auth.getName()).thenReturn("someuser");
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThat(resolver.resolve()).isEqualTo("someuser");
    }

    @Test
    void resolveReturnsNullWhenNotAuthenticated() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(false);
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThat(resolver.resolve()).isNull();
    }

    @Test
    void resolveReturnsNullWhenNoAuthentication() {
        SecurityContextHolder.clearContext();

        assertThat(resolver.resolve()).isNull();
    }
}

