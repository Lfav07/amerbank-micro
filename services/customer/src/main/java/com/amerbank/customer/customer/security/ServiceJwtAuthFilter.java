package com.amerbank.customer.customer.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

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

        if (!jwtService.validateAuthServiceToken(token)) {
           response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
          return;
       }

        String subject = jwtService.extractSubject(token);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        subject, null, List.of(new SimpleGrantedAuthority("SCOPE_service"))
                )
        );
        filterChain.doFilter(request, response);

    }
}