package com.bni.api.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "login_attempts")
public class LoginAttempt {

    @Id
    //@GeneratedValue(strategy = GenerationType.IDENTITY)
    private String id;

    @Column(name = "user_id", unique = true, nullable = false)
    private String userId;

    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @Column(name = "last_failed_attempt")
    private LocalDateTime lastFailedAttempt;

    // Constructors
    public LoginAttempt() {}

    public LoginAttempt(String userId, int failedAttempts, LocalDateTime lockedUntil, LocalDateTime lastFailedAttempt) {
        this.userId = userId;
        this.failedAttempts = failedAttempts;
        this.lockedUntil = lockedUntil;
        this.lastFailedAttempt = lastFailedAttempt;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public int getFailedAttempts() {
        return failedAttempts;
    }

    public void setFailedAttempts(int failedAttempts) {
        this.failedAttempts = failedAttempts;
    }

    public LocalDateTime getLockedUntil() {
        return lockedUntil;
    }

    public void setLockedUntil(LocalDateTime lockedUntil) {
        this.lockedUntil = lockedUntil;
    }

    public LocalDateTime getLastFailedAttempt() {
        return lastFailedAttempt;
    }

    public void setLastFailedAttempt(LocalDateTime lastFailedAttempt) {
        this.lastFailedAttempt = lastFailedAttempt;
    }
}