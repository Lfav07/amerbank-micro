package com.amerbank.customer.customer.integration.service;

import com.amerbank.customer.customer.dto.*;
import com.amerbank.customer.customer.exception.*;
import com.amerbank.customer.customer.model.Customer;
import com.amerbank.customer.customer.repository.CustomerRepository;
import com.amerbank.customer.customer.service.CustomerService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest(properties = "spring.cloud.config.enabled=false")
@Testcontainers
@Transactional
@ActiveProfiles("test")
public class CustomerServiceIntegrationTests {

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

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private CustomerRepository customerRepository;

    // -------------------------------------------------------------------------
    // Registration
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Customer Registration")
    class RegistrationTests {
        @Test
        @DisplayName("Should register customer")
        void shouldRegisterCustomer() {
            Long userId = 100L;
            CustomerRegistrationRequest request = new CustomerRegistrationRequest(
                    "John", "Doe", userId, LocalDate.of(1990, 1, 1));

            CustomerRegistrationResponse response = customerService.registerCustomer(request);

            assertNotNull(response);
            assertNotNull(response.id());
            assertTrue(customerRepository.existsById(response.id()));
        }

        @Test
        @DisplayName("Should register customer with correct data")
        void shouldRegisterCustomerWithCorrectData() {
            Long userId = 101L;
            CustomerRegistrationRequest request = new CustomerRegistrationRequest(
                    "Jane", "Smith", userId, LocalDate.of(1985, 5, 15));

            CustomerRegistrationResponse response = customerService.registerCustomer(request);

            Customer saved = customerRepository.findById(response.id()).orElseThrow();
            assertEquals(userId, saved.getUserId());
            assertEquals("Jane", saved.getFirstName());
            assertEquals("Smith", saved.getLastName());
            assertEquals(LocalDate.of(1985, 5, 15), saved.getDateOfBirth());
            assertTrue(saved.isKycVerified());
        }

        @Test
        @DisplayName("Should fail registration when userId already exists")
        void shouldFailRegistrationWhenUserIdAlreadyExists() {
            Long userId = 102L;
            CustomerRegistrationRequest request1 = new CustomerRegistrationRequest(
                    "John", "Doe", userId, LocalDate.of(1990, 1, 1));
            customerService.registerCustomer(request1);

            CustomerRegistrationRequest request2 = new CustomerRegistrationRequest(
                    "Jane", "Smith", userId, LocalDate.of(1985, 5, 15));

            assertThrows(CustomerRegistrationFailedException.class,
                    () -> customerService.registerCustomer(request2));
        }
    }

    // -------------------------------------------------------------------------
    // Retrieval
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Retrieval")
    class RetrievalTests {

        private Long createTestCustomer(Long userId, String firstName, String lastName) {
            CustomerRegistrationRequest request = new CustomerRegistrationRequest(
                    firstName, lastName, userId, LocalDate.of(1990, 1, 1));
            return customerService.registerCustomer(request).id();
        }

        @Test
        @DisplayName("Should find customer by ID")
        void shouldFindCustomerById() {
            Long customerId = createTestCustomer(200L, "John", "Doe");

            Customer found = customerService.findCustomerById(customerId);

            assertNotNull(found);
            assertEquals(customerId, found.getId());
            assertEquals("John", found.getFirstName());
        }

        @Test
        @DisplayName("Should throw CustomerNotFoundException when customer not found by ID")
        void shouldThrowExceptionWhenCustomerNotFoundById() {
            assertThrows(CustomerNotFoundException.class,
                    () -> customerService.findCustomerById(9999L));
        }

        @Test
        @DisplayName("Should find customer by user ID")
        void shouldFindCustomerByUserId() {
            Long userId = 201L;
            createTestCustomer(userId, "Jane", "Smith");

            Customer found = customerService.findCustomerByUserId(userId);

            assertNotNull(found);
            assertEquals(userId, found.getUserId());
        }

        @Test
        @DisplayName("Should throw CustomerNotFoundException when customer not found by user ID")
        void shouldThrowExceptionWhenCustomerNotFoundByUserId() {
            assertThrows(CustomerNotFoundException.class,
                    () -> customerService.findCustomerByUserId(9999L));
        }

        @Test
        @DisplayName("Should get customer ID by user ID")
        void shouldGetCustomerIdByUserId() {
            Long userId = 202L;
            Long customerId = createTestCustomer(userId, "Bob", "Wilson");

            Long foundId = customerService.getCustomerIdByUserId(userId);

            assertEquals(customerId, foundId);
        }

        @Test
        @DisplayName("Should find customer by ID mapped to response")
        void shouldFindCustomerByIdMapped() {
            Long customerId = createTestCustomer(203L, "Alice", "Brown");

            CustomerResponse response = customerService.findCustomerByIdMapped(customerId);

            assertNotNull(response);
            assertEquals(customerId, response.id());
            assertEquals("Alice", response.firstName());
            assertEquals("Brown", response.lastName());
        }

