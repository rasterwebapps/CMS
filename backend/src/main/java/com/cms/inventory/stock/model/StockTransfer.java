package com.cms.inventory.stock.model;

import java.time.Instant;
import java.time.LocalDate;

import com.cms.inventory.stock.model.enums.StockTransferStatus;

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
 * Moves stock between two {@link InventoryLocation}s — Phase 3's ("Receiving & Stock Movement")
 * second slice, giving the {@code TRANSFER} {@code StockTxnType} (reserved since Phase 1, unused
 * until now) a real screen. DRAFT (build lines) -> COMPLETED (posts a decrease-at-source /
 * increase-at-destination movement pair per line through the existing {@code
 * StockMovementService}) -> no further transition; CANCELLED reachable only from DRAFT. Scoped to
 * unbatched stock only in this pass — same "no batch-level workflow yet" simplification {@code
 * CycleCount}'s own posting step already established. See the "Stock Transfer slice"
 * decision-log entry.
 */
@Entity
@Table(name = "stock_transfers")
public class StockTransfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_location_id", nullable = false)
    private InventoryLocation sourceLocation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_location_id", nullable = false)
    private InventoryLocation destinationLocation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StockTransferStatus status = StockTransferStatus.DRAFT;

    @Column(name = "transfer_date", nullable = false)
    private LocalDate transferDate;

    @Column(length = 500)
    private String notes;

    @Column(name = "created_by", length = 255)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "completed_by", length = 255)
    private String completedBy;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public InventoryLocation getSourceLocation() { return sourceLocation; }
    public void setSourceLocation(InventoryLocation sourceLocation) { this.sourceLocation = sourceLocation; }

    public InventoryLocation getDestinationLocation() { return destinationLocation; }
    public void setDestinationLocation(InventoryLocation destinationLocation) { this.destinationLocation = destinationLocation; }

    public StockTransferStatus getStatus() { return status; }
    public void setStatus(StockTransferStatus status) { this.status = status; }

    public LocalDate getTransferDate() { return transferDate; }
    public void setTransferDate(LocalDate transferDate) { this.transferDate = transferDate; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public String getCompletedBy() { return completedBy; }
    public void setCompletedBy(String completedBy) { this.completedBy = completedBy; }

    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
