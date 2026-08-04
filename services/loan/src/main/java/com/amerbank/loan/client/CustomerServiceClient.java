package com.amerbank.loan.client;

import com.amerbank.loan.config.LoanProperties;
import com.amerbank.loan.exception.CustomerServiceUnavailableException;
import com.amerbank.loan.security.JwtService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
@Slf4j
public class CustomerServiceClient implements CustomerServiceClientInterface {

    private final RestClient restClient;
    private final JwtService jwtService;
    private final LoanProperties loanProperties;

    public CustomerServiceClient(RestClient.Builder restClientBuilder, JwtService jwtService, LoanProperties loanProperties) {
        this.restClient = restClientBuilder.build();
        this.jwtService = jwtService;
        this.loanProperties = loanProperties;
    }

    public boolean customerExists(Long customerId) {
        String url = loanProperties.getCustomerServiceBase() + "/customer/internal/" + customerId;
        String serviceToken = jwtService.generateServiceToken();

        try {
            Boolean result = restClient
                    .get()
                    .uri(url)
                    .headers(h -> h.setBearerAuth(serviceToken))
                    .retrieve()
                    .body(Boolean.class);
            log.debug("Customer existence check - customerId: {}, exists: {}", customerId, result);
            return Boolean.TRUE.equals(result);
        } catch (HttpClientErrorException e) {
            String responseBody = e.getResponseBodyAsString();
            log.error("Customer service call failed - url: {}, status: {}, customerId: {}, responseBody: {}",
                    url, e.getStatusCode(), customerId, responseBody);
            throw new CustomerServiceUnavailableException(
                    String.format("Customer service call failed. Status: %s, URL: %s", e.getStatusCode(), url)
            );
        } catch (RestClientException e) {
            log.error("Could not reach customer service - url: {}, customerId: {}, error: {}",
                    url, customerId, e.getMessage());
            throw new CustomerServiceUnavailableException(
                    String.format("Could not reach customer service. URL: %s", url)
            );
        }
    }
}