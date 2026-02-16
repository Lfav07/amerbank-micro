package com.amerbank.customer.customer.integration.application;

import com.amerbank.customer.customer.dto.CustomerRegistrationRequest;
import com.amerbank.customer.customer.dto.CustomerRegistrationResponse;
import com.amerbank.customer.customer.model.Customer;
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

import  static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;

@SpringBootTest(properties = "spring.cloud.config.enabled=false",
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@Testcontainers
@ActiveProfiles("test")
public class CustomerRegistrationIT {
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



    @Nested
    @DisplayName("Customer Registration")
        class RegistrationTests{

        @Test
        @DisplayName("Should register customer")
        void shouldRegisterCustomer(){
            String firstName = "John";
            String lastName = "Doe";
            Long userId = 1L;
            LocalDate dateOfBirth = LocalDate.of(1990, 5, 6);
            String endpoint = "/customer/internal/register";
            CustomerRegistrationRequest request = new CustomerRegistrationRequest(firstName, lastName, userId, dateOfBirth);
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(testJwtFactory.generateServiceToken());
            HttpEntity<CustomerRegistrationRequest> entity = new HttpEntity<>(request, headers);
            ResponseEntity<CustomerRegistrationResponse> response = testRestTemplate.exchange(
                    endpoint,
                    HttpMethod.POST,
                    entity,
                    CustomerRegistrationResponse.class
            );
            assertEquals(HttpStatus.CREATED, response.getStatusCode());
            assertNotNull(response.getBody());
            CustomerRegistrationResponse registrationResponse = response.getBody();
            assertNotNull(registrationResponse.id());
        }

        @Test
        @DisplayName("Should register customer with correct data")
        void shouldRegisterCustomerWithCorrectData(){
            String firstName = "Jane";
            String lastName = "Smith";
            Long userId = 2L;
            LocalDate dateOfBirth = LocalDate.of(1985, 3, 15);
            String endpoint = "/customer/internal/register";
            CustomerRegistrationRequest request = new CustomerRegistrationRequest(firstName, lastName, userId, dateOfBirth);
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(testJwtFactory.generateServiceToken());
            HttpEntity<CustomerRegistrationRequest> entity = new HttpEntity<>(request, headers);
            ResponseEntity<CustomerRegistrationResponse> response = testRestTemplate.exchange(
                    endpoint,
                    HttpMethod.POST,
                    entity,
                    CustomerRegistrationResponse.class
            );
            assertEquals(HttpStatus.CREATED, response.getStatusCode());
            assertNotNull(response.getBody());
            
            var saved = customerRepository.findById(response.getBody().id()).orElseThrow();
            assertEquals(userId, saved.getUserId());
            assertEquals(firstName, saved.getFirstName());
            assertEquals(lastName, saved.getLastName());
            assertEquals(dateOfBirth, saved.getDateOfBirth());
            assertTrue(saved.isKycVerified());
        }

        @Test
        @DisplayName("Should not register customer with invalid service token")
        void shouldNotRegisterCustomerWithInvalidToken(){
            String firstName = "John";
            String lastName = "Doe";
            Long userId = 3L;
            LocalDate dateOfBirth = LocalDate.of(1990, 5, 6);
            String endpoint = "/customer/internal/register";
            CustomerRegistrationRequest request = new CustomerRegistrationRequest(firstName, lastName, userId, dateOfBirth);
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth("invalidToken");
            HttpEntity<CustomerRegistrationRequest> entity = new HttpEntity<>(request, headers);
            ResponseEntity<CustomerRegistrationResponse> response = testRestTemplate.exchange(
                    endpoint,
                    HttpMethod.POST,
                    entity,
                    CustomerRegistrationResponse.class
            );
            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        }

        @Test
        @DisplayName("Should not register customer when userId already exists")
        void shouldNotRegisterCustomerWhenUserIdAlreadyExists(){
            String firstName = "John";
            String lastName = "Doe";
            Long userId = 4L;
            LocalDate dateOfBirth = LocalDate.of(1990, 5, 6);
            String endpoint = "/customer/internal/register";
            CustomerRegistrationRequest request1 = new CustomerRegistrationRequest(firstName, lastName, userId, dateOfBirth);
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(testJwtFactory.generateServiceToken());
            HttpEntity<CustomerRegistrationRequest> entity1 = new HttpEntity<>(request1, headers);
            
            testRestTemplate.exchange(endpoint, HttpMethod.POST, entity1, CustomerRegistrationResponse.class);
            
            CustomerRegistrationRequest request2 = new CustomerRegistrationRequest("Jane", "Smith", userId, LocalDate.of(1985, 1, 1));
            HttpEntity<CustomerRegistrationRequest> entity2 = new HttpEntity<>(request2, headers);
            
            ResponseEntity<CustomerRegistrationResponse> response = testRestTemplate.exchange(
                    endpoint,
                    HttpMethod.POST,
                    entity2,
                    CustomerRegistrationResponse.class
            );
            assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        }

    }

    @Nested
    @DisplayName("Get Customer ID by User ID")
    class GetCustomerIdByUserIdTests {

        @Test
        @DisplayName("Should get customer ID by user ID")
        void shouldGetCustomerIdByUserId(){
            Long userId = 100L;
            String registerEndpoint = "/customer/internal/register";
            CustomerRegistrationRequest registerRequest = new CustomerRegistrationRequest("John", "Doe", userId, LocalDate.of(1990, 5, 6));
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(testJwtFactory.generateServiceToken());
            HttpEntity<CustomerRegistrationRequest> registerEntity = new HttpEntity<>(registerRequest, headers);
            
            ResponseEntity<CustomerRegistrationResponse> registerResponse = testRestTemplate.exchange(
                    registerEndpoint,
                    HttpMethod.POST,
                    registerEntity,
                    CustomerRegistrationResponse.class
            );
            Long expectedCustomerId = registerResponse.getBody().id();
            
            String getEndpoint = "/customer/internal/by-user/" + userId;
            HttpEntity<?> getEntity = new HttpEntity<>(headers);
            ResponseEntity<Long> getResponse = testRestTemplate.exchange(
                    getEndpoint,
                    HttpMethod.GET,
                    getEntity,
                    Long.class
            );
            
            assertEquals(HttpStatus.OK, getResponse.getStatusCode());
            assertNotNull(getResponse.getBody());
            assertEquals(expectedCustomerId, getResponse.getBody());
        }

        @Test
        @DisplayName("Should return 404 when user ID does not exist")
        void shouldReturn404WhenUserIdDoesNotExist(){
            Long userId = 99999L;
            String endpoint = "/customer/internal/by-user/" + userId;
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(testJwtFactory.generateServiceToken());
            HttpEntity<?> entity = new HttpEntity<>(headers);
            
            ResponseEntity<?> response = testRestTemplate.exchange(
                    endpoint,
                    HttpMethod.GET,
                    entity,
                    Object.class
            );
            
            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        }

        @Test
        @DisplayName("Should not get customer ID with invalid token")
        void shouldNotGetCustomerIdWithInvalidToken(){
            Long userId = 100L;
            String endpoint = "/customer/internal/by-user/" + userId;
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth("invalidToken");
            HttpEntity<?> entity = new HttpEntity<>(headers);
            
            ResponseEntity<Long> response = testRestTemplate.exchange(
                    endpoint,
                    HttpMethod.GET,
                    entity,
                    Long.class
            );
            
            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        }

    }


}
