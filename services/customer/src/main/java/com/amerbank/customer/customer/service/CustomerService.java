package com.amerbank.customer.customer.service;

import com.amerbank.common_dto.*;
import com.amerbank.customer.customer.exception.AuthServiceUnavailableException;
import com.amerbank.customer.customer.exception.CustomerNotFoundException;
import com.amerbank.customer.customer.dto.CustomerRequest;
import com.amerbank.customer.customer.dto.CustomerUpdateRequest;
import com.amerbank.customer.customer.model.Customer;
import com.amerbank.customer.customer.repository.CustomerRepository;
import com.amerbank.customer.customer.security.JwtService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

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
    private final RestTemplate restTemplate;
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
     * Retrieves all the customers in the database.
     * @return a List of customers
     */

    public List<Customer> findAllCustomers() {
        return customerRepository.findAll();
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
        return customerMapper.fromCustomer(findCustomerById(customerId));
    }

    /**
     * Returns customer information as a response DTO using the user ID.
     *
     * @param userId the user's ID
     * @return a response DTO with customer information
     */
    public CustomerResponse getCustomerInfoByUserId(Long userId) {
        return customerMapper.fromCustomer(findCustomerByUserId(userId));
    }

    /**
     * Registers a new customer and links them to a user in the auth server.
     *
     * @param request the registration data
     * @return a response DTO of the newly registered customer
     * @throws IllegalArgumentException if a customer already exists for the user
     */
    public CustomerResponse registerCustomer(CustomerRequest request) {
        UserRegisterRequest userRegisterRequest = new UserRegisterRequest(
                request.email(),
                request.password()
        );

        UserResponse userResponse = restTemplate.postForObject(
                "http://auth-server/auth/register",
                userRegisterRequest,
                UserResponse.class
        );

        assert userResponse != null;
        Long userId = userResponse.id();

        if (customerRepository.existsByUserId(userId)) {
            throw new IllegalArgumentException("Customer already exists for userId: " + userId);
        }

        Customer customer = customerMapper.toCustomer(request, userId);
        customer.setKycVerified(true);
        Customer saved = customerRepository.save(customer);
        return customerMapper.fromCustomer(saved);
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
        return customerMapper.fromCustomer(customer);
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
    public void unVerifyKyc(Long customerId) {
        Customer customer = findCustomerById(customerId);
        customer.setKycVerified(false);
        customerRepository.save(customer);
    }

    public void deleteAllCustomers() {
        customerRepository.deleteAll();
    }

    /**
     * Deletes a customer by ID.
     *
     * @param id the customer's ID
     * @throws CustomerNotFoundException if no customer is found
     */
    @Transactional
    public void deleteCustomerById(Long id) {
        if (!customerRepository.existsById(id)) {
            throw new CustomerNotFoundException("Customer not found with ID " + id);
        }
        Customer customer = findCustomerById(id);
        customerRepository.delete(customer);
        Long userId = customer.getUserId();

        CustomerDeletedEvent event = new CustomerDeletedEvent(userId);
        kafkaTemplate.send("customer.deleted", event);

    }

    /**
     * Deletes a customer and their user by ID.
     *
     * @param id the customer's ID
     * @throws CustomerNotFoundException if no customer is found
     */
    @Transactional
    public void deleteCustomerById2(Long id) {
        if (!customerRepository.existsById(id)) {
            throw new CustomerNotFoundException("Customer not found with ID " + id);
        }
        customerRepository.deleteById(id);
    }

    /**
     * Retrieves customer info for the authenticated user by calling /auth/manage/me.
     *
     * @param jwtToken the JWT token for authorization
     * @return the corresponding CustomerResponse
     * @throws CustomerNotFoundException if the user or customer is not found
     * @throws AuthServiceUnavailableException if the auth service is unavailable
     */
    public CustomerResponse getMyCustomerInfo(String jwtToken) {
        Long customerId = jwtService.extractCustomerId(jwtToken);

        Customer customer = findCustomerById(customerId);

        return customerMapper.fromCustomer(customer);
    }

    /**
     * Retrieves customer info for the authenticated user by calling /auth/manage/me.
     *
     * @param id the Customer's id
     * @return the corresponding CustomerResponse
     * @throws CustomerNotFoundException if the user or customer is not found
     * @throws AuthServiceUnavailableException if the auth service is unavailable
     */
    public CustomerResponse getMyCustomerInfoById(Long id) {

        Customer customer = findCustomerById(id);

        return customerMapper.fromCustomer(customer);
    }

    /**
     * Retrieves customer info by email. Intended for admin users only.
     *
     * @param email the user's email
     * @param jwtToken the JWT token for authorization
     * @return the corresponding CustomerResponse
     * @throws CustomerNotFoundException if the user or customer is not found
     * @throws AuthServiceUnavailableException if the auth service is unavailable
     */
    public CustomerResponse getCustomerInfoByEmail(String email, String jwtToken) {
        String url = "http://auth-server/auth/manage/by-email/" + email;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtToken);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<UserResponse> response;
        try {
            response = restTemplate.exchange(url, HttpMethod.GET, entity, UserResponse.class);
        } catch (RestClientException e) {
            throw new AuthServiceUnavailableException("Auth service unavailable");
        }

        UserResponse userResponse = response.getBody();

        if (userResponse == null || userResponse.id() == null) {
            throw new CustomerNotFoundException("User not found with email: " + email);
        }

        Customer customer = customerRepository.findByUserId(userResponse.id())
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found for user with email: " + email));

        return customerMapper.fromCustomer(customer);
    }

    /**
     *
     * @param userId the user's id
     * @return the customer's id for the corresponding user
     * @throws CustomerNotFoundException if no customer is found for the user
     */

    public Long getCustomerIdByUserId(Long userId) {
        Customer customer = findCustomerByUserId(userId);
        return  customer.getId();
    }
}
