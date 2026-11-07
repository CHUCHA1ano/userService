package com.innowise.userservice.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Card flow through the real MVC stack: the 5-cards limit, spec paths, cache eviction
 * (GET of the user after card mutations returns the fresh card list), lifecycle statuses.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PaymentCardControllerIntegrationTest extends AbstractIntegrationTest {

    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final String CREATE_USER_JSON = """
            {
              "name": "Bob",
              "surname": "Johnson",
              "birthDate": "1988-10-20",
              "email": "bob@example.com",
              "password": "password123"
            }
            """;
    private static final String CREATE_CARD_JSON = """
            {"number": "4111111111111111", "holder": "Bob Johnson", "expirationDate": "2030-12-01"}
            """;
    private static final JwtRequestPostProcessor ADMIN =
            jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    private static UUID createdCardId;

    @Test
    @Order(1)
    @DisplayName("Creates the owner user")
    void createCardOwnerUser() throws Exception {
        when(keycloakAdminClient.createUser(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(USER_ID);
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_USER_JSON))
                .andExpect(status().isCreated());
    }

    @Test
    @Order(2)
    @DisplayName("POST /api/users/{userId}/cards accepts 5 cards")
    void createFiveCards_shouldReturnCreated() throws Exception {
        for (int i = 0; i < 5; i++) {
            MvcResult result = mockMvc.perform(post("/api/users/{userId}/cards", USER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CREATE_CARD_JSON)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNotEmpty())
                    .andReturn();
            JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
            createdCardId = UUID.fromString(body.get("id").asString());
        }
    }

    @Test
    @Order(3)
    @DisplayName("The 6th card is rejected with 409")
    void createSixthCard_shouldReturnConflict() throws Exception {
        mockMvc.perform(post("/api/users/{userId}/cards", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_CARD_JSON)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @Order(4)
    @DisplayName("A card with an expired date is rejected by @Future with 400")
    void createCard_withPastExpiration_shouldReturnBadRequest() throws Exception {
        String expiredCard = """
                {"number": "4111111111111111", "holder": "Bob Johnson", "expirationDate": "2020-01-01"}
                """;
        mockMvc.perform(post("/api/users/{userId}/cards", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(expiredCard)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.expirationDate").exists());
    }

    @Test
    @Order(5)
    @DisplayName("GET /api/users/{userId}/cards lists the 5 cards")
    void getCardsByUser_shouldReturnFiveCards() throws Exception {
        mockMvc.perform(get("/api/users/{userId}/cards", USER_ID)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)));
    }

    @Test
    @Order(6)
    @DisplayName("GET /api/users/{id} returns the user with the cards list inside")
    void getUser_shouldIncludeCards() throws Exception {
        mockMvc.perform(get("/api/users/{id}", USER_ID)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cards", hasSize(5)));
    }

    @Test
    @Order(7)
    @DisplayName("PUT /api/cards/{id} updates the holder")
    void updateCard_shouldReturnUpdated() throws Exception {
        String body = """
                {"holder": "Updated Holder"}
                """;
        mockMvc.perform(put("/api/cards/{id}", createdCardId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.holder").value("Updated Holder"));
    }

    @Test
    @Order(8)
    @DisplayName("PATCH /api/cards/{id}/deactivate and activate keep the flag in sync")
    void deactivateThenActivateCard_shouldToggleActive() throws Exception {
        mockMvc.perform(patch("/api/cards/{id}/deactivate", createdCardId)
                        .with(ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(patch("/api/cards/{id}/activate", createdCardId)
                        .with(ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    @Order(9)
    @DisplayName("DELETE /api/cards/{id} completes the lifecycle: 204 then 404, list shrinks to 4")
    void deleteCard_thenGetShouldReturnNotFound() throws Exception {
        mockMvc.perform(delete("/api/cards/{id}", createdCardId)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/cards/{id}", createdCardId)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/users/{userId}/cards", USER_ID)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(4)));
    }

    @Test
    @Order(10)
    @DisplayName("GET /api/users/{id} after card delete returns 4 cards (cache evicted)")
    void getUser_afterCardDelete_shouldRefreshCards() throws Exception {
        mockMvc.perform(get("/api/users/{id}", USER_ID)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cards", hasSize(4)));
    }
}
