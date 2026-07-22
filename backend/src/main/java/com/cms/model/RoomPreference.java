package com.cms.model;

import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.cms.model.enums.RoomPreferenceStatus;

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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Non-binding room request (R2-4.1.3): a student's preferred {@link HostelRoomType} and,
 * optionally, {@link Zone}, captured at enquiry and/or admission before any {@link HostelRoom} is
 * actually assigned. Exactly one of {@code enquiry}/{@code student} is set at a time — the same
 * row is carried forward (its {@code student} FK populated) when the enquiry converts, rather than
 * duplicated, since a preference can keep changing across enquiry -> admission -> later. Distinct
 * from {@link RoomAllocation}, which is the binding, capacity-consuming assignment.
 */
@Entity
@Table(name = "room_preferences")
@EntityListeners(AuditingEntityListener.class)
public class RoomPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enquiry_id")
    private Enquiry enquiry;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "preferred_room_type_id", nullable = false)
    private HostelRoomType preferredRoomType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "preferred_zone_id")
    private Zone preferredZone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RoomPreferenceStatus status = RoomPreferenceStatus.PENDING;

    @Column(length = 500)
    private String remarks;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public RoomPreference() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Enquiry getEnquiry() { return enquiry; }
    public void setEnquiry(Enquiry enquiry) { this.enquiry = enquiry; }

    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }

    public HostelRoomType getPreferredRoomType() { return preferredRoomType; }
    public void setPreferredRoomType(HostelRoomType preferredRoomType) { this.preferredRoomType = preferredRoomType; }

    public Zone getPreferredZone() { return preferredZone; }
    public void setPreferredZone(Zone preferredZone) { this.preferredZone = preferredZone; }

    public RoomPreferenceStatus getStatus() { return status; }
    public void setStatus(RoomPreferenceStatus status) { this.status = status; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
