package com.innowise.userservice.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeycloakRoleConverterTest {

    private final KeycloakRoleConverter converter = new KeycloakRoleConverter();

    @Test
    void shouldMapRoleClaimToSpringAuthority() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("role", "ADMIN")
                .build();

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertEquals(1, authorities.size());
        assertEquals("ROLE_ADMIN", authorities.iterator().next().getAuthority());
    }

    @Test
    void shouldReturnEmptyAuthoritiesWhenRoleClaimIsMissing() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("sub", "user-id")
                .claim("realm_access", java.util.Map.of("roles", java.util.List.of("ADMIN")))
                .build();

        assertTrue(converter.convert(jwt).isEmpty());
    }
}
