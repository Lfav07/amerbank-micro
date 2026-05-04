package com.amerbank.audit_logging.service;

import com.amerbank.audit_logging.dto.AuditEventMessage;
import com.amerbank.audit_logging.model.AuditEvent;
import org.springframework.stereotype.Component;

@Component
public class AuditMapper {
    public AuditEvent toDomain(AuditEventMessage message) {
        return AuditEvent.builder().
                eventId(message.eventId()).
                eventType(message.eventType()).
                timestamp(message.timestamp()).
                service(message.service()).
                actorId(message.actorId())
                .entityId(message.entityId())
                .entityType(message.entityType())
                .status(message.status())
                .correlationId(message.correlationId())
                .payload(message.payload())
                .build();
    }
}
