package com.amerbank.auth_server.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kafka")
@Getter
@Setter
public class KafkaProperties {

    private String bootstrapServers;

}
