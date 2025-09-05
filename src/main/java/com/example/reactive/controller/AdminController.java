package com.example.reactive.controller;

import com.example.reactive.model.User;
import com.example.reactive.service.ReactiveSecurityService;
import com.example.reactive.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;
    private final ReactiveSecurityService securityService;

    @GetMapping("/users")
    @PreAuthorize("@reactiveSecurityService.hasRole('ADMIN')")
    public Flux<User> getAllUsers() {
        return userService.findAllUsers();
    }

    @GetMapping("/users/{id}")
    @PreAuthorize("@reactiveSecurityService.hasRole('ADMIN')")
    public Mono<ResponseEntity<User>> getUser(@PathVariable Long id) {
        return userService.findById(id)
            .map(ResponseEntity::ok)
            .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/users/{id}")
    public Mono<ResponseEntity<Void>> deleteUser(@PathVariable Long id) {
        return securityService.hasRole("ADMIN")
            .filter(Boolean::booleanValue)
            .flatMap(__ -> userService.deleteUser(id))
            .then(Mono.just(ResponseEntity.noContent().<Void>build()))
            .switchIfEmpty(Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).build()))
            .onErrorReturn(ResponseEntity.notFound().build());
    }

    @PostMapping("/users/bulk-disable")
    public Mono<ResponseEntity<String>> bulkDisableUsers(@RequestBody Flux<Long> userIds) {
        return securityService.hasRole("ADMIN")
            .filter(Boolean::booleanValue)
            .flatMap(__ -> userService.bulkDisableUsers(userIds))
            .map(count -> ResponseEntity.ok("Disabled " + count + " users"))
            .switchIfEmpty(Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).body("Not authorized")));
    }
}
