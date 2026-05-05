package com.amerbank.audit_logging.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AuditEventResponse(
        UUID eventId,

        String eventType,

        Instant timestamp,

        String service,


        String actorId,


        String entityId,
        String entityType,

        String status,

        String correlationId,

        Map<String, Object> payload
) {
}
