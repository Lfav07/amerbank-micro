package com.amerbank.customer.customer.controller;

import com.amerbank.customer.customer.dto.CustomerResponse;
import com.amerbank.customer.customer.dto.CustomerUpdateRequest;
import com.amerbank.customer.customer.security.JwtUserPrincipal;
import com.amerbank.customer.customer.service.CustomerService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/customer/admin")
public class CustomerAdminController {
    private final CustomerService service;

    // -------------------- Helper --------------------
    private Map<String, String> message(String msg) {
        return Map.of("message", msg);
    }


    @GetMapping("/customers")
    public ResponseEntity<List<CustomerResponse>> getAllCustomers() {
        List<CustomerResponse> responseList = service.findAllCustomers();
        return ResponseEntity.ok(responseList);
    }

    @GetMapping("/customers/{id}")
    public ResponseEntity<CustomerResponse> getCustomerById(@PathVariable Long id) {
        CustomerResponse response = service.getCustomerInfo(id);
        return ResponseEntity.ok(response);
    }


    @GetMapping("/customers/by-email")
    public ResponseEntity<CustomerResponse> getCustomerByEmail(@RequestParam @Email String email) {
        CustomerResponse resp = service.getCustomerInfoByEmail(email);
        return ResponseEntity.ok(resp);
    }


    @PutMapping("/customers/{id}")
    public ResponseEntity<Map<String, String>> updateCustomer(
            @PathVariable Long id,
            @RequestBody @Valid CustomerUpdateRequest request) {
         service.editCustomerInfo(id, request);
        return ResponseEntity.ok(message("Successfully updated customer's data for customer " + id));
    }


    @PatchMapping("/customers/{id}/kyc/verify")
    public ResponseEntity<Map<String, String>> verifyKyc(@PathVariable Long id) {
        service.verifyKyc(id);
        return ResponseEntity.ok(message("KYC verified successfully"));
    }

    @PatchMapping("/customers/{id}/kyc/revoke")
    public ResponseEntity<Map<String, String>> unVerifyKyc(@PathVariable Long id) {
        service.revokeKyc(id);
        return ResponseEntity.ok(message("KYC unverified successfully"));
    }


    @DeleteMapping("/customers/{id}")
    public ResponseEntity<Map<String, String>> deleteCustomer(@PathVariable Long id) {
        service.deleteCustomerById(id);
        return ResponseEntity.ok(message("Successfully deleted customer " + id));
    }
}
