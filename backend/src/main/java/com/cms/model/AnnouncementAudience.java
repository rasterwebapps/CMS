package com.cms.model;

import java.time.Instant;

import com.cms.model.enums.AnnouncementAudienceType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "announcement_audiences")
public class AnnouncementAudience {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "announcement_id", nullable = false)
    private Announcement announcement;

    @Enumerated(EnumType.STRING)
    @Column(name = "audience_type", nullable = false, length = 10)
    private AnnouncementAudienceType audienceType;

    @Column(name = "audience_ref_id")
    private Long audienceRefId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public AnnouncementAudience() {}

    public AnnouncementAudience(AnnouncementAudienceType audienceType, Long audienceRefId) {
        this.audienceType = audienceType;
        this.audienceRefId = audienceRefId;
    }

    public Long getId() { return id; }
    public Announcement getAnnouncement() { return announcement; }
    public void setAnnouncement(Announcement announcement) { this.announcement = announcement; }
    public AnnouncementAudienceType getAudienceType() { return audienceType; }
    public Long getAudienceRefId() { return audienceRefId; }
    public Instant getCreatedAt() { return createdAt; }
}
