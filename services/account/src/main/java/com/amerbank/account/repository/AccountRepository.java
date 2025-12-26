package com.amerbank.account.repository;

import com.amerbank.account.model.Account;
import com.amerbank.account.model.AccountType;
import jakarta.persistence.LockModeType;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;


import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID> {
    boolean existsByAccountNumber(String accountNumber);

    Optional<Account> findByAccountNumber(String accountNumber);


    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.accountNumber = :accountNumber")
    Optional<Account> findByAccountNumberForUpdate(String accountNumber);

    List<Account> findAllByCustomerId(Long customerId);

    boolean existsByCustomerIdAndType(Long customerId, @NotNull AccountType type);

    Optional<Account> findByCustomerIdAndType(Long customerId, AccountType type);
}
