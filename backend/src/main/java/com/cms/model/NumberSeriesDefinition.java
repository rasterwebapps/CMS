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

@Entity
@Table(name = "number_series_definitions")
@EntityListeners(AuditingEntityListener.class)
public class NumberSeriesDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "series_code", nullable = false, unique = true, updatable = false, length = 50)
    private String seriesCode;

    @Column(name = "series_name", nullable = false, length = 100)
    private String seriesName;

    @Column(name = "scope_type", nullable = false, length = 30)
    private String scopeType;

    @Column(name = "prefix", length = 30)
    private String prefix;

    @Column(name = "separator", nullable = false, length = 5)
    private String separator;

    @Column(name = "sequence_padding", nullable = false)
    private int sequencePadding;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public NumberSeriesDefinition() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSeriesCode() { return seriesCode; }
    public void setSeriesCode(String seriesCode) { this.seriesCode = seriesCode; }
    public String getSeriesName() { return seriesName; }
    public void setSeriesName(String seriesName) { this.seriesName = seriesName; }
    public String getScopeType() { return scopeType; }
    public void setScopeType(String scopeType) { this.scopeType = scopeType; }
    public String getPrefix() { return prefix; }
    public void setPrefix(String prefix) { this.prefix = prefix; }
    public String getSeparator() { return separator; }
    public void setSeparator(String separator) { this.separator = separator; }
    public int getSequencePadding() { return sequencePadding; }
    public void setSequencePadding(int sequencePadding) { this.sequencePadding = sequencePadding; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
