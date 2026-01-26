package com.amerbank.account.service;

import com.amerbank.account.dto.AccountInfo;
import com.amerbank.account.dto.AccountRequest;
import com.amerbank.account.dto.AccountResponse;
import com.amerbank.account.model.Account;
import com.amerbank.account.model.AccountStatus;
import org.springframework.stereotype.Component;

@Component
public class AccountMapper {

    public Account toAccount(AccountRequest request) {
        Account account = new Account();
        account.setStatus(AccountStatus.ACTIVE);
        account.setType(request.type());
        return account;
    }

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


    public AccountInfo getAccountInfo(AccountResponse response) {
        return new AccountInfo(
                response.id(),
                response.accountNumber(),
                response.balance(),
                response.type(),
                response.status()
        );
    }
}