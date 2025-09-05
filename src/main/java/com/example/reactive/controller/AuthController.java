package com.example.reactive.controller;

import com.example.reactive.model.JwtResponse;
import com.example.reactive.model.LoginRequest;
import com.example.reactive.model.SignupRequest;
import com.example.reactive.model.User;
import com.example.reactive.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public Mono<ResponseEntity<JwtResponse>> login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req.getUsername(), req.getPassword())
            .map(ResponseEntity::ok)
            .onErrorReturn(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    @PostMapping("/signup")
    public Mono<ResponseEntity<User>> signup(@Valid @RequestBody SignupRequest req) {
        return authService.signup(req)
            .map(u -> ResponseEntity.status(HttpStatus.CREATED).body(u))
            .onErrorReturn(ResponseEntity.badRequest().build());
    }
}
