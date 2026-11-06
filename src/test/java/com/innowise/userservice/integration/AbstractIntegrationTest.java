package com.innowise.userservice.integration;

import com.innowise.userservice.integration.keycloak.KeycloakAdminClient;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Singleton-container pattern: containers are started once per JVM from the static
 * initializer and cleaned up by Ryuk afterwards. The Spring context (together with the
 * containers it points to) is cached and shared by all integration test classes.
 * <p>
 * Dynamic properties have the highest precedence, so they override the driverless
 * placeholders of application-local.properties.
 * <p>
 * KeycloakAdminClient is mocked: the real Admin API is an external system with its own
 * integration tests; here it is replaced with a stub so tests focus on the service itself.
 */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class AbstractIntegrationTest {

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:15-alpine"))
            .withDatabaseName("userdb")
            .withUsername("postgres")
            .withPassword("postgres");

    static final GenericContainer<?> REDIS = new GenericContainer<>(
            DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    static {
        POSTGRES.start();
        REDIS.start();
    }

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> String.valueOf(REDIS.getMappedPort(6379)));
    }

    @MockitoBean
    protected KeycloakAdminClient keycloakAdminClient;
}
