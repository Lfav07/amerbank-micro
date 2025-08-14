package com.amerbank.auth_server.service;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class CustomerServiceClient {

    private final RestTemplate restTemplate;
    private final String customerServiceUrl = "http://customer-service";

    public CustomerServiceClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public Long getCustomerIdByUserId(Long userId) {
        String url = customerServiceUrl + "/customer/get/id/user/" + userId;
        ResponseEntity<Long> response = restTemplate.getForEntity(url, Long.class);
        if (response.getStatusCode().is2xxSuccessful()) {
            return response.getBody();
        }
        throw new IllegalStateException("Failed to fetch customerId");
    }
}
