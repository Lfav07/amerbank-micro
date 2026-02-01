package com.amerbank.transaction.security;

import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;


public record JwtUserPrincipal(String email, Long customerId, Collection<? extends GrantedAuthority> authorities) {

}
