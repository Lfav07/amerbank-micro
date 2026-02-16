package com.amerbank.customer.customer.controller;

import com.amerbank.customer.customer.dto.CustomerRegistrationRequest;
import com.amerbank.customer.customer.dto.CustomerRegistrationResponse;
import com.amerbank.customer.customer.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/customer/internal")
public class CustomerInternalController {
        private  final CustomerService service;


    @PostMapping("/register")
    public ResponseEntity<CustomerRegistrationResponse> registerCustomer(@RequestBody @Valid CustomerRegistrationRequest request) {
        CustomerRegistrationResponse response = service.registerCustomer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/by-user/{userId}")
    public ResponseEntity<Long> getCustomerIdByUserId(@PathVariable Long userId) {
        Long id = service.getCustomerIdByUserId(userId);
        return ResponseEntity.ok(id);
    }
}
