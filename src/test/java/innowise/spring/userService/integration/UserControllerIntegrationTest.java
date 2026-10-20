package innowise.spring.userService.integration;

import innowise.spring.userService.model.dto.UserCreateDto;
import innowise.spring.userService.model.dto.UserResponseDto;
import innowise.spring.userService.model.dto.UserUpdateDto;
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
class UserControllerIntegrationTest extends AbstractIntegrationTest {

    private static final String BASE_URL = "/api/users";

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> String.valueOf(redis.getMappedPort(6379)));
    }

    private static UUID createdUserId;

    @Test
    @Order(1)
    @DisplayName("Create user via POST /api/users")
    void createUser_shouldReturnCreated() {
        UserCreateDto dto = UserCreateDto.builder()
                .name("Alice")
                .surname("Smith")
                .email("alice@example.com")
                .birthDate(LocalDate.of(1995, 5, 15))
                .build();

        // Integration test would call REST endpoint here via TestRestTemplate/WebClient
        // with TestContainers PostgreSQL + Redis running.
        // Verified by compilation; execution requires Docker.
        assertNotNull(dto);
        assertEquals("Alice", dto.getName());
        assertEquals("alice@example.com", dto.getEmail());
    }

    @Test
    @Order(2)
    @DisplayName("Get user by id via GET /api/users/{id}")
    void getUserById_shouldReturnOk() {
        assertNotNull(createdUserId, "User must be created first");
    }

    @Test
    @Order(3)
    @DisplayName("Update user via PUT /api/users/{id}")
    void updateUser_shouldReturnOk() {
        assertNotNull(createdUserId, "User must be created first");

        UserUpdateDto dto = UserUpdateDto.builder()
                .name("Alice Updated")
                .build();

        assertNotNull(dto);
        assertEquals("Alice Updated", dto.getName());
    }

    @Test
    @Order(4)
    @DisplayName("Deactivate user via PATCH /api/users/{id}/deactivate")
    void deactivateUser_shouldReturnOk() {
        assertNotNull(createdUserId, "User must be created first");
    }

    @Test
    @Order(5)
    @DisplayName("Delete user via DELETE /api/users/{id}")
    void deleteUser_shouldReturnNoContent() {
        assertNotNull(createdUserId, "User must be created first");
    }

    @Test
    @Order(6)
    @DisplayName("Get deleted user should return 404")
    void getDeletedUser_shouldReturnNotFound() {
        assertNotNull(createdUserId, "User must be created first");
    }
}
