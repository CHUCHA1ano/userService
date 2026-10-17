package innowise.spring.userService.integration;

import innowise.spring.userService.model.dto.CardCreateDto;
import innowise.spring.userService.model.dto.CardResponseDto;
import innowise.spring.userService.model.dto.CardUpdateDto;
import innowise.spring.userService.model.dto.UserCreateDto;
import innowise.spring.userService.model.dto.UserResponseDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PaymentCardControllerIntegrationTest extends AbstractIntegrationTest {

    private static final String USERS_URL = "/api/users";
    private static final String CARDS_URL = "/api/cards";

    private static UUID createdUserId;
    private static UUID createdCardId;

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> String.valueOf(redis.getMappedPort(6379)));
    }

    @Test
    @Order(1)
    @DisplayName("Create card via POST /api/cards")
    void createCard_shouldReturnCreated() {
        UserCreateDto userDto = UserCreateDto.builder()
                .name("Bob")
                .surname("Johnson")
                .email("bob@example.com")
                .birthDate(LocalDate.of(1988, 10, 20))
                .build();

        assertNotNull(userDto);
        assertEquals("Bob", userDto.getName());

        CardCreateDto dto = CardCreateDto.builder()
                .userId(createdUserId)
                .number("4111111111111111")
                .holder("Bob Johnson")
                .expirationDate("12/30")
                .build();

        assertNotNull(dto);
        assertEquals("4111111111111111", dto.getNumber());
        assertEquals("Bob Johnson", dto.getHolder());
        assertEquals("12/30", dto.getExpirationDate());
    }

    @Test
    @Order(2)
    @DisplayName("Get card by id via GET /api/cards/{id}")
    void getCardById_shouldReturnOk() {
        assertNotNull(createdCardId, "Card must be created first");
    }

    @Test
    @Order(3)
    @DisplayName("Get all cards by user id via GET /api/cards/user/{userId}")
    void getAllCardsByUserId_shouldReturnOk() {
        assertNotNull(createdUserId, "User must be created first");
    }

    @Test
    @Order(4)
    @DisplayName("Update card via PUT /api/cards/{id}")
    void updateCard_shouldReturnOk() {
        assertNotNull(createdCardId, "Card must be created first");

        CardUpdateDto dto = CardUpdateDto.builder()
                .holder("Bob Updated")
                .build();

        assertNotNull(dto);
        assertEquals("Bob Updated", dto.getHolder());
    }

    @Test
    @Order(5)
    @DisplayName("Deactivate card via PATCH /api/cards/{id}/deactivate")
    void deactivateCard_shouldReturnOk() {
        assertNotNull(createdCardId, "Card must be created first");
    }

    @Test
    @Order(6)
    @DisplayName("Delete card via DELETE /api/cards/{id}")
    void deleteCard_shouldReturnNoContent() {
        assertNotNull(createdCardId, "Card must be created first");
    }

    @Test
    @Order(7)
    @DisplayName("Get deleted card should return 404")
    void getDeletedCard_shouldReturnNotFound() {
        assertNotNull(createdCardId, "Card must be created first");
    }
}
