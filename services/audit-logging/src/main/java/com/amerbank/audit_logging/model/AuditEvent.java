package com.amerbank.audit_logging.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.sql.SQLType;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "audit_log")
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class AuditEvent {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID eventId;

    @Column(nullable = false, updatable = false)
    private String eventType;

    @Column(nullable = false, updatable = false)
    private Instant timestamp;

    @Column(nullable = false, updatable = false)
    private String service;

    // Who triggered it (user/system)
    @Column(updatable = false)
    private String actorId;

    // What was affected (account, transfer, etc.)
    @Column(updatable = false)
    private String entityId;
    @Column(updatable = false)
    private String entityType;

    @Column(nullable = false, updatable = false)
    private String status;

    // Trace across services
    @Column(updatable = false)
    private String correlationId;

    // Flexible data
    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> payload;

}