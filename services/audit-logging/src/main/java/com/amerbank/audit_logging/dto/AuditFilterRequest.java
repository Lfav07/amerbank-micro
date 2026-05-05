package com.amerbank.audit_logging.dto;

import jakarta.persistence.Column;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

public record AuditFilterRequest(
        String eventType,
        String service,
        String actorId,
        String entityId,
        String entityType,
        String status,
        Instant from,
        Instant to
) {
}