package com.amerbank.account.service;

import com.amerbank.account.dto.AccountInfo;
import com.amerbank.account.dto.AccountRequest;
import com.amerbank.account.dto.AccountResponse;
import com.amerbank.account.model.Account;
import com.amerbank.account.model.AccountStatus;
import org.springframework.stereotype.Component;

@Component
public class AccountMapper {


    public AccountResponse toResponse(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getCustomerId(),
                account.getBalance(),
                account.getType(),
                account.getStatus()
        );
    }
    public AccountInfo getAccountInfoFromAccount(Account account) {
        return new AccountInfo(
                account.getId(),
                account.getAccountNumber(),
                account.getBalance(),
                account.getType(),
                account.getStatus()
        );
    }
}