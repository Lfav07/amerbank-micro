package com.amerbank.account.controller;

import com.amerbank.account.dto.*;
import com.amerbank.account.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for internal service-to-service account operations.
 */
@RestController
@RequestMapping("/account/internal")
@RequiredArgsConstructor
@Validated
public class InternalAccountController {

    private final AccountService accountService;


    /**
     * Checks if the specified account number belongs to a customer.
     *
     * @param request the verification request containg the customer id and account number
     * @return true if the account belongs to the current customer, false otherwise
     */
    @PostMapping("/owned")
    public ResponseEntity<Boolean> isAccountOwnedByCustomer(
            @RequestBody ServiceAccountOwnedRequest request) {

        return ResponseEntity.ok(accountService.isAccountOwnedByCustomer(
                request.customerId(), request.accountNumber()));
    }

    /**
     * Deposits funds into a customer's account.
     * Used by internal services such as payment processing or loan disbursement services.
     *
     * @param request the deposit request containing customer ID, account number, and amount
     * @return 200 OK if deposit is successful
     */
    @PostMapping("/deposit")
    public ResponseEntity<Void> performDeposit(
            @Valid @RequestBody ServiceDepositBalanceRequest request) {

        accountService.performDeposit(
                request.customerId(),
                new DepositBalanceRequest(
                        request.accountNumber(),
                        request.amount()
                )
        );
        return ResponseEntity.ok().build();
    }

    /**
     * Performs a payment between two accounts.
     * Used by internal services such as bill payment or transfer services.
     *
     * @param request the payment request containing customer ID, source account, destination account, and amount
     * @return 200 OK if payment is successful
     */
    @PostMapping("/payment")
    public ResponseEntity<Void> performPayment(
            @Valid @RequestBody ServicePaymentRequest request) {

        accountService.performPayment(
                request.customerId(),
                new PaymentBalanceRequest(
                        request.fromAccountNumber(),
                        request.toAccountNumber(),
                        request.amount()
                )
        );
        return ResponseEntity.ok().build();
    }

    /**
     * Performs a refund between two accounts.
     * Used by internal services to reverse transactions or process refunds.
     *
     * @param request the refund request containing customer ID, source account, destination account, and amount
     * @return 200 OK if refund is successful
     */
    @PostMapping("/refund")
    public ResponseEntity<Void> performRefund(
            @Valid @RequestBody ServiceRefundBalanceRequest request) {

        accountService.performRefund(
                request.customerId(),
                new RefundBalanceRequest(
                        request.fromAccountNumber(),
                        request.toAccountNumber(),
                        request.amount()
                )
        );
        return ResponseEntity.ok().build();
    }
}