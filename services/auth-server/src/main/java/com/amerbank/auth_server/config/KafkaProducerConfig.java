package com.amerbank.auth_server.config;

import com.amerbank.auth_server.dto.audit.AuditEventMessage;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

@Configuration
public class KafkaProducerConfig {
    @Bean
    public ProducerFactory<String, AuditEventMessage> producerFactory(KafkaProperties kafkaProperties) {
        return new DefaultKafkaProducerFactory<>(kafkaProperties.buildProducerProperties(null));
    }

    @Bean
    public KafkaTemplate<String, AuditEventMessage> kafkaTemplate(ProducerFactory<String, AuditEventMessage> factory) {
        return new KafkaTemplate<>(factory);
    }
}
