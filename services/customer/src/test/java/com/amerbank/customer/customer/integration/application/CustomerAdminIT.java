package com.amerbank.customer.customer.integration.application;

import com.amerbank.customer.customer.dto.CustomerRegistrationRequest;
import com.amerbank.customer.customer.dto.CustomerRegistrationResponse;
import com.amerbank.customer.customer.dto.CustomerResponse;
import com.amerbank.customer.customer.dto.CustomerUpdateRequest;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = "spring.cloud.config.enabled=false",
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@Testcontainers
@ActiveProfiles("test")
public class CustomerAdminIT {
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

    private String getAdminToken(String email, Long customerId) {
        return testJwtFactory.generateAdminToken(email, customerId);
    }

    private String getUserToken(String email, Long customerId) {
        return testJwtFactory.generateCustomerUserToken(email, customerId);
    }

    @Nested
    @DisplayName("Get All Customers")
    class GetAllCustomersTests {

        @Test
        @DisplayName("Should get all customers with admin token")
        void shouldGetAllCustomers() {
            Long adminCustomerId = createCustomer(1L, "Admin", "User");
            createCustomer(2L, "User", "One");
            createCustomer(3L, "User", "Two");

            String token = getAdminToken("admin@example.com", adminCustomerId);
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<List> response = testRestTemplate.exchange(
                    "/customer/admin/customers",
                    HttpMethod.GET,
                    entity,
                    List.class
            );

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(3, response.getBody().size());
        }

        @Test
        @DisplayName("Should not get all customers with regular user token")
        void shouldNotGetAllCustomersWithUserToken() {
            Long userCustomerId = createCustomer(1L, "Regular", "User");
            String token = getUserToken("user@example.com", userCustomerId);

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = testRestTemplate.exchange(
                    "/customer/admin/customers",
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        }

        @Test
        @DisplayName("Should not get all customers without token")
        void shouldNotGetAllCustomersWithoutToken() {
            HttpEntity<?> entity = new HttpEntity<>(new HttpHeaders());

            ResponseEntity<Map> response = testRestTemplate.exchange(
                    "/customer/admin/customers",
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        }
    }

    @Nested
    @DisplayName("Get Customer By ID")
    class GetCustomerByIdTests {

        @Test
        @DisplayName("Should get customer by ID with admin token")
        void shouldGetCustomerById() {
            Long adminCustomerId = createCustomer(1L, "Admin", "User");
            Long customerId = createCustomer(2L, "John", "Doe");

            String token = getAdminToken("admin@example.com", adminCustomerId);
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<CustomerResponse> response = testRestTemplate.exchange(
                    "/customer/admin/customers/" + customerId,
                    HttpMethod.GET,
                    entity,
                    CustomerResponse.class
            );

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("John", response.getBody().firstName());
            assertEquals("Doe", response.getBody().lastName());
        }

        @Test
        @DisplayName("Should return 404 for non-existent customer ID")
        void shouldReturn404ForNonExistentCustomerId() {
            Long adminCustomerId = createCustomer(1L, "Admin", "User");

            String token = getAdminToken("admin@example.com", adminCustomerId);
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<CustomerResponse> response = testRestTemplate.exchange(
                    "/customer/admin/customers/99999",
                    HttpMethod.GET,
                    entity,
                    CustomerResponse.class
            );

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        }

        @Test
        @DisplayName("Should not get customer by ID with regular user token")
        void shouldNotGetCustomerByIdWithUserToken() {
            Long userCustomerId = createCustomer(1L, "Regular", "User");
            Long customerId = createCustomer(2L, "John", "Doe");

            String token = getUserToken("user@example.com", userCustomerId);
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = testRestTemplate.exchange(
                    "/customer/admin/customers/" + customerId,
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        }
    }

    @Nested
    @DisplayName("Update Customer")
    class UpdateCustomerTests {

        @Test
        @DisplayName("Should update customer with admin token")
        void shouldUpdateCustomer() {
            Long adminCustomerId = createCustomer(1L, "Admin", "User");
            Long customerId = createCustomer(2L, "John", "Doe");

            String token = getAdminToken("admin@example.com", adminCustomerId);
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);

            CustomerUpdateRequest request = new CustomerUpdateRequest("Jane", "Smith");
            HttpEntity<CustomerUpdateRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Map> response = testRestTemplate.exchange(
                    "/customer/admin/customers/" + customerId,
                    HttpMethod.PUT,
                    entity,
                    Map.class
            );

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("Successfully updated customer's data for customer " + customerId, response.getBody().get("message"));

            var updated = customerRepository.findById(customerId).orElseThrow();
            assertEquals("Jane", updated.getFirstName());
            assertEquals("Smith", updated.getLastName());
        }

        @Test
        @DisplayName("Should return 404 when updating non-existent customer")
        void shouldReturn404WhenUpdatingNonExistentCustomer() {
            Long adminCustomerId = createCustomer(1L, "Admin", "User");

            String token = getAdminToken("admin@example.com", adminCustomerId);
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);

            CustomerUpdateRequest request = new CustomerUpdateRequest("Jane", "Smith");
            HttpEntity<CustomerUpdateRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Map> response = testRestTemplate.exchange(
                    "/customer/admin/customers/99999",
                    HttpMethod.PUT,
                    entity,
                    Map.class
            );

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        }
    }

