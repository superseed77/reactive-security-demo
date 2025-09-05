package com.example.reactive.security;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationManager implements ReactiveAuthenticationManager {

    private final JwtService jwtService;

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        return Mono.justOrEmpty(authentication)
            .map(auth -> auth.getCredentials() == null ? null : auth.getCredentials().toString())
            .flatMap(this::toAuth)
            .doOnSuccess(a -> { if (a != null) log.debug("Authenticated: {}", a.getName()); })
            .doOnError(e -> log.debug("Auth failed: {}", e.getMessage()));
    }

    private Mono<Authentication> toAuth(String token) {
        return jwtService.validateAndGetClaims(token)
            .map(claims -> buildAuth(claims, token));
    }

    private Authentication buildAuth(Claims claims, String token) {
        String username = claims.getSubject();
        @SuppressWarnings("unchecked")
        List<String> roles = claims.get("roles", List.class);
        var authorities = roles == null ? List.<SimpleGrantedAuthority>of() :
            roles.stream()
                .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
        return new UsernamePasswordAuthenticationToken(username, token, authorities);
    }
}
