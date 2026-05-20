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
    name = "application_number_sequences",
    uniqueConstraints = @UniqueConstraint(columnNames = {"series_code", "scope_key"})
)
@EntityListeners(AuditingEntityListener.class)
public class ApplicationNumberSequence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "series_code", nullable = false, length = 50)
    private String seriesCode;

    @Column(name = "series_name", nullable = false, length = 100)
    private String seriesName;

    @Column(name = "scope_type", nullable = false, length = 50)
    private String scopeType;

    @Column(name = "scope_key", nullable = false, length = 50)
    private String scopeKey;

    @Column(nullable = false, length = 20)
    private String prefix;

    @Column(name = "sequence_padding", nullable = false)
    private Integer sequencePadding;

    @Column(name = "last_sequence", nullable = false)
    private Integer lastSequence;

    @Column(name = "separator", nullable = false, length = 5)
    private String separator = "-";

    @Column(name = "include_scope_in_number", nullable = false)
    private boolean includeScopeInNumber = true;

    @Column(columnDefinition = "TEXT")
    private String description;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public ApplicationNumberSequence() {
    }

    public ApplicationNumberSequence(String seriesCode, String seriesName, String scopeType, String scopeKey,
                                     String prefix, Integer sequencePadding, Integer lastSequence,
                                     String description) {
        this.seriesCode = seriesCode;
        this.seriesName = seriesName;
        this.scopeType = scopeType;
        this.scopeKey = scopeKey;
        this.prefix = prefix;
        this.sequencePadding = sequencePadding;
        this.lastSequence = lastSequence;
        this.description = description;
        this.separator = "-";
        this.includeScopeInNumber = true;
    }

    public ApplicationNumberSequence(String seriesCode, String seriesName, String scopeType, String scopeKey,
                                     String prefix, Integer sequencePadding, Integer lastSequence,
                                     String description, String separator, boolean includeScopeInNumber) {
        this.seriesCode = seriesCode;
        this.seriesName = seriesName;
        this.scopeType = scopeType;
        this.scopeKey = scopeKey;
        this.prefix = prefix;
        this.sequencePadding = sequencePadding;
        this.lastSequence = lastSequence;
        this.description = description;
        this.separator = separator;
        this.includeScopeInNumber = includeScopeInNumber;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSeriesCode() { return seriesCode; }
    public void setSeriesCode(String seriesCode) { this.seriesCode = seriesCode; }
    public String getSeriesName() { return seriesName; }
    public void setSeriesName(String seriesName) { this.seriesName = seriesName; }
    public String getScopeType() { return scopeType; }
    public void setScopeType(String scopeType) { this.scopeType = scopeType; }
    public String getScopeKey() { return scopeKey; }
    public void setScopeKey(String scopeKey) { this.scopeKey = scopeKey; }
    public String getPrefix() { return prefix; }
    public void setPrefix(String prefix) { this.prefix = prefix; }
    public Integer getSequencePadding() { return sequencePadding; }
    public void setSequencePadding(Integer sequencePadding) { this.sequencePadding = sequencePadding; }
    public Integer getLastSequence() { return lastSequence; }
    public void setLastSequence(Integer lastSequence) { this.lastSequence = lastSequence; }
    public String getSeparator() { return separator; }
    public void setSeparator(String separator) { this.separator = separator; }
    public boolean isIncludeScopeInNumber() { return includeScopeInNumber; }
    public void setIncludeScopeInNumber(boolean includeScopeInNumber) { this.includeScopeInNumber = includeScopeInNumber; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
