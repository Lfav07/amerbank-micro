package com.amerbank.customer.customer.service;

import com.amerbank.customer.customer.config.CustomerProperties;
import com.amerbank.customer.customer.dto.*;
import com.amerbank.customer.customer.exception.*;
import com.amerbank.customer.customer.model.Customer;
import com.amerbank.customer.customer.repository.CustomerRepository;
import com.amerbank.customer.customer.security.JwtService;
import org.aspectj.lang.annotation.Before;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Spy
    private CustomerMapper customerMapper;

    @Mock
    private RestClient restClient;

    @Mock
    private RestClient.Builder restClientBuilder;

    @Mock
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private RestClient.RequestBodySpec requestBodySpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    @Mock
    private JwtService jwtService;

    @Mock
    private KafkaTemplate<String, CustomerDeletedEvent> kafkaTemplate;

    private CustomerProperties props;

    private CustomerService customerService;





    @BeforeEach
    void setUp() {
        props = new CustomerProperties();
        props.setAuthServiceUrl("http://auth");
        props.setAuthRegisterPath("/register");
        TransactionSynchronizationManager.initSynchronization();

        when(restClientBuilder.build()).thenReturn(restClient);

        customerService = new CustomerService(
                customerRepository,
                customerMapper,
                restClientBuilder,
                jwtService,
                kafkaTemplate,
                props
        );
    }
    @AfterEach
    public void tearDown() {
    TransactionSynchronizationManager.clearSynchronization();
    }

    // ==================== saveCustomerTransactional Tests ====================

    @Test
    @DisplayName("Should throw CustomerRegistrationFailedException on data integrity violation")
    void shouldThrowCustomerRegistrationFailedOnDataIntegrityViolation() {
        // Arrange
        CustomerRequest request = new CustomerRequest(
                "test@email.com",
                "password123",
                "Test",
                "User",
                LocalDate.of(1990, 1, 1)
        );

        Long userId = 123L;
        Customer customer = new Customer();
        customer.setUserId(userId);

        when(customerRepository.existsByUserId(userId)).thenReturn(false);
        when(customerMapper.toCustomer(request, userId)).thenReturn(customer);
        when(customerRepository.save(any(Customer.class)))
                .thenThrow(new DataIntegrityViolationException("Constraint violation"));

        // Act & Assert
        assertThrows(CustomerRegistrationFailedException.class, () -> {
            customerService.saveCustomerTransactional(request, userId);
        });

        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    @DisplayName("Should throw CustomerAlreadyExistsException when customer exists for userId")
    void shouldThrowCustomerAlreadyExistsException() {
        // Arrange
        CustomerRequest request = new CustomerRequest(
                "test@email.com",
                "password123",
                "Test",
                "User",
                LocalDate.of(1990, 1, 1)
        );

        Long userId = 123L;

        when(customerRepository.existsByUserId(userId)).thenReturn(true);

        // Act & Assert
        assertThrows(CustomerAlreadyExistsException.class, () -> {
            customerService.saveCustomerTransactional(request, userId);
        });

        verify(customerRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should set KYC as verified when saving customer")
    void shouldSetKycAsVerified() {
        // Arrange
        CustomerRequest request = new CustomerRequest(
                "test@email.com",
                "password123",
                "Test",
                "User",
                LocalDate.of(1990, 1, 1)
        );

        Long userId = 123L;
        Customer customer = new Customer();
        customer.setUserId(userId);
        customer.setKycVerified(false);

        when(customerRepository.existsByUserId(userId)).thenReturn(false);
        when(customerMapper.toCustomer(request, userId)).thenReturn(customer);
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        customerService.saveCustomerTransactional(request, userId);

        // Assert
        ArgumentCaptor<Customer> customerCaptor = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository).save(customerCaptor.capture());
        assertTrue(customerCaptor.getValue().isKycVerified());
    }

    // ==================== Retrieval Tests ====================

    @Test
    @DisplayName("Should find customer by ID successfully")
    void shouldFindCustomerById() {
        // Arrange
        Long customerId = 1L;
        Customer customer = new Customer();
        customer.setId(customerId);
        customer.setFirstName("John");
        customer.setLastName("Doe");

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));

        // Act
        Customer result = customerService.findCustomerById(customerId);

        // Assert
        assertNotNull(result);
        assertEquals(customerId, result.getId());
        assertEquals("John", result.getFirstName());
        verify(customerRepository).findById(customerId);
    }

    @Test
    @DisplayName("Should throw CustomerNotFoundException when customer ID not found")
    void shouldThrowCustomerNotFoundExceptionById() {
        // Arrange
        Long customerId = 999L;
        when(customerRepository.findById(customerId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(CustomerNotFoundException.class, () -> {
            customerService.findCustomerById(customerId);
        });

        verify(customerRepository).findById(customerId);
    }

    @Test
    @DisplayName("Should find customer by user ID successfully")
    void shouldFindCustomerByUserId() {
        // Arrange
        Long userId = 100L;
        Customer customer = new Customer();
        customer.setId(1L);
        customer.setUserId(userId);
        customer.setFirstName("Jane");
        customer.setLastName("Smith");

        when(customerRepository.findByUserId(userId)).thenReturn(Optional.of(customer));

        // Act
        Customer result = customerService.findCustomerByUserId(userId);

        // Assert
        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        assertEquals("Jane", result.getFirstName());
        verify(customerRepository).findByUserId(userId);
    }

    @Test
    @DisplayName("Should throw CustomerNotFoundException when user ID not found")
    void shouldThrowCustomerNotFoundExceptionByUserId() {
        // Arrange
        Long userId = 999L;
        when(customerRepository.findByUserId(userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(CustomerNotFoundException.class, () -> {
            customerService.findCustomerByUserId(userId);
        });

        verify(customerRepository).findByUserId(userId);
    }

    @Test
    @DisplayName("Should get customer ID by user ID successfully")
    void shouldGetCustomerIdByUserId() {
        // Arrange
        Long userId = 100L;
        Long customerId = 1L;
        Customer customer = new Customer();
        customer.setId(customerId);
        customer.setUserId(userId);

        when(customerRepository.findByUserId(userId)).thenReturn(Optional.of(customer));

        // Act
        Long result = customerService.getCustomerIdByUserId(userId);

        // Assert
        assertEquals(customerId, result);
        verify(customerRepository).findByUserId(userId);
    }

    @Test
    @DisplayName("Should find customer by ID and map to response")
    void shouldFindCustomerByIdMapped() {
        // Arrange
        Long customerId = 1L;
        Long userID = 1L;
        Customer customer = new Customer();
        customer.setId(customerId);
        customer.setFirstName("John");
        customer.setLastName("Doe");
        customer.setDateOfBirth(LocalDate.of(1990, 1, 1));
        customer.setKycVerified(true);

        CustomerResponse expectedResponse = new CustomerResponse(
                customerId,
                userID,
                "John",
                "Doe",
                LocalDate.of(1990, 1, 1),
                true,
                LocalDateTime.now()
        );

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));

        // Act
        CustomerResponse result = customerService.findCustomerByIdMapped(customerId);

        // Assert
        assertNotNull(result);
        assertEquals(customerId, result.id());
        assertEquals("John", result.firstName());
        assertEquals("Doe", result.lastName());
        verify(customerRepository).findById(customerId);
    }

    @Test
    @DisplayName("Should find all customers successfully")
    void shouldFindAllCustomers() {
        // Arrange
        Customer customer1 = new Customer();
        customer1.setId(1L);
        customer1.setFirstName("John");
        customer1.setLastName("Doe");
        customer1.setDateOfBirth(LocalDate.of(1990, 1, 1));

        Customer customer2 = new Customer();
        customer2.setId(2L);
        customer2.setFirstName("Jane");
        customer2.setLastName("Smith");
        customer2.setDateOfBirth(LocalDate.of(1985, 5, 15));

        List<Customer> customers = Arrays.asList(customer1, customer2);

        when(customerRepository.findAll()).thenReturn(customers);

        // Act
        List<CustomerResponse> result = customerService.findAllCustomers();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("John", result.get(0).firstName());
        assertEquals("Jane", result.get(1).firstName());
        verify(customerRepository).findAll();
    }

    @Test
    @DisplayName("Should return empty list when no customers exist")
    void shouldReturnEmptyListWhenNoCustomers() {
        // Arrange
        when(customerRepository.findAll()).thenReturn(List.of());

        // Act
        List<CustomerResponse> result = customerService.findAllCustomers();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(customerRepository).findAll();
    }

    @Test
    @DisplayName("Should get customer info successfully")
    void shouldGetCustomerInfo() {
        // Arrange
        Long customerId = 1L;
        Customer customer = new Customer();
        customer.setId(customerId);
        customer.setFirstName("John");
        customer.setLastName("Doe");
        customer.setDateOfBirth(LocalDate.of(1990, 1, 1));
        customer.setKycVerified(true);

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));

        // Act
        CustomerResponse result = customerService.getCustomerInfo(customerId);

        // Assert
        assertNotNull(result);
        assertEquals(customerId, result.id());
        assertEquals("John", result.firstName());
        verify(customerRepository).findById(customerId);
    }

    @Test
    @DisplayName("Should get my customer info successfully")
    void shouldGetMyCustomerInfo() {
        // Arrange
        Long customerId = 1L;
        Customer customer = new Customer();
        customer.setId(customerId);
        customer.setFirstName("John");
        customer.setLastName("Doe");
        customer.setDateOfBirth(LocalDate.of(1990, 1, 1));
        customer.setKycVerified(true);

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));

        // Act
        CustomerInfo result = customerService.getMyCustomerInfo(customerId);

        // Assert
        assertNotNull(result);
        assertEquals("John", result.firstName());
        assertEquals("Doe", result.lastName());
        verify(customerRepository).findById(customerId);
    }

    // ==================== Update Tests ====================

    @Test
    @DisplayName("Should edit customer info successfully")
    void shouldEditCustomerInfo() {
        // Arrange
        Long customerId = 1L;
        Customer customer = new Customer();
        customer.setId(customerId);
        customer.setFirstName("John");
        customer.setLastName("Doe");

        CustomerUpdateRequest updateRequest = new CustomerUpdateRequest("Jane", "Smith");

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        customerService.editCustomerInfo(customerId, updateRequest);

        // Assert
        ArgumentCaptor<Customer> customerCaptor = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository).save(customerCaptor.capture());
        assertEquals("Jane", customerCaptor.getValue().getFirstName());
        assertEquals("Smith", customerCaptor.getValue().getLastName());
    }

    @Test
    @DisplayName("Should throw CustomerNotFoundException when editing non-existent customer")
    void shouldThrowCustomerNotFoundWhenEditingNonExistentCustomer() {
        // Arrange
        Long customerId = 999L;
        CustomerUpdateRequest updateRequest = new CustomerUpdateRequest("Jane", "Smith");

        when(customerRepository.findById(customerId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(CustomerNotFoundException.class, () -> {
            customerService.editCustomerInfo(customerId, updateRequest);
        });

        verify(customerRepository, never()).save(any());
    }

    // ==================== KYC Management Tests ====================

    @Test
    @DisplayName("Should verify KYC successfully")
    void shouldVerifyKyc() {
        // Arrange
        Long customerId = 1L;
        Customer customer = new Customer();
        customer.setId(customerId);
        customer.setKycVerified(false);

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        customerService.verifyKyc(customerId);

        // Assert
        ArgumentCaptor<Customer> customerCaptor = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository).save(customerCaptor.capture());
        assertTrue(customerCaptor.getValue().isKycVerified());
    }

    @Test
    @DisplayName("Should throw CustomerNotFoundException when verifying KYC for non-existent customer")
    void shouldThrowCustomerNotFoundWhenVerifyingKycForNonExistentCustomer() {
        // Arrange
        Long customerId = 999L;
        when(customerRepository.findById(customerId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(CustomerNotFoundException.class, () -> {
            customerService.verifyKyc(customerId);
        });

        verify(customerRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should revoke KYC successfully")
    void shouldRevokeKyc() {
        // Arrange
        Long customerId = 1L;
        Customer customer = new Customer();
        customer.setId(customerId);
        customer.setKycVerified(true);

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        customerService.revokeKyc(customerId);

        // Assert
        ArgumentCaptor<Customer> customerCaptor = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository).save(customerCaptor.capture());
        assertFalse(customerCaptor.getValue().isKycVerified());
    }

    @Test
    @DisplayName("Should throw CustomerNotFoundException when revoking KYC for non-existent customer")
    void shouldThrowCustomerNotFoundWhenRevokingKycForNonExistentCustomer() {
        // Arrange
        Long customerId = 999L;
        when(customerRepository.findById(customerId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(CustomerNotFoundException.class, () -> {
            customerService.revokeKyc(customerId);
        });

        verify(customerRepository, never()).save(any());
    }

    // ==================== Deletion Tests ====================

    @Test
    @DisplayName("Should delete customer by ID successfully")
    void shouldDeleteCustomerById() {
        // Arrange
        Long customerId = 1L;
        Long userId = 100L;
        Customer customer = new Customer();
        customer.setId(customerId);
        customer.setUserId(userId);

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        doNothing().when(customerRepository).delete(customer);

        // Act
        customerService.deleteCustomerById(customerId);

        // Assert
        verify(customerRepository).findById(customerId);
        verify(customerRepository).delete(customer);
    }

    @Test
    @DisplayName("Should throw CustomerNotFoundException when deleting non-existent customer")
    void shouldThrowCustomerNotFoundWhenDeletingNonExistentCustomer() {
        // Arrange
        Long customerId = 999L;
        when(customerRepository.findById(customerId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(CustomerNotFoundException.class, () -> {
            customerService.deleteCustomerById(customerId);
        });

        verify(customerRepository, never()).delete(any());
    }
}