package com.amerbank.loan.controller;

import com.amerbank.loan.dto.request.ServiceDisbursementRequest;
import com.amerbank.loan.dto.request.ServiceRepaymentRequest;
import com.amerbank.loan.dto.response.LoanResponse;
import com.amerbank.loan.service.LoanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/loan/internal")
@RequiredArgsConstructor
@Tag(name = "Internal Loan Service", description = "Internal endpoints for service-to-service communication. Requires service JWT token.")
public class InternalLoanController {

    private final LoanService loanService;

    @Operation(
            summary = "Get loan details by number",
            description = "Retrieves loan details by loan number for internal service use."
    )
    @GetMapping("/{loanNumber}")
    public ResponseEntity<LoanResponse> getLoanByNumber(@PathVariable String loanNumber) {
        LoanResponse response = loanService.getLoanByNumber(loanNumber);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Disburse loan funds to account",
            description = "Processes loan disbursement internally."
    )
    @PostMapping("/disburse")
    public ResponseEntity<Void> disburseLoan(@RequestBody @Valid ServiceDisbursementRequest request) {
        loanService.processDisbursement(request.customerId(), request.loanNumber(), request.amount(), request.accountNumber());
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Process loan repayment",
            description = "Processes loan repayment internally."
    )
    @PostMapping("/repay")
    public ResponseEntity<Void> processRepayment(@RequestBody @Valid ServiceRepaymentRequest request) {
        loanService.processRepayment(request.loanNumber(), request.amount());
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Check if customer has active loans",
            description = "Returns true if customer has active loans."
    )
    @GetMapping("/customer/{customerId}/active")
    public ResponseEntity<Boolean> hasActiveLoans(@PathVariable Long customerId) {
        boolean hasActive = loanService.hasActiveLoans(customerId);
        return ResponseEntity.ok(hasActive);
    }
}