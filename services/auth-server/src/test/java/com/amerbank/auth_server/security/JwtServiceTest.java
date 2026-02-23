package com.amerbank.auth_server.security;

import com.amerbank.auth_server.config.JwtProperties;
import com.amerbank.auth_server.dto.Role;
import com.amerbank.auth_server.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;

import javax.crypto.SecretKey;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtProperties props;
    private JwtService jwtService;
    private User testUser;
    private static final String TEST_SECRET = "f3zxdc27v0613fbm9784zfe25f981f43";
    private static final Long TEST_CUSTOMER_ID = 123L;
    private static final Long TEST_USER_ID = 1L;
    private static final String TEST_EMAIL = "email@test.com";

    @BeforeEach
    void setUp() {
        props = new JwtProperties();
        props.setSecret(TEST_SECRET);
        props.setExpirationMs(3_600_000L);
        props.setServiceTokenExpirationMs(120_000L);
        jwtService = new JwtService(props);

        testUser = User.builder()
                .id(TEST_USER_ID)
                .email(TEST_EMAIL)
                .password("myPassword")
                .active(true)
                .roles(Set.of(Role.ROLE_USER))
                .build();
    }

    @Nested
    @DisplayName("Token Generation")
    class TokenGenerationTests {

        @Test
        @DisplayName("Should successfully generate user JWT token")
        void shouldGenerateUserToken() {
            String token = jwtService.generateToken(testUser, TEST_CUSTOMER_ID);

            assertNotNull(token);
            assertFalse(token.isEmpty());
        }

        @Test
        @DisplayName("Should generate token with correct subject")
        void shouldGenerateTokenWithCorrectSubject() {
            String token = jwtService.generateToken(testUser, TEST_CUSTOMER_ID);

            String extractedSubject = jwtService.extractSubject(token);

            assertEquals(TEST_EMAIL, extractedSubject);
        }

        @Test
        @DisplayName("Should generate token with correct userId claim")
        void shouldGenerateTokenWithCorrectUserId() {
            String token = jwtService.generateToken(testUser, TEST_CUSTOMER_ID);

            Long extractedUserId = jwtService.extractUserId(token);

            assertEquals(TEST_USER_ID, extractedUserId);
        }

        @Test
        @DisplayName("Should generate token with correct customerId claim")
        void shouldGenerateTokenWithCorrectCustomerId() {
            String token = jwtService.generateToken(testUser, TEST_CUSTOMER_ID);

            Integer extractedCustomerId = jwtService.extractClaim(token, claims ->
                    claims.get("customerId", Integer.class));

            assertEquals(TEST_CUSTOMER_ID.intValue(), extractedCustomerId);
        }

        @Test
        @DisplayName("Should generate token with correct roles claim")
        void shouldGenerateTokenWithCorrectRoles() {
            String token = jwtService.generateToken(testUser, TEST_CUSTOMER_ID);

            List<String> extractedRoles = jwtService.extractClaim(token, claims ->
                    claims.get("roles", List.class));

            assertNotNull(extractedRoles);
            assertTrue(extractedRoles.contains("ROLE_USER"));
        }

        @Test
        @DisplayName("Should generate token with correct issuer")
        void shouldGenerateTokenWithCorrectIssuer() {
            String token = jwtService.generateToken(testUser, TEST_CUSTOMER_ID);

            String issuer = jwtService.extractClaim(token, Claims::getIssuer);

            assertEquals("auth-server", issuer);
        }

        @Test
        @DisplayName("Should generate token with future expiration date")
        void shouldGenerateTokenWithFutureExpiration() {
            Date beforeGeneration = new Date();
            String token = jwtService.generateToken(testUser, TEST_CUSTOMER_ID);
            Date afterGeneration = new Date();

            Date expiration = jwtService.extractClaim(token, Claims::getExpiration);

            assertTrue(expiration.after(beforeGeneration));
            assertTrue(expiration.after(afterGeneration));
        }

        @Test
        @DisplayName("Should generate token with correct issuedAt date")
        void shouldGenerateTokenWithCorrectIssuedAt() {
            Date date = new Date();

            Date beforeGeneration = Date.from(
                    date.toInstant().minus(2, ChronoUnit.MINUTES)
            );

            String token = jwtService.generateToken(testUser, TEST_CUSTOMER_ID);
            Date afterGeneration = Date.from(
                    date.toInstant().plus(2, ChronoUnit.MINUTES)
            );

            Date issuedAt = jwtService.extractClaim(token, Claims::getIssuedAt);

            assertTrue(issuedAt.after(beforeGeneration));
            assertTrue(issuedAt.before(afterGeneration));
        }
    }

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

            assertEquals("auth-server", subject);
        }

        @Test
        @DisplayName("Should generate service token with correct serviceName claim")
        void shouldGenerateServiceTokenWithCorrectServiceName() {
            String token = jwtService.generateServiceToken();

            String serviceName = jwtService.extractClaim(token, claims ->
                    claims.get("serviceName", String.class));

            assertEquals("auth-server", serviceName);
        }

        @Test
        @DisplayName("Should generate service token with correct issuer")
        void shouldGenerateServiceTokenWithCorrectIssuer() {
            String token = jwtService.generateServiceToken();

            String issuer = jwtService.extractClaim(token, Claims::getIssuer);

            assertEquals("auth-server", issuer);
        }

        @Test
        @DisplayName("Should generate service token with audience containing customer-service")
        void shouldGenerateServiceTokenWithCorrectAudience() {
            String token = jwtService.generateServiceToken();

            Set<String> audience = jwtService.extractClaim(token, Claims::getAudience);

            assertNotNull(audience);
            assertTrue(audience.contains("customer-service"));
        }

        @Test
        @DisplayName("Should generate service token with JWT ID")
        void shouldGenerateServiceTokenWithJwtId() {
            String token = jwtService.generateServiceToken();

            String jti = jwtService.extractClaim(token, Claims::getId);

            assertNotNull(jti);
            assertFalse(jti.isEmpty());
        }
    }

    @Nested
    @DisplayName("Admin Token Generation")
    class AdminTokenGenerationTests {

        @Test
        @DisplayName("Should successfully generate admin JWT token")
        void shouldGenerateAdminToken() {
            User adminUser = User.builder()
                    .id(2L)
                    .email("admin@test.com")
                    .password("adminPassword")
                    .active(true)
                    .roles(Set.of(Role.ROLE_ADMIN))
                    .build();

            String token = jwtService.generateAdminToken(adminUser);

            assertNotNull(token);
            assertFalse(token.isEmpty());
        }

        @Test
        @DisplayName("Should generate admin token with correct subject")
        void shouldGenerateAdminTokenWithCorrectSubject() {
            User adminUser = User.builder()
                    .id(2L)
                    .email("admin@test.com")
                    .password("adminPassword")
                    .active(true)
                    .roles(Set.of(Role.ROLE_ADMIN))
                    .build();

            String token = jwtService.generateAdminToken(adminUser);

            assertEquals("admin@test.com", jwtService.extractSubject(token));
        }

        @Test
        @DisplayName("Should generate admin token with correct userId")
        void shouldGenerateAdminTokenWithCorrectUserId() {
            User adminUser = User.builder()
                    .id(2L)
                    .email("admin@test.com")
                    .password("adminPassword")
                    .active(true)
                    .roles(Set.of(Role.ROLE_ADMIN))
                    .build();

            String token = jwtService.generateAdminToken(adminUser);

            assertEquals(2L, jwtService.extractUserId(token));
        }

        @Test
        @DisplayName("Should generate admin token with admin role")
        void shouldGenerateAdminTokenWithAdminRole() {
            User adminUser = User.builder()
                    .id(2L)
                    .email("admin@test.com")
                    .password("adminPassword")
                    .active(true)
                    .roles(Set.of(Role.ROLE_ADMIN))
                    .build();

            String token = jwtService.generateAdminToken(adminUser);

            List<String> roles = jwtService.extractClaim(token, claims ->
                    claims.get("roles", List.class));

            assertTrue(roles.contains("ROLE_ADMIN"));
        }
    }

    @Nested
    @DisplayName("Token Validation - User Token")
    class TokenValidationTests {

        @Test
        @DisplayName("Should return true when token is valid for user details")
        void shouldReturnTrueWhenTokenValidForUserDetails() {
            String token = jwtService.generateToken(testUser, TEST_CUSTOMER_ID);

            UserDetails userDetails = org.springframework.security.core.userdetails.User
                    .withUsername(TEST_EMAIL)
                    .password("dummy")
                    .authorities("ROLE_USER")
                    .build();

            assertTrue(jwtService.isTokenValid(token, userDetails));
        }

        @Test
        @DisplayName("Should return false when token username does not match user details")
        void shouldReturnFalseWhenUsernameDoesNotMatch() {
            String token = jwtService.generateToken(testUser, TEST_CUSTOMER_ID);

            UserDetails userDetails = org.springframework.security.core.userdetails.User
                    .withUsername("different@test.com")
                    .password("dummy")
                    .authorities("ROLE_USER")
                    .build();

            assertFalse(jwtService.isTokenValid(token, userDetails));
        }

        @Test
        @DisplayName("Should correctly identify non-expired token")
        void shouldCorrectlyIdentifyNonExpiredToken() {
            String token = jwtService.generateToken(testUser, TEST_CUSTOMER_ID);

            assertFalse(jwtService.isTokenExpired(token));
        }
    }

    @Nested
    @DisplayName("Token Validation - Service Token")
    class ServiceTokenValidationTests {


        @Test
        @DisplayName("Should return false for token with wrong issuer")
        void shouldReturnFalseForWrongIssuer() {
            String invalidToken = createTokenWithWrongIssuer();

            assertFalse(jwtService.validateCustomerServiceToken(invalidToken));
        }

        @Test
        @DisplayName("Should return false for token with wrong audience")
        void shouldReturnFalseForWrongAudience() {
            String invalidToken = createTokenWithWrongAudience();

            assertFalse(jwtService.validateCustomerServiceToken(invalidToken));
        }

        @Test
        @DisplayName("Should return false for expired service token")
        void shouldReturnFalseForExpiredServiceToken() {
            String expiredToken = createExpiredServiceToken();

            assertFalse(jwtService.validateCustomerServiceToken(expiredToken));
        }

        @Test
        @DisplayName("Should return false for malformed token")
        void shouldReturnFalseForMalformedToken() {
            assertFalse(jwtService.validateCustomerServiceToken("malformed.token.here"));
        }

        @Test
        @DisplayName("Should return false for completely invalid token")
        void shouldReturnFalseForInvalidToken() {
            assertFalse(jwtService.validateCustomerServiceToken("invalid"));
        }

        private String createTokenWithWrongIssuer() {
            SecretKey key = Keys.hmacShaKeyFor(TEST_SECRET.getBytes());
            return Jwts.builder()
                    .issuer("wrong-service")
                    .subject("auth-server")
                    .audience().add("customer-service").and()
                    .claim("serviceName", "auth-server")
                    .issuedAt(new Date())
                    .expiration(new Date(System.currentTimeMillis() + 120_000))
                    .signWith(key)
                    .compact();
        }

        private String createTokenWithWrongAudience() {
            SecretKey key = Keys.hmacShaKeyFor(TEST_SECRET.getBytes());
            return Jwts.builder()
                    .issuer("auth-server")
                    .subject("auth-server")
                    .audience().add("wrong-service").and()
                    .claim("serviceName", "auth-server")
                    .issuedAt(new Date())
                    .expiration(new Date(System.currentTimeMillis() + 120_000))
                    .signWith(key)
                    .compact();
        }

        private String createExpiredServiceToken() {
            SecretKey key = Keys.hmacShaKeyFor(TEST_SECRET.getBytes());
            return Jwts.builder()
                    .issuer("auth-server")
                    .subject("auth-server")
                    .audience().add("customer-service").and()
                    .claim("serviceName", "auth-server")
                    .issuedAt(new Date(System.currentTimeMillis() - 300_000))
                    .expiration(new Date(System.currentTimeMillis() - 60_000))
                    .signWith(key)
                    .compact();
        }
    }

    @Nested
    @DisplayName("Token Extraction")
    class TokenExtractionTests {

        @Test
        @DisplayName("Should extract username from token")
        void shouldExtractUsernameFromToken() {
            String token = jwtService.generateToken(testUser, TEST_CUSTOMER_ID);

            String username = jwtService.extractSubject(token);

            assertEquals(TEST_EMAIL, username);
        }

        @Test
        @DisplayName("Should extract subject from token")
        void shouldExtractSubjectFromToken() {
            String token = jwtService.generateToken(testUser, TEST_CUSTOMER_ID);

            String subject = jwtService.extractSubject(token);

            assertEquals(TEST_EMAIL, subject);
        }

        @Test
        @DisplayName("Should extract userId from token")
        void shouldExtractUserIdFromToken() {
            String token = jwtService.generateToken(testUser, TEST_CUSTOMER_ID);

            Long userId = jwtService.extractUserId(token);

            assertEquals(TEST_USER_ID, userId);
        }



        @Test
        @DisplayName("Should throw exception when extracting claims from invalid token")
        void shouldThrowExceptionForInvalidToken() {
            assertThrows(Exception.class, () ->
                    jwtService.extractClaim("invalid.token", Claims::getSubject));
        }
    }

    @Nested
    @DisplayName("Multi-Role User Token")
    class MultiRoleTokenTests {

        @Test
        @DisplayName("Should generate token with multiple roles")
        void shouldGenerateTokenWithMultipleRoles() {
            User multiRoleUser = User.builder()
                    .id(3L)
                    .email("multi@test.com")
                    .password("password")
                    .active(true)
                    .roles(Set.of(Role.ROLE_USER, Role.ROLE_ADMIN))
                    .build();

            String token = jwtService.generateToken(multiRoleUser, 456L);

            List<String> roles = jwtService.extractClaim(token, claims ->
                    claims.get("roles", List.class));

            assertTrue(roles.contains("ROLE_USER"));
            assertTrue(roles.contains("ROLE_ADMIN"));
            assertEquals(2, roles.size());
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle user with no roles")
        void shouldHandleUserWithNoRoles() {
            User noRoleUser = User.builder()
                    .id(4L)
                    .email("norole@test.com")
                    .password("password")
                    .active(true)
                    .roles(Set.of())
                    .build();

            String token = jwtService.generateToken(noRoleUser, 789L);

            List<String> roles = jwtService.extractClaim(token, claims ->
                    claims.get("roles", List.class));

            assertNotNull(roles);
            assertTrue(roles.isEmpty());
        }

        @Test
        @DisplayName("Should handle user with null customerId")
        void shouldHandleUserWithNullCustomerId() {
            String token = jwtService.generateToken(testUser, null);

            assertNotNull(token);
            assertEquals(TEST_EMAIL, jwtService.extractSubject(token));
        }

        @Test
        @DisplayName("Should handle different secret lengths")
        void shouldHandleDifferentSecretLengths() {
            JwtProperties customProps = new JwtProperties();
            customProps.setSecret("averylongersecretkeythatshouldbe256bitsormore");
            customProps.setExpirationMs(3_600_000L);
            customProps.setServiceTokenExpirationMs(120_000L);
            JwtService customJwtService = new JwtService(customProps);

            String token = customJwtService.generateToken(testUser, TEST_CUSTOMER_ID);

            assertNotNull(token);
            assertEquals(TEST_EMAIL, customJwtService.extractSubject(token));
        }
    }

    @Nested
    @DisplayName("Expired Token Handling")
    class ExpiredTokenTests {

        @Test
        @DisplayName("Should correctly identify expired token")
        void shouldCorrectlyIdentifyExpiredToken() {
            String expiredToken = createExpiredToken();

            assertThrows(ExpiredJwtException.class, () -> jwtService.isTokenExpired(expiredToken));
        }

        @Test
        @DisplayName("Should detect expired token in isTokenValid")
        void shouldDetectExpiredTokenInIsTokenValid() {
            String expiredToken = createExpiredToken();

            UserDetails userDetails = org.springframework.security.core.userdetails.User
                    .withUsername(TEST_EMAIL)
                    .password("dummy")
                    .authorities("ROLE_USER")
                    .build();

            assertThrows(ExpiredJwtException.class, () -> jwtService.isTokenValid(expiredToken, userDetails));
        }

        private String createExpiredToken() {
            SecretKey key = Keys.hmacShaKeyFor(TEST_SECRET.getBytes());
            return Jwts.builder()
                    .issuer("auth-server")
                    .subject(TEST_EMAIL)
                    .claim("userId", TEST_USER_ID)
                    .claim("roles", List.of("ROLE_USER"))
                    .issuedAt(new Date(System.currentTimeMillis() - 7200000))
                    .expiration(new Date(System.currentTimeMillis() - 3600000))
                    .signWith(key)
                    .compact();
        }
    }
}