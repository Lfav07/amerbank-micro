package com.amerbank.account.service;

import com.amerbank.account.dto.AccountInfo;
import com.amerbank.account.dto.response.AccountResponse;
import com.amerbank.account.model.Account;
import org.springframework.stereotype.Component;

@Component
public class AccountMapper {


    /**
     * Maps an Account entity to an AccountResponse DTO.
     *
     * @param account the account entity to map.
     * @return the mapped account response DTO.
     */
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
    /**
     * Maps an Account entity to an AccountInfo DTO.
     *
     * @param account the account entity to map.
     * @return the mapped account info DTO.
     */
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