package com.amerbank.account.security;

import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;


public record JwtUserPrincipal(Long customerId, Collection<? extends GrantedAuthority> authorities) {

}
