package com.quantumledger.app.user;


import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "user_credentials")
public class UserCredential {
    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "password_changed_at", nullable = false)
    private Instant passwordChangedAt;

    protected UserCredential() { }

    public UserCredential(Long userId, String passwordHash) {
        this.userId = userId;
        this.passwordHash = passwordHash;
        this.passwordChangedAt = Instant.now();
    }

    public Long getUserId() { return userId; }
    public String getPasswordHash() { return passwordHash; }

}
