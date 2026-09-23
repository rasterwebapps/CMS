package com.cms.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "category_key", nullable = false, length = 50)
    private String categoryKey;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false)
    private String message;

    @Column(length = 255)
    private String link;

    @Column(name = "source_type", length = 50)
    private String sourceType;

    @Column(name = "source_id")
    private Long sourceId;

    /** Null means broadcast-to-category (every user who can see the category, unchanged existing
     *  behavior). Non-null scopes this row to exactly one faculty member -- e.g. the
     *  holidayDisruption alert notifying just the affected faculty and their HOD/coordinator
     *  instead of everyone. See {@code HolidayDisruptionNotificationService}. */
    @Column(name = "recipient_faculty_id")
    private Long recipientFacultyId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    public Notification() {}

    public Notification(String categoryKey, String title, String message, String link,
                         String sourceType, Long sourceId) {
        this(categoryKey, title, message, link, sourceType, sourceId, null);
    }

    public Notification(String categoryKey, String title, String message, String link,
                         String sourceType, Long sourceId, Long recipientFacultyId) {
        this.categoryKey = categoryKey;
        this.title = title;
        this.message = message;
        this.link = link;
        this.sourceType = sourceType;
        this.sourceId = sourceId;
        this.recipientFacultyId = recipientFacultyId;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCategoryKey() { return categoryKey; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public String getLink() { return link; }
    public String getSourceType() { return sourceType; }
    public Long getSourceId() { return sourceId; }
    public Long getRecipientFacultyId() { return recipientFacultyId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(Instant resolvedAt) { this.resolvedAt = resolvedAt; }
}
