package com.amerbank.customer.customer.service;

import com.amerbank.customer.customer.CustomerNotFoundException;
import com.amerbank.customer.customer.repository.CustomerRepository;
import com.amerbank.customer.customer.dto.CustomerRequest;
import com.amerbank.customer.customer.dto.CustomerResponse;
import com.amerbank.customer.customer.dto.CustomerUpdateRequest;
import com.amerbank.customer.customer.dto.PasswordUpdateRequest;
import com.amerbank.customer.customer.model.Customer;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;

    private final PasswordEncoder passwordEncoder;


    public Customer findCustomerById(Long id) {
        return customerRepository.findById(id).orElseThrow(() -> new CustomerNotFoundException("Customer not found with ID " + id));
    }

    private void validateCustomerExistsOrThrow(Long id) {
        if (!customerRepository.existsById(id)) {
            throw new CustomerNotFoundException("Customer not found with ID " + id);
        }
    }

    public Customer findCustomerByEmail(String email) {
        return customerRepository.findCustomerByEmail(email).orElseThrow(() -> new CustomerNotFoundException("Customer not found with Email " + email));
    }

    public CustomerResponse getCustomerInfo(Long id) {
        return customerMapper.fromCustomer(findCustomerById(id));
    }

    public CustomerResponse registerCustomer(CustomerRequest customerRequest, Long userId) {

        if (customerRepository.existsByUserId(userId)) {
            throw new IllegalArgumentException("Customer already exists for userId " + userId);
        }

        String encodedPassword = passwordEncoder.encode(customerRequest.password());

        Customer customer = customerMapper.toCustomer(customerRequest);

        Customer saved = customerRepository.save(customer);
        return customerMapper.fromCustomer(saved);
    }


    @Transactional
    public CustomerResponse editCustomerInfo(CustomerUpdateRequest request) {
        validateCustomerExistsOrThrow(request.customerId());
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
        validateCustomerExistsOrThrow(id);
        customerRepository.deleteById(id);
    }


}
