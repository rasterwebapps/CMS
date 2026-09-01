package com.cms.model;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "batches",
    uniqueConstraints = @UniqueConstraint(columnNames = {"course_offering_id", "name"}))
@EntityListeners(AuditingEntityListener.class)
public class Batch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_offering_id", nullable = false)
    private CourseOffering courseOffering;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private Integer capacity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "term_instance_id", nullable = false)
    private TermInstance termInstance;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coordinator_faculty_id")
    private Faculty coordinatorFaculty;

    /** Physical venue this batch was assigned during Cohort Room Allocation — at most one of
     *  {@link #lab}/{@link #clinicalVenue} is set, mirroring ClassSchedule's session-shape CHECK.
     *  Null for batches created outside that flow (e.g. the older manual/auto-create paths). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lab_id")
    private Lab lab;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinical_venue_id")
    private ClinicalVenue clinicalVenue;

    /** Traces this batch back to the CohortRoomAllocation commit that created it, so a revert can
     *  find and deactivate exactly the batches it produced without touching unrelated ones. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cohort_room_allocation_id")
    private CohortRoomAllocation cohortRoomAllocation;

    /** Which CohortSection sub-cohort this batch belongs to, once its cohort's Theory room has
     *  been split into sections. Null for batches under an unsectioned (single-section)
     *  allocation, or created outside the Cohort Room Allocation flow. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cohort_section_id")
    private CohortSection cohortSection;

    /** Which off-campus shift window this batch's clinical block runs under, when the offering
     *  uses shift-based scheduling (OC-175). Several batches — each keeping their own
     *  {@link #lab}/{@link #clinicalVenue} — can share one group when they run clinical in
     *  parallel at different venues under the same shift; the group's shared theory block is
     *  attended by the whole reconvened roster, not scoped to any single batch. Null for batches
     *  outside shift-based clinical scheduling. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinical_shift_group_id")
    private ClinicalShiftGroup clinicalShiftGroup;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "batch_students",
        joinColumns = @JoinColumn(name = "batch_id"),
        inverseJoinColumns = @JoinColumn(name = "student_id")
    )
    private Set<Student> students = new HashSet<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Batch() {
    }

    public Batch(CourseOffering courseOffering, String name, Integer capacity, TermInstance termInstance) {
        this.courseOffering = courseOffering;
        this.name = name;
        this.capacity = capacity;
        this.termInstance = termInstance;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public CourseOffering getCourseOffering() {
        return courseOffering;
    }

    public void setCourseOffering(CourseOffering courseOffering) {
        this.courseOffering = courseOffering;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public TermInstance getTermInstance() {
        return termInstance;
    }

    public void setTermInstance(TermInstance termInstance) {
        this.termInstance = termInstance;
    }

    public Faculty getCoordinatorFaculty() {
        return coordinatorFaculty;
    }

    public void setCoordinatorFaculty(Faculty coordinatorFaculty) {
        this.coordinatorFaculty = coordinatorFaculty;
    }

    public Lab getLab() {
        return lab;
    }

    public void setLab(Lab lab) {
        this.lab = lab;
    }

    public ClinicalVenue getClinicalVenue() {
        return clinicalVenue;
    }

    public void setClinicalVenue(ClinicalVenue clinicalVenue) {
        this.clinicalVenue = clinicalVenue;
    }

    public CohortRoomAllocation getCohortRoomAllocation() {
        return cohortRoomAllocation;
    }

    public void setCohortRoomAllocation(CohortRoomAllocation cohortRoomAllocation) {
        this.cohortRoomAllocation = cohortRoomAllocation;
    }

    public CohortSection getCohortSection() {
        return cohortSection;
    }

    public void setCohortSection(CohortSection cohortSection) {
        this.cohortSection = cohortSection;
    }

    public ClinicalShiftGroup getClinicalShiftGroup() {
        return clinicalShiftGroup;
    }

    public void setClinicalShiftGroup(ClinicalShiftGroup clinicalShiftGroup) {
        this.clinicalShiftGroup = clinicalShiftGroup;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Set<Student> getStudents() {
        return students;
    }

    public void setStudents(Set<Student> students) {
        this.students = students;
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
}
