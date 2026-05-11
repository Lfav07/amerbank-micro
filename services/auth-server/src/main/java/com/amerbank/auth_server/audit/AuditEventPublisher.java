package com.amerbank.auth_server.audit;

import com.amerbank.auth_server.dto.audit.AuditEventMessage;
import com.amerbank.auth_server.dto.response.Role;
import com.amerbank.auth_server.model.User;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Component
public class AuditEventPublisher {

    private static final String SERVICE_NAME = "auth-server";
    private static final String ENTITY_TYPE = "USER";
    private static final String KAFKA_TOPIC = "audit.auth";
    private static final String CORRELATION_ID = "TODO";

    private final KafkaTemplate<String, AuditEventMessage> kafkaTemplate;

    public AuditEventPublisher(KafkaTemplate<String, AuditEventMessage> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishUserRegistered(User user) {
        publish(buildAuditMessage(
                "USER_REGISTERED",
                user.getId(),
                user.getId(),
                "SUCCESS",
                Map.of(
                        "email", user.getEmail(),
                        "role", getPrimaryRole(user)
                )
        ));
    }

    public void publishLoginSuccess(User user) {
        publish(buildAuditMessage(
                "LOGIN_SUCCESS",
                user.getId(),
                user.getId(),
                "SUCCESS",
                Map.of(
                        "email", user.getEmail(),
                        "role", getPrimaryRole(user)
                )
        ));
    }

    public void publishLoginFailed() {
        publish(buildAuditMessage(
                "LOGIN_FAILED",
                null,
                null,
                "FAILED",
                Map.of("reason", "BAD_CREDENTIALS")
        ));
    }

    public void publishAccessDenied(Long actorId, String path) {
        publish(buildAuditMessage(
                "ACCESS_DENIED",
                actorId,
                actorId,
                "FAILED",
                Map.of("path", path)
        ));
    }

    private AuditEventMessage buildAuditMessage(
            String eventType,
            Long actorId,
            Long entityId,
            String status,
            Map<String, Object> payload
    ) {
        return new AuditEventMessage(
                UUID.randomUUID(),
                eventType,
                Instant.now(),
                SERVICE_NAME,
                actorId == null ? null : String.valueOf(actorId),
                entityId == null ? null : String.valueOf(entityId),
                ENTITY_TYPE,
                status,
                CORRELATION_ID,
                payload
        );
    }

    private void publish(AuditEventMessage message) {
        kafkaTemplate.send(KAFKA_TOPIC, message);
    }

    private String getPrimaryRole(User user) {
        if (user.getRoles().contains(Role.ROLE_ADMIN)) {
            return "ADMIN";
        }
        return "USER";
    }
}
