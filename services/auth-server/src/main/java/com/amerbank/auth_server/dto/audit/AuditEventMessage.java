package com.amerbank.auth_server.dto.audit;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AuditEventMessage(
        @NotNull
        UUID eventId,

        @NotBlank
        String eventType,

        @NotNull
        Instant timestamp,

        @NotBlank
        String service,


        String actorId,


        String entityId,

        String entityType,

        @NotBlank
        String status,

        String correlationId,

        Map<String, Object> payload
) {
}

