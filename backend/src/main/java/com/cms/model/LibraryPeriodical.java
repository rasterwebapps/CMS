package com.cms.model;

import java.time.Instant;
import java.time.LocalDate;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.cms.model.enums.JournalType;
import com.cms.model.enums.SubscriptionStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "library_periodicals")
@EntityListeners(AuditingEntityListener.class)
public class LibraryPeriodical {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "journal_name", nullable = false, length = 300)
    private String journalName;

    @Enumerated(EnumType.STRING)
    @Column(name = "journal_type", nullable = false, length = 20)
    private JournalType journalType = JournalType.NATIONAL;

    @Column(length = 200)
    private String organization;

    @Column(name = "volume_number", length = 20)
    private String volumeNumber;

    @Column(name = "issue_number", length = 20)
    private String issueNumber;

    @Column(name = "month_range", length = 30)
    private String monthRange;

    private Integer year;

    @Column(name = "copies_count", nullable = false)
    private int copiesCount = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "subscription_status", nullable = false, length = 20)
    private SubscriptionStatus subscriptionStatus = SubscriptionStatus.ACTIVE;

    @Column(name = "received_date")
    private LocalDate receivedDate;

    @Column(name = "received_by", length = 100)
    private String receivedBy;

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

    public String getJournalName() { return journalName; }
    public void setJournalName(String journalName) { this.journalName = journalName; }

    public JournalType getJournalType() { return journalType; }
    public void setJournalType(JournalType journalType) { this.journalType = journalType; }

    public String getOrganization() { return organization; }
    public void setOrganization(String organization) { this.organization = organization; }

    public String getVolumeNumber() { return volumeNumber; }
    public void setVolumeNumber(String volumeNumber) { this.volumeNumber = volumeNumber; }

    public String getIssueNumber() { return issueNumber; }
    public void setIssueNumber(String issueNumber) { this.issueNumber = issueNumber; }

    public String getMonthRange() { return monthRange; }
    public void setMonthRange(String monthRange) { this.monthRange = monthRange; }

    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }

    public int getCopiesCount() { return copiesCount; }
    public void setCopiesCount(int copiesCount) { this.copiesCount = copiesCount; }

    public SubscriptionStatus getSubscriptionStatus() { return subscriptionStatus; }
    public void setSubscriptionStatus(SubscriptionStatus subscriptionStatus) { this.subscriptionStatus = subscriptionStatus; }

    public LocalDate getReceivedDate() { return receivedDate; }
    public void setReceivedDate(LocalDate receivedDate) { this.receivedDate = receivedDate; }

    public String getReceivedBy() { return receivedBy; }
    public void setReceivedBy(String receivedBy) { this.receivedBy = receivedBy; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
