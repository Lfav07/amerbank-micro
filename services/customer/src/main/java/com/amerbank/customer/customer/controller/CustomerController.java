package com.amerbank.customer.customer.controller;

import com.amerbank.customer.customer.dto.CustomerInfo;
import com.amerbank.customer.customer.dto.CustomerRegistrationRequest;
import com.amerbank.customer.customer.dto.CustomerRegistrationResponse;
import com.amerbank.customer.customer.dto.CustomerRequest;
import com.amerbank.customer.customer.security.JwtUserPrincipal;
import com.amerbank.customer.customer.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/customer")
public class CustomerController {

    private final CustomerService service;


    @GetMapping("/me")
    public ResponseEntity<CustomerInfo> getMyProfile(@AuthenticationPrincipal JwtUserPrincipal principal) {
        CustomerInfo resp = service.getMyCustomerInfo(principal.customerId());
        return ResponseEntity.ok(resp);
    }


}
