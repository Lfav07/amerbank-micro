package com.amerbank.loan.client;

import com.amerbank.loan.config.LoanProperties;
import com.amerbank.loan.exception.AccountServiceUnavailableException;
import com.amerbank.loan.exception.LoanDisbursementFailedException;
import com.amerbank.loan.exception.LoanRepaymentFailedException;
import com.amerbank.loan.security.JwtService;
import java.math.BigDecimal;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
@Slf4j
public class AccountServiceClient implements AccountServiceClientInterface {

    private final RestClient restClient;
    private final JwtService jwtService;
    private final LoanProperties loanProperties;

    public AccountServiceClient(RestClient.Builder restClientBuilder, JwtService jwtService, LoanProperties loanProperties) {
        this.restClient = restClientBuilder.build();
        this.jwtService = jwtService;
        this.loanProperties = loanProperties;
    }

    public void deposit(Long customerId, String accountNumber, BigDecimal amount) {
        executeAccountCall(
                loanProperties.getEndpointDeposit(),
                new ServiceDepositRequest(customerId, accountNumber, amount),
                LoanDisbursementFailedException::new
        );
    }

    public void withdraw(Long customerId, String accountNumber, BigDecimal amount) {
        executeAccountCall(
                loanProperties.getEndpointWithdraw(),
                new ServiceWithdrawRequest(customerId, accountNumber, amount),
                LoanRepaymentFailedException::new
        );
    }

    public void payment(Long customerId, String fromAccountNumber, String toAccountNumber, BigDecimal amount) {
        executeAccountCall(
                loanProperties.getEndpointPayment(),
                new ServicePaymentRequest(customerId, fromAccountNumber, toAccountNumber, amount),
                LoanRepaymentFailedException::new
        );
    }

    public boolean isAccountOwned(Long customerId, String accountNumber) {
        String url = loanProperties.getAccountServiceBase() + loanProperties.getEndpointOwned();
        String serviceToken = jwtService.generateServiceToken();
        ServiceOwnedRequest requestBody = new ServiceOwnedRequest(customerId, accountNumber);

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

    private void executeAccountCall(
            String endpoint,
            Object body,
            Function<String, RuntimeException> exceptionFactory
    ) {
        String url = loanProperties.getAccountServiceBase() + endpoint;
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

    public record ServiceOwnedRequest(Long customerId, String accountNumber) {}
    public record ServiceDepositRequest(Long customerId, String accountNumber, BigDecimal amount) {}
    public record ServiceWithdrawRequest(Long customerId, String accountNumber, BigDecimal amount) {}
    public record ServicePaymentRequest(Long customerId, String fromAccountNumber, String toAccountNumber, BigDecimal amount) {}
}