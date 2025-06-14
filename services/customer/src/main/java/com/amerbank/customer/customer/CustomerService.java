package com.amerbank.customer.customer;

import com.amerbank.customer.customer.dto.CustomerRequest;
import com.amerbank.customer.customer.dto.CustomerResponse;
import com.amerbank.customer.customer.dto.CustomerUpdateRequest;
import com.amerbank.customer.customer.dto.PasswordUpdateRequest;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
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

    public CustomerResponse registerCustomer(CustomerRequest customerRequest) {

        if (isEmailTaken(customerRequest.email())) {
            throw new IllegalArgumentException("Email is already taken");
        }

        String encodedPassword = passwordEncoder.encode(customerRequest.password());

        Customer customer = customerMapper.toCustomer(customerRequest);
        customer.setPassword(encodedPassword);

        Customer saved = customerRepository.save(customer);
        return customerMapper.fromCustomer(saved);
    }


    @Transactional
    public CustomerResponse editCustomerInfo(CustomerUpdateRequest request) {
        validateCustomerExistsOrThrow(request.customerId());
        Customer customer = findCustomerById(request.customerId());
        customer.setFirstName(request.firstName());
        customer.setLastName(request.lastName());
        customer.setEmail(request.email());
        customerRepository.save(customer);
        return customerMapper.fromCustomer(customer);
    }

    public boolean isEmailTaken(String email) {
        return customerRepository.existsByEmail(email);
    }


    @Transactional
    public void verifyKyc(Long customerId) {
        Customer customer = findCustomerById(customerId);
        customer.setKycVerified(true);
        customerRepository.save(customer);
    }

    public void editCustomerPassword(PasswordUpdateRequest request) {
        Long id = request.customerId();
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found with ID " + id));

        boolean matches = passwordEncoder.matches(request.oldPassword(), customer.getPassword());

        if (!matches) {
            throw new IllegalArgumentException("Old password does not match");
        }

        customer.setPassword(passwordEncoder.encode(request.newPassword()));
    }

    @Transactional
    public void deleteCustomerById(Long id) {
        validateCustomerExistsOrThrow(id);
        customerRepository.deleteById(id);
    }


}
