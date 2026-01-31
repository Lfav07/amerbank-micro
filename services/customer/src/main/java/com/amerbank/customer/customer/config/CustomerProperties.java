package com.amerbank.customer.customer.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "customer")
public class CustomerProperties {

    @NotBlank
    private  String TRACE_ID_HEADER;

    @NotBlank
    private  String REQUEST_ID_HEADER;

    @NotBlank
    private String authServiceUrl;

    @NotBlank
    private String authRegisterPath;

    @NotBlank
    private String authUserByEmailPath;
}
