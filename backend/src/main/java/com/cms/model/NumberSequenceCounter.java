package com.cms.model;

import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "number_sequence_counters",
    uniqueConstraints = @UniqueConstraint(columnNames = {"series_code", "scope_key"})
)
@EntityListeners(AuditingEntityListener.class)
public class NumberSequenceCounter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "series_code", nullable = false, updatable = false, length = 50)
    private String seriesCode;

    @Column(name = "scope_key", nullable = false, updatable = false, length = 100)
    private String scopeKey;

    @Column(name = "last_sequence", nullable = false)
    private int lastSequence;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public NumberSequenceCounter() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSeriesCode() { return seriesCode; }
    public void setSeriesCode(String seriesCode) { this.seriesCode = seriesCode; }
    public String getScopeKey() { return scopeKey; }
    public void setScopeKey(String scopeKey) { this.scopeKey = scopeKey; }
    public int getLastSequence() { return lastSequence; }
    public void setLastSequence(int lastSequence) { this.lastSequence = lastSequence; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
