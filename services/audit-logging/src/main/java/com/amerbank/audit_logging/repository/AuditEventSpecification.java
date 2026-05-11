package com.amerbank.audit_logging.repository;

import com.amerbank.audit_logging.dto.AuditFilterRequest;
import com.amerbank.audit_logging.model.AuditEvent;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;

public class AuditEventSpecification {
    public static Specification<AuditEvent> withFilters(AuditFilterRequest request){
        Specification<AuditEvent> spec = Specification.where(null);

        spec = spec.and(hasEventType(request.eventType()));
        spec = spec.and(hasActorId(request.actorId()));
        spec = spec.and(hasEntityId(request.entityId()));
        spec = spec.and(hasEntityType(request.entityType()));
        spec = spec.and(hasStatus(request.status()));
        spec = spec.and(hasService(request.service()));
        spec = spec.and(createdBetween(request.from(), request.to()));
        return spec;
    }

    public static Specification<AuditEvent> hasEventType(String eventType) {
        return (root, query, builder) ->
                eventType == null ? null :
                        builder.equal(root.get("eventType"), eventType);
    }

    public static Specification<AuditEvent> hasActorId(String actorId) {
        return (root, query, builder) ->
                actorId == null ? null :
                        builder.equal(root.get("actorId"), actorId);
    }

    public static Specification<AuditEvent> hasEntityId(String entityId) {
        return (root, query, builder) ->
                entityId == null ? null :
                        builder.equal(root.get("entityId"), entityId);
    }

    public static Specification<AuditEvent> hasEntityType(String entityType) {
        return (root, query, builder) ->
                entityType == null ? null :
                        builder.equal(root.get("entityType"), entityType);
    }

    public static Specification<AuditEvent> hasStatus(String status) {
        return (root, query, builder) ->
                status == null ? null :
                        builder.equal(root.get("status"), status);
    }


    public static Specification<AuditEvent> hasService(String service) {
        return (root, query, builder) ->
                service == null ? null :
                        builder.equal(root.get("service"), service);
    }

    public static Specification<AuditEvent> createdBetween(Instant from, Instant to) {
        return (root, query, builder) -> {
            if (from == null && to == null) return null;
            if (from != null && to != null) {
                return builder.between(root.get("timestamp"), from, to);
            }
            if (from != null) {
                return builder.greaterThanOrEqualTo(root.get("timestamp"), from);
            }
            return builder.lessThanOrEqualTo(root.get("timestamp"), to);
        };
    }
}
