package com.amerbank.customer.customer.controller;

import com.amerbank.common_dto.AuthenticationResponse;
import com.amerbank.common_dto.UserLoginRequest;
import com.amerbank.customer.customer.dto.CustomerRequest;
import com.amerbank.customer.customer.model.Customer;
import com.amerbank.customer.customer.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/customer")
public class CustomerController {

    private CustomerService service;

    @PostMapping("/register")
    public ResponseEntity<?> registerCustomer(CustomerRequest request) {
        service.registerCustomer(request);
        return  ResponseEntity.ok("Customer successfully registered");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody UserLoginRequest request) {
        try {
            AuthenticationResponse auth = service.login(request);
            return ResponseEntity.ok(auth);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(401).body("Invalid credentials");
        } catch (IllegalStateException e) {
            return ResponseEntity.status(500).body("Auth service unavailable");
        }
    }



}
