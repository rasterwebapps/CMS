package com.cms.inventory.ticket.model;

import java.time.Instant;
import java.time.LocalDate;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.cms.inventory.stock.model.InventoryLocation;
import com.cms.inventory.ticket.model.enums.ServiceTicketPriority;
import com.cms.inventory.ticket.model.enums.ServiceTicketStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
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
 * Phase 7's ("Gate Pass, Vendor-Owned Stock & Service Requests") third and final slice — a
 * generic complaint/service-request ticket raised at an {@link InventoryLocation}, per the
 * reference architecture ({@code ER_DIAGRAM_AND_MODULE_BOUNDARIES.md} §6). Independent of the
 * rest of the module — it doesn't reference a {@code Product}/{@code Asset}, since "something is
 * wrong, please come look at it" is a standalone request, not itself a stock/asset transaction.
 * No separate stored {@code TicketNumber}/{@code TicketDate}, unlike the ER doc's original field
 * list — {@code id}/{@code createdAt} already serve that role, consistent with how every other
 * business document in this app (Purchase Order, Gate Pass, etc.) displays "#{id}" rather than
 * maintaining a second generated number; see the "Service Ticket slice" decision-log entry.
 * {@code OPEN → IN_PROGRESS → RESOLVED → CLOSED}, see {@link ServiceTicketStatus}.
 */
@Entity
@Table(name = "service_tickets")
@EntityListeners(AuditingEntityListener.class)
public class ServiceTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private InventoryLocation location;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private ServiceTicketCategory category;

    @Column(name = "requested_by", nullable = false, length = 200)
    private String requestedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ServiceTicketPriority priority = ServiceTicketPriority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ServiceTicketStatus status = ServiceTicketStatus.OPEN;

    @Column(nullable = false, length = 1000)
    private String description;

    @Column(name = "assigned_to", length = 200)
    private String assignedTo;

    @Column(name = "assigned_at")
    private Instant assignedAt;

    @Column(name = "resolution_notes", length = 1000)
    private String resolutionNotes;

    @Column(name = "resolution_date")
    private LocalDate resolutionDate;

    @Column(name = "resolved_by", length = 255)
    private String resolvedBy;

    @Column(name = "feedback_rating")
    private Integer feedbackRating;

    @Column(name = "closed_by", length = 255)
    private String closedBy;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "cancelled_by", length = 255)
    private String cancelledBy;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    @Column(name = "created_by", length = 255)
    private String createdBy;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public InventoryLocation getLocation() { return location; }
    public void setLocation(InventoryLocation location) { this.location = location; }

    public ServiceTicketCategory getCategory() { return category; }
    public void setCategory(ServiceTicketCategory category) { this.category = category; }

    public String getRequestedBy() { return requestedBy; }
    public void setRequestedBy(String requestedBy) { this.requestedBy = requestedBy; }

    public ServiceTicketPriority getPriority() { return priority; }
    public void setPriority(ServiceTicketPriority priority) { this.priority = priority; }

    public ServiceTicketStatus getStatus() { return status; }
    public void setStatus(ServiceTicketStatus status) { this.status = status; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getAssignedTo() { return assignedTo; }
    public void setAssignedTo(String assignedTo) { this.assignedTo = assignedTo; }

    public Instant getAssignedAt() { return assignedAt; }
    public void setAssignedAt(Instant assignedAt) { this.assignedAt = assignedAt; }

    public String getResolutionNotes() { return resolutionNotes; }
    public void setResolutionNotes(String resolutionNotes) { this.resolutionNotes = resolutionNotes; }

    public LocalDate getResolutionDate() { return resolutionDate; }
    public void setResolutionDate(LocalDate resolutionDate) { this.resolutionDate = resolutionDate; }

    public String getResolvedBy() { return resolvedBy; }
    public void setResolvedBy(String resolvedBy) { this.resolvedBy = resolvedBy; }

    public Integer getFeedbackRating() { return feedbackRating; }
    public void setFeedbackRating(Integer feedbackRating) { this.feedbackRating = feedbackRating; }

    public String getClosedBy() { return closedBy; }
    public void setClosedBy(String closedBy) { this.closedBy = closedBy; }

    public Instant getClosedAt() { return closedAt; }
    public void setClosedAt(Instant closedAt) { this.closedAt = closedAt; }

    public String getCancelledBy() { return cancelledBy; }
    public void setCancelledBy(String cancelledBy) { this.cancelledBy = cancelledBy; }

    public Instant getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(Instant cancelledAt) { this.cancelledAt = cancelledAt; }

    public String getCancellationReason() { return cancellationReason; }
    public void setCancellationReason(String cancellationReason) { this.cancellationReason = cancellationReason; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
