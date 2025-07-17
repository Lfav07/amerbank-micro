package com.amerbank.account.repository;

import com.amerbank.account.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID> {
    boolean existsByAccountNumber(String accountNumber);
    Optional<Account> findByAccountNumber(String accountNumber);
    Optional<Account> findByCustomerId(Long CustomerID);

    List<Account> findAllByCustomerId(UUID customerId);
}
