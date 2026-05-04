package com.amerbank.audit_logging.model;

import com.amerbank.audit_logging.util.JsonConverter;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID eventId;

    @Column(nullable = false)
    private String eventType;

    @CreationTimestamp
    @Column(nullable = false)
    private Instant timestamp;

    @Column(nullable = false)
    private String service;

    // Who triggered it (user/system)
    private String actorId;

    // What was affected (account, transfer, etc.)
    private String entityId;
    private String entityType;

    @Column(nullable = false)
    private String status;

    // Trace across services
    private String correlationId;

    // Flexible data
    @Column(columnDefinition = "jsonb")
    @Convert(converter = JsonConverter.class)
    private Map<String, Object> payload;

}