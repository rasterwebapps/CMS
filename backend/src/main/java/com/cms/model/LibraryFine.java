package com.cms.model;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.cms.model.enums.FineStatus;

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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "library_fines")
@EntityListeners(AuditingEntityListener.class)
public class LibraryFine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issue_id", nullable = false, unique = true)
    private LibraryIssue issue;

    @Column(name = "overdue_days", nullable = false)
    private int overdueDays;

    @Column(name = "fine_per_day", nullable = false, precision = 10, scale = 2)
    private BigDecimal finePerDay;

    @Column(name = "total_fine", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalFine;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FineStatus status = FineStatus.PENDING;

    @Column(name = "waived_by", length = 100)
    private String waivedBy;

    @Column(name = "collected_at")
    private Instant collectedAt;

    @Column(length = 500)
    private String remarks;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LibraryIssue getIssue() { return issue; }
    public void setIssue(LibraryIssue issue) { this.issue = issue; }

    public int getOverdueDays() { return overdueDays; }
    public void setOverdueDays(int overdueDays) { this.overdueDays = overdueDays; }

    public BigDecimal getFinePerDay() { return finePerDay; }
    public void setFinePerDay(BigDecimal finePerDay) { this.finePerDay = finePerDay; }

    public BigDecimal getTotalFine() { return totalFine; }
    public void setTotalFine(BigDecimal totalFine) { this.totalFine = totalFine; }

    public FineStatus getStatus() { return status; }
    public void setStatus(FineStatus status) { this.status = status; }

    public String getWaivedBy() { return waivedBy; }
    public void setWaivedBy(String waivedBy) { this.waivedBy = waivedBy; }

    public Instant getCollectedAt() { return collectedAt; }
    public void setCollectedAt(Instant collectedAt) { this.collectedAt = collectedAt; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
