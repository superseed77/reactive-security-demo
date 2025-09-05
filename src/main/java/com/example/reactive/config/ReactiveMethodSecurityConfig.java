package com.example.reactive.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;

@Configuration
@EnableReactiveMethodSecurity(useAuthorizationManager = true)
public class ReactiveMethodSecurityConfig {
}
