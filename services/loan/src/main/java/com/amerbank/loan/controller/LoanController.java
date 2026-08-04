package com.amerbank.loan.controller;

import com.amerbank.loan.dto.LoanInfo;
import com.amerbank.loan.dto.request.LoanApplicationRequest;
import com.amerbank.loan.dto.request.LoanRepaymentRequest;
import com.amerbank.loan.dto.response.LoanPaymentResponse;
import com.amerbank.loan.dto.response.LoanResponse;
import com.amerbank.loan.security.JwtUserPrincipal;
import com.amerbank.loan.service.LoanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/loan")
@RequiredArgsConstructor
@Tag(name = "Loans", description = "Loan management endpoints for authenticated customers")
public class LoanController {

    private final LoanService loanService;

    private static final String JWT_SCHEME = "Bearer JWT";

    @Operation(
            summary = "Apply for a new loan",
            description = "Creates a new loan application for the authenticated customer.",
            security = @SecurityRequirement(name = JWT_SCHEME)
    )
    @PostMapping("/apply")
    public ResponseEntity<LoanResponse> applyForLoan(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestBody @Valid LoanApplicationRequest request) {
        LoanResponse response = loanService.applyForLoan(request, principal.customerId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Get all loans for authenticated customer",
            description = "Retrieves all loans belonging to the authenticated customer.",
            security = @SecurityRequirement(name = JWT_SCHEME)
    )
    @GetMapping("/me")
    public ResponseEntity<List<LoanInfo>> getMyLoans(
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        List<LoanInfo> loans = loanService.getMyLoans(principal.customerId());
        return ResponseEntity.ok(loans);
    }

    @Operation(
            summary = "Get specific loan details",
            description = "Retrieves details of a specific loan by loan number.",
            security = @SecurityRequirement(name = JWT_SCHEME)
    )
    @GetMapping("/me/{loanNumber}")
    public ResponseEntity<LoanResponse> getMyLoanByNumber(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable String loanNumber) {
        LoanResponse response = loanService.getMyLoanByNumber(principal.customerId(), loanNumber);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get payment schedule for a loan",
            description = "Retrieves the payment schedule for a specific loan.",
            security = @SecurityRequirement(name = JWT_SCHEME)
    )
    @GetMapping("/me/{loanNumber}/payments")
    public ResponseEntity<List<LoanPaymentResponse>> getLoanPayments(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable String loanNumber) {
        List<LoanPaymentResponse> payments = loanService.getLoanPayments(principal.customerId(), loanNumber);
        return ResponseEntity.ok(payments);
    }

    @Operation(
            summary = "Make a loan repayment",
            description = "Processes a repayment for a specific loan.",
            security = @SecurityRequirement(name = JWT_SCHEME)
    )
    @PostMapping("/repay")
    public ResponseEntity<Void> makeRepayment(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestBody @Valid LoanRepaymentRequest request) {
        loanService.makeRepayment(principal.customerId(), request);
        return ResponseEntity.ok().build();
    }
}