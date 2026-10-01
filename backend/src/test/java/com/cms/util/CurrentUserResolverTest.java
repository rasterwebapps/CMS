package com.cms.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import com.cms.model.AppUser;
import com.cms.repository.AppUserRepository;

class CurrentUserResolverTest {

    private AppUserRepository appUserRepository;
    private CurrentUserResolver resolver;

    @BeforeEach
    void setUp() {
        appUserRepository = mock(AppUserRepository.class);
        resolver = new CurrentUserResolver(appUserRepository);
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

    @Test
    void resolveFullNameReturnsAppUserFullNameWhenMatchFound() {
        Jwt jwt = Jwt.withTokenValue("token")
            .header("alg", "none")
            .claim("preferred_username", "devadmin")
            .build();
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(jwt, null, List.of()));

        AppUser appUser = new AppUser();
        appUser.setFullName("Developer Administrator");
        when(appUserRepository.findByKeycloakUsernameWithRole("devadmin")).thenReturn(Optional.of(appUser));

        assertThat(resolver.resolveFullName()).isEqualTo("Developer Administrator");
    }

    @Test
    void resolveFullNameFallsBackToUsernameWhenNoMatchingAppUser() {
        Jwt jwt = Jwt.withTokenValue("token")
            .header("alg", "none")
            .claim("preferred_username", "deactivated.user")
            .build();
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(jwt, null, List.of()));

        when(appUserRepository.findByKeycloakUsernameWithRole("deactivated.user")).thenReturn(Optional.empty());

        assertThat(resolver.resolveFullName()).isEqualTo("deactivated.user");
    }

    @Test
    void resolveFullNameFallsBackToUsernameWhenAppUserFullNameIsBlank() {
        Jwt jwt = Jwt.withTokenValue("token")
            .header("alg", "none")
            .claim("preferred_username", "jdoe")
            .build();
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(jwt, null, List.of()));

        AppUser appUser = new AppUser();
        appUser.setFullName("   ");
        when(appUserRepository.findByKeycloakUsernameWithRole("jdoe")).thenReturn(Optional.of(appUser));

        assertThat(resolver.resolveFullName()).isEqualTo("jdoe");
    }

    @Test
    void resolveFullNameReturnsSystemWhenNotAuthenticated() {
        SecurityContextHolder.clearContext();

        assertThat(resolver.resolveFullName()).isEqualTo("system");
    }
}

