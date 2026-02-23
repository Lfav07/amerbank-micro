package com.amerbank.auth_server.security;


import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;


public record JwtUserPrincipal(Long userId, Collection<? extends GrantedAuthority> authorities) {

}
