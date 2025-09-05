package com.example.reactive.security;

import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.ReactiveAuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class ResourceBasedAuthorizationManager implements ReactiveAuthorizationManager<AuthorizationContext> {
    @Override
    public Mono<AuthorizationDecision> check(Mono<Authentication> authentication, AuthorizationContext ctx) {
        String requestedId = null;
        if (ctx.getVariables() != null && ctx.getVariables().get("id") != null) {
            requestedId = (String) ctx.getVariables().get("id");
        }
        final String reqId = requestedId;
        return authentication
            .map(auth -> new AuthorizationDecision(reqId != null && reqId.equals(auth.getName())))
            .defaultIfEmpty(new AuthorizationDecision(false));
    }
}
