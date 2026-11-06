package com.adaptiq.exception;

/** Wraps persistence failures with an application-safe message. */
public class DatabaseException extends RuntimeException {
    public DatabaseException(String message) { super(message); }
    public DatabaseException(String message, Throwable cause) { super(message, cause); }
}
