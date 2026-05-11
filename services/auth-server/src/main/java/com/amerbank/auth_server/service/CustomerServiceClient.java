package com.amerbank.auth_server.service;

import com.amerbank.auth_server.dto.response.CustomerRegistrationResponse;
import com.amerbank.auth_server.dto.request.CustomerRegistrationRequest;
import com.amerbank.auth_server.exception.CustomerRegistrationFailedException;
import com.amerbank.auth_server.security.JwtService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;


/**
 * Client for communicating with the customer-service via REST.
 * Used for service-to-service authentication and customer registration.
 */
@Service
public class CustomerServiceClient {

    private final RestClient restClient;
    private final JwtService service;

    public CustomerServiceClient(RestClient.Builder builder, JwtService service) {
        this.restClient = builder.build();
        this.service = service;
    }



    /**
     * Registers a new customer in the customer-service.
     *
     * @param request the customer registration request
     * @return the customer registration response with customer ID
     * @throws CustomerRegistrationFailedException if registration fails
     */
    public CustomerRegistrationResponse registerCustomer(CustomerRegistrationRequest request) {
        String customerServiceUrl = "http://customer";
        String url = customerServiceUrl + "/customer/internal/register";
        ResponseEntity<CustomerRegistrationResponse> response = restClient.post()
                .uri(url).
                accept(MediaType.APPLICATION_JSON)
                .headers(h -> h.setBearerAuth(service.generateServiceToken()))
                .body(request)
                .retrieve()
                .toEntity(CustomerRegistrationResponse.class);
        if (response.getStatusCode().is2xxSuccessful()) {
            return response.getBody();
        }
        throw new CustomerRegistrationFailedException("Failed to fetch customer registration response");
    }
}
