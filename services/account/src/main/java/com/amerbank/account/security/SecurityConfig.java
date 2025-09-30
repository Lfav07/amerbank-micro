package com.amerbank.account.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@EnableMethodSecurity(prePostEnabled = true)
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    SecurityFilterChain chain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // Endpoints for authenticated users
                        .requestMatchers(
                                "/accounts",                        // register account
                                "/accounts/me",                      // get my accounts
                                "/accounts/me/owned",                // check ownership
                                "/accounts/me/balances",             // get balances
                                "/accounts/me/balance",              // get balance by type
                                "/accounts/me/has-funds",            // check funds
                                "/accounts/deposit",                 // deposit
                                "/accounts/payment",                 // payment
                                "/accounts/refund"                   // refund
                        ).authenticated()
                        // Admin-only endpoints
                        .requestMatchers(
                                "/accounts/customers/**",            // get accounts by customer
                                "/accounts/{accountNumber}",         // get account details
                                "/accounts/{accountNumber}/type",    // update type
                                "/accounts/{accountNumber}/status",  // update status
                                "/accounts/{accountNumber}/suspend", // suspend account
                                "/accounts/{accountNumber}/balance", // get balance
                                "/accounts/{accountNumber}"          // delete account
                        ).hasRole("ADMIN")
                        .anyRequest().authenticated())  // fallback: authenticated
                .sessionManagement(sess -> sess
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(httpBasic -> httpBasic.disable())
                .addFilterBefore(jwtAuthFilter,
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
