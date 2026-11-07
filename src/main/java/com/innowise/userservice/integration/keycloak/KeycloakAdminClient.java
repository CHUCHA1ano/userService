package com.innowise.userservice.integration.keycloak;

import com.innowise.userservice.config.KeycloakProperties;
import com.innowise.userservice.exception.EmailAlreadyExistsException;
import com.innowise.userservice.exception.KeycloakIntegrationException;
import com.innowise.userservice.integration.keycloak.dto.CredentialRepresentation;
import com.innowise.userservice.integration.keycloak.dto.KeycloakUserRequest;
import com.innowise.userservice.integration.keycloak.dto.RoleRepresentation;
import com.innowise.userservice.integration.keycloak.dto.TokenResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thin REST client for the Keycloak Admin API.
 * <p>
 * UserService is the owner of user profiles, so when a profile is created, updated, deleted or
 * activated/deactivated, the matching Keycloak user is kept in sync through this client.
 */
@Service
public class KeycloakAdminClient {

    private static final Logger log = LoggerFactory.getLogger(KeycloakAdminClient.class);

    private static final String USER_ROLE = "USER";
    private static final String PASSWORD_CREDENTIAL = "password";

    private final RestClient restClient;
    private final KeycloakProperties properties;
    private final Map<String, String> roleIds = new ConcurrentHashMap<>();

    private volatile String accessToken;
    private volatile Instant accessTokenExpiresAt = Instant.EPOCH;

