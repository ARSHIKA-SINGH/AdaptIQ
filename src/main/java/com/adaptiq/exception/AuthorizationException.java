package com.adaptiq.exception;

/** Raised when an authenticated user attempts a role-restricted operation. */
public class AuthorizationException extends RuntimeException {
    public AuthorizationException(String message) { super(message); }
}
