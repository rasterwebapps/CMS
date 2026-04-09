package com.cms.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JwtRoleConverterTest {

    private final JwtRoleConverter converter = new JwtRoleConverter();

    @Test
    void convert_withRealmRoles_returnsAuthorities() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("realm_access", Map.of("roles", List.of("ROLE_ADMIN", "ROLE_FACULTY")))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertNotNull(authorities);
        assertEquals(2, authorities.size());
        assertTrue(authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
        assertTrue(authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_FACULTY")));
    }

    @Test
    void convert_withNoRealmAccess_returnsEmptyList() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("sub", "user1")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertNotNull(authorities);
        assertTrue(authorities.isEmpty());
    }

    @Test
    void convert_withEmptyRoles_returnsEmptyList() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("realm_access", Map.of("other_key", "value"))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertNotNull(authorities);
        assertTrue(authorities.isEmpty());
    }

    @Test
    void convert_withSingleRole_returnsSingleAuthority() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("realm_access", Map.of("roles", List.of("ROLE_STUDENT")))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertEquals(1, authorities.size());
        assertEquals("ROLE_STUDENT", authorities.iterator().next().getAuthority());
    }
}
