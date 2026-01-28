package com.amerbank.account.exception;

import com.amerbank.account.model.AccountType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class AccountAlreadyExistsException extends RuntimeException {

    private final Long customerId;
    private final AccountType accountType;

    public AccountAlreadyExistsException(Long customerId, AccountType accountType) {
        super("Customer %d already has an account of type %s"
                .formatted(customerId, accountType));
        this.customerId = customerId;
        this.accountType = accountType;
    }

}
