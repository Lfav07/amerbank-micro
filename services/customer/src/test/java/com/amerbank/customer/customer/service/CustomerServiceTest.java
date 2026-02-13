package com.amerbank.customer.customer.service;

import com.amerbank.customer.customer.config.CustomerProperties;
import com.amerbank.customer.customer.dto.*;
import com.amerbank.customer.customer.exception.*;
import com.amerbank.customer.customer.model.Customer;
import com.amerbank.customer.customer.repository.CustomerRepository;
import com.amerbank.customer.customer.security.JwtService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
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
    private RestClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private RestClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    @Mock
    private JwtService jwtService;

    private CustomerProperties props;

    private CustomerService customerService;

    @BeforeEach
    void setUp() {
        props = new CustomerProperties();
        props.setAuthServiceUrl("http://auth");
        props.setAuthUserByEmailPath("/user/by-email");
        TransactionSynchronizationManager.initSynchronization();

        when(restClientBuilder.build()).thenReturn(restClient);

        customerService = new CustomerService(
                customerRepository,
                customerMapper,
                restClientBuilder,
                jwtService,
                props
        );
    }

    @AfterEach
    public void tearDown() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    // ==================== Registration Tests ====================

    @Test
    @DisplayName("Should register customer successfully")
    void shouldRegisterCustomerSuccessfully() {
        // Arrange
        Long userId = 123L;
        CustomerRegistrationRequest request = new CustomerRegistrationRequest(
                "John",
                "Doe",
                userId,
                LocalDate.of(1990, 1, 1)
        );

        Customer savedCustomer = Customer.builder()
                .id(1L)
                .userId(userId)
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .kycVerified(true)
                .build();

        when(customerRepository.saveAndFlush(any(Customer.class))).thenReturn(savedCustomer);

        // Act
        CustomerRegistrationResponse response = customerService.registerCustomer(request);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.id());

        ArgumentCaptor<Customer> customerCaptor = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository).saveAndFlush(customerCaptor.capture());

        Customer capturedCustomer = customerCaptor.getValue();
        assertEquals(userId, capturedCustomer.getUserId());
        assertEquals("John", capturedCustomer.getFirstName());
        assertEquals("Doe", capturedCustomer.getLastName());
        assertEquals(LocalDate.of(1990, 1, 1), capturedCustomer.getDateOfBirth());
        assertTrue(capturedCustomer.isKycVerified());
    }

    @Test
    @DisplayName("Should throw CustomerRegistrationFailedException on data integrity violation")
    void shouldThrowCustomerRegistrationFailedOnDataIntegrityViolation() {
        // Arrange
        Long userId = 123L;
        CustomerRegistrationRequest request = new CustomerRegistrationRequest(
                "John",
                "Doe",
                userId,
                LocalDate.of(1990, 1, 1)
        );

        when(customerRepository.saveAndFlush(any(Customer.class)))
                .thenThrow(new DataIntegrityViolationException("Constraint violation"));

        // Act & Assert
        assertThrows(CustomerRegistrationFailedException.class, () -> {
            customerService.registerCustomer(request);
        });

        verify(customerRepository).saveAndFlush(any(Customer.class));
    }

    @Test
    @DisplayName("Should set KYC as verified when registering customer")
    void shouldSetKycAsVerifiedWhenRegisteringCustomer() {
        // Arrange
        Long userId = 123L;
        CustomerRegistrationRequest request = new CustomerRegistrationRequest(
                "John",
                "Doe",
                userId,
                LocalDate.of(1990, 1, 1)
        );

        Customer savedCustomer = Customer.builder()
                .id(1L)
                .userId(userId)
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .kycVerified(true)
                .build();

        when(customerRepository.saveAndFlush(any(Customer.class))).thenReturn(savedCustomer);

        // Act
        customerService.registerCustomer(request);

        // Assert
        ArgumentCaptor<Customer> customerCaptor = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository).saveAndFlush(customerCaptor.capture());
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
        Long userId = 100L;
        Customer customer = new Customer();
        customer.setId(customerId);
        customer.setUserId(userId);
        customer.setFirstName("John");
        customer.setLastName("Doe");
        customer.setDateOfBirth(LocalDate.of(1990, 1, 1));
        customer.setKycVerified(true);

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
        customer1.setUserId(100L);
        customer1.setFirstName("John");
        customer1.setLastName("Doe");
        customer1.setDateOfBirth(LocalDate.of(1990, 1, 1));

        Customer customer2 = new Customer();
        customer2.setId(2L);
        customer2.setUserId(101L);
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
        Long userId = 100L;
        Customer customer = new Customer();
        customer.setId(customerId);
        customer.setUserId(userId);
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
        Long userId = 100L;
        Customer customer = new Customer();
        customer.setId(customerId);
        customer.setUserId(userId);
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

    @Test
    @DisplayName("Should get customer info by email successfully")
    void shouldGetCustomerInfoByEmail() {
        // Arrange
        String email = "test@email.com";
        Long userId = 100L;
        Long customerId = 1L;

        UserResponse userResponse = new UserResponse(userId, email);
        Customer customer = new Customer();
        customer.setId(customerId);
        customer.setUserId(userId);
        customer.setFirstName("John");
        customer.setLastName("Doe");
        customer.setDateOfBirth(LocalDate.of(1990, 1, 1));
        customer.setKycVerified(true);

        when(jwtService.generateServiceToken()).thenReturn("service-token");
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.accept(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.headers(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toEntity(UserResponse.class))
                .thenReturn(org.springframework.http.ResponseEntity.ok(userResponse));

        when(customerRepository.findByUserId(userId)).thenReturn(Optional.of(customer));

        // Act
        CustomerResponse result = customerService.getCustomerInfoByEmail(email);

        // Assert
        assertNotNull(result);
        assertEquals(customerId, result.id());
        assertEquals("John", result.firstName());
        verify(customerRepository).findByUserId(userId);
    }



    @Test
    @DisplayName("Should throw AuthServiceUnavailableException when auth service is down")
    void shouldThrowAuthServiceUnavailableWhenServiceDown() {
        // Arrange
        String email = "test@email.com";

        when(jwtService.generateServiceToken()).thenReturn("service-token");
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.accept(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.headers(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toEntity(UserResponse.class))
                .thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR));

        // Act & Assert
        assertThrows(AuthServiceUnavailableException.class, () -> {
            customerService.getCustomerInfoByEmail(email);
        });
    }

    @Test
    @DisplayName("Should throw CustomerNotFoundException when customer not found for user email")
    void shouldThrowCustomerNotFoundWhenCustomerNotFoundForUserEmail() {
        // Arrange
        String email = "test@email.com";
        Long userId = 100L;

        UserResponse userResponse = new UserResponse(userId, email);

        when(jwtService.generateServiceToken()).thenReturn("service-token");
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.accept(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.headers(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toEntity(UserResponse.class))
                .thenReturn(org.springframework.http.ResponseEntity.ok(userResponse));

        when(customerRepository.findByUserId(userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(CustomerNotFoundException.class, () -> {
            customerService.getCustomerInfoByEmail(email);
        });
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
        doNothing().when(customerRepository).deleteById(customerId);

        // Act
        customerService.deleteCustomerById(customerId);

        // Assert
        verify(customerRepository).deleteById(customerId);
    }
}