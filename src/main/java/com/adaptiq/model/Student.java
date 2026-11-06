package com.adaptiq.model;

import java.time.Instant;

/** Student specialization retained for role-oriented service APIs. */
public final class Student extends User {
    public Student() { super(); }

    public Student(Long userId, String fullName, String email, String passwordHash, UserStatus status, Instant createdAt) {
        super(userId, fullName, email, passwordHash, Role.STUDENT, status, createdAt);
    }
}
