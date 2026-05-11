package com.amerbank.audit_logging.service;

import com.amerbank.audit_logging.dto.AuditEventMessage;
import com.amerbank.audit_logging.dto.AuditEventResponse;
import com.amerbank.audit_logging.dto.AuditEventSummaryResponse;
import com.amerbank.audit_logging.model.AuditEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuditMapperTest {

    private final AuditMapper mapper = new AuditMapper();

    @Test
    @DisplayName("Should map audit message to domain entity")
    void shouldMapMessageToDomain() {
        UUID eventId = UUID.randomUUID();
        Instant timestamp = Instant.parse("2026-05-09T18:00:00Z");
        AuditEventMessage message = new AuditEventMessage(
                eventId,
                "ACCOUNT_CREATED",
                timestamp,
                "account-service",
                "user-1",
                "acc-10",
                "ACCOUNT",
                "SUCCESS",
                "corr-123",
                Map.of("balance", 100)
        );

        AuditEvent event = mapper.toDomain(message);

        assertEquals(eventId, event.getEventId());
        assertEquals("ACCOUNT_CREATED", event.getEventType());
        assertEquals(timestamp, event.getTimestamp());
        assertEquals("account-service", event.getService());
        assertEquals("user-1", event.getActorId());
        assertEquals("acc-10", event.getEntityId());
        assertEquals("ACCOUNT", event.getEntityType());
        assertEquals("SUCCESS", event.getStatus());
        assertEquals("corr-123", event.getCorrelationId());
        assertEquals(Map.of("balance", 100), event.getPayload());
    }

    @Test
    @DisplayName("Should map domain entity to detailed response")
    void shouldMapDomainToResponse() {
        AuditEvent event = buildEvent();

        AuditEventResponse response = mapper.toResponse(event);

        assertEquals(event.getEventId(), response.eventId());
        assertEquals(event.getEventType(), response.eventType());
        assertEquals(event.getTimestamp(), response.timestamp());
        assertEquals(event.getService(), response.service());
        assertEquals(event.getActorId(), response.actorId());
        assertEquals(event.getEntityId(), response.entityId());
        assertEquals(event.getEntityType(), response.entityType());
        assertEquals(event.getStatus(), response.status());
        assertEquals(event.getCorrelationId(), response.correlationId());
        assertEquals(event.getPayload(), response.payload());
    }

    @Test
    @DisplayName("Should map domain entity to summary response")
    void shouldMapDomainToSummaryResponse() {
        AuditEvent event = buildEvent();

        AuditEventSummaryResponse response = mapper.toSummaryResponse(event);

        assertEquals(event.getEventId(), response.eventId());
        assertEquals(event.getEventType(), response.eventType());
        assertEquals(event.getTimestamp(), response.timestamp());
        assertEquals(event.getService(), response.service());
        assertEquals(event.getStatus(), response.status());
    }

    private AuditEvent buildEvent() {
        return AuditEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType("ACCOUNT_CREATED")
                .timestamp(Instant.parse("2026-05-09T18:00:00Z"))
                .service("account-service")
                .actorId("user-1")
                .entityId("acc-10")
                .entityType("ACCOUNT")
                .status("SUCCESS")
                .correlationId("corr-123")
                .payload(Map.of("balance", 100))
                .build();
    }
}
