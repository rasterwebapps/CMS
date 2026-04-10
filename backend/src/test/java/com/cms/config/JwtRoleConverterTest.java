package com.cms.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class JwtRoleConverterTest {

    private final JwtRoleConverter converter = new JwtRoleConverter();

    @Test
    void shouldExtractRealmRolesFromJwt() {
        Jwt jwt = buildJwt(Map.of(
            "realm_access", Map.of("roles", List.of("ROLE_ADMIN", "ROLE_FACULTY"))
        ));

        JwtAuthenticationToken token = (JwtAuthenticationToken) converter.convert(jwt);

        assertThat(token).isNotNull();
        List<String> authorities = token.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .toList();
        assertThat(authorities).containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_FACULTY");
    }

    @Test
    void shouldReturnEmptyAuthoritiesWhenNoRealmAccess() {
        Jwt jwt = buildJwt(Map.of());

        JwtAuthenticationToken token = (JwtAuthenticationToken) converter.convert(jwt);

        assertThat(token).isNotNull();
        assertThat(token.getAuthorities()).isEmpty();
    }

    @Test
    void shouldReturnEmptyAuthoritiesWhenRolesNotAList() {
        Jwt jwt = buildJwt(Map.of(
            "realm_access", Map.of("roles", "not-a-list")
        ));

        JwtAuthenticationToken token = (JwtAuthenticationToken) converter.convert(jwt);

        assertThat(token).isNotNull();
        assertThat(token.getAuthorities()).isEmpty();
    }

    @Test
    void shouldUsePreferredUsernameAsName() {
        Jwt jwt = buildJwt(Map.of(
            "preferred_username", "admin",
            "realm_access", Map.of("roles", List.of("ROLE_ADMIN"))
        ));

        JwtAuthenticationToken token = (JwtAuthenticationToken) converter.convert(jwt);

        assertThat(token).isNotNull();
        assertThat(token.getName()).isEqualTo("admin");
    }

    @Test
    void shouldHandleEmptyRolesList() {
        Jwt jwt = buildJwt(Map.of(
            "realm_access", Map.of("roles", List.of())
        ));

        JwtAuthenticationToken token = (JwtAuthenticationToken) converter.convert(jwt);

        assertThat(token).isNotNull();
        assertThat(token.getAuthorities()).isEmpty();
    }

    private Jwt buildJwt(Map<String, Object> claims) {
        Jwt.Builder builder = Jwt.withTokenValue("mock-token")
            .header("alg", "RS256")
            .claim("sub", "test-user")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(300));

        claims.forEach(builder::claim);

        return builder.build();
    }
}
