package com.cms.model;

import java.time.Instant;

import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "user_notification_preferences",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "category_key"}))
@EntityListeners(AuditingEntityListener.class)
public class UserNotificationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, length = 255)
    private String userId;

    @Column(name = "category_key", nullable = false, length = 50)
    private String categoryKey;

    @Column(nullable = false)
    private Boolean enabled = true;

    /** Delivery channel: IN_APP, EMAIL, BOTH */
    @Column(nullable = false, length = 20)
    private String channel = "IN_APP";

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UserNotificationPreference() {}

    public UserNotificationPreference(String userId, String categoryKey, Boolean enabled, String channel) {
        this.userId = userId;
        this.categoryKey = categoryKey;
        this.enabled = enabled;
        this.channel = channel;
    }

    public Long getId() { return id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getCategoryKey() { return categoryKey; }
    public void setCategoryKey(String categoryKey) { this.categoryKey = categoryKey; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public Instant getUpdatedAt() { return updatedAt; }
}
