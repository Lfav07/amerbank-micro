package com.amerbank.customer.customer.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;


public record JwtUserPrincipal(Long customerId, Collection<? extends GrantedAuthority> authorities) {

}
