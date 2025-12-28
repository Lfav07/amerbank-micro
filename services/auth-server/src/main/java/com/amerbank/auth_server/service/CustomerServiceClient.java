package com.amerbank.auth_server.service;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;


@Service
public class CustomerServiceClient {

    private final RestClient restClient;

    public CustomerServiceClient( RestClient.Builder builder) {
        this.restClient = builder.build();
    }

    public Long getCustomerIdByUserId(Long userId) {
        String customerServiceUrl = "http://customer";
        String url = customerServiceUrl + "/customer/get/id/user/" + userId;
      //  ResponseEntity<Long> response = restTemplate.getForEntity(url, Long.class);
        ResponseEntity<Long> response = restClient.get()
                .uri(url).accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .toEntity(Long.class);
        if (response.getStatusCode().is2xxSuccessful()) {
            return response.getBody();
        }
        throw new IllegalStateException("Failed to fetch customerId");
    }
}
