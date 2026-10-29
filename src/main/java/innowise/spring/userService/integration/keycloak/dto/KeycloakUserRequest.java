package innowise.spring.userService.integration.keycloak.dto;

import java.util.List;

public record KeycloakUserRequest(
        String username,
        String email,
        String firstName,
        String lastName,
        boolean enabled,
        boolean emailVerified,
        List<CredentialRepresentation> credentials
) {
}
