package com.amerbank.customer.customer.security;

import com.amerbank.customer.customer.config.JwtProperties;
import com.amerbank.customer.customer.dto.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.crypto.SecretKey;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    private JwtProperties props;
    private JwtService jwtService;
    private static final String TEST_SECRET = "f3zxdc27v0613fbm9784zfe25f981f43";
    private static final Long TEST_CUSTOMER_ID = 123L;
    private static final Long TEST_USER_ID = 1L;

    @BeforeEach
    void setUp() {
        props = new JwtProperties();
        props.setSecret(TEST_SECRET);
        props.setServiceTokenExpirationMs(120_000L);
        jwtService = new JwtService(props);
    }

    // -------------------------------------------------------------------------
    // Service Token Generation
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Service Token Generation")
    class ServiceTokenGenerationTests {

        @Test
        @DisplayName("Should successfully generate service JWT token")
        void shouldGenerateServiceToken() {
            String token = jwtService.generateServiceToken();

            assertNotNull(token);
            assertFalse(token.isEmpty());
        }

        @Test
        @DisplayName("Should generate service token with correct subject")
        void shouldGenerateServiceTokenWithCorrectSubject() {
            String token = jwtService.generateServiceToken();

            String subject = jwtService.extractSubject(token);

            assertEquals("customer-service", subject);
        }

        @Test
        @DisplayName("Should generate service token with correct issuer")
        void shouldGenerateServiceTokenWithCorrectIssuer() {
            String token = jwtService.generateServiceToken();

            String issuer = jwtService.extractClaim(token, Claims::getIssuer);

            assertEquals("customer-service", issuer);
        }

        @Test
        @DisplayName("Should generate service token with correct audience")
        void shouldGenerateServiceTokenWithCorrectAudience() {
            String token = jwtService.generateServiceToken();

            Set<String> audience = jwtService.extractClaim(token, Claims::getAudience);

            assertNotNull(audience);
            assertTrue(audience.contains("auth-server"));
        }

        @Test
        @DisplayName("Should generate service token with serviceName claim")
        void shouldGenerateServiceTokenWithServiceNameClaim() {
            String token = jwtService.generateServiceToken();

            String serviceName = jwtService.extractClaim(token, claims ->
                    claims.get("serviceName", String.class));

            assertEquals("customer-service", serviceName);
        }

        @Test
        @DisplayName("Should generate service token with JWT ID")
        void shouldGenerateServiceTokenWithJwtId() {
            String token = jwtService.generateServiceToken();

            String jti = jwtService.extractClaim(token, Claims::getId);

            assertNotNull(jti);
            assertFalse(jti.isEmpty());
        }

        @Test
        @DisplayName("Should generate service token with future expiration date")
        void shouldGenerateServiceTokenWithFutureExpiration() {
            Date beforeGeneration = new Date();
            String token = jwtService.generateServiceToken();
            Date afterGeneration = new Date();

            Date expiration = jwtService.extractClaim(token, Claims::getExpiration);

            assertTrue(expiration.after(beforeGeneration));
            assertTrue(expiration.after(afterGeneration));
        }

        @Test
        @DisplayName("Should generate service token with correct issuedAt date")
        void shouldGenerateServiceTokenWithCorrectIssuedAt() {
            Date date = new Date();

            Date beforeGeneration = Date.from(
                    date.toInstant().minus(2, ChronoUnit.MINUTES)
            );

            String token = jwtService.generateServiceToken();
            Date afterGeneration = Date.from(
                    date.toInstant().plus(2, ChronoUnit.MINUTES)
            );

            Date issuedAt = jwtService.extractClaim(token, Claims::getIssuedAt);

            assertTrue(issuedAt.after(beforeGeneration));
            assertTrue(issuedAt.before(afterGeneration));
        }
    }

    // -------------------------------------------------------------------------
    // Auth Service Token Validation
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Auth Service Token Validation")
    class AuthServiceTokenValidationTests {

        @Test
        @DisplayName("Should return true for valid auth service token")
        void shouldReturnTrueForValidAuthServiceToken() {
            String token = createValidAuthServiceToken();

            assertTrue(jwtService.validateAuthServiceToken(token));
        }

        @Test
        @DisplayName("Should return false for token with wrong issuer")
        void shouldReturnFalseForWrongIssuer() {
            String token = createAuthServiceTokenWithWrongIssuer();

            assertFalse(jwtService.validateAuthServiceToken(token));
        }

        @Test
        @DisplayName("Should return false for token with wrong audience")
        void shouldReturnFalseForWrongAudience() {
            String token = createAuthServiceTokenWithWrongAudience();

            assertFalse(jwtService.validateAuthServiceToken(token));
        }

        @Test
        @DisplayName("Should return false for expired auth service token")
        void shouldReturnFalseForExpiredAuthServiceToken() {
            String token = createExpiredAuthServiceToken();

            assertFalse(jwtService.validateAuthServiceToken(token));
        }

        @Test
        @DisplayName("Should return false for malformed token")
        void shouldReturnFalseForMalformedToken() {
            assertFalse(jwtService.validateAuthServiceToken("malformed.token.here"));
        }

        @Test
        @DisplayName("Should return false for completely invalid token")
        void shouldReturnFalseForInvalidToken() {
            assertFalse(jwtService.validateAuthServiceToken("invalid"));
        }

        private String createValidAuthServiceToken() {
            SecretKey key = Keys.hmacShaKeyFor(TEST_SECRET.getBytes());
            return Jwts.builder()
                    .issuer("auth-server")
                    .subject("test@email.com")
                    .audience().add("customer-service").and()
                    .claim("userId", TEST_USER_ID)
                    .claim("customerId", TEST_CUSTOMER_ID)
                    .claim("roles", List.of("ROLE_USER"))
                    .issuedAt(new Date())
                    .expiration(new Date(System.currentTimeMillis() + 120_000))
                    .signWith(key)
                    .compact();
        }

        private String createAuthServiceTokenWithWrongIssuer() {
            SecretKey key = Keys.hmacShaKeyFor(TEST_SECRET.getBytes());
            return Jwts.builder()
                    .issuer("wrong-service")
                    .subject("test@email.com")
                    .audience().add("customer-service").and()
                    .claim("userId", TEST_USER_ID)
                    .issuedAt(new Date())
                    .expiration(new Date(System.currentTimeMillis() + 120_000))
                    .signWith(key)
                    .compact();
        }

        private String createAuthServiceTokenWithWrongAudience() {
            SecretKey key = Keys.hmacShaKeyFor(TEST_SECRET.getBytes());
            return Jwts.builder()
                    .issuer("auth-server")
                    .subject("test@email.com")
                    .audience().add("wrong-service").and()
                    .claim("userId", TEST_USER_ID)
                    .issuedAt(new Date())
                    .expiration(new Date(System.currentTimeMillis() + 120_000))
                    .signWith(key)
                    .compact();
        }

        private String createExpiredAuthServiceToken() {
            SecretKey key = Keys.hmacShaKeyFor(TEST_SECRET.getBytes());
            return Jwts.builder()
                    .issuer("auth-server")
                    .subject("test@email.com")
                    .audience().add("customer-service").and()
                    .claim("userId", TEST_USER_ID)
                    .issuedAt(new Date(System.currentTimeMillis() - 300_000))
                    .expiration(new Date(System.currentTimeMillis() - 60_000))
                    .signWith(key)
                    .compact();
        }
    }

    // -------------------------------------------------------------------------
    // General Token Validation
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("General Token Validation")
    class GeneralTokenValidationTests {

        @Test
        @DisplayName("Should return true for valid service token")
        void shouldReturnTrueForValidServiceToken() {
            String token = jwtService.generateServiceToken();

            assertTrue(jwtService.isTokenValid(token));
        }

        @Test
        @DisplayName("Should return false for invalid token")
        void shouldReturnFalseForInvalidToken() {
            assertFalse(jwtService.isTokenValid("invalid.token"));
        }

        @Test
        @DisplayName("Should return false for malformed token")
        void shouldReturnFalseForMalformedToken() {
            assertFalse(jwtService.isTokenValid("malformed.token.here"));
        }

        @Test
        @DisplayName("Should correctly identify non-expired token")
        void shouldCorrectlyIdentifyNonExpiredToken() {
            String token = jwtService.generateServiceToken();

            assertFalse(jwtService.isTokenExpired(token));
        }

        @Test
        @DisplayName("Should correctly identify expired token")
        void shouldCorrectlyIdentifyExpiredToken() {
            String expiredToken = createExpiredToken();

            assertThrows(ExpiredJwtException.class, () -> jwtService.isTokenExpired(expiredToken));
        }

        private String createExpiredToken() {
            SecretKey key = Keys.hmacShaKeyFor(TEST_SECRET.getBytes());
            return Jwts.builder()
                    .issuer("customer-service")
                    .subject("customer-service")
                    .issuedAt(new Date(System.currentTimeMillis() - 7200000))
                    .expiration(new Date(System.currentTimeMillis() - 3600000))
                    .signWith(key)
                    .compact();
        }
    }

    // -------------------------------------------------------------------------
    // Token Extraction
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Token Extraction")
    class TokenExtractionTests {

        @Test
        @DisplayName("Should extract username from token")
        void shouldExtractUsernameFromToken() {
            String token = createTokenWithSubject("test@email.com");

            String username = jwtService.extractUsername(token);

            assertEquals("test@email.com", username);
        }

        @Test
        @DisplayName("Should extract subject from token")
        void shouldExtractSubjectFromToken() {
            String token = createTokenWithSubject("test@email.com");

            String subject = jwtService.extractSubject(token);

            assertEquals("test@email.com", subject);
        }

        @Test
        @DisplayName("Should extract userId from token")
        void shouldExtractUserIdFromToken() {
            String token = createTokenWithUserId(TEST_USER_ID);

            Long userId = jwtService.extractUserId(token);

            assertEquals(TEST_USER_ID, userId);
        }

        @Test
        @DisplayName("Should extract customerId from token")
        void shouldExtractCustomerIdFromToken() {
            String token = createTokenWithCustomerId(TEST_CUSTOMER_ID);

            Long customerId = jwtService.extractCustomerId(token);

            assertEquals(TEST_CUSTOMER_ID, customerId);
        }

        @Test
        @DisplayName("Should extract roles from token")
        void shouldExtractRolesFromToken() {
            String token = createTokenWithRoles(List.of("ROLE_USER", "ROLE_ADMIN"));

            Set<Role> roles = jwtService.extractRoles(token);

            assertEquals(2, roles.size());
            assertTrue(roles.contains(Role.ROLE_USER));
            assertTrue(roles.contains(Role.ROLE_ADMIN));
        }

        @Test
        @DisplayName("Should throw exception when extracting claims from invalid token")
        void shouldThrowExceptionForInvalidToken() {
            assertThrows(Exception.class, () ->
                    jwtService.extractClaim("invalid.token", Claims::getSubject));
        }

        private String createTokenWithSubject(String subject) {
            SecretKey key = Keys.hmacShaKeyFor(TEST_SECRET.getBytes());
            return Jwts.builder()
                    .issuer("customer-service")
                    .subject(subject)
                    .issuedAt(new Date())
                    .expiration(new Date(System.currentTimeMillis() + 120_000))
                    .signWith(key)
                    .compact();
        }

        private String createTokenWithUserId(Long userId) {
            SecretKey key = Keys.hmacShaKeyFor(TEST_SECRET.getBytes());
            return Jwts.builder()
                    .issuer("auth-server")
                    .subject("test@email.com")
                    .audience().add("customer-service").and()
                    .claim("userId", userId)
                    .issuedAt(new Date())
                    .expiration(new Date(System.currentTimeMillis() + 120_000))
                    .signWith(key)
                    .compact();
        }

        private String createTokenWithCustomerId(Long customerId) {
            SecretKey key = Keys.hmacShaKeyFor(TEST_SECRET.getBytes());
            return Jwts.builder()
                    .issuer("auth-server")
                    .subject("test@email.com")
                    .audience().add("customer-service").and()
                    .claim("customerId", customerId)
                    .issuedAt(new Date())
                    .expiration(new Date(System.currentTimeMillis() + 120_000))
                    .signWith(key)
                    .compact();
        }

        private String createTokenWithRoles(List<String> roles) {
            SecretKey key = Keys.hmacShaKeyFor(TEST_SECRET.getBytes());
            return Jwts.builder()
                    .issuer("auth-server")
                    .subject("test@email.com")
                    .audience().add("customer-service").and()
                    .claim("userId", TEST_USER_ID)
                    .claim("roles", roles)
                    .issuedAt(new Date())
                    .expiration(new Date(System.currentTimeMillis() + 120_000))
                    .signWith(key)
                    .compact();
        }
    }

    // -------------------------------------------------------------------------
    // Edge Cases
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle token with empty roles")
        void shouldHandleTokenWithEmptyRoles() {
            String token = createTokenWithRoles(List.of());

            Set<Role> roles = jwtService.extractRoles(token);

            assertNotNull(roles);
            assertTrue(roles.isEmpty());
        }

        @Test
        @DisplayName("Should handle token with invalid roles gracefully")
        void shouldHandleTokenWithInvalidRoles() {
            String token = createTokenWithInvalidRoles();

            Set<Role> roles = jwtService.extractRoles(token);

            assertNotNull(roles);
            assertTrue(roles.isEmpty());
        }

        @Test
        @DisplayName("Should handle different secret lengths")
        void shouldHandleDifferentSecretLengths() {
            JwtProperties customProps = new JwtProperties();
            customProps.setSecret("averylongersecretkeythatshouldbe256bitsormore");
            customProps.setServiceTokenExpirationMs(120_000L);
            JwtService customJwtService = new JwtService(customProps);

            String token = customJwtService.generateServiceToken();

            assertNotNull(token);
            assertEquals("customer-service", customJwtService.extractSubject(token));
        }

        private String createTokenWithRoles(List<String> roles) {
            SecretKey key = Keys.hmacShaKeyFor(TEST_SECRET.getBytes());
            return Jwts.builder()
                    .issuer("auth-server")
                    .subject("test@email.com")
                    .audience().add("customer-service").and()
                    .claim("userId", TEST_USER_ID)
                    .claim("roles", roles)
                    .issuedAt(new Date())
                    .expiration(new Date(System.currentTimeMillis() + 120_000))
                    .signWith(key)
                    .compact();
        }

        private String createTokenWithInvalidRoles() {
            SecretKey key = Keys.hmacShaKeyFor(TEST_SECRET.getBytes());
            return Jwts.builder()
                    .issuer("auth-server")
                    .subject("test@email.com")
                    .audience().add("customer-service").and()
                    .claim("userId", TEST_USER_ID)
                    .claim("roles", "NOT_A_LIST")
                    .issuedAt(new Date())
                    .expiration(new Date(System.currentTimeMillis() + 120_000))
                    .signWith(key)
                    .compact();
        }
    }
}
