package com.example.reactive.exception;

public class CustomAuthenticationException extends RuntimeException {
    public CustomAuthenticationException(String message) { super(message); }
    public CustomAuthenticationException(String message, Throwable cause) { super(message, cause); }
}
