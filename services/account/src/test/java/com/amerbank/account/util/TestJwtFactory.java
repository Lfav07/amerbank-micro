package com.amerbank.account.util;

import com.amerbank.account.dto.Role;
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

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateServiceToken() {
        return Jwts.builder()
                .issuer("transaction-service")
                .subject("transaction-service")
                .audience().add("account-service").and()
                .claim("serviceName", "transaction-service")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + serviceTokenExpirationMs))
                .signWith(getSigningKey())
                .id(UUID.randomUUID().toString())
                .compact();
    }

    public String generateUserToken(Long customerId, String email) {
        return Jwts.builder()
                .subject(email)
                .claim("customerId", customerId)
                .claim("roles", List.of("ROLE_USER"))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + serviceTokenExpirationMs))
                .signWith(getSigningKey())
                .id(UUID.randomUUID().toString())
                .compact();
    }

    public String generateAdminToken(String email) {
        return Jwts.builder()
                .subject(email)
                .claim("roles", List.of("ROLE_ADMIN"))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + serviceTokenExpirationMs))
                .signWith(getSigningKey())
                .id(UUID.randomUUID().toString())
                .compact();
    }

    public String generateUserTokenWithRoles(Long customerId, String email, List<Role> roles) {
        List<String> roleStrings = roles.stream()
                .map(Role::name)
                .toList();
        return Jwts.builder()
                .subject(email)
                .claim("customerId", customerId)
                .claim("roles", roleStrings)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + serviceTokenExpirationMs))
                .signWith(getSigningKey())
                .id(UUID.randomUUID().toString())
                .compact();
    }
}
