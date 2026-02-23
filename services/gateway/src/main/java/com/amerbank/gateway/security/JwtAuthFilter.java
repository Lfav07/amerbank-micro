package com.amerbank.gateway.security;

import com.amerbank.gateway.config.GatewayConfigProperties;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;


@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private final JwtService jwtService;
    private final GatewayConfigProperties gatewayConfigProperties;
    private final AntPathMatcher matcher = new AntPathMatcher();
    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    public JwtAuthFilter(JwtService jwtService, GatewayConfigProperties gatewayConfigProperties) {
        this.jwtService = jwtService;
        this.gatewayConfigProperties = gatewayConfigProperties;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        String path = exchange.getRequest().getURI().getPath();

        if (isPublic(path)) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest()
                .getHeaders()
                .getFirst(HttpHeaders.AUTHORIZATION);




        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Unauthorized request to {}", path);
            return unauthorized(exchange, "Missing or invalid Authorization header");
        }


        String token = authHeader.substring(7);
        Long userId;
        List<String> roles;
        Long customerId;
        try {
            if (!jwtService.isTokenValid(token)) {
                log.warn("Unauthorized request to {} - invalid token", path);
                return unauthorized(exchange, "Invalid or expired token");
            }

             userId = jwtService.extractUserId(token, Long.class);
            roles = jwtService.extractRoles(token);
             customerId = jwtService.extractCustomerId(token);
            if (userId == null || roles == null || roles.isEmpty()) {
                return unauthorized(exchange, "Invalid token claims");
            }
        } catch (Exception e) {
            log.error("JWT processing error for path {}", path, e);
            return unauthorized(exchange, "Invalid token");
        }




        ServerHttpRequest mutatedRequest = exchange.getRequest()
                .mutate()
                .headers(headers -> {
                    headers.remove("X-User-Id");
                    headers.remove("X-Roles");
                    headers.remove("X-Customer-Id");
                    headers.add("X-Roles", String.join(",", roles));
                    headers.add("X-User-Id", String.valueOf(userId));
                    headers.add("X-Customer-Id", String.valueOf(customerId));
                })
                .build();

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    private boolean isPublic(String path) {
        return gatewayConfigProperties.getPublicPaths()
                .stream()
                .anyMatch(pattern -> matcher.match(pattern, path));
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);


        String body = "{\"error\":\"unauthorized\",\"message\":\"" + message + "\"}";

        DataBuffer buffer = exchange.getResponse()
                .bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));

        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -1;
    }
}

