package com.amerbank.auth_server.integration.application;

import com.amerbank.auth_server.dto.UserRegisterRequest;
import com.amerbank.auth_server.dto.UserResponse;
import com.amerbank.auth_server.repository.UserRepository;
import com.amerbank.auth_server.security.JwtService;
import com.amerbank.auth_server.util.TestJwtFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;


import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = "spring.cloud.config.enabled=false",
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@Testcontainers
@ActiveProfiles("test")
@Transactional
public class UserRegistrationIT {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @TestConfiguration
    static class JwtTestConfig extends TestJwtFactory {
    }

    @Autowired
    private TestJwtFactory testJwtFactory;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;


    @Test
    @DisplayName("Should register user when called by customer service with valid service JWT")
    void shouldRegisterUser() {
        String email = "test@email.com";
        String password = "testPassword";
        String endpoint = "/auth/internal/register";
        UserRegisterRequest request = new UserRegisterRequest(email, password);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(testJwtFactory.generateServiceToken());
        HttpEntity<UserRegisterRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<UserResponse> response = restTemplate.exchange(
                endpoint,
                HttpMethod.POST,
                entity,
                UserResponse.class
        );

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(userRepository.existsByEmailIgnoreCase(email));
    }
}
