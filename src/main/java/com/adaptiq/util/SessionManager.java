package com.adaptiq.util;

import com.adaptiq.model.User;
import java.util.Optional;

/** Process-local authenticated session. Database authorization is still enforced in services. */
public final class SessionManager {
    private static User currentUser;

    private SessionManager() { }

    public static synchronized void start(User user) { currentUser = user; }
    public static synchronized Optional<User> currentUser() { return Optional.ofNullable(currentUser); }
    public static synchronized void clear() { currentUser = null; }
}
