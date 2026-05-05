package com.amerbank.audit_logging.service;

import com.amerbank.audit_logging.dto.AuditEventMessage;
import com.amerbank.audit_logging.dto.AuditEventResponse;
import com.amerbank.audit_logging.dto.AuditEventSummaryResponse;
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
    public AuditEventResponse toResponse(AuditEvent event){
        return new AuditEventResponse(
                event.getEventId(),
                event.getEventType(),
                event.getTimestamp(),
                event.getService(),
                event.getActorId(),
                event.getEntityId(),
                event.getEntityType(),
                event.getStatus(),
                event.getCorrelationId(),
                event.getPayload()
        );
    }
    public AuditEventSummaryResponse toSummaryResponse(AuditEvent event) {
        return new AuditEventSummaryResponse(event.getEventId(),
                event.getEventType(),
                event.getTimestamp(),
                event.getService(),
                event.getStatus());
    }
}
