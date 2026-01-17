package com.amerbank.customer.customer.controller;

import com.amerbank.customer.customer.dto.CustomerInfo;
import com.amerbank.customer.customer.dto.CustomerRequest;
import com.amerbank.customer.customer.dto.CustomerResponse;
import com.amerbank.customer.customer.dto.CustomerUpdateRequest;
import com.amerbank.customer.customer.model.Customer;
import com.amerbank.customer.customer.security.JwtUserPrincipal;
import com.amerbank.customer.customer.service.CustomerMapper;
import com.amerbank.customer.customer.service.CustomerService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RequiredArgsConstructor
@RestController
@RequestMapping("/customer")
public class CustomerController {

    private final CustomerService service;
    private final CustomerMapper mapper;

    @PostMapping("/register")
    public ResponseEntity<?> registerCustomer(@RequestBody CustomerRequest request) {
        service.registerCustomer(request);
        return ResponseEntity.ok("Customer successfully registered");
    }



    @GetMapping("/me")
    public ResponseEntity<CustomerInfo> getMyProfile(@AuthenticationPrincipal JwtUserPrincipal principal) {
        CustomerInfo resp = service.getMyCustomerInfo(principal.customerId());
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/internal/by-user/{userId}")
    public ResponseEntity<Long> getCustomerIdByUserId(@PathVariable Long userId) {
        Long id = service.getCustomerIdByUserId(userId);
        return ResponseEntity.ok(id);
    }
}
