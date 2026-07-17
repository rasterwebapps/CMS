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
@Table(name = "notification_dismissals",
    uniqueConstraints = @UniqueConstraint(columnNames = {"notification_id", "user_id"}))
public class NotificationDismissal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "notification_id", nullable = false)
    private Notification notification;

    @Column(name = "user_id", nullable = false, length = 255)
    private String userId;

    @Column(name = "dismissed_at", nullable = false)
    private Instant dismissedAt = Instant.now();

    public NotificationDismissal() {}

    public NotificationDismissal(Notification notification, String userId) {
        this.notification = notification;
        this.userId = userId;
    }

    public Long getId() { return id; }
    public Notification getNotification() { return notification; }
    public String getUserId() { return userId; }
    public Instant getDismissedAt() { return dismissedAt; }
}
