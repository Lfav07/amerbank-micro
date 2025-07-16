package com.amerbank.account.service;

import com.amerbank.account.dto.AccountInfo;
import com.amerbank.account.dto.AccountRequest;
import com.amerbank.account.dto.AccountResponse;
import com.amerbank.account.model.Account;
import org.springframework.stereotype.Component;

@Component
public class AccountMapper {

    public Account toAccount(AccountRequest request) {
        Account account = new Account();
        account.setType(request.type());
        account.setStatus(request.status());
        return account;
    }

    public AccountResponse fromAccount(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getCustomerId(),
                account.getBalance(),
                account.getType(),
                account.getStatus()
        );
    }
    public AccountInfo getAccountInfo(AccountResponse response) {
        return  new AccountInfo(
                response.id(),
                response.accountNumber(),
                response.balance(),
                response.type(),
                response.status()
        );
    }
}