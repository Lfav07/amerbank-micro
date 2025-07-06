package com.amerbank.customer.customer.repository;

import com.amerbank.customer.customer.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findCustomerByEmail(String email);

    boolean existsByUserId(Long id);
    boolean existsByEmail(String email);

    Optional<Customer> findByUserId(Long userId);
}
