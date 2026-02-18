package com.amerbank.transaction.service;

import com.amerbank.transaction.config.TransactionProperties;
import com.amerbank.transaction.dto.ServiceAccountOwnedRequest;
import com.amerbank.transaction.dto.ServiceDepositBalanceRequest;
import com.amerbank.transaction.dto.ServicePaymentRequest;
import com.amerbank.transaction.dto.ServiceRefundBalanceRequest;
import com.amerbank.transaction.exception.AccountServiceUnavailableException;
import com.amerbank.transaction.exception.DepositFailedException;
import com.amerbank.transaction.exception.PaymentFailedException;
import com.amerbank.transaction.exception.RefundFailedException;
import com.amerbank.transaction.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.function.Function;

@Service

@Slf4j
public class AccountServiceClient {

    private final RestClient restClient;
    private final JwtService jwtService;
    private final TransactionProperties transactionProperties;

    public AccountServiceClient(RestClient.Builder restClientBuilder, JwtService jwtService, TransactionProperties transactionProperties) {
        this.restClient = restClientBuilder.build();
        this.jwtService = jwtService;
        this.transactionProperties = transactionProperties;
    }

    public boolean isAccountOwned(Long customerId, String accountNumber) {
        String url = transactionProperties.getAccountServiceBase() + transactionProperties.getEndpointOwned();
        String serviceToken = jwtService.generateServiceToken();
        ServiceAccountOwnedRequest requestBody = new ServiceAccountOwnedRequest(customerId, accountNumber);

        try {
            Boolean result = restClient
                    .post()
                    .uri(url)
                    .headers(h -> h.setBearerAuth(serviceToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(Boolean.class);
            log.debug("Successfully verified account ownership - accountNumber: {}, customerId: {}, owned: {}",
                    accountNumber, customerId, result);
            return Boolean.TRUE.equals(result);
        } catch (HttpClientErrorException e) {
            String responseBody = e.getResponseBodyAsString();
            log.error("Account ownership check failed - url: {}, status: {}, accountNumber: {}, customerId: {}, responseBody: {}",
                    url, e.getStatusCode(), accountNumber, customerId, responseBody);
            throw new AccountServiceUnavailableException(
                    String.format("Account ownership check failed. Status: %s, URL: %s", e.getStatusCode(), url)
            );
        } catch (RestClientException e) {
            log.error("Could not reach account service - url: {}, accountNumber: {}, customerId: {}, error: {}",
                    url, accountNumber, customerId, e.getMessage());
            throw new AccountServiceUnavailableException(
                    String.format("Could not reach account service. URL: %s", url)
            );
        }
    }

    public void deposit(Long customerId, String accountNumber, BigDecimal amount) {
        executeAccountCall(
                transactionProperties.getEndpointDeposit(),
                new ServiceDepositBalanceRequest(customerId, accountNumber, amount),
                DepositFailedException::new
        );
    }

    public void payment(Long customerId, String fromAccountNumber, String toAccountNumber, BigDecimal amount) {
        executeAccountCall(
                transactionProperties.getEndpointPayment(),
                new ServicePaymentRequest(customerId, fromAccountNumber, toAccountNumber, amount),
                PaymentFailedException::new
        );
    }

    public void refund(Long customerId, String fromAccountNumber, String toAccountNumber, BigDecimal amount) {
        executeAccountCall(
                transactionProperties.getEndpointRefund(),
                new ServiceRefundBalanceRequest(customerId, fromAccountNumber, toAccountNumber, amount),
                RefundFailedException::new
        );
    }

    private void executeAccountCall(
            String endpoint,
            Object body,
            Function<String, RuntimeException> exceptionFactory
    ) {
        String url = transactionProperties.getAccountServiceBase() + endpoint;
        String serviceToken = jwtService.generateServiceToken();

        try {
            restClient
                    .post()
                    .uri(url)
                    .headers(h -> h.setBearerAuth(serviceToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            log.debug("Successfully executed account call - endpoint: {}, url: {}", endpoint, url);
        } catch (HttpClientErrorException e) {
            String responseBody = e.getResponseBodyAsString();
            log.error("Account service call rejected - endpoint: {}, url: {}, status: {}, responseBody: {}",
                    endpoint, url, e.getStatusCode(), responseBody);
            throw exceptionFactory.apply(
                    String.format("Account service rejected request. Status: %s, URL: %s", e.getStatusCode(), url)
            );
        } catch (RestClientException e) {
            log.error("Account service unavailable - endpoint: {}, url: {}, error: {}", endpoint, url, e.getMessage());
            throw exceptionFactory.apply(
                    String.format("Account service unavailable. URL: %s", url)
            );
        }
    }
}
