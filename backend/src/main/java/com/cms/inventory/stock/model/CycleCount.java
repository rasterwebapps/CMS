package com.cms.inventory.stock.model;

import java.time.Instant;
import java.time.LocalDate;

import com.cms.inventory.stock.model.enums.CycleCountScope;
import com.cms.inventory.stock.model.enums.CycleCountStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * A physical stock count/reconciliation exercise against one {@link InventoryLocation} — the
 * header record; its count sheet is {@link CycleCountLine}. Timestamps/actor fields are set
 * explicitly by {@code CycleCountService}, the same manual style {@link StockLedger} uses, rather
 * than a generic auditing listener — this header has several distinct actor/timestamp pairs
 * (created/submitted/completed) a single listener doesn't fit. See the 2026-09-08 "Cycle Count
 * slice" decision-log entry for the full design.
 */
@Entity
@Table(name = "cycle_counts")
public class CycleCount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private InventoryLocation location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CycleCountScope scope;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CycleCountStatus status = CycleCountStatus.DRAFT;

    @Column(name = "count_date", nullable = false)
    private LocalDate countDate;

    @Column(length = 500)
    private String notes;

    @Column(name = "created_by", length = 255)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "submitted_by", length = 255)
    private String submittedBy;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public InventoryLocation getLocation() { return location; }
    public void setLocation(InventoryLocation location) { this.location = location; }

    public CycleCountScope getScope() { return scope; }
    public void setScope(CycleCountScope scope) { this.scope = scope; }

    public CycleCountStatus getStatus() { return status; }
    public void setStatus(CycleCountStatus status) { this.status = status; }

    public LocalDate getCountDate() { return countDate; }
    public void setCountDate(LocalDate countDate) { this.countDate = countDate; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public String getSubmittedBy() { return submittedBy; }
    public void setSubmittedBy(String submittedBy) { this.submittedBy = submittedBy; }

    public Instant getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Instant submittedAt) { this.submittedAt = submittedAt; }

    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
