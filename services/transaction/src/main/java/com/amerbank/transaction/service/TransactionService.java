package com.amerbank.transaction.service;

import com.amerbank.common_dto.DepositBalanceRequest;
import com.amerbank.common_dto.PaymentBalanceRequest;
import com.amerbank.transaction.dto.DepositTransactionRequest;
import com.amerbank.transaction.dto.PaymentTransactionRequest;
import com.amerbank.transaction.dto.TransactionResponse;
import com.amerbank.transaction.exception.AccountServiceUnavailableException;
import com.amerbank.transaction.exception.TransactionNotFoundException;
import com.amerbank.transaction.model.Transaction;
import com.amerbank.transaction.model.TransactionStatus;
import com.amerbank.transaction.model.TransactionType;
import com.amerbank.transaction.security.JwtService;
import com.amerbank.transaction.repository.TransactionRepository;
import lombok.AllArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;
    private final RestTemplate restTemplate;
    private final JwtService jwtService;

    public Transaction findTransactionById(UUID id) {
        return  transactionRepository.findById(id)
                .orElseThrow(() -> new TransactionNotFoundException("Transaction not found"));
    }

    public List<Transaction> findTransactionsByFromAccountNumber(String fromAccount) {
        return transactionRepository.findByFromAccountNumber(fromAccount);
    }

    public List<Transaction> findTransactionsByToAccountNumber(String toAccount) {
        return transactionRepository.findByToAccountNumber(toAccount);
    }

    public  List<Transaction> findTransactionsByFromAndToAccountNumber(String fromAccount, String toAccount) {
        return  transactionRepository.findByFromAccountNumberAndToAccountNumber(fromAccount, toAccount);
    }

    public  List<Transaction> findTransactionsByStatus(TransactionStatus status) {
        return  transactionRepository.findByStatus(status);
    }
    public  List<Transaction> findTransactionsByType(TransactionType type) {
        return  transactionRepository.findByType(type);
    }

    public  List<Transaction> findByFromAccountNumberAndStatus(String fromAccount, TransactionStatus status){
        return  transactionRepository.findByFromAccountNumberAndStatus(fromAccount, status);
    }

    public  List<Transaction> findByFromAccountNumberAndType(String fromAccount, TransactionType type){
        return  transactionRepository.findByFromAccountNumberAndType(fromAccount, type);
    }

    public TransactionResponse createDepositTransaction(String jwtToken, DepositTransactionRequest request){

        if (!isAccountOwnedByCurrentCustomer(jwtToken, request.fromAccountNumber())) {
            throw new RuntimeException("Account does not belong to current user");
        }


        Transaction transaction = transactionMapper.toTransaction(request);


        performDeposit(jwtToken, request.toAccountNumber(), request.amount());
        transactionRepository.save(transaction);
        return transactionMapper.toResponse(transaction);
    }
    public TransactionResponse createPaymentTransaction(String jwtToken, PaymentTransactionRequest request){
        if (!isAccountOwnedByCurrentCustomer(jwtToken, request.fromAccountNumber())) {
            throw new RuntimeException("Account does not belong to current user");
        }

        Transaction transaction = transactionMapper.toTransaction(request);

        performPayment(jwtToken, request.fromAccountNumber(), request.toAccountNumber(), request.amount());
        transactionRepository.save(transaction);
        return transactionMapper.toResponse(transaction);

    }



    public boolean isAccountOwnedByCurrentCustomer(String jwtToken, String accountNumber) {
        String url = "http://account-service/account/manage/owned?accountNumber=" + accountNumber;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<Boolean> response;
        try {
            response = restTemplate.exchange(url, HttpMethod.GET, entity, Boolean.class);
        } catch (RestClientException e) {
            throw new AccountServiceUnavailableException("Account service unavailable");
        }

        Boolean owned = response.getBody();
        return owned != null && owned;
    }

    private void performDeposit(String jwtToken, String accountNumber, BigDecimal amount) {
        String url = "http://account-service/account/manage/deposit";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Create request body (DTO)
        DepositBalanceRequest body = new DepositBalanceRequest(accountNumber, amount);
        HttpEntity<DepositBalanceRequest> entity = new HttpEntity<>(body, headers);

        try {
            restTemplate.exchange(url, HttpMethod.POST, entity, Void.class);
        } catch (RestClientException e) {
            throw new AccountServiceUnavailableException("Failed to update account balance");
        }
    }

    private void performPayment(String jwtToken, String fromAccountNumber, String toAccountNumber, BigDecimal amount) {
        String url = "http://account-service/account/manage/payment";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Create request body (DTO)
        PaymentBalanceRequest body = new PaymentBalanceRequest(fromAccountNumber, toAccountNumber, amount);
        HttpEntity<PaymentBalanceRequest> entity = new HttpEntity<>(body, headers);

        try {
            restTemplate.exchange(url, HttpMethod.POST, entity, Void.class);
        } catch (RestClientException e) {
            throw new AccountServiceUnavailableException("Failed to update account balance");
        }
    }

}
