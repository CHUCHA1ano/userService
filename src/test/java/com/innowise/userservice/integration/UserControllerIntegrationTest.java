package com.innowise.userservice.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end tests through the real MVC stack (security, validation, Redis, Testcontainers
 * PostgreSQL) against the API spec paths.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UserControllerIntegrationTest extends AbstractIntegrationTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String CREATE_USER_JSON = """
            {
              "name": "Alice",
              "surname": "Smith",
              "birthDate": "1995-05-15",
              "email": "alice@example.com",
              "password": "password123"
            }
            """;
    private static final String UPDATE_USER_JSON = """
            {"name": "Alice Updated"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void stubKeycloak() {
        when(keycloakAdminClient.createUser(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(USER_ID);
    }

    @Test
    @Order(1)
    @DisplayName("POST /api/users creates a user and returns 201 with the response body")
    void createUser_shouldReturnCreated() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_USER_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(USER_ID.toString()))
                .andExpect(jsonPath("$.name").value("Alice"))
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.cards").isEmpty());

        verifyUserPersists();
    }

    /**
     * The row must really exist in Testcontainers PostgreSQL; reading through the API
     * additionally proves auditing filled created_at (NOT NULL) on insert.
     */
    private void verifyUserPersists() throws Exception {
        mockMvc.perform(get("/api/users/{id}", USER_ID).with(jwt()
                        .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.createdAt").isNotEmpty());
    }

    @Test
    @Order(2)
    @DisplayName("POST /api/users with a duplicate email returns 409")
    void createUser_withDuplicateEmail_shouldReturnConflict() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_USER_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @Order(3)
    @DisplayName("POST /api/users with invalid payload returns 400 with field errors")
    void createUser_withInvalidPayload_shouldReturnValidationErrors() throws Exception {
        String invalidUser = """
                {"name": "", "surname": "", "birthDate": "2050-01-01", "email": "not-an-email", "password": "123"}
                """;
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidUser))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists())
                .andExpect(jsonPath("$.errors.birthDate").exists());
    }

    @Test
    @Order(4)
    @DisplayName("GET /api/users/{id} without a token returns 401")
    void getUser_withoutAuthentication_shouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/api/users/{id}", USER_ID))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(5)
    @DisplayName("GET /api/users/{id} returns the user as ADMIN")
    void getUserById_shouldReturnOk() throws Exception {
        mockMvc.perform(get("/api/users/{id}", USER_ID).with(jwt()
                        .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Alice"));
    }

    @Test
    @Order(6)
    @DisplayName("GET /api/users with filters returns the matching page")
    void getAll_withFilters_shouldReturnMatchingUsers() throws Exception {
        mockMvc.perform(get("/api/users")
                        .param("name", "Updated")
                        .param("active", "true")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0))); // not updated yet, name is still Alice
    }

    @Test
    @Order(7)
    @DisplayName("PUT /api/users/{id} updates the user")
    void updateUser_shouldReturnUpdated() throws Exception {
        mockMvc.perform(put("/api/users/{id}", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(UPDATE_USER_JSON)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Alice Updated"));

        mockMvc.perform(get("/api/users")
                        .param("name", "updated")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(USER_ID.toString()));
    }

    @Test
    @Order(8)
    @DisplayName("PATCH /api/users/{id}/status deactivates the user")
    void changeStatus_shouldDeactivate() throws Exception {
        mockMvc.perform(patch("/api/users/{id}/status", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\": false}")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    @Order(9)
    @DisplayName("PATCH /api/users/{id}/status reactivates the user")
    void changeStatus_shouldReactivate() throws Exception {
        mockMvc.perform(patch("/api/users/{id}/status", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\": true}")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    @Order(10)
    @DisplayName("DELETE /api/users/{id} returns 204")
    void deleteUser_shouldReturnNoContent() throws Exception {
        MvcResult result = mockMvc.perform(delete("/api/users/{id}", USER_ID)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isNoContent())
                .andReturn();
        assertEquals(204, result.getResponse().getStatus());
    }

    @Test
    @Order(11)
    @DisplayName("GET /api/users/{id} for a deleted user returns 404")
    void getDeletedUser_shouldReturnNotFound() throws Exception {
        mockMvc.perform(get("/api/users/{id}", USER_ID)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
