package com.amerbank.auth_server.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.ActiveProfiles;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

@TestConfiguration
@ActiveProfiles("test")
public class TestJwtFactory {

    @Value("${jwt.secret}")
    private String secret;


    @Value("${jwt.service-token-expiration-ms:120000}")
    private long serviceTokenExpirationMs;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateServiceToken(){
        return Jwts.builder()
                .issuer("customer-service")
                .subject("customer-service")
                .audience().add("auth-server").and()
                .claim("serviceName", "customer-service")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + serviceTokenExpirationMs))
                .signWith(getSigningKey())
                .id(UUID.randomUUID().toString())
                .compact();
    }
}
