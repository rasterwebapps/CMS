package com.cms.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "announcement_reads",
    uniqueConstraints = @UniqueConstraint(columnNames = {"announcement_id", "user_id"}))
public class AnnouncementRead {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "announcement_id", nullable = false)
    private Announcement announcement;

    @Column(name = "user_id", nullable = false, length = 255)
    private String userId;

    @Column(name = "read_at", nullable = false)
    private Instant readAt = Instant.now();

    public AnnouncementRead() {}

    public AnnouncementRead(Announcement announcement, String userId) {
        this.announcement = announcement;
        this.userId = userId;
    }

    public Long getId() { return id; }
    public Announcement getAnnouncement() { return announcement; }
    public String getUserId() { return userId; }
    public Instant getReadAt() { return readAt; }
}
