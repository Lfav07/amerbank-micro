package com.amerbank.transaction.service;

import com.amerbank.common_dto.*;
import com.amerbank.transaction.dto.DepositTransactionRequest;
import com.amerbank.transaction.dto.PaymentTransactionRequest;
import com.amerbank.transaction.dto.RefundTransactionRequest;
import com.amerbank.transaction.dto.TransactionResponse;
import com.amerbank.transaction.exception.*;
import com.amerbank.transaction.model.Transaction;
import com.amerbank.transaction.model.TransactionStatus;
import com.amerbank.transaction.model.TransactionType;
import com.amerbank.transaction.security.JwtService;
import com.amerbank.transaction.repository.TransactionRepository;
import lombok.AllArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
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

    public  List<Transaction> findByFromAccountNumberOrToAccountNumber(String accountNumber){
        return  transactionRepository.findByFromAccountNumberOrToAccountNumber(accountNumber, accountNumber);
    }

    public  List<Transaction> findByFromAccountNumberAndType(String fromAccount, TransactionType type){
        return  transactionRepository.findByFromAccountNumberAndType(fromAccount, type);
    }

    public List<Transaction> getMyTransactions(String jwtToken, String accountNumber) {

        return  findByFromAccountNumberOrToAccountNumber(accountNumber);

    }

    public TransactionResponse createDepositTransaction(String jwtToken, DepositTransactionRequest request){



        Long customerId = jwtService.extractCustomerId(jwtToken);

        Transaction transaction = transactionMapper.toTransaction(request);

        performDeposit(customerId, request.toAccountNumber(), request.amount());
        transaction.setStatus(TransactionStatus.APPROVED);
        transactionRepository.save(transaction);
        return transactionMapper.toResponse(transaction);
    }
    public TransactionResponse createPaymentTransaction(String jwtToken, PaymentTransactionRequest request){

        Transaction transaction = transactionMapper.toTransaction(request);
        Long customerId = jwtService.extractCustomerId(jwtToken);

        performPayment(customerId, request.fromAccountNumber(), request.toAccountNumber(), request.amount());
        transaction.setStatus(TransactionStatus.APPROVED);
        transactionRepository.save(transaction);
        return transactionMapper.toResponse(transaction);

    }

    public TransactionResponse createRefundTransaction(String jwtToken, RefundTransactionRequest request) {
        Transaction originalTransaction = findTransactionById(request.transactionId());



        if (originalTransaction.getStatus() == TransactionStatus.REVERSED) {
            throw new TransactionAlreadyRefundedException("Transaction already refunded");
        }

        Long customerId = jwtService.extractCustomerId(jwtToken);

        // Create refund transaction
        Transaction transaction = new Transaction();
        transaction.setAmount(originalTransaction.getAmount());
        transaction.setFromAccountNumber(originalTransaction.getToAccountNumber());
        transaction.setToAccountNumber(originalTransaction.getFromAccountNumber());
        transaction.setType(TransactionType.REFUND);
        transaction.setStatus(TransactionStatus.WAITING);

        // Perform the refund (reverse the direction)
        performRefund(customerId,
                transaction.getFromAccountNumber(),
                transaction.getToAccountNumber(),
                transaction.getAmount());


        originalTransaction.setStatus(TransactionStatus.REVERSED);
        transaction.setStatus(TransactionStatus.APPROVED);

        transactionRepository.save(transaction);
        transactionRepository.save(originalTransaction);

        return transactionMapper.toResponse(transaction);
    }







    private void performDeposit(Long customerId, String accountNumber, BigDecimal amount) {
        String url = "http://account/accounts/internal/deposit";

        HttpHeaders headers = new HttpHeaders();
        String serviceToken = jwtService.generateServiceToken();
        headers.setBearerAuth(serviceToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Create request body (DTO)
        ServiceDepositBalanceRequest body = new ServiceDepositBalanceRequest(customerId, accountNumber, amount);
        HttpEntity<ServiceDepositBalanceRequest> entity = new HttpEntity<>(body, headers);

        try {
            restTemplate.exchange(url, HttpMethod.POST, entity, Void.class);
        } catch (HttpClientErrorException e) {
            throw new DepositFailedException("Deposit rejected:" + e.getStatusCode());
        } catch (RestClientException e) {
            throw new DepositFailedException("Could not reach account service");
        }
    }

    private void performPayment(Long customerId, String fromAccountNumber, String toAccountNumber, BigDecimal amount) {
        String url = "http://account/accounts/internal/payment";
        String serviceToken = jwtService.generateServiceToken();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(serviceToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Create request body (DTO)
        ServicePaymentRequest body = new ServicePaymentRequest(customerId, fromAccountNumber, toAccountNumber, amount);
        HttpEntity<ServicePaymentRequest> entity = new HttpEntity<>(body, headers);

        try {
            restTemplate.exchange(url, HttpMethod.POST, entity, Void.class);
        } catch (HttpClientErrorException e) {
           // log.error("Account service rejected payment: {}", e.getResponseBodyAsString());
            throw new PaymentFailedException("Payment rejected: " + e.getStatusCode());
        } catch (RestClientException e) {
          //  log.error("Error communicating with account service", e);
            throw new PaymentFailedException("Could not contact account service");
        }
    }

    private void performRefund(Long customerId,String fromAccountNumber, String toAccountNumber, BigDecimal amount) {
        String url = "http://account/accounts/internal/refund";

        HttpHeaders headers = new HttpHeaders();
        String serviceToken = jwtService.generateServiceToken();
        headers.setBearerAuth(serviceToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Create request body (DTO)
        ServiceRefundBalanceRequest body = new ServiceRefundBalanceRequest(customerId, toAccountNumber,  fromAccountNumber, amount);
        HttpEntity<ServiceRefundBalanceRequest> entity = new HttpEntity<>(body, headers);

        try {
            restTemplate.exchange(url, HttpMethod.POST, entity, Void.class);
        } catch (HttpClientErrorException e) {
            throw new RefundFailedException("Refund rejected:" + e.getStatusCode());
        } catch (RestClientException e) {
            throw new RefundFailedException("Could not reach account service");
        }
    }
}
