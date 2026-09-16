package com.cms.model;

import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** Many-to-many link between a Student and a Guardian -- the real cardinality this app needs
 *  (one guardian can have multiple wards/siblings; a student can have more than one guardian),
 *  which neither a 1:1 FK on Student nor a plain @ManyToMany (no room for {@code isPrimary})
 *  could represent. This codebase has no existing composite-key entity to mirror, so it follows
 *  every other entity's own style: a surrogate id PK plus a real unique constraint. */
@Entity
@Table(name = "student_guardians", uniqueConstraints = @UniqueConstraint(
    name = "uq_student_guardians_pair", columnNames = {"student_id", "guardian_id"}))
@EntityListeners(AuditingEntityListener.class)
public class StudentGuardian {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "guardian_id", nullable = false)
    private Guardian guardian;

    /** Which guardian receives default notifications, if that's ever built. Purely a display/
     *  ordering hint -- carries no access-control meaning of its own. */
    @Column(name = "is_primary", nullable = false)
    private boolean isPrimary = false;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }

    public Guardian getGuardian() { return guardian; }
    public void setGuardian(Guardian guardian) { this.guardian = guardian; }

    public boolean isPrimary() { return isPrimary; }
    public void setPrimary(boolean primary) { isPrimary = primary; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
