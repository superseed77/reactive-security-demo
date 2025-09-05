package com.example.reactive.service;

import com.example.reactive.model.User;
import com.example.reactive.repository.UserRepository;
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

    private final UserRepository userRepository; // <— inject repo instead of UserService

    /** True if current user owns the resource (by userId). */
    public Mono<Boolean> canAccessUserResource(Long userId) {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication().getName())
                .flatMap(currentUsername ->
                        userRepository.findById(userId)
                                .map(u -> u.getUsername().equals(currentUsername))
                                .defaultIfEmpty(false)
                );
    }

    /** Emit the User if current user owns it; otherwise error. */
    public Mono<User> ensureUserOwnership(Long userId) {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication().getName())
                .flatMap(currentUsername ->
                        userRepository.findById(userId)
                                .filter(u -> u.getUsername().equals(currentUsername))
                                .switchIfEmpty(Mono.error(new AccessDeniedException(
                                        "You don't have permission to access this resource"
                                )))
                );
    }

    /** True if current user has ROLE_{role}. */
    public Mono<Boolean> hasRole(String role) {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication().getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_" + role)))
                .defaultIfEmpty(false);
    }
}
