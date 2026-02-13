package com.amerbank.customer.customer.service;

import com.amerbank.customer.customer.config.CustomerProperties;
import com.amerbank.customer.customer.dto.*;
import com.amerbank.customer.customer.exception.*;
import com.amerbank.customer.customer.model.Customer;
import com.amerbank.customer.customer.repository.CustomerRepository;
import com.amerbank.customer.customer.security.JwtService;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.client.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

/**
 * Service layer for handling customer-related operations such as registration,
 * authentication, KYC verification, and information updates.
 */
@Slf4j
@Service

public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;
    private final RestClient restClient;
    private final JwtService jwtService;
    private  final CustomerProperties customerProperties;

    public CustomerService(CustomerRepository customerRepository, CustomerMapper customerMapper, RestClient.Builder restClientBuilder, JwtService jwtService, CustomerProperties customerProperties) {
        this.customerRepository = customerRepository;
        this.customerMapper = customerMapper;
        this.restClient = restClientBuilder.build();
        this.jwtService = jwtService;
        this.customerProperties = customerProperties;
    }


    // -------------------------------------------------------------------------
    // Utilities
    // -------------------------------------------------------------------------

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        int at = email.indexOf('@');
        return email.substring(0, Math.min(2, at)) + "***" + email.substring(at);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    // -------------------- Registration --------------------

    /**
     * Registers a new customer and links them to a user in the authentication service.
     * <p>
     * This method performs the following steps:
     * <ol>
     *     <li>Creates a {@link UserRegisterRequest} from the provided {@link CustomerRequest} with normalized email.</li>
     *     <li>Calls the authentication service via HTTP to register a new user.</li>
     *     <li>Validates the response from the authentication service.</li>
     *     <li>Checks if a customer already exists for the returned user ID.</li>
     *     <li>Maps the request to a {@link Customer} entity, sets KYC as verified, and saves it in the repository.</li>
     * </ol>
     *
     * @param request the registration data containing email, password, and customer details
     * @throws CustomerAlreadyExistsException      if a customer already exists for the given user ID
     * @throws UserRegistrationFailedException     if the authentication service returns null or a client-side error occurs
     * @throws AuthServiceUnavailableException     if the authentication service cannot be reached or returns a server-side error
     * @throws CustomerRegistrationFailedException if an unexpected error occurs during REST call or saving the customer (e.g., data integrity violation)
     */

    @Transactional
    public CustomerRegistrationResponse registerCustomer(CustomerRegistrationRequest request) {
        log.info("Attempting to register new customer");
        Customer customer = Customer.builder().
                userId(request.userId()).
                firstName(request.firstName()).
                lastName(request.lastName()).
                dateOfBirth(request.dateOfBirth()).
                kycVerified(true).
                build();
        try {
         customer = customerRepository.saveAndFlush(customer);
            log.info("Customer successfully registered");
        } catch (DataIntegrityViolationException e) {
            log.warn("Failed to register customer");
            throw  new CustomerRegistrationFailedException("Failed to register customer");
        }
        Long customerId = customer.getId();
        return new CustomerRegistrationResponse(customerId);
    }


    // -------------------- Retrieval --------------------

    /**
     * Retrieves a customer by their database ID.
     *
     * @param id the customer's ID
     * @return the found Customer entity
     * @throws CustomerNotFoundException if no customer is found with the given ID
     */
    public Customer findCustomerById(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found with ID " + id));
    }

    /**
     *
     * @param userId the user's id
     * @return the customer's id for the corresponding user
     * @throws CustomerNotFoundException if no customer is found for the user
     */

    public Long getCustomerIdByUserId(Long userId) {
        Customer customer = findCustomerByUserId(userId);
        return customer.getId();
    }

    /**
     * Retrieves a customer using their associated user ID.
     *
     * @param userId the user's ID
     * @return the corresponding Customer entity
     * @throws CustomerNotFoundException if no customer is found for the user
     */
    public Customer findCustomerByUserId(Long userId) {
        return customerRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found for User ID " + userId));
    }

    /**
     * Retrieves a customer by their database ID and maps it to a CustomerResponse DTO.
     *
     * @param id the customer's ID
     * @return the found Customer entity
     * @throws CustomerNotFoundException if no customer is found with the given ID
     */
    public CustomerResponse findCustomerByIdMapped(Long id) {
        return customerMapper.toResponse(findCustomerById(id));
    }

    /**
     * Retrieves all the customers in the database.
     *
     * @return a List of customers
     */
    public List<CustomerResponse> findAllCustomers() {
        return customerRepository.findAll().stream().map(customerMapper::toResponse).toList();
    }

    /**
     * Returns customer information as a response DTO using the customer's ID.
     *
     * @param customerId the customer's ID
     * @return a response DTO with customer information
     */
    public CustomerResponse getCustomerInfo(Long customerId) {
        return customerMapper.toResponse(findCustomerById(customerId));
    }

    /**
     * Retrieves customer info for the current authenticated user.
     *
     * @param customerId the authenticated user's customer id
     * @return the corresponding CustomerResponse
     * @throws CustomerNotFoundException       if the user or customer is not found
     * @throws AuthServiceUnavailableException if the auth service is unavailable
     */
    public CustomerInfo getMyCustomerInfo(Long customerId) {
        CustomerResponse customer = findCustomerByIdMapped(customerId);
        return customerMapper.getInfoFromCustomer(customer);
    }

    /**
     * Retrieves customer info by email. Intended for admin users only.
     *
     * @param email the user's email
     * @return the corresponding CustomerResponse
     * @throws CustomerNotFoundException       if the user or customer is not found
     * @throws AuthServiceUnavailableException if the auth service is unavailable
     */
    public CustomerResponse getCustomerInfoByEmail(String email) {
        String maskedEmail = maskEmail(email);
        log.debug("Attempting to find customer by email {} ...", maskedEmail);

        String normalizedEmail = normalizeEmail(email);

        String url = UriComponentsBuilder.fromUriString(customerProperties.getAuthServiceUrl())
                .path(customerProperties.getAuthUserByEmailPath())
                .queryParam("email", normalizedEmail)
                .build()
                .toUriString();

        String serviceToken = jwtService.generateServiceToken();

        UserResponse userResponse;
        try {
            userResponse = restClient.get()
                    .uri(url)
                    .accept(MediaType.APPLICATION_JSON)
                    .headers(h -> h.setBearerAuth(serviceToken))
                    .retrieve()
                    .toEntity(UserResponse.class).getBody();
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("Customer not found for user email {}", maskedEmail);
            throw new CustomerNotFoundException("Customer not found for user email " + email);
        } catch (HttpServerErrorException e) {
            log.error("Auth server error while looking up user by email {}", maskedEmail);
            throw new AuthServiceUnavailableException("Cannot reach auth server");
        }

        if (userResponse == null || userResponse.id() == null) {
            log.warn("User not found with email {}", maskedEmail);
            throw new CustomerNotFoundException("User not found with email: " + email);
        }

        Customer customer = customerRepository.findByUserId(userResponse.id())
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found for user with email: " + email));

        log.info("Customer successfully found with email {}", maskedEmail);
        return customerMapper.toResponse(customer);
    }

    // -------------------- Updates --------------------

    /**
     * Edits customer profile information.
     *
     * @param request the update request containing new first and last name
     */
    @Transactional
    public void editCustomerInfo(Long customerId, CustomerUpdateRequest request) {
        Customer customer = findCustomerById(customerId);
        customer.setFirstName(request.firstName());
        customer.setLastName(request.lastName());
        customerRepository.save(customer);
        log.info("Customer with id {} successfully updated their profile", customerId);
    }

    // -------------------- KYC Management --------------------

    /**
     * Sets the customer's KYC verification status to true.
     *
     * @param customerId the customer's ID
     * @throws CustomerNotFoundException if the customer is not found
     */
    @Transactional
    public void verifyKyc(Long customerId) {
        Customer customer = findCustomerById(customerId);
        customer.setKycVerified(true);
        customerRepository.save(customer);
        log.info("Customer with id {} successfully verified KYC", customerId);
    }

    /**
     * Sets the customer's KYC verification status to false.
     *
     * @param customerId the customer's ID
     * @throws CustomerNotFoundException if the customer is not found
     */
    @Transactional
    public void revokeKyc(Long customerId) {
        Customer customer = findCustomerById(customerId);
        customer.setKycVerified(false);
        customerRepository.save(customer);
        log.info("Customer with id {} successfully revoked KYC", customerId);
    }

    // -------------------- Deletion --------------------
    public void deleteCustomerById(Long id) {
        customerRepository.deleteById(id);
        log.info("Customer successfully deleted");
    }

}