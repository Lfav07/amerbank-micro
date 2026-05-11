package com.amerbank.audit_logging.service;

import com.amerbank.audit_logging.dto.AuditEventMessage;
import com.amerbank.audit_logging.dto.AuditEventSummaryResponse;
import com.amerbank.audit_logging.dto.AuditFilterRequest;
import com.amerbank.audit_logging.exception.EventNotFoundException;
import com.amerbank.audit_logging.model.AuditEvent;
import com.amerbank.audit_logging.repository.AuditEventJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditEventJpaRepository repository;

    private final AuditMapper mapper = new AuditMapper();

    private AuditService auditService;

    @BeforeEach
    void setUp() {
        auditService = new AuditService(mapper, repository);
    }

    @Test
    @DisplayName("Should save consumed audit message")
    void shouldSaveConsumedAuditMessage() {
        AuditEventMessage message = buildMessage();
        auditService.consume(message);

        ArgumentCaptor<AuditEvent> eventCaptor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(repository).save(eventCaptor.capture());
        AuditEvent saved = eventCaptor.getValue();

        assertEquals(message.eventId(), saved.getEventId());
        assertEquals(message.eventType(), saved.getEventType());
        assertEquals(message.timestamp(), saved.getTimestamp());
        assertEquals(message.service(), saved.getService());
        assertEquals(message.actorId(), saved.getActorId());
        assertEquals(message.entityId(), saved.getEntityId());
        assertEquals(message.payload(), saved.getPayload());
    }

    @Test
    @DisplayName("Should ignore duplicate consumed audit message")
    void shouldIgnoreDuplicateConsumedAuditMessage() {
        AuditEventMessage message = buildMessage();
        when(repository.save(any(AuditEvent.class))).thenThrow(new DataIntegrityViolationException("duplicate key"));

        assertDoesNotThrow(() -> auditService.consume(message));
    }

    @Test
    @DisplayName("Should throw when audit event is missing")
    void shouldThrowWhenAuditEventIsMissing() {
        UUID eventId = UUID.randomUUID();
        when(repository.findById(eventId)).thenReturn(Optional.empty());

        assertThrows(EventNotFoundException.class, () -> auditService.findById(eventId));
    }

    @Test
    @DisplayName("Should search summaries ordered by timestamp descending")
    void shouldSearchSummariesOrderedByTimestampDescending() {
        AuditFilterRequest filter = new AuditFilterRequest(
                "ACCOUNT_CREATED",
                "account-service",
                "user-1",
                null,
                null,
                "SUCCESS",
                Instant.parse("2026-05-09T00:00:00Z"),
                Instant.parse("2026-05-10T00:00:00Z")
        );
        AuditEvent first = buildEvent(UUID.randomUUID(), Instant.parse("2026-05-09T10:00:00Z"));
        AuditEvent second = buildEvent(UUID.randomUUID(), Instant.parse("2026-05-09T11:00:00Z"));
        when(repository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(first, second)));

        Page<AuditEventSummaryResponse> page = auditService.searchSummary(filter, PageRequest.of(1, 5));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findAll(any(Specification.class), pageableCaptor.capture());
        Pageable usedPageable = pageableCaptor.getValue();

        assertEquals(1, usedPageable.getPageNumber());
        assertEquals(5, usedPageable.getPageSize());
        assertEquals("timestamp: DESC", usedPageable.getSort().toString());
        assertEquals(2, page.getContent().size());
        assertEquals(first.getEventId(), page.getContent().getFirst().eventId());
    }

    private AuditEventMessage buildMessage() {
        return new AuditEventMessage(
                UUID.randomUUID(),
                "ACCOUNT_CREATED",
                Instant.parse("2026-05-09T18:00:00Z"),
                "account-service",
                "user-1",
                "acc-10",
                "ACCOUNT",
                "SUCCESS",
                "corr-123",
                Map.of("balance", 100)
        );
    }

    private AuditEvent buildEvent(UUID eventId, Instant timestamp) {
        return AuditEvent.builder()
                .eventId(eventId)
                .eventType("ACCOUNT_CREATED")
                .timestamp(timestamp)
                .service("account-service")
                .actorId("user-1")
                .entityId("acc-10")
                .entityType("ACCOUNT")
                .status("SUCCESS")
                .correlationId("corr-123")
                .payload(Map.of("balance", 100))
                .build();
    }
}
