package com.adaptiq.dao;

import com.adaptiq.model.User;
import java.util.Optional;

/** Authentication-facing persistence contract, allowing service tests without MySQL. */
public interface UserAccountRepository {
    Optional<User> findByEmail(String email);
    boolean emailExists(String email);
    long create(User user);
}
