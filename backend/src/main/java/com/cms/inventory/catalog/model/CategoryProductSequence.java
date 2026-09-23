package com.cms.inventory.catalog.model;

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

/**
 * The running counter backing auto-generated Product codes (&lt;Category.shortCode&gt;-&lt;sequence&gt;,
 * e.g. STA-000001) — one row per category, incremented under a pessimistic lock by
 * {@code ProductCodeGeneratorService}. Mirrors {@code RollNumberSequence}.
 */
@Entity
@Table(
    name = "category_product_sequences",
    uniqueConstraints = @UniqueConstraint(columnNames = {"category_id"})
)
@EntityListeners(AuditingEntityListener.class)
public class CategoryProductSequence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Column(name = "last_sequence", nullable = false)
    private Long lastSequence;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public CategoryProductSequence() {
    }

    public CategoryProductSequence(Long categoryId, Long lastSequence) {
        this.categoryId = categoryId;
        this.lastSequence = lastSequence;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public Long getLastSequence() { return lastSequence; }
    public void setLastSequence(Long lastSequence) { this.lastSequence = lastSequence; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
