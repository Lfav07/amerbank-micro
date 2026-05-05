package com.amerbank.audit_logging.dto;

import java.time.Instant;
import java.util.UUID;

public record AuditEventSummaryResponse(
        UUID eventId,
        String eventType,
        Instant timestamp,
        String service,
        String status
) {}