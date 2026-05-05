package com.cms.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Persistent audit record written whenever a user (or system) mutates
 * a role, permission grant, or user account.
 */
@Entity
@Table(name = "audit_log")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Keycloak username of the person who performed the action. */
    @Column(nullable = false, length = 100)
    private String actor;

    /** Verb describing the action, e.g. ROLE_CREATED, PERMISSION_UPDATED, USER_DEACTIVATED. */
    @Column(nullable = false, length = 50)
    private String action;

    /** High-level resource type, e.g. AppRole, Permission, AppUser. */
    @Column(name = "entity_type", length = 100)
    private String entityType;

    /** String form of the affected entity's primary key. */
    @Column(name = "entity_id", length = 100)
    private String entityId;

    /** Free-form JSON or human-readable description of the change. */
    @Column(columnDefinition = "TEXT")
    private String detail;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt = Instant.now();

    // -------------------------------------------------------------------------
    // No-arg constructor required by JPA

    protected AuditLog() {}

    private AuditLog(Builder builder) {
        this.actor      = builder.actor;
        this.action     = builder.action;
        this.entityType = builder.entityType;
        this.entityId   = builder.entityId;
        this.detail     = builder.detail;
        this.occurredAt = Instant.now();
    }

    // -------------------------------------------------------------------------

    public Long getId()          { return id; }
    public String getActor()     { return actor; }
    public String getAction()    { return action; }
    public String getEntityType(){ return entityType; }
    public String getEntityId()  { return entityId; }
    public String getDetail()    { return detail; }
    public Instant getOccurredAt(){ return occurredAt; }

    // -------------------------------------------------------------------------

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String actor;
        private String action;
        private String entityType;
        private String entityId;
        private String detail;

        public Builder actor(String actor)           { this.actor = actor; return this; }
        public Builder action(String action)         { this.action = action; return this; }
        public Builder entityType(String entityType) { this.entityType = entityType; return this; }
        public Builder entityId(String entityId)     { this.entityId = entityId; return this; }
        public Builder detail(String detail)         { this.detail = detail; return this; }
        public AuditLog build()                      { return new AuditLog(this); }
    }
}

