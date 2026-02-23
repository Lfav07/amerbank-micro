package com.amerbank.customer.customer.security;


import com.amerbank.customer.customer.config.JwtProperties;
import com.amerbank.customer.customer.dto.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class JwtService {
    private  final JwtProperties jwtProperties;

    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes());
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractSubject(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> resolver) {
        return resolver.apply(extractAllClaims(token));
    }


    public boolean validateAuthServiceToken(String token) {
        try {
            Claims claims = extractAllClaims(token);

            // Check expiration
            if (claims.getExpiration().before(new Date())) return false;

            // Check audience
            Set<String> audience = claims.getAudience();
            if (audience == null || !audience.contains("customer-service")) return false;
            // Check issuer
            return "auth-server".equals(claims.getIssuer());
        } catch (JwtException e) {
            return false;
        }
    }

    public Long extractUserId(String token) {
        return Long.parseLong(extractSubject(token));
    }

    public  Long extractCustomerId(String token) {
        return  extractClaim(token, claims -> claims.get("customerId", Long.class));
    }

    public String generateServiceToken(){
        return Jwts.builder()
                .issuer("customer-service")
                .subject("customer-service")
                .audience().add("auth-server").and()
                .claim("serviceName", "customer-service")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtProperties.getServiceTokenExpirationMs()))
                .signWith(getSigningKey())
                .id(UUID.randomUUID().toString())
                .compact();
    }



    public boolean isTokenValid(String token) {
        try {
            extractAllClaims(token);
            return !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }



    public Set<Role> extractRoles(String token) {
        Claims claims = extractAllClaims(token);
        Object roles = claims.get("roles");
        if (roles instanceof List<?>) {
            return ((List<?>) roles).stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .map(roleStr -> {
                        try {
                            return Role.valueOf(roleStr);
                        } catch (IllegalArgumentException e) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
        }
        return Set.of();
    }

    public boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }



    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

}
