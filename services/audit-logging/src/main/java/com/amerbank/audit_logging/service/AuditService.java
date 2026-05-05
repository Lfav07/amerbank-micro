package com.amerbank.audit_logging.service;

import com.amerbank.audit_logging.dto.AuditEventMessage;
import com.amerbank.audit_logging.dto.AuditEventResponse;
import com.amerbank.audit_logging.dto.AuditEventSummaryResponse;
import com.amerbank.audit_logging.dto.AuditFilterRequest;
import com.amerbank.audit_logging.exception.EventNotFoundException;
import com.amerbank.audit_logging.model.AuditEvent;
import com.amerbank.audit_logging.repository.AuditEventJpaRepository;
import com.amerbank.audit_logging.repository.AuditEventSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {
    private final AuditMapper mapper;
    private final AuditEventJpaRepository repository;

    @KafkaListener(
            topics = {
                    "audit.transactions",
                    "audit.accounts",
                    "audit.auth",
                    "audit.customers"
            },
            groupId = "audit-service"
    )
    public void consume(AuditEventMessage message) {
        process(message);
    }


    private void process(AuditEventMessage message) {
        try {
            AuditEvent domain = mapper.toDomain(message);
            repository.save(domain);
        } catch (DataIntegrityViolationException ex) {
            log.debug("Duplicate audit event ignored: {}", message.eventId());
        }
    }

    @KafkaListener(
            topics = {
                    "audit.transactions.dlq",
                    "audit.accounts.dlq",
                    "audit.auth.dlq",
                    "audit.customers.dlq"
            },
            groupId = "audit-dlt-consumer"
    )
    public void handleDlt(
            ConsumerRecord<String, String> record,
            @Header(KafkaHeaders.DLT_EXCEPTION_MESSAGE) String exceptionMessage,
            @Header(KafkaHeaders.DLT_ORIGINAL_TOPIC) String originalTopic) {

        log.error("DLT message from topic={} exception={} payload={}",
                originalTopic, exceptionMessage, record.value());
    }

    public AuditEventResponse findById(UUID id){
        AuditEvent event =  repository.findById(id).orElseThrow(() -> new EventNotFoundException("Event not found for id " + id));
        return mapper.toResponse(event);
    }

    public Page<AuditEventResponse> search(
            AuditFilterRequest filter,
            Pageable pageable) {

        Pageable sorted = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "timestamp")
        );

        Page<AuditEvent> page = repository.findAll(
                AuditEventSpecification.withFilters(filter),
                sorted
        );

        return page.map(mapper::toResponse);
    }
    public Page<AuditEventSummaryResponse> findSummary(AuditFilterRequest filter, Pageable pageable){
        Pageable sorted = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "timestamp")
        );
        Page<AuditEvent> page = repository.findAll(
                AuditEventSpecification.withFilters(
                        filter),
                sorted
        );
        return page.map(mapper::toSummaryResponse);
    }
}
