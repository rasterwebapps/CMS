package com.cms.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "revoked_tokens")
public class RevokedToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String jti;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at", nullable = false, updatable = false)
    private Instant revokedAt = Instant.now();

    protected RevokedToken() {}

    public RevokedToken(String jti, Instant expiresAt) {
        this.jti       = jti;
        this.expiresAt = expiresAt;
        this.revokedAt = Instant.now();
    }

    public Long getId()          { return id; }
    public String getJti()       { return jti; }
    public Instant getExpiresAt(){ return expiresAt; }
    public Instant getRevokedAt(){ return revokedAt; }
}
