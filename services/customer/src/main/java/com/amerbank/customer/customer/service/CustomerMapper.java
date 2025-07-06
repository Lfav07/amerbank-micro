package com.amerbank.customer.customer.service;

import com.amerbank.customer.customer.dto.CustomerRequest;
import com.amerbank.customer.customer.dto.CustomerResponse;
import com.amerbank.customer.customer.model.Customer;
import org.springframework.stereotype.Component;

@Component
public class CustomerMapper {

    public Customer toCustomer(CustomerRequest request, Long userId) {
        if (request == null) {
            return null;
        }

        Customer customer = new Customer();
        customer.setUserId(userId);
        customer.setFirstName(request.firstName());
        customer.setLastName(request.lastName());
        customer.setDateOfBirth(request.dateOfBirth());
        customer.setKycVerified(false);
        return customer;
    }

    public CustomerResponse fromCustomer(Customer customer) {
        if (customer == null) {
            return null;
        }

        return new CustomerResponse(
                customer.getId(),
                customer.getUserId(),
                customer.getFirstName(),
                customer.getLastName(),
                customer.getDateOfBirth(),
                customer.isKycVerified(),
                customer.getCreatedAt()
        );
    }
}
