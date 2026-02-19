package com.amerbank.gateway.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@ConfigurationProperties(prefix = "gateway")
@Component
@Getter
@Setter
public class GatewayConfigProperties {
   private List<String> publicPaths;

}
