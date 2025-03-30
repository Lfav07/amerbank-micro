package com.amerbank.customer.customer;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Customer findCustomerByEmail(String email);
   Customer findCustomerByFirstName(String firstname);
   Customer findCustomerByLastName(String lastname);
   Customer findCustomerByFirstNameAndLastName(String firstname, String lastname);

}
