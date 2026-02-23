package com.amerbank.auth_server.security;

import com.amerbank.auth_server.config.JwtProperties;
import com.amerbank.auth_server.model.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class JwtService {

    private  final JwtProperties jwtProperties;



    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes());
    }

    public String extractSubject(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Long extractUserId(String token) {
        String subject = extractClaim(token, Claims::getSubject);
        return Long.parseLong(subject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> resolver) {
        return resolver.apply(extractAllClaims(token));
    }


    public boolean validateCustomerServiceToken(String token) {
        try {
            Claims claims = extractAllClaims(token);

            // Check expiration
            if (claims.getExpiration().before(new Date())) return false;

            // Check audience
            Set<String> audience = claims.getAudience();
            if (audience == null || !audience.contains("auth-server")) return false;
            // Check issuer
            return "customer-service".equals(claims.getIssuer());
        } catch (JwtException e) {
            return false;
        }
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final Long userId = extractUserId(token);
        return userId.toString().equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    public boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    public String generateToken(User user, Long customerId) {
        return Jwts.builder()
                .issuer("auth-server")
                .subject(user.getId().toString())
                .claim("customerId", customerId)
                .claim("roles", user.getRoles().stream().map(Enum::name).toList())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtProperties.getExpirationMs()))
                .signWith(getSigningKey())
                .compact();
    }

    public String generateServiceToken() {
        return Jwts.builder()
                .issuer("auth-server")
                .subject("auth-server")
                .audience().add("customer-service").and()
                .claim("serviceName", "auth-server")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtProperties.getServiceTokenExpirationMs()))
                .signWith(getSigningKey())
                .id(UUID.randomUUID().toString())
                .compact();
    }


    public String generateAdminToken(User user) {
        return Jwts.builder()
                .issuer("auth-server")
                .subject(user.getId().toString())
                .claim("roles", user.getRoles().stream().map(Enum::name).toList())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtProperties.getExpirationMs()))
                .signWith(getSigningKey())
                .compact();
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
