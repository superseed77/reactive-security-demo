package com.example.reactive.service;

import com.example.reactive.model.JwtResponse;
import com.example.reactive.model.SignupRequest;
import com.example.reactive.model.User;
import com.example.reactive.repository.UserRepository;
import com.example.reactive.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public Mono<JwtResponse> login(String username, String rawPassword) {
        return userRepository.findByUsername(username)
            .flatMap(user -> Mono.fromCallable(() -> passwordEncoder.matches(rawPassword, user.getPassword()))
                .subscribeOn(Schedulers.boundedElastic())
                .filter(Boolean::booleanValue)
                .switchIfEmpty(Mono.error(new RuntimeException("Invalid credentials")))
                .map(__ -> new UsernamePasswordAuthenticationToken(user.getUsername(), null, user.getAuthorities()))
                .flatMap(jwtService::generateToken)
                .map(tok -> new JwtResponse(tok, "Bearer", user.getUsername()))
            );
    }

    public Mono<User> signup(SignupRequest req) {
        return Mono.zip(userRepository.existsByUsername(req.getUsername()),
                        userRepository.existsByEmail(req.getEmail()))
            .flatMap(t -> {
                if (t.getT1()) return Mono.error(new RuntimeException("Username already exists"));
                if (t.getT2()) return Mono.error(new RuntimeException("Email already exists"));
                return Mono.fromCallable(() -> passwordEncoder.encode(req.getPassword()))
                    .subscribeOn(Schedulers.boundedElastic())
                    .map(hash -> User.builder()
                        .username(req.getUsername())
                        .email(req.getEmail())
                        .password(hash)
                        .roles(Set.of("USER"))
                        .enabled(true)
                        .accountNonExpired(true)
                        .accountNonLocked(true)
                        .credentialsNonExpired(true)
                        .build())
                    .flatMap(userRepository::save);
            });
    }
}
