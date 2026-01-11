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


    @GetMapping("/{id}")
    public ResponseEntity<CustomerResponse> getCustomerById(@PathVariable Long id) {
        CustomerResponse response = service.getCustomerInfo(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<CustomerInfo> getMyProfile(@AuthenticationPrincipal JwtUserPrincipal principal) {

        CustomerResponse resp = service.getMyCustomerInfoById(principal.customerId());
        CustomerInfo info = mapper.getInfoFromCustomer(resp);

        return ResponseEntity.ok(info);
    }

    @GetMapping("/get/{email}")
    public ResponseEntity<CustomerResponse> getCustomerByEmail(
            HttpServletRequest request,
            @PathVariable @Email String email) {

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String jwtToken = authHeader.substring(7);
        CustomerResponse resp = service.getCustomerInfoByEmail(email, jwtToken);
        return ResponseEntity.ok(resp);
    }


    @GetMapping("/user/{userId}")
    public ResponseEntity<CustomerResponse> getCustomerByUserId(@PathVariable Long userId) {
        CustomerResponse response = service.getCustomerInfoByUserId(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/get/id/user/{userId}")
    public ResponseEntity<Long> getCustomerIdByUserId(@PathVariable Long userId) {
        Long id = service.getCustomerIdByUserId(userId);
        return ResponseEntity.ok(id);
    }

    @GetMapping("/get/all")
    public ResponseEntity<List<Customer>> getAllCustomersInfo() {
        List<Customer> responseList = service.findAllCustomers();
        return ResponseEntity.ok(responseList);
    }

    @PutMapping("/update")
    public ResponseEntity<CustomerResponse> updateCustomer(@RequestBody CustomerUpdateRequest request) {
        CustomerResponse updated = service.editCustomerInfo(request);
        return ResponseEntity.ok(updated);
    }


    @PatchMapping("/{id}/verify-kyc")
    public ResponseEntity<String> verifyKyc(@PathVariable Long id) {
        service.verifyKyc(id);
        return ResponseEntity.ok("KYC verified successfully");
    }

    @PatchMapping("/{id}/unverify-kyc")
    public ResponseEntity<String> unVerifyKyc(@PathVariable Long id) {
        service.unVerifyKyc(id);
        return ResponseEntity.ok("KYC unverified successfully");
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteCustomer(@PathVariable Long id) {
        service.deleteCustomerById(id);
        return ResponseEntity.ok("Customer deleted successfully");
    }

    @DeleteMapping("/all/delete")
    public ResponseEntity<String> deleteAllCustomers() {
        service.deleteAllCustomers();
        return ResponseEntity.ok("Customers deleted successfully");
    }
}
