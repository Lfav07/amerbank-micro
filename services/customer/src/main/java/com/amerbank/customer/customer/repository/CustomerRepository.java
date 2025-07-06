package com.amerbank.customer.customer.repository;

import com.amerbank.customer.customer.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {



    boolean existsByUserId(Long id);


    Optional<Customer> findByUserId(Long userId);
}
