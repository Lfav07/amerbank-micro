package com.amerbank.customer.customer.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.ActiveProfiles;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@TestConfiguration
@ActiveProfiles("test")
public class TestJwtFactory {

    @Value("${jwt.secret}")
    private String secret;


    @Value("${jwt.service-token-expiration-ms:120000}")
    private long serviceTokenExpirationMs;

    @Value("${jwt.expiration-ms:3600000}")
    private long tokenExpirationMs;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateServiceToken(){
        return Jwts.builder()
                .issuer("auth-server")
                .subject("auth-server")
                .audience().add("customer-service").and()
                .claim("serviceName", "auth-server")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() +serviceTokenExpirationMs))
                .signWith(getSigningKey())
                .id(UUID.randomUUID().toString())
                .compact();
    }

    public String generateUserToken(String email, Long customerId, List<String> roles) {
        return Jwts.builder()
                .issuer("auth-server")
                .subject(email)
                .claim("customerId", customerId)
                .claim("roles", roles)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + tokenExpirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    public String generateAdminToken(String email, Long customerId) {
        return generateUserToken(email, customerId, List.of("ROLE_ADMIN"));
    }

    public String generateCustomerUserToken(String email, Long customerId) {
        return generateUserToken(email, customerId, List.of("ROLE_USER"));
    }
}
