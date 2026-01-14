package com.amerbank.customer.customer.service;

import com.amerbank.customer.customer.dto.*;
import com.amerbank.customer.customer.exception.*;
import com.amerbank.customer.customer.model.Customer;
import com.amerbank.customer.customer.repository.CustomerRepository;
import com.amerbank.customer.customer.security.JwtService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.client.*;

import java.util.List;

/**
 * Service layer for handling customer-related operations such as registration,
 * authentication, KYC verification, and information updates.
 */
@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;
    private final RestClient restClient;
    private  final JwtService jwtService;
    private final KafkaTemplate<String, CustomerDeletedEvent> kafkaTemplate;
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
     * @return a List of customers
     */

    public List<CustomerResponse> findAllCustomers() {
        return customerRepository.findAll().stream().map(customerMapper::toResponse).toList();
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
     * Returns customer information as a response DTO using the customer's ID.
     *
     * @param customerId the customer's ID
     * @return a response DTO with customer information
     */
    public CustomerResponse getCustomerInfo(Long customerId) {
        return customerMapper.toResponse(findCustomerById(customerId));
    }

    /**
     * Returns customer information as a response DTO using the user ID.
     *
     * @param userId the user's ID
     * @return a response DTO with customer information
     */
    public CustomerResponse findCustomerByUserIdMapped(Long userId) {
        return customerMapper.toResponse(findCustomerByUserId(userId));
    }

    /**
     * Registers a new customer and links them to a user in the authentication service.
     * <p>
     * This method performs the following steps:
     * <ol>
     *     <li>Creates a {@link UserRegisterRequest} from the provided {@link CustomerRequest}.</li>
     *     <li>Calls the authentication service via HTTP to register a new user.</li>
     *     <li>Validates the response from the authentication service.</li>
     *     <li>Checks if a customer already exists for the returned user ID.</li>
     *     <li>Maps the request to a {@link Customer} entity, sets KYC as verified, and saves it in the repository.</li>
     * </ol>
     *
     * @param request the registration data containing email, password, and customer details
     * @return a {@link CustomerResponse} DTO representing the newly registered customer
     * @throws CustomerAlreadyExistsException if a customer already exists for the given user ID
     * @throws UserRegistrationFailedException if the authentication service returns null or a client-side error occurs
     * @throws AuthServiceUnavailableException if the authentication service cannot be reached or returns a server-side error
     * @throws CustomerRegistrationFailedException if an unexpected error occurs during REST call or saving the customer (e.g., data integrity violation)
     */
    public CustomerResponse registerCustomer(CustomerRequest request) {
        UserRegisterRequest userRegisterRequest = new UserRegisterRequest(
                request.email(),
                request.password()
        );

        UserResponse userResponse;
        try {
            String serviceToken = jwtService.generateServiceToken();

            userResponse = restClient.post()
                    .uri("http://auth-server/auth/internal/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .headers(h -> h.setBearerAuth(serviceToken))
                    .body(userRegisterRequest)
                    .retrieve()
                    .body(UserResponse.class);
        } catch (HttpClientErrorException e) {
            throw new UserRegistrationFailedException("User registration failed: " + e);
        } catch (HttpServerErrorException e) {
            throw new AuthServiceUnavailableException("Auth server error: " + e);
        } catch (ResourceAccessException e) {
            throw new AuthServiceUnavailableException("Cannot reach auth server " + e);
        } catch (RestClientException e) {
            throw new UserRegistrationFailedException("Unexpected error during user registration " +  e);
        }

        if (userResponse == null || userResponse.id() == null) {
            throw new UserRegistrationFailedException("Auth server returned null response");
        }

        Long userId = userResponse.id();

        if (customerRepository.existsByUserId(userId)) {
            throw new CustomerAlreadyExistsException("Customer already exists for userId: " + userId);
        }

        Customer customer = customerMapper.toCustomer(request, userId);
        customer.setKycVerified(true);

        try {
            Customer saved = customerRepository.save(customer);
            return customerMapper.toResponse(saved);
        } catch (DataIntegrityViolationException e) {
            CustomerDeletedEvent event = new CustomerDeletedEvent(userId);
            kafkaTemplate.send("customer.deleted", event);
            throw new CustomerRegistrationFailedException("Failed to save customer: data integrity violation " + e);
        }
    }




    /**
     * Edits customer profile information.
     *
     * @param request the update request containing new first and last name
     * @return an updated customer response
     */
    @Transactional
    public CustomerResponse editCustomerInfo(CustomerUpdateRequest request) {
        Customer customer = findCustomerById(request.customerId());
        customer.setFirstName(request.firstName());
        customer.setLastName(request.lastName());
        customerRepository.save(customer);
        return customerMapper.toResponse(customer);
    }

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
    }

    /**
     * Deletes a customer by ID.
     *
     * @param id the customer's ID
     * @throws CustomerNotFoundException if no customer is found
     */
    @Transactional
    public void deleteCustomerById(Long id) {
        Customer customer = findCustomerById(id);
        customerRepository.delete(customer);

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        kafkaTemplate.send("customer.deleted",
                                new CustomerDeletedEvent(customer.getUserId()));
                    }
                }
        );
    }


    /**
     * Retrieves customer info for the current authenticated user.
     *
     * @param customerId the authenticated user's customer id
     * @return the corresponding CustomerResponse
     * @throws CustomerNotFoundException if the user or customer is not found
     * @throws AuthServiceUnavailableException if the auth service is unavailable
     */
    public CustomerInfo getMyCustomerInfo(Long customerId) {
        CustomerResponse customer = findCustomerByIdMapped(customerId);
        return customerMapper.getInfoFromCustomer(customer);
    }


    // NOTE: In production, this would be handled via Saga or Outbox pattern.
    // This Kafka event is used here to demonstrate eventual consistency handling.
    /**
     * Retrieves customer info by email. Intended for admin users only.
     *
     * @param email the user's email
     * @return the corresponding CustomerResponse
     * @throws CustomerNotFoundException if the user or customer is not found
     * @throws AuthServiceUnavailableException if the auth service is unavailable
     */
    public CustomerResponse getCustomerInfoByEmail(String email){
        String url = "http://auth-server/admin/auth/users/by-email/" + email;

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
            throw new CustomerNotFoundException("Customer not found for user email " + email);
        }
        catch (HttpServerErrorException e) {
            throw new AuthServiceUnavailableException("Cannot reach auth server " + e);
        }

        if (userResponse == null || userResponse.id() == null) {
            throw new CustomerNotFoundException("User not found with email: " + email);
        }

        Customer customer = customerRepository.findByUserId(userResponse.id())
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found for user with email: " + email));

        return customerMapper.toResponse(customer);
    }

    /**
     * Returns a customer's id based on their associated user id.
     * @param userId the user's id
     * @return the customer's id for the corresponding user
     * @throws CustomerNotFoundException if no customer is found for the user
     */

    public Long getCustomerIdByUserId(Long userId) {
        Customer customer = findCustomerByUserId(userId);
        return  customer.getId();
    }
}
