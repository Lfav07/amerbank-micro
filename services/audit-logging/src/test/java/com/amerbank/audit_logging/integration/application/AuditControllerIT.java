package com.amerbank.audit_logging.integration.application;

import com.amerbank.audit_logging.model.AuditEvent;
import com.amerbank.audit_logging.repository.AuditEventJpaRepository;
import com.amerbank.audit_logging.util.TestJwtFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(
        properties = "spring.cloud.config.enabled=false",
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@Testcontainers
@ActiveProfiles("test")
class AuditControllerIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Container
    static ConfluentKafkaContainer kafka = new ConfluentKafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.8.0")
    );

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @TestConfiguration
    static class JwtTestConfig extends TestJwtFactory {
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private TestJwtFactory testJwtFactory;

    @Autowired
    private AuditEventJpaRepository repository;

    @Autowired
    private ObjectMapper objectMapper;

    @AfterEach
    void clearDatabase() {
        repository.deleteAllInBatch();
    }

    @Nested
    @DisplayName("Get Event")
    class GetEventTests {

        @Test
        @DisplayName("Should return audit event by id for admin")
        void shouldReturnAuditEventByIdForAdmin() {
            AuditEvent event = repository.save(buildEvent(
                    UUID.randomUUID(),
                    "ACCOUNT_CREATED",
                    Instant.parse("2026-05-09T10:15:30Z"),
                    "account-service",
                    "SUCCESS",
                    "acc-101"
            ));

            HttpEntity<Void> entity = new HttpEntity<>(adminHeaders());

            ResponseEntity<String> response = restTemplate.exchange(
                    "/audit/" + event.getEventId(),
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            JsonNode body = readTree(response.getBody());
            assertEquals(event.getEventId().toString(), body.get("eventId").asText());
            assertEquals("ACCOUNT_CREATED", body.get("eventType").asText());
            assertEquals("acc-101", body.get("entityId").asText());
        }
    }

    @Nested
    @DisplayName("List Events")
    class ListEventsTests {

        @Test
        @DisplayName("Should filter and sort audit summaries for admin")
        void shouldFilterAndSortAuditSummariesForAdmin() {
            repository.save(buildEvent(
                    UUID.randomUUID(),
                    "ACCOUNT_CREATED",
                    Instant.parse("2026-05-09T10:15:30Z"),
                    "account-service",
                    "SUCCESS",
                    "acc-201"
            ));
            repository.save(buildEvent(
                    UUID.randomUUID(),
                    "ACCOUNT_CREATED",
                    Instant.parse("2026-05-09T12:15:30Z"),
                    "account-service",
                    "SUCCESS",
                    "acc-202"
            ));
            repository.save(buildEvent(
                    UUID.randomUUID(),
                    "LOGIN",
                    Instant.parse("2026-05-09T13:15:30Z"),
                    "auth-service",
                    "FAILED",
                    "user-300"
            ));

            HttpEntity<Void> entity = new HttpEntity<>(adminHeaders());
            ResponseEntity<String> response = restTemplate.exchange(
                    "/audit?eventType=ACCOUNT_CREATED&service=account-service&status=SUCCESS&page=0&size=1",
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            assertEquals(HttpStatus.OK, response.getStatusCode());
            JsonNode root = readTree(response.getBody());
            assertEquals(1, root.get("content").size());
            assertEquals("ACCOUNT_CREATED", root.get("content").get(0).get("eventType").asText());
            assertEquals("account-service", root.get("content").get(0).get("service").asText());
            assertEquals("SUCCESS", root.get("content").get(0).get("status").asText());
            assertEquals(
                    Instant.parse("2026-05-09T12:15:30Z").toString(),
                    root.get("content").get(0).get("timestamp").asText()
            );
        }

        @Test
        @DisplayName("Should reject non admin access")
        void shouldRejectNonAdminAccess() {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(testJwtFactory.generateUserToken("user@amerbank.com"));

            ResponseEntity<String> response = restTemplate.exchange(
                    "/audit",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            );

            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        }

        @Test
        @DisplayName("Should reject invalid token")
        void shouldRejectInvalidToken() {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth("invalid-token");

            ResponseEntity<String> response = restTemplate.exchange(
                    "/audit",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            );

            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        }
    }

    private HttpHeaders adminHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(testJwtFactory.generateAdminToken("admin@amerbank.com"));
        return headers;
    }

    private JsonNode readTree(String body) {
        try {
            return objectMapper.readTree(body);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private AuditEvent buildEvent(
            UUID eventId,
            String eventType,
            Instant timestamp,
            String service,
            String status,
            String entityId) {
        return AuditEvent.builder()
                .eventId(eventId)
                .eventType(eventType)
                .timestamp(timestamp)
                .service(service)
                .actorId("user-1")
                .entityId(entityId)
                .entityType("ACCOUNT")
                .status(status)
                .correlationId("corr-" + entityId)
                .payload(Map.of("entityId", entityId))
                .build();
    }
}
