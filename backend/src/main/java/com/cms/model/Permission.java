package com.cms.model;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "permissions")
@EntityListeners(AuditingEntityListener.class)
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String code;

    @Column(name = "display_name", nullable = false, length = 150)
    private String displayName;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(length = 500)
    private String description;

    @Column(name = "screen_label", length = 100)
    private String screenLabel;

    /**
     * Delegation tier (1–4).
     * 1 = Dev Only, 2 = Support+, 3 = Hold Only (senior roles hold; only Support+ delegates),
     * 4 = Open (anyone who holds it can delegate it).
     */
    @Column(nullable = false)
    private int tier = 4;

    @ManyToMany(mappedBy = "permissions", fetch = FetchType.LAZY)
    private Set<AppRole> roles = new HashSet<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Permission() {
    }

    public Permission(String code, String displayName, String category, String description) {
        this.code = code;
        this.displayName = displayName;
        this.category = category;
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getTier() {
        return tier;
    }

    public void setTier(int tier) {
        this.tier = tier;
    }

    public String getScreenLabel() {
        return screenLabel;
    }

    public void setScreenLabel(String screenLabel) {
        this.screenLabel = screenLabel;
    }

    public Set<AppRole> getRoles() {
        return roles;
    }

    public void setRoles(Set<AppRole> roles) {
        this.roles = roles;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Whether a role at the given hierarchy level is allowed to hold (or delegate) a
     * permission of the given tier. Tier 1 = DEV_ADMIN only, 2 & 3 = level ≤ 2
     * (Support+), 4 = anyone. Shared by delegation-picker filtering, role-permission
     * save validation, and the tier-change auto-revoke sweep — keep these in sync.
     */
    public static boolean tierAllowsLevel(int tier, int hierarchyLevel) {
        return switch (tier) {
            case 1 -> hierarchyLevel <= 1;
            case 2, 3 -> hierarchyLevel <= 2;
            default -> true;
        };
    }
}
