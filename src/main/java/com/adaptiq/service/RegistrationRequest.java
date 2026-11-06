package com.adaptiq.service;

/** Raw registration form values, validated only by AuthService. */
public record RegistrationRequest(String fullName, String email, String password, String confirmPassword) { }
