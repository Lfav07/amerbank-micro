package com.amerbank.loan.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class ServiceJwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public ServiceJwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String token = authHeader.substring(7);

        String subject = jwtService.extractSubject(token);
        boolean validAccountService = jwtService.validateServiceToken(token, "account-service", "loan-service");
        boolean validCustomerService = jwtService.validateServiceToken(token, "customer-service", "loan-service");

        if (!validAccountService && !validCustomerService) {
            if (jwtService.isTokenValid(token)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            } else {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            }
            return;
        }

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        subject, null, List.of(new SimpleGrantedAuthority("SCOPE_service"))
                )
        );
        filterChain.doFilter(request, response);
    }
}