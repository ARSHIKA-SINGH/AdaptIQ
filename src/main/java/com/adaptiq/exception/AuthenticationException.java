package com.adaptiq.exception;

/** Raised for failed sign-in attempts without disclosing sensitive account details. */
public class AuthenticationException extends RuntimeException {
    public AuthenticationException(String message) { super(message); }
}
