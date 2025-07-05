package com.amerbank.customer.customer.service;

import com.amerbank.customer.customer.dto.CustomerRequest;
import com.amerbank.customer.customer.dto.CustomerResponse;
import com.amerbank.customer.customer.model.Customer;
import org.springframework.stereotype.Component;

@Component
public class CustomerMapper {

    public Customer toCustomer(CustomerRequest request) {
        if (request == null) {
            return null;
        }
        return new Customer(
                request.firstName(),
                request.lastName(),
                request.password(),
                request.email(),
                request.dateOfBirth()
        );
    }

    public CustomerResponse fromCustomer(Customer customer) {
        if (customer == null) {
            return null;
        }
        return new CustomerResponse(
                customer.getId(),
                customer.getFirstName(),
                customer.getLastName(),
                customer.getPassword(),
                customer.getEmail(),
                customer.getDateOfBirth(),
                customer.isKycVerified()
        );
    }


}