    public KeycloakAdminClient(KeycloakProperties properties) {
        this.properties = properties;
        // Explicit timeouts: one hung Keycloak call must not block the request thread
        // (and, in UserService.create, the DB connection) indefinitely.
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(10));
        this.restClient = RestClient.builder()
                .baseUrl(properties.url())
                .requestFactory(requestFactory)
                .build();
    }

    /**
     * Creates a user in Keycloak and assigns the {@code USER} realm role.
     *
     * @return Keycloak user id, which is also used as the local user id
     */
    public UUID createUser(String email, String password, String firstName, String lastName) {
        // Keycloak 26 declarative user profile requires firstName/lastName for users with the
        // "user" role, otherwise login fails with "Account is not fully set up".
        KeycloakUserRequest request = new KeycloakUserRequest(
                email,
                email,
                firstName,
                lastName,
                true,
                true,
                List.of(new CredentialRepresentation(PASSWORD_CREDENTIAL, password, false))
        );

        try {
            ResponseEntity<Void> response = restClient.post()
                    .uri("/admin/realms/{realm}/users", properties.realm())
                    .header(HttpHeaders.AUTHORIZATION, bearer())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();

            UUID userId = extractUserId(response);
            try {
                assignRealmRole(userId, USER_ROLE);
            } catch (RuntimeException ex) {
                deleteUserQuietly(userId);
                throw ex;
            }
            return userId;
        } catch (RestClientResponseException ex) {
            throw translate(ex, "create user");
        } catch (RestClientException ex) {
            throw new KeycloakIntegrationException("Keycloak is unavailable while creating user", ex);
        }
    }

    public void updateProfile(UUID userId, String email, String firstName, String lastName) {
        Map<String, Object> body = new HashMap<>();
        body.put("username", email);
        body.put("email", email);
        body.put("firstName", firstName);
        body.put("lastName", lastName);
        execute("update user", () -> restClient.put()
                .uri("/admin/realms/{realm}/users/{id}", properties.realm(), userId)
                .header(HttpHeaders.AUTHORIZATION, bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity());
    }

    public void setEnabled(UUID userId, boolean enabled) {
        execute("change user state", () -> restClient.put()
                .uri("/admin/realms/{realm}/users/{id}", properties.realm(), userId)
                .header(HttpHeaders.AUTHORIZATION, bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("enabled", enabled))
                .retrieve()
                .toBodilessEntity());
    }

    /**
     * Revokes all active sessions of the user. Needed because access tokens live for 12 hours.
     */
    public void logoutUser(UUID userId) {
        execute("logout user", () -> restClient.post()
                .uri("/admin/realms/{realm}/users/{id}/logout", properties.realm(), userId)
                .header(HttpHeaders.AUTHORIZATION, bearer())
                .retrieve()
                .toBodilessEntity());
    }

    public void deleteUser(UUID userId) {
        try {
            restClient.delete()
                    .uri("/admin/realms/{realm}/users/{id}", properties.realm(), userId)
                    .header(HttpHeaders.AUTHORIZATION, bearer())
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == HttpStatus.NOT_FOUND.value()) {
                return;
            }
            throw translate(ex, "delete user");
        } catch (RestClientException ex) {
            throw new KeycloakIntegrationException("Keycloak is unavailable while deleting user", ex);
        }
    }

    private void deleteUserQuietly(UUID userId) {
        try {
            deleteUser(userId);
        } catch (RuntimeException ex) {
            // best-effort compensation: never mask the original failure,
            // but the ghost account must not go unnoticed
            log.error("Failed to compensate: Keycloak user {} remains without the assigned role", userId, ex);
        }
    }

    private void assignRealmRole(UUID userId, String roleName) {
        RoleRepresentation role = new RoleRepresentation(realmRoleId(roleName), roleName);
        execute("assign role " + roleName, () -> restClient.post()
                .uri("/admin/realms/{realm}/users/{id}/role-mappings/realm", properties.realm(), userId)
                .header(HttpHeaders.AUTHORIZATION, bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .body(List.of(role))
                .retrieve()
                .toBodilessEntity());
    }

    private String realmRoleId(String roleName) {
        String cached = roleIds.get(roleName);
        if (cached != null) {
            return cached;
        }
        try {
            RoleRepresentation role = restClient.get()
                    .uri("/admin/realms/{realm}/roles/{name}", properties.realm(), roleName)
                    .header(HttpHeaders.AUTHORIZATION, bearer())
                    .retrieve()
                    .body(RoleRepresentation.class);
            if (role == null || role.id() == null) {
                throw new KeycloakIntegrationException("Keycloak realm role not found: " + roleName);
            }
            roleIds.put(roleName, role.id());
            return role.id();
        } catch (RestClientResponseException ex) {
            throw translate(ex, "read role " + roleName);
        } catch (RestClientException ex) {
            throw new KeycloakIntegrationException("Keycloak is unavailable while reading role " + roleName, ex);
        }
    }

    private UUID extractUserId(ResponseEntity<Void> response) {
        URI location = response.getHeaders().getLocation();
        if (location == null) {
            throw new KeycloakIntegrationException("Keycloak did not return a user location");
        }
        String path = location.getPath();
        String id = path.substring(path.lastIndexOf('/') + 1);
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException ex) {
            throw new KeycloakIntegrationException("Keycloak returned an invalid user id: " + id, ex);
        }
    }

    private String bearer() {
        return "Bearer " + adminToken();
    }

    private synchronized String adminToken() {
        if (accessToken != null && Instant.now().isBefore(accessTokenExpiresAt.minusSeconds(10))) {
            return accessToken;
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", properties.clientId());
        form.add("client_secret", properties.clientSecret());

        try {
            TokenResponse token = restClient.post()
                    .uri("/realms/{realm}/protocol/openid-connect/token", properties.realm())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(TokenResponse.class);

            if (token == null || token.accessToken() == null) {
                throw new KeycloakIntegrationException("Keycloak did not return an admin access token");
            }
            accessToken = token.accessToken();
            accessTokenExpiresAt = Instant.now().plusSeconds(Math.max(token.expiresIn(), 60));
            return accessToken;
        } catch (RestClientResponseException ex) {
            throw translate(ex, "obtain admin token");
        } catch (RestClientException ex) {
            throw new KeycloakIntegrationException("Keycloak is unavailable while obtaining an admin token", ex);
        }
    }

    private void execute(String action, Runnable call) {
        try {
            call.run();
        } catch (RestClientResponseException ex) {
            throw translate(ex, action);
        } catch (RestClientException ex) {
            throw new KeycloakIntegrationException("Keycloak is unavailable while trying to " + action, ex);
        }
    }

    /**
     * Client-visible messages are generic — the raw Keycloak response body is logged
     * for diagnostics but never propagated to {@code GlobalExceptionHandler} callers.
     */
    private RuntimeException translate(RestClientResponseException ex, String action) {
        log.warn("Keycloak call \"{}\" failed with status {}: {}", action, ex.getStatusCode(),
                ex.getResponseBodyAsString());
        if (ex.getStatusCode().value() == HttpStatus.CONFLICT.value()) {
            if (action.equals("create user")) {
                return new EmailAlreadyExistsException("Email is already registered in Keycloak");
            }
            return new KeycloakIntegrationException("Keycloak conflict while trying to " + action, ex);
        }
        return new KeycloakIntegrationException("Keycloak failed to " + action
                + " (" + ex.getStatusCode().value() + ")", ex);
    }
}
