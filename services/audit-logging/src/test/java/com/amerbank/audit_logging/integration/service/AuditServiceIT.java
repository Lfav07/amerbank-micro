package com.amerbank.audit_logging.integration.service;

import com.amerbank.audit_logging.dto.AuditEventMessage;
import com.amerbank.audit_logging.model.AuditEvent;
import com.amerbank.audit_logging.persistence.AbstractIntegrationTest;
import com.amerbank.audit_logging.repository.AuditEventJpaRepository;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = "spring.cloud.config.enabled=false")
@Testcontainers
@ActiveProfiles("test")
class AuditServiceIT extends AbstractIntegrationTest {

    @Container
    static ConfluentKafkaContainer kafka = new ConfluentKafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.8.0")
    );


    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Autowired
    private AuditEventJpaRepository repository;

    private static KafkaTemplate<String, AuditEventMessage> kafkaTemplate;

    @BeforeAll
   static void setUpKafka() throws Exception {
        try (AdminClient adminClient = AdminClient.create(Map.of(
                org.apache.kafka.clients.admin.AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG,
                kafka.getBootstrapServers()
        ))) {
            adminClient.createTopics(List.of(
                    new NewTopic("audit.transactions", 1, (short) 1),
                    new NewTopic("audit.accounts", 1, (short) 1),
                    new NewTopic("audit.auth", 1, (short) 1),
                    new NewTopic("audit.customers", 1, (short) 1),
                    new NewTopic("audit.transactions.dlq", 1, (short) 1),
                    new NewTopic("audit.accounts.dlq", 1, (short) 1),
                    new NewTopic("audit.auth.dlq", 1, (short) 1),
                    new NewTopic("audit.customers.dlq", 1, (short) 1)
            )).all().get();
        }

        Map<String, Object> props = Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers(),
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class
        );
        ProducerFactory<String, AuditEventMessage> producerFactory = new DefaultKafkaProducerFactory<>(props);
        kafkaTemplate = new KafkaTemplate<>(producerFactory);
        kafkaTemplate.setDefaultTopic("audit.accounts");
    }

    @AfterEach
    void clearDatabase() {
        repository.deleteAllInBatch();
    }

    @Test
    @DisplayName("Should persist audit event consumed from Kafka")
    void shouldPersistAuditEventConsumedFromKafka() {
        AuditEventMessage message = buildMessage(UUID.randomUUID(), "acc-100");

        kafkaTemplate.send("audit.accounts", message.entityId(), message);
        kafkaTemplate.flush();

        assertTrue(waitUntil(() -> repository.count() == 1, Duration.ofSeconds(15)));

        AuditEvent stored = repository.findById(message.eventId()).orElseThrow();
        assertEquals(message.eventType(), stored.getEventType());
        assertEquals(message.service(), stored.getService());
        assertEquals(message.actorId(), stored.getActorId());
        assertEquals(message.entityId(), stored.getEntityId());
        assertEquals(message.payload(), stored.getPayload());
    }

    @Test
    @DisplayName("Should ignore duplicate audit events consumed from Kafka")
    void shouldIgnoreDuplicateAuditEventsConsumedFromKafka() {
        UUID eventId = UUID.randomUUID();
        AuditEventMessage message = buildMessage(eventId, "acc-200");

        kafkaTemplate.send("audit.accounts", message.entityId(), message);
        kafkaTemplate.send("audit.accounts", message.entityId(), message);
        kafkaTemplate.flush();

        assertTrue(waitUntil(() -> repository.count() == 1, Duration.ofSeconds(15)));
        assertNotNull(repository.findById(eventId).orElse(null));
    }

    private AuditEventMessage buildMessage(UUID eventId, String entityId) {
        return new AuditEventMessage(
                eventId,
                "ACCOUNT_CREATED",
                Instant.now(),
                "account-service",
                "user-1",
                entityId,
                "ACCOUNT",
                "SUCCESS",
                "corr-" + entityId,
                Map.of("entityId", entityId, "amount", 100)
        );
    }

    private boolean waitUntil(Check check, Duration timeout) {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            if (check.evaluate()) {
                return true;
            }
            try {
                Thread.sleep(200);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    @FunctionalInterface
    private interface Check {
        boolean evaluate();
    }
}