    @Nested
    @DisplayName("KYC Management")
    class KycManagementTests {

        @Test
        @DisplayName("Should verify KYC with admin token")
        void shouldVerifyKyc() {
            Long adminCustomerId = createCustomer(1L, "Admin", "User");
            Long customerId = createCustomer(2L, "John", "Doe");

            String token = getAdminToken("admin@example.com", adminCustomerId);
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = testRestTemplate.exchange(
                    "/customer/admin/customers/" + customerId + "/kyc/verify",
                    HttpMethod.PATCH,
                    entity,
                    Map.class
            );

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("KYC verified successfully", response.getBody().get("message"));

            var updated = customerRepository.findById(customerId).orElseThrow();
            assertTrue(updated.isKycVerified());
        }

        @Test
        @DisplayName("Should revoke KYC with admin token")
        void shouldRevokeKyc() {
            Long adminCustomerId = createCustomer(1L, "Admin", "User");
            Long customerId = createCustomer(2L, "John", "Doe");

            String token = getAdminToken("admin@example.com", adminCustomerId);
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = testRestTemplate.exchange(
                    "/customer/admin/customers/" + customerId + "/kyc/revoke",
                    HttpMethod.PATCH,
                    entity,
                    Map.class
            );

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("KYC unverified successfully", response.getBody().get("message"));

            var updated = customerRepository.findById(customerId).orElseThrow();
            assertFalse(updated.isKycVerified());
        }

        @Test
        @DisplayName("Should return 404 for KYC on non-existent customer")
        void shouldReturn404ForKycOnNonExistentCustomer() {
            Long adminCustomerId = createCustomer(1L, "Admin", "User");

            String token = getAdminToken("admin@example.com", adminCustomerId);
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = testRestTemplate.exchange(
                    "/customer/admin/customers/99999/kyc/verify",
                    HttpMethod.PATCH,
                    entity,
                    Map.class
            );

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        }

        @Test
        @DisplayName("Should not verify KYC with regular user token")
        void shouldNotVerifyKycWithUserToken() {
            Long userCustomerId = createCustomer(1L, "Regular", "User");
            Long customerId = createCustomer(2L, "John", "Doe");

            String token = getUserToken("user@example.com", userCustomerId);
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = testRestTemplate.exchange(
                    "/customer/admin/customers/" + customerId + "/kyc/verify",
                    HttpMethod.PATCH,
                    entity,
                    Map.class
            );

            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        }
    }

    @Nested
    @DisplayName("Delete Customer")
    class DeleteCustomerTests {

        @Test
        @DisplayName("Should delete customer with admin token")
        void shouldDeleteCustomer() {
            Long adminCustomerId = createCustomer(1L, "Admin", "User");
            Long customerId = createCustomer(2L, "John", "Doe");

            assertTrue(customerRepository.existsById(customerId));

            String token = getAdminToken("admin@example.com", adminCustomerId);
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = testRestTemplate.exchange(
                    "/customer/admin/customers/" + customerId,
                    HttpMethod.DELETE,
                    entity,
                    Map.class
            );

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertFalse(customerRepository.existsById(customerId));
        }

        @Test
        @DisplayName("Should return 404 when deleting non-existent customer")
        void shouldReturn404WhenDeletingNonExistentCustomer() {
            Long adminCustomerId = createCustomer(1L, "Admin", "User");

            String token = getAdminToken("admin@example.com", adminCustomerId);
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = testRestTemplate.exchange(
                    "/customer/admin/customers/99999",
                    HttpMethod.DELETE,
                    entity,
                    Map.class
            );

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        }

        @Test
        @DisplayName("Should not delete customer with regular user token")
        void shouldNotDeleteCustomerWithUserToken() {
            Long userCustomerId = createCustomer(1L, "Regular", "User");
            Long customerId = createCustomer(2L, "John", "Doe");

            String token = getUserToken("user@example.com", userCustomerId);
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = testRestTemplate.exchange(
                    "/customer/admin/customers/" + customerId,
                    HttpMethod.DELETE,
                    entity,
                    Map.class
            );

            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
            assertTrue(customerRepository.existsById(customerId));
        }
    }
}
