package com.amerbank.loan.security;

import java.util.Collection;
import org.springframework.security.core.GrantedAuthority;

public record JwtUserPrincipal(Long customerId, Collection<? extends GrantedAuthority> authorities) {

}