package com.cms.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

@Entity
@Table(name = "app_users")
@EntityListeners(AuditingEntityListener.class)
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "keycloak_username", nullable = false, unique = true)
    private String keycloakUsername;

    @Column(name = "keycloak_user_id")
    private String keycloakUserId;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "app_role_id")
    private AppRole appRole;

    /** Direct link to the student this account belongs to. Null for non-student roles. */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    private com.cms.model.Student linkedStudent;

    /** Direct link to the faculty member this account belongs to. Null for non-faculty roles. */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "faculty_id")
    private com.cms.model.Faculty linkedFaculty;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "created_by")
    private String createdBy;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Personal dashboard layout — empty means fall back to role default. */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("widgetOrder ASC")
    private List<UserDashboardWidgetConfig> widgetConfigs = new ArrayList<>();

    @Column(name = "profile_photo")
    private byte[] profilePhoto;

    @Column(name = "profile_photo_type", length = 50)
    private String profilePhotoType;

    @Column(name = "cover_photo")
    private byte[] coverPhoto;

    @Column(name = "cover_photo_type", length = 50)
    private String coverPhotoType;

    @Column(name = "bio", length = 500)
    private String bio;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "blood_group", length = 10)
    private String bloodGroup;

    @Column(name = "emergency_contact_name",         length = 100)
    private String emergencyContactName;
    @Column(name = "emergency_contact_relationship", length = 50)
    private String emergencyContactRelationship;
    @Column(name = "emergency_contact_phone",        length = 20)
    private String emergencyContactPhone;

    public AppUser() {
    }

    public AppUser(String keycloakUsername, String email, String fullName, AppRole appRole,
                   boolean isActive, String createdBy) {
        this.keycloakUsername = keycloakUsername;
        this.email = email;
        this.fullName = fullName;
        this.appRole = appRole;
        this.isActive = isActive;
        this.createdBy = createdBy;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getKeycloakUsername() {
        return keycloakUsername;
    }

    public void setKeycloakUsername(String keycloakUsername) {
        this.keycloakUsername = keycloakUsername;
    }

    public String getKeycloakUserId() {
        return keycloakUserId;
    }

    public void setKeycloakUserId(String keycloakUserId) {
        this.keycloakUserId = keycloakUserId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public AppRole getAppRole() {
        return appRole;
    }

    public void setAppRole(AppRole appRole) {
        this.appRole = appRole;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean isActive) {
        this.isActive = isActive;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public byte[] getProfilePhoto() {
        return profilePhoto;
    }

    public void setProfilePhoto(byte[] profilePhoto) {
        this.profilePhoto = profilePhoto;
    }

    public String getProfilePhotoType() {
        return profilePhotoType;
    }

    public void setProfilePhotoType(String profilePhotoType) {
        this.profilePhotoType = profilePhotoType;
    }

    public byte[] getCoverPhoto() { return coverPhoto; }
    public void setCoverPhoto(byte[] coverPhoto) { this.coverPhoto = coverPhoto; }

    public String getCoverPhotoType() { return coverPhotoType; }
    public void setCoverPhotoType(String coverPhotoType) { this.coverPhotoType = coverPhotoType; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getBloodGroup() { return bloodGroup; }
    public void setBloodGroup(String bloodGroup) { this.bloodGroup = bloodGroup; }

    public String getEmergencyContactName()         { return emergencyContactName; }
    public void setEmergencyContactName(String v)   { this.emergencyContactName = v; }
    public String getEmergencyContactRelationship() { return emergencyContactRelationship; }
    public void setEmergencyContactRelationship(String v) { this.emergencyContactRelationship = v; }
    public String getEmergencyContactPhone()        { return emergencyContactPhone; }
    public void setEmergencyContactPhone(String v)  { this.emergencyContactPhone = v; }

    public com.cms.model.Student getLinkedStudent()                     { return linkedStudent; }
    public void setLinkedStudent(com.cms.model.Student s)               { this.linkedStudent = s; }

    public com.cms.model.Faculty getLinkedFaculty()                     { return linkedFaculty; }
    public void setLinkedFaculty(com.cms.model.Faculty f)               { this.linkedFaculty = f; }

    public List<UserDashboardWidgetConfig> getWidgetConfigs() {
        return widgetConfigs;
    }

    public void setWidgetConfigs(List<UserDashboardWidgetConfig> widgetConfigs) {
        this.widgetConfigs = widgetConfigs;
    }

    /** True when the user has saved a personal dashboard layout. */
    public boolean hasPersonalDashboard() {
        return !widgetConfigs.isEmpty();
    }
}
