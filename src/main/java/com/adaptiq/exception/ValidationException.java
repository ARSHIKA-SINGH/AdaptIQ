package com.adaptiq.exception;

/** Raised when a domain request does not meet validation requirements. */
public class ValidationException extends RuntimeException {
    public ValidationException(String message) { super(message); }
}