        @Test
        @DisplayName("Should find all customers")
        void shouldFindAllCustomers() {
            createTestCustomer(300L, "User", "One");
            createTestCustomer(301L, "User", "Two");

            List<CustomerResponse> customers = customerService.findAllCustomers();

            assertEquals(2, customers.size());
        }

        @Test
        @DisplayName("Should return empty list when no customers exist")
        void shouldReturnEmptyListWhenNoCustomers() {
            List<CustomerResponse> customers = customerService.findAllCustomers();

            assertNotNull(customers);
            assertTrue(customers.isEmpty());
        }

        @Test
        @DisplayName("Should get customer info")
        void shouldGetCustomerInfo() {
            Long customerId = createTestCustomer(302L, "Charlie", "Davis");

            CustomerResponse response = customerService.getCustomerInfo(customerId);

            assertNotNull(response);
            assertEquals(customerId, response.id());
            assertEquals("Charlie", response.firstName());
        }

        @Test
        @DisplayName("Should get my customer info")
        void shouldGetMyCustomerInfo() {
            Long customerId = createTestCustomer(303L, "Diana", "Evans");

            CustomerInfo info = customerService.getMyCustomerInfo(customerId);

            assertNotNull(info);
            assertEquals("Diana", info.firstName());
            assertEquals("Evans", info.lastName());
        }
    }

    // -------------------------------------------------------------------------
    // Updates
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Update Customer Info")
    class UpdateTests {

        private Long createTestCustomer(Long userId) {
            CustomerRegistrationRequest request = new CustomerRegistrationRequest(
                    "John", "Doe", userId, LocalDate.of(1990, 1, 1));
            return customerService.registerCustomer(request).id();
        }

        @Test
        @DisplayName("Should update customer info")
        void shouldUpdateCustomerInfo() {
            Long customerId = createTestCustomer(400L);
            CustomerUpdateRequest updateRequest = new CustomerUpdateRequest("Jane", "Smith");

            customerService.editCustomerInfo(customerId, updateRequest);

            Customer updated = customerRepository.findById(customerId).orElseThrow();
            assertEquals("Jane", updated.getFirstName());
            assertEquals("Smith", updated.getLastName());
        }

        @Test
        @DisplayName("Should fail update customer info for non-existent customer")
        void shouldFailUpdateCustomerInfoForNonExistentCustomer() {
            CustomerUpdateRequest updateRequest = new CustomerUpdateRequest("Jane", "Smith");

            assertThrows(CustomerNotFoundException.class,
                    () -> customerService.editCustomerInfo(9999L, updateRequest));
        }
    }

    // -------------------------------------------------------------------------
    // KYC Management
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("KYC Management")
    class KycManagementTests {

        private Long createTestCustomer(Long userId) {
            CustomerRegistrationRequest request = new CustomerRegistrationRequest(
                    "John", "Doe", userId, LocalDate.of(1990, 1, 1));
            return customerService.registerCustomer(request).id();
        }

        @Test
        @DisplayName("Should verify KYC")
        void shouldVerifyKyc() {
            Long customerId = createTestCustomer(500L);
            customerService.revokeKyc(customerId);

            customerService.verifyKyc(customerId);

            Customer customer = customerRepository.findById(customerId).orElseThrow();
            assertTrue(customer.isKycVerified());
        }

        @Test
        @DisplayName("Should revoke KYC")
        void shouldRevokeKyc() {
            Long customerId = createTestCustomer(501L);

            customerService.revokeKyc(customerId);

            Customer customer = customerRepository.findById(customerId).orElseThrow();
            assertFalse(customer.isKycVerified());
        }

        @Test
        @DisplayName("Should fail verify KYC for non-existent customer")
        void shouldFailVerifyKycForNonExistentCustomer() {
            assertThrows(CustomerNotFoundException.class,
                    () -> customerService.verifyKyc(9999L));
        }

        @Test
        @DisplayName("Should fail revoke KYC for non-existent customer")
        void shouldFailRevokeKycForNonExistentCustomer() {
            assertThrows(CustomerNotFoundException.class,
                    () -> customerService.revokeKyc(9999L));
        }
    }

    // -------------------------------------------------------------------------
    // Deletion
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Deletion")
    class DeletionTests {

        private Long createTestCustomer(Long userId) {
            CustomerRegistrationRequest request = new CustomerRegistrationRequest(
                    "John", "Doe", userId, LocalDate.of(1990, 1, 1));
            return customerService.registerCustomer(request).id();
        }

        @Test
        @DisplayName("Should delete customer by ID")
        void shouldDeleteCustomerById() {
            Long customerId = createTestCustomer(600L);
            assertTrue(customerRepository.existsById(customerId));

            customerService.deleteCustomerById(customerId);

            assertFalse(customerRepository.existsById(customerId));
        }

        @Test
        @DisplayName("Should fail delete non-existent customer")
        void shouldFailDeleteNonExistentCustomer() {
            assertThrows(CustomerNotFoundException.class,
                    () -> customerService.deleteCustomerById(9999L));
        }
    }
}
