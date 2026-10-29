package innowise.spring.userService.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Maps Keycloak {@code realm_access.roles} to Spring Security authorities.
 */
public class KeycloakRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private static final String REALM_ACCESS = "realm_access";
    private static final String ROLES = "roles";

    @Override
    @SuppressWarnings("unchecked")
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Object realmAccess = jwt.getClaims().get(REALM_ACCESS);
        if (!(realmAccess instanceof Map<?, ?> access)) {
            return List.of();
        }
        Object roles = access.get(ROLES);
        if (!(roles instanceof Collection<?> roleCollection)) {
            return List.of();
        }
        return roleCollection.stream()
                .filter(Objects::nonNull)
                .map(Object::toString)
                .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role))
                .toList();
    }
}
