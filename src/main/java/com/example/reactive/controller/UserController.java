package com.example.reactive.controller;

import com.example.reactive.model.User;
import com.example.reactive.service.ReactiveSecurityService;
import com.example.reactive.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.security.Principal;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final ReactiveSecurityService securityService;

    @GetMapping("/profile")
    public Mono<ResponseEntity<User>> getProfile(@AuthenticationPrincipal Mono<Principal> principalMono) {
        return principalMono
            .map(Principal::getName)
            .flatMap(userService::findByUsername)
            .cast(User.class)
            .map(ResponseEntity::ok)
            .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PutMapping("/profile/{id}")
    public Mono<ResponseEntity<User>> updateProfile(
        @PathVariable Long id,
        @Valid @RequestBody User updateRequest,
        @AuthenticationPrincipal Mono<Principal> principalMono) {

        return principalMono
            .map(Principal::getName)
            .flatMap(current -> userService.findById(id)
                .filter(u -> u.getUsername().equals(current))
                .switchIfEmpty(Mono.error(new org.springframework.security.access.AccessDeniedException("You can only update your own profile")))
                .flatMap(__ -> userService.updateUser(id, updateRequest)))
            .map(ResponseEntity::ok)
            .onErrorResume(org.springframework.security.access.AccessDeniedException.class,
                e -> Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).build()))
            .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<User>> getUserById(@PathVariable Long id,
                                                  @AuthenticationPrincipal Mono<Principal> principalMono) {
        return principalMono
            .flatMap(__ -> securityService.canAccessUserResource(id)
                .filter(Boolean::booleanValue)
                .flatMap(___ -> userService.findById(id)))
            .map(ResponseEntity::ok)
            .defaultIfEmpty(ResponseEntity.status(HttpStatus.FORBIDDEN).build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@reactiveSecurityService.canAccessUserResource(#id)")
    public Mono<ResponseEntity<Void>> deleteOwnAccount(@PathVariable Long id) {
        return userService.deleteUser(id)
            .then(Mono.just(ResponseEntity.noContent().<Void>build()))
            .onErrorReturn(ResponseEntity.status(HttpStatus.FORBIDDEN).build());
    }

    @GetMapping("/secure-data")
    public Mono<ResponseEntity<String>> getSecureData(@AuthenticationPrincipal Mono<Principal> principalMono) {
        return principalMono
            .map(p -> ResponseEntity.ok("Secure data for user: " + p.getName()))
            .defaultIfEmpty(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("No authentication found"));
    }
}
