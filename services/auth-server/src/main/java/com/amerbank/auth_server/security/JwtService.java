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

/**
 * Service for generating, parsing, and validating JWT tokens.
 * Handles token generation for users, admins, and service-to-service communication.
 */
@Service
@RequiredArgsConstructor
public class JwtService {

    private  final JwtProperties jwtProperties;


    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes());
    }

    /**
     * Extracts the subject claim from a JWT token.
     *
     * @param token the JWT token to parse
     * @return the subject (user ID) from the token
     */
    public String extractSubject(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extracts the user ID from the subject claim of a JWT token.
     *
     * @param token the JWT token to parse
     * @return the user ID extracted from the token subject
     */
    public Long extractUserId(String token) {
        String subject = extractClaim(token, Claims::getSubject);
        return Long.parseLong(subject);
    }

    /**
     * Extracts a specific claim from a JWT token using a resolver function.
     *
     * @param token    the JWT token to parse
     * @param resolver function to extract the desired claim
     * @return the extracted claim value
     */
    public <T> T extractClaim(String token, Function<Claims, T> resolver) {
        return resolver.apply(extractAllClaims(token));
    }


    /**
     * Validates a service-to-service token from customer-service.
     * Checks expiration, audience, and issuer claims.
     *
     * @param token the service JWT token to validate
     * @return true if the token is valid for service-to-service auth; false otherwise
     */
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

    /**
     * Validates a JWT token against user details.
     * Checks that the token's user ID matches and the token is not expired.
     *
     * @param token       the JWT token to validate
     * @param userDetails the user details to validate against
     * @return true if the token is valid; false otherwise
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final Long userId = extractUserId(token);
        return userId.toString().equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    /**
     * Checks if a JWT token is expired.
     *
     * JWT token to check @param token the
     * @return true if the token is expired; false otherwise
     */
    public boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    /**
     * Generates a JWT token for a regular user with customer ID and roles.
     *
     * @param user       the user entity to generate token for
     * @param customerId the associated customer ID
     * @return the generated JWT token string
     */
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

    /**
     * Generates a service-to-service JWT token for communication with customer-service.
     *
     * @return the generated service token string
     */
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


    /**
     * Generates a JWT token for an admin user with roles.
     *
     * @param user the admin user entity to generate token for
     * @return the generated JWT token string
     */
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
