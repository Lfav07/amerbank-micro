package com.amerbank.customer.customer.integration.application;

import com.amerbank.customer.customer.dto.CustomerInfo;
import com.amerbank.customer.customer.dto.CustomerRegistrationRequest;
import com.amerbank.customer.customer.dto.CustomerRegistrationResponse;
import com.amerbank.customer.customer.repository.CustomerRepository;
import com.amerbank.customer.customer.util.TestJwtFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;

@SpringBootTest(properties = "spring.cloud.config.enabled=false",
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@Testcontainers
@ActiveProfiles("test")
public class CustomerProfileIT {
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
    private TestRestTemplate testRestTemplate;

    @Autowired
    private CustomerRepository customerRepository;

    @AfterEach
    void clearDatabase() {
        customerRepository.deleteAllInBatch();
    }

    private Long createCustomer(Long userId, String firstName, String lastName) {
        String registerEndpoint = "/customer/internal/register";
        CustomerRegistrationRequest request = new CustomerRegistrationRequest(
                firstName, lastName, userId, LocalDate.of(1990, 5, 6));
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(testJwtFactory.generateServiceToken());
        HttpEntity<CustomerRegistrationRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<CustomerRegistrationResponse> response = testRestTemplate.exchange(
                registerEndpoint,
                HttpMethod.POST,
                entity,
                CustomerRegistrationResponse.class
        );
        return response.getBody().id();
    }

    private String getUserToken(String email, Long customerId) {
        return testJwtFactory.generateCustomerUserToken(email, customerId);
    }

    @Nested
    @DisplayName("Get Own Profile")
    class GetOwnProfileTests {

        @Test
        @DisplayName("Should get own profile with valid token")
        void shouldGetOwnProfile() {
            Long customerId = createCustomer(1L, "John", "Doe");
            String email = "john@example.com";
            String token = getUserToken(email, customerId);

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<CustomerInfo> response = testRestTemplate.exchange(
                    "/customer/me",
                    HttpMethod.GET,
                    entity,
                    CustomerInfo.class
            );

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("John", response.getBody().firstName());
            assertEquals("Doe", response.getBody().lastName());
        }

        @Test
        @DisplayName("Should not get profile without token")
        void shouldNotGetProfileWithoutToken() {
            HttpEntity<?> entity = new HttpEntity<>(new HttpHeaders());

            ResponseEntity<CustomerInfo> response = testRestTemplate.exchange(
                    "/customer/me",
                    HttpMethod.GET,
                    entity,
                    CustomerInfo.class
            );

            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        }

        @Test
        @DisplayName("Should not get profile with invalid token")
        void shouldNotGetProfileWithInvalidToken() {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth("invalidToken");
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<CustomerInfo> response = testRestTemplate.exchange(
                    "/customer/me",
                    HttpMethod.GET,
                    entity,
                    CustomerInfo.class
            );

            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        }
    }
}
