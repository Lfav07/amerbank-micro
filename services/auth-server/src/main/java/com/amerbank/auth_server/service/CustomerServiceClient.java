package com.amerbank.auth_server.service;

import com.amerbank.auth_server.security.JwtService;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;


@Service
public class CustomerServiceClient {

    private final RestClient restClient;
    private final JwtService service;

    public CustomerServiceClient(RestClient.Builder builder, JwtService service) {
        this.restClient = builder.build();
        this.service = service;
    }


    public Long getCustomerIdByUserId(Long userId) {
        String customerServiceUrl = "http://customer";
        String url = customerServiceUrl + "/customer/internal/by-user/" + userId;
      //  ResponseEntity<Long> response = restTemplate.getForEntity(url, Long.class);
        ResponseEntity<Long> response = restClient.get()
                .uri(url).accept(MediaType.APPLICATION_JSON)
                .headers(h -> h.setBearerAuth(service.generateServiceToken()))
                .retrieve()
                .toEntity(Long.class);
        if (response.getStatusCode().is2xxSuccessful()) {
            return response.getBody();
        }
        throw new IllegalStateException("Failed to fetch customerId");
    }
}
