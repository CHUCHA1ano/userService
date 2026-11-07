package com.innowise.userservice.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.List;

/**
 * Maps the task-compliant {@code role} claim (a single {@code ADMIN} or {@code USER} string emitted
 * by the authService claims mapper) to a Spring Security authority.
 */
public class KeycloakRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private static final String ROLE_CLAIM = "role";

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Object role = jwt.getClaims().get(ROLE_CLAIM);
        if (role == null) {
            return List.of();
        }
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.toString()));
    }
}
