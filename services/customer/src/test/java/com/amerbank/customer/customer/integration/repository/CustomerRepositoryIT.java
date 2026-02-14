package com.amerbank.customer.customer.integration.repository;

import com.amerbank.customer.customer.model.Customer;
import com.amerbank.customer.customer.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(properties = "spring.cloud.config.enabled=false")
@Testcontainers
@ActiveProfiles("test")
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
public class CustomerRepositoryIT {

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
    private CustomerRepository customerRepository;

    @Nested
    @DisplayName("Lookup Operations")
    class LookupTests {

        Customer savedCustomer;

        @BeforeEach
        void setUp() {
            savedCustomer = customerRepository.save(Customer.builder()
                    .userId(1L)
                    .firstName("John")
                    .lastName("Doe")
                    .dateOfBirth(LocalDate.of(1990, 5, 6))
                    .kycVerified(true)
                    .createdAt(LocalDateTime.now())
                    .build()
            );
        }

        @Test
        @DisplayName("Should find customer by id")
        void shouldFindCustomerById() {
            Optional<Customer> found = customerRepository.findById(savedCustomer.getId());

            assertTrue(found.isPresent());
            assertEquals(savedCustomer, found.get());
        }

        @Test
        @DisplayName("Should find customer by user id")
        void shouldFindCustomerByUserId() {
            Optional<Customer> found = customerRepository.findByUserId(savedCustomer.getUserId());

            assertTrue(found.isPresent());
            assertEquals(savedCustomer.getId(), found.get().getId());
        }

        @Test
        @DisplayName("Should return empty when customer not found by id")
        void shouldReturnEmptyWhenCustomerNotFoundById() {
            Optional<Customer> found = customerRepository.findById(9999L);

            assertTrue(found.isEmpty());
        }

        @Test
        @DisplayName("Should return empty when customer not found by user id")
        void shouldReturnEmptyWhenCustomerNotFoundByUserId() {
            Optional<Customer> found = customerRepository.findByUserId(9999L);

            assertTrue(found.isEmpty());
        }
    }

    @Nested
    @DisplayName("Existence Checking Operations")
    class ExistenceTests {

        Customer savedCustomer;

        @BeforeEach
        void setUp() {
            savedCustomer = customerRepository.save(Customer.builder()
                    .userId(2L)
                    .firstName("Jane")
                    .lastName("Smith")
                    .dateOfBirth(LocalDate.of(1985, 3, 15))
                    .kycVerified(false)
                    .createdAt(LocalDateTime.now())
                    .build()
            );
        }

        @Test
        @DisplayName("Should return true when customer exists by id")
        void shouldReturnTrueWhenCustomerExistsById() {
            boolean result = customerRepository.existsById(savedCustomer.getId());

            assertTrue(result);
        }

        @Test
        @DisplayName("Should return true when customer exists by user id")
        void shouldReturnTrueWhenCustomerExistsByUserId() {
            boolean result = customerRepository.existsByUserId(savedCustomer.getUserId());

            assertTrue(result);
        }

        @Test
        @DisplayName("Should return false when customer does not exist by id")
        void shouldReturnFalseWhenCustomerDoesNotExistById() {
            boolean exists = customerRepository.existsById(9999L);

            assertFalse(exists);
        }

        @Test
        @DisplayName("Should return false when customer does not exist by user id")
        void shouldReturnFalseWhenCustomerDoesNotExistByUserId() {
            boolean exists = customerRepository.existsByUserId(9999L);

            assertFalse(exists);
        }
    }

    @Nested
    @DisplayName("Create Operations")
    class CreateTests {

        @Test
        @DisplayName("Should save customer to database")
        void shouldSaveCustomer() {
            Customer customer = Customer.builder()
                    .userId(3L)
                    .firstName("Bob")
                    .lastName("Wilson")
                    .dateOfBirth(LocalDate.of(1992, 8, 20))
                    .kycVerified(true)
                    .createdAt(LocalDateTime.now())
                    .build();

            Customer savedCustomer = customerRepository.save(customer);

            assertNotNull(savedCustomer.getId());
            assertEquals(1, customerRepository.count());
        }
    }

    @Nested
    @DisplayName("Delete Operations")
    class DeleteTests {

        Customer savedCustomer;

        @BeforeEach
        void setUp() {
            savedCustomer = customerRepository.save(Customer.builder()
                    .userId(4L)
                    .firstName("Alice")
                    .lastName("Brown")
                    .dateOfBirth(LocalDate.of(1988, 11, 30))
                    .kycVerified(true)
                    .createdAt(LocalDateTime.now())
                    .build()
            );
        }

        @Test
        @DisplayName("Should delete customer")
        void shouldDeleteCustomer() {
            assertEquals(1, customerRepository.count());

            customerRepository.delete(savedCustomer);

            assertEquals(0, customerRepository.count());
        }

        @Test
        @DisplayName("Should delete customer by id")
        void shouldDeleteCustomerById() {
            assertEquals(1, customerRepository.count());

            customerRepository.deleteById(savedCustomer.getId());

            assertEquals(0, customerRepository.count());
        }

        @Test
        @DisplayName("Should delete all customers")
        void shouldDeleteAllCustomers() {
            customerRepository.save(Customer.builder()
                    .userId(5L)
                    .firstName("Charlie")
                    .lastName("Davis")
                    .dateOfBirth(LocalDate.of(1995, 2, 10))
                    .kycVerified(false)
                    .createdAt(LocalDateTime.now())
                    .build()
            );

            assertEquals(2, customerRepository.count());

            customerRepository.deleteAll();

            assertEquals(0, customerRepository.count());
        }
    }
}
