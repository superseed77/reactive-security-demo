package com.example.reactive.config;

import com.example.reactive.security.CustomReactiveAuthorizationManager;
import com.example.reactive.security.JwtServerAuthenticationConverter;
import com.example.reactive.security.ResourceBasedAuthorizationManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.SecurityWebFiltersOrder;

import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final ReactiveAuthenticationManager authenticationManager;
    private final CustomReactiveAuthorizationManager customAuthorizationManager;
    private final ResourceBasedAuthorizationManager resourceBasedAuthorizationManager;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
            .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((exchange, ex) -> {
                    var response = exchange.getResponse();
                    response.setStatusCode(HttpStatus.UNAUTHORIZED);
                    response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                    String body = String.format("{\"error\":\"Unauthorized\",\"message\":\"%s\",\"timestamp\":\"%s\"}",
                            ex.getMessage(), java.time.Instant.now());
                    return response.writeWith(Mono.just(response.bufferFactory().wrap(body.getBytes())));
                })
                .accessDeniedHandler((exchange, denied) -> {
                    var response = exchange.getResponse();
                    response.setStatusCode(HttpStatus.FORBIDDEN);
                    response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                    String body = String.format("{\"error\":\"Access Denied\",\"message\":\"%s\",\"timestamp\":\"%s\"}",
                            denied.getMessage(), java.time.Instant.now());
                    return response.writeWith(Mono.just(response.bufferFactory().wrap(body.getBytes())));
                })
            )
            .authorizeExchange(exchanges -> exchanges
                .pathMatchers(HttpMethod.OPTIONS).permitAll()
                .pathMatchers("/api/auth/**", "/api/public/**", "/actuator/health").permitAll()
                .pathMatchers("/api/admin/**").access(customAuthorizationManager)
                .pathMatchers("/api/user/{id}/**").access(resourceBasedAuthorizationManager)
                .pathMatchers("/api/user/**").hasAnyRole("USER","ADMIN")
                .pathMatchers("/api/special/**").access((authentication, context) ->
                    authentication.map(auth -> {
                        boolean has = auth.getAuthorities().stream()
                                .anyMatch(a -> a.getAuthority().equals("SCOPE_special"));
                        return new AuthorizationDecision(has);
                    }).defaultIfEmpty(new AuthorizationDecision(false)))
                .anyExchange().authenticated()
            )
            .headers(h -> h
                .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'none'"))
            )
                .addFilterAt(
                        jwtAuthenticationFilter(),
                        org.springframework.security.web.server.SecurityWebFiltersOrder.AUTHENTICATION
                )
            .build();
    }

    @Bean
    public AuthenticationWebFilter jwtAuthenticationFilter() {
        var filter = new AuthenticationWebFilter(authenticationManager);
        filter.setServerAuthenticationConverter(jwtServerAuthenticationConverter());
        filter.setSecurityContextRepository(NoOpServerSecurityContextRepository.getInstance());
        return filter;
    }

    @Bean
    public ServerAuthenticationConverter jwtServerAuthenticationConverter() {
        return new JwtServerAuthenticationConverter();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        var configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("http://localhost:[*]", "https://*.example.com"));
        configuration.setAllowedMethods(List.of("GET","POST","PUT","DELETE","PATCH","OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("X-Total-Count","X-Page-Number"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
