package com.example.reactive.service;

import com.example.reactive.model.User;
import com.example.reactive.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService implements ReactiveUserDetailsService {

    private final UserRepository userRepository;
    private final ReactiveSecurityService securityService;

    @Override
    public Mono<UserDetails> findByUsername(String username) {
        return userRepository.findByUsername(username)
            .cast(UserDetails.class)
            .switchIfEmpty(Mono.error(new RuntimeException("User not found: " + username)));
    }

    public Mono<User> findById(Long id) {
        return userRepository.findById(id)
            .switchIfEmpty(Mono.error(new RuntimeException("User not found with id: " + id)));
    }

    public Flux<User> findAllUsers() {
        return userRepository.findAll();
    }

    public Mono<User> updateUser(Long id, User updateRequest) {
        return securityService.canAccessUserResource(id)
            .filter(Boolean::booleanValue)
            .switchIfEmpty(Mono.error(new AccessDeniedException("Not authorized to update this user")))
            .flatMap(__ -> userRepository.findById(id)
                .map(u -> {
                    if (updateRequest.getEmail() != null) u.setEmail(updateRequest.getEmail());
                    if (updateRequest.getRoles() != null) u.setRoles(updateRequest.getRoles());
                    return u;
                })
                .flatMap(userRepository::save));
    }

    public Mono<Void> deleteUser(Long id) {
        return securityService.hasRole("ADMIN")
            .filter(Boolean::booleanValue)
            .switchIfEmpty(Mono.error(new AccessDeniedException("Only admins can delete users")))
            .flatMap(__ -> userRepository.deleteById(id));
    }

    public Mono<Boolean> existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    public Mono<Boolean> existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public Mono<User> getCurrentUserProfile() {
        return ReactiveSecurityContextHolder.getContext()
            .map(ctx -> ctx.getAuthentication().getName())
            .flatMap(name -> userRepository.findByUsername(name));
    }

    public Mono<Long> bulkDisableUsers(reactor.core.publisher.Flux<Long> ids) {
        return ids.flatMap(id -> userRepository.findById(id)
                .map(u -> { u.setEnabled(false); return u; })
                .flatMap(userRepository::save))
            .count();
    }
}
