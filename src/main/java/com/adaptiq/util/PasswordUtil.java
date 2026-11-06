package com.adaptiq.util;

import org.mindrot.jbcrypt.BCrypt;

/** BCrypt is intentionally the only password representation persisted by AdaptIQ. */
public final class PasswordUtil {
    private static final int WORK_FACTOR = 12;

    private PasswordUtil() { }

    public static String hash(String plainPassword) {
        if (plainPassword == null || plainPassword.isBlank()) {
            throw new IllegalArgumentException("Password is required.");
        }
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(WORK_FACTOR));
    }

    public static boolean matches(String plainPassword, String passwordHash) {
        return plainPassword != null && passwordHash != null && BCrypt.checkpw(plainPassword, passwordHash);
    }
}
