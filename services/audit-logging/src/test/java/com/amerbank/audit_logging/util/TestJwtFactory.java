package com.amerbank.audit_logging.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@TestConfiguration
public class TestJwtFactory {

    @Value("${jwt.secret}")
    private String secret;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateAdminToken(String subject) {
        return Jwts.builder()
                .subject(subject)
                .claim("roles", List.of("ROLE_ADMIN"))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 120_000))
                .signWith(getSigningKey())
                .id(UUID.randomUUID().toString())
                .compact();
    }

    public String generateUserToken(String subject) {
        return Jwts.builder()
                .subject(subject)
                .claim("roles", List.of("ROLE_USER"))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 120_000))
                .signWith(getSigningKey())
                .id(UUID.randomUUID().toString())
                .compact();
    }
}
