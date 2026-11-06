package com.adaptiq.util;

import com.adaptiq.exception.ValidationException;
import java.util.Locale;
import java.util.regex.Pattern;

/** Reusable server-side validation; UI validation supplements but never replaces it. */
public final class ValidationUtil {
    private static final Pattern EMAIL = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);

    private ValidationUtil() { }

    public static String normalizeAndValidateEmail(String email) {
        String normalized = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        if (!EMAIL.matcher(normalized).matches()) {
            throw new ValidationException("Enter a valid email address.");
        }
        return normalized;
    }

    public static String validateName(String fullName) {
        String normalized = fullName == null ? "" : fullName.trim().replaceAll("\\s+", " ");
        if (normalized.length() < 2 || normalized.length() > 120) {
            throw new ValidationException("Full name must contain between 2 and 120 characters.");
        }
        return normalized;
    }

    public static void validatePassword(String password) {
        if (password == null || password.length() < 10
                || !password.matches(".*[A-Z].*") || !password.matches(".*[a-z].*")
                || !password.matches(".*\\d.*")) {
            throw new ValidationException("Password must be at least 10 characters and include uppercase, lowercase, and a number.");
        }
    }
}
