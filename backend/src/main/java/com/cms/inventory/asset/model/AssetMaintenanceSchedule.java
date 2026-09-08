package com.cms.inventory.asset.model;

import java.time.Instant;
import java.time.LocalDate;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.cms.inventory.asset.model.enums.MaintenanceScheduleType;

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
 * Phase 5's second slice, first half — a planned maintenance visit against one {@link Asset},
 * either {@code ONE_OFF} or {@code RECURRING} (a fixed number of days between visits, kept as a
 * plain interval rather than a frequency enum + custom-value pair — simplest thing that covers
 * monthly/quarterly/yearly/anything-else uniformly). {@link #markPerformed} in the service is
 * what advances {@code nextDueDate} for a recurring schedule; "due soon"/"overdue" are computed
 * at read time from {@code nextDueDate}, never stored, same pattern {@code LoanableItemIssue}'s
 * overdue flag already established. See the "Maintenance & Service Contracts slice"
 * decision-log entry.
 */
@Entity
@Table(name = "asset_maintenance_schedules")
@EntityListeners(AuditingEntityListener.class)
public class AssetMaintenanceSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @Enumerated(EnumType.STRING)
    @Column(name = "schedule_type", nullable = false, length = 20)
    private MaintenanceScheduleType scheduleType;

    @Column(name = "recurrence_interval_days")
    private Integer recurrenceIntervalDays;

    @Column(name = "next_due_date", nullable = false)
    private LocalDate nextDueDate;

    @Column(name = "last_performed_date")
    private LocalDate lastPerformedDate;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(length = 500)
    private String notes;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Asset getAsset() { return asset; }
    public void setAsset(Asset asset) { this.asset = asset; }

    public MaintenanceScheduleType getScheduleType() { return scheduleType; }
    public void setScheduleType(MaintenanceScheduleType scheduleType) { this.scheduleType = scheduleType; }

    public Integer getRecurrenceIntervalDays() { return recurrenceIntervalDays; }
    public void setRecurrenceIntervalDays(Integer recurrenceIntervalDays) { this.recurrenceIntervalDays = recurrenceIntervalDays; }

    public LocalDate getNextDueDate() { return nextDueDate; }
    public void setNextDueDate(LocalDate nextDueDate) { this.nextDueDate = nextDueDate; }

    public LocalDate getLastPerformedDate() { return lastPerformedDate; }
    public void setLastPerformedDate(LocalDate lastPerformedDate) { this.lastPerformedDate = lastPerformedDate; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
