package com.amerbank.loan.client;

import java.math.BigDecimal;

public interface AccountServiceClientInterface {
    void deposit(Long customerId, String accountNumber, BigDecimal amount);
    void withdraw(Long customerId, String accountNumber, BigDecimal amount);
    void payment(Long customerId, String fromAccountNumber, String toAccountNumber, BigDecimal amount);
    boolean isAccountOwned(Long customerId, String accountNumber);
}
