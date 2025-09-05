package com.example.reactive.security;

import com.example.reactive.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtProperties props;
    private SecretKey key;

    @PostConstruct
    void init() {
        this.key = io.jsonwebtoken.security.Keys.hmacShaKeyFor(props.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public Mono<String> generateToken(Authentication authentication) {
        return Mono.fromCallable(() -> Jwts.builder()
            .subject(authentication.getName())
            .claims(Map.of(
                "roles", authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList()),
                "type", "access"
            ))
            .issuer(props.getIssuer())
            .issuedAt(Date.from(Instant.now()))
            .expiration(Date.from(Instant.now().plusMillis(props.getExpiration())))
            .signWith(key, Jwts.SIG.HS256)
            .compact());
    }

    public Mono<Claims> validateAndGetClaims(String token) {
        return Mono.fromCallable(() -> {
            try {
                return Jwts.parser()
                    .clockSkewSeconds(30)
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            } catch (ExpiredJwtException e) {
                throw new RuntimeException("Token expired");
            } catch (Exception e) {
                throw new RuntimeException("Invalid token");
            }
        });
    }
}
