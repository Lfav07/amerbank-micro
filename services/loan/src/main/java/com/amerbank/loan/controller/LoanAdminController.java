package com.amerbank.loan.controller;

import com.amerbank.loan.dto.request.LoanDecisionRequest;
import com.amerbank.loan.dto.response.LoanResponse;
import com.amerbank.loan.service.LoanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/loan/admin")
@RequiredArgsConstructor
@Tag(name = "Admin Loan Management", description = "Administrative endpoints for loan management. Requires ADMIN role.")
public class LoanAdminController {

    private final LoanService loanService;

    private static final String JWT_SCHEME = "Bearer JWT";

    @Operation(
            summary = "Get all loans",
            description = "Retrieves all loans (paginated).",
            security = @SecurityRequirement(name = JWT_SCHEME)
    )
    @GetMapping("/all")
    public ResponseEntity<List<LoanResponse>> getAllLoans() {
        List<LoanResponse> loans = loanService.getAllLoans();
        return ResponseEntity.ok(loans);
    }

    @Operation(
            summary = "Get specific loan by number",
            description = "Retrieves a specific loan by loan number.",
            security = @SecurityRequirement(name = JWT_SCHEME)
    )
    @GetMapping("/{loanNumber}")
    public ResponseEntity<LoanResponse> getLoanByNumber(@PathVariable String loanNumber) {
        LoanResponse response = loanService.getLoanByNumber(loanNumber);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Approve a pending loan",
            description = "Approves a pending loan application.",
            security = @SecurityRequirement(name = JWT_SCHEME)
    )
    @PostMapping("/approve")
    public ResponseEntity<LoanResponse> approveLoan(@RequestBody @Valid LoanDecisionRequest request) {
        LoanResponse response = loanService.approveLoan(request.loanNumber());
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Reject a pending loan",
            description = "Rejects a pending loan application with a reason.",
            security = @SecurityRequirement(name = JWT_SCHEME)
    )
    @PostMapping("/reject")
    public ResponseEntity<LoanResponse> rejectLoan(@RequestBody @Valid LoanDecisionRequest request) {
        LoanResponse response = loanService.rejectLoan(request.loanNumber(), request.rejectionReason());
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Disburse an approved loan",
            description = "Disburses funds for an approved loan.",
            security = @SecurityRequirement(name = JWT_SCHEME)
    )
    @PostMapping("/disburse")
    public ResponseEntity<LoanResponse> disburseLoan(@RequestBody @Valid LoanDecisionRequest request) {
        LoanResponse response = loanService.disburseLoan(request.loanNumber());
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get all loans for a customer",
            description = "Retrieves all loans for a specific customer.",
            security = @SecurityRequirement(name = JWT_SCHEME)
    )
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<LoanResponse>> getLoansByCustomerId(@PathVariable Long customerId) {
        List<LoanResponse> loans = loanService.getLoansByCustomerId(customerId);
        return ResponseEntity.ok(loans);
    }
}