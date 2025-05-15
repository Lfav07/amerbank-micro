package com.amerbank.customer.customer;

import com.amerbank.customer.customer.dto.CustomerRequest;
import com.amerbank.customer.customer.dto.CustomerResponse;
import com.amerbank.customer.customer.dto.CustomerUpdateRequest;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class CustomerService {

    private final  CustomerRepository customerRepository;
    private  final CustomerMapper customerMapper;

    public  Customer findCustomerById(Long id) {
        return customerRepository.findById(id).orElseThrow(() -> new CustomerNotFoundException("Customer not found with ID " + id));
    }

    public  Customer findCustomerByEmail(String email) {
        return customerRepository.findCustomerByEmail(email).orElseThrow(() -> new CustomerNotFoundException("Customer not found with Email " + email));
    }

    public void registerCustomer(CustomerRequest customerRequest) {
        customerRepository.save(customerMapper.toCustomer(customerRequest));
    }

    public CustomerResponse getCustomerInfo(Long id) {
        return customerMapper.fromCustomer(findCustomerById(id));
    }

    @Transactional
    public CustomerResponse editCustomerInfo(CustomerUpdateRequest request) {
        Customer customer = findCustomerById(request.customerId());
        customer.setFirstName(request.firstName());
        customer.setLastName(request.lastname());
        customer.setEmail(request.email());
        customerRepository.save(customer);
        return customerMapper.fromCustomer(customer);

    }


}
