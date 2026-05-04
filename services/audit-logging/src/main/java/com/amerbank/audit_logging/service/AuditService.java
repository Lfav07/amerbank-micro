package com.amerbank.audit_logging.service;

import com.amerbank.audit_logging.dto.AuditEventMessage;
import com.amerbank.audit_logging.model.AuditEvent;
import com.amerbank.audit_logging.repository.AuditEventJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

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

}
