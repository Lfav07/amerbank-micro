package com.amerbank.customer.customer.service;

import com.amerbank.common_dto.AuthenticationResponse;
import com.amerbank.common_dto.Role;
import com.amerbank.common_dto.UserLoginRequest;
import com.amerbank.common_dto.UserRegisterRequest;
import com.amerbank.customer.customer.CustomerNotFoundException;
import com.amerbank.customer.customer.dto.CustomerRequest;
import com.amerbank.customer.customer.dto.CustomerResponse;
import com.amerbank.customer.customer.dto.CustomerUpdateRequest;
import com.amerbank.common_dto.UserResponse;
import com.amerbank.customer.customer.model.Customer;
import com.amerbank.customer.customer.repository.CustomerRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;
    private final RestTemplate restTemplate;

    public Customer findCustomerById(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found with ID " + id));
    }

    public Customer findCustomerByUserId(Long userId) {
        return customerRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found for User ID " + userId));
    }

    public CustomerResponse getCustomerInfo(Long customerId) {
        return customerMapper.fromCustomer(findCustomerById(customerId));
    }

    public CustomerResponse getCustomerInfoByUserId(Long userId) {
        return customerMapper.fromCustomer(findCustomerByUserId(userId));
    }

    public CustomerResponse registerCustomer(CustomerRequest request) {
        // 1. Register user via UserService
        UserRegisterRequest userRegisterRequest = new UserRegisterRequest(
                request.email(),
                request.password(),
                Set.of(Role.ROLE_USER)
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

    @Transactional
    public CustomerResponse editCustomerInfo(CustomerUpdateRequest request) {
        Customer customer = findCustomerById(request.customerId());
        customer.setFirstName(request.firstName());
        customer.setLastName(request.lastName());
        customerRepository.save(customer);
        return customerMapper.fromCustomer(customer);
    }

    @Transactional
    public void verifyKyc(Long customerId) {
        Customer customer = findCustomerById(customerId);
        customer.setKycVerified(true);
        customerRepository.save(customer);
    }

    @Transactional
    public void deleteCustomerById(Long id) {
        if (!customerRepository.existsById(id)) {
            throw new CustomerNotFoundException("Customer not found with ID " + id);
        }
        customerRepository.deleteById(id);
    }

    public AuthenticationResponse login(UserLoginRequest request) {
        String authUrl = "http://auth-server/auth/login";

        try {
            ResponseEntity<AuthenticationResponse> response = restTemplate.postForEntity(
                    authUrl,
                    request,
                    AuthenticationResponse.class
            );

            return response.getBody();

        } catch (HttpClientErrorException.Unauthorized ex) {
            throw new IllegalArgumentException("Invalid credentials");
        } catch (RestClientException ex) {
            throw new IllegalStateException("Authentication service is unavailable");
        }
    }



}
