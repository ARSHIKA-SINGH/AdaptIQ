package com.adaptiq.model;

import java.time.Instant;
import java.util.Objects;

/** Authenticated account model; password hashes are never exposed in JavaFX views. */
public class User {
    private Long userId;
    private String fullName;
    private String email;
    private String passwordHash;
    private Role role;
    private UserStatus status;
    private Instant createdAt;

    public User() { }

    public User(Long userId, String fullName, String email, String passwordHash, Role role, UserStatus status, Instant createdAt) {
        this.userId = userId;
        this.fullName = Objects.requireNonNull(fullName, "fullName");
        this.email = Objects.requireNonNull(email, "email");
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash");
        this.role = Objects.requireNonNull(role, "role");
        this.status = Objects.requireNonNull(status, "status");
        this.createdAt = createdAt;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = Objects.requireNonNull(fullName, "fullName"); }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = Objects.requireNonNull(email, "email"); }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash"); }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = Objects.requireNonNull(role, "role"); }
    public UserStatus getStatus() { return status; }
    public void setStatus(UserStatus status) { this.status = Objects.requireNonNull(status, "status"); }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
