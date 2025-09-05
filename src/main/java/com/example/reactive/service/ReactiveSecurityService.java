package com.example.reactive.service;

import com.example.reactive.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReactiveSecurityService {

    private final UserService userService;

    public Mono<Boolean> canAccessUserResource(Long userId) {
        return ReactiveSecurityContextHolder.getContext()
            .map(ctx -> ctx.getAuthentication().getName())
            .flatMap(current -> userService.findById(userId)
                .map(u -> u.getUsername().equals(current))
                .defaultIfEmpty(false));
    }

    public Mono<User> ensureUserOwnership(Long userId) {
        return ReactiveSecurityContextHolder.getContext()
            .map(ctx -> ctx.getAuthentication().getName())
            .flatMap(current -> userService.findById(userId)
                .filter(u -> u.getUsername().equals(current))
                .switchIfEmpty(Mono.error(new AccessDeniedException("You don't have permission"))));
    }

    public Mono<Boolean> hasRole(String role) {
        return ReactiveSecurityContextHolder.getContext()
            .map(ctx -> ctx.getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + role)))
            .defaultIfEmpty(false);
    }
}
