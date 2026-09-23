package com.cms.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "announcements")
public class Announcement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false)
    private String body;

    @Column(name = "created_by", nullable = false, length = 255)
    private String createdBy;

    @Column(name = "published_at", nullable = false)
    private Instant publishedAt = Instant.now();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @OneToMany(mappedBy = "announcement", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AnnouncementAudience> audiences = new ArrayList<>();

    public Announcement() {}

    public Announcement(String title, String body, String createdBy) {
        this.title = title;
        this.body = body;
        this.createdBy = createdBy;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public String getCreatedBy() { return createdBy; }
    public Instant getPublishedAt() { return publishedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public List<AnnouncementAudience> getAudiences() { return audiences; }

    public void addAudience(AnnouncementAudience audience) {
        audience.setAnnouncement(this);
        this.audiences.add(audience);
    }
}
