package com.cms.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.cms.model.enums.ClassSessionType;
import com.cms.model.enums.DayOfWeek;
import com.cms.model.enums.OccurrenceSource;
import com.cms.model.enums.OccurrenceStatus;
import com.cms.model.enums.SpecialClassApprovalStatus;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * "This recurring {@link ClassSchedule} row actually happened on this specific date" anchor — the
 * shared spine for portion-completion progress (this table's original purpose), Phase 6's
 * faculty-absence substitution, and (BR-55) an ad-hoc special/remedial class or one row of a
 * whole-day-repeat batch that has no backing {@link ClassSchedule} row at all. Rows are created
 * lazily, only once something is actually logged/requested for a date — never pre-populated for
 * every theoretical occurrence.
 *
 * <p>{@link #getOccurrenceSource()} discriminates the two shapes: for {@code REGULAR} rows
 * {@link #getClassSchedule()} is always non-null and every field below {@code swapPartnerOccurrence}
 * is irrelevant; for {@code SPECIAL_CLASS}/{@code DAY_REPEAT} rows {@link #getClassSchedule()} is
 * always null and the special-class fields below carry the session's identity directly. The
 * database enforces this shape via {@code chk_session_occurrences_special_shape} (V374) — never
 * construct a row that violates it.
 */
@Entity
@Table(name = "session_occurrences",
    uniqueConstraints = @UniqueConstraint(columnNames = {"class_schedule_id", "occurrence_date"}))
@EntityListeners(AuditingEntityListener.class)
public class SessionOccurrence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Null for a {@code SPECIAL_CLASS}/{@code DAY_REPEAT} row (BR-55) — see class javadoc. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_schedule_id")
    private ClassSchedule classSchedule;

    @Column(name = "occurrence_date", nullable = false)
    private LocalDate occurrenceDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorded_by_faculty_id")
    private Faculty recordedByFaculty;

    @Column(length = 1000)
    private String remarks;

    @OneToMany(mappedBy = "sessionOccurrence", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SessionOccurrenceUnit> unitCoverages = new ArrayList<>();

    /** Null unless a substitute was applied for this date -- the recurring ClassSchedule.faculty
     *  is never mutated by the absence/substitution feature (Phase 6); this is the one-date-only
     *  override instead. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "effective_faculty_id")
    private Faculty effectiveFaculty;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "faculty_absence_id")
    private FacultyAbsence facultyAbsence;

    @Enumerated(EnumType.STRING)
    @Column(name = "occurrence_status", nullable = false, length = 20)
    private OccurrenceStatus occurrenceStatus = OccurrenceStatus.HELD;

    /** The other session's occurrence row when this one is one half of a Phase 7 staff-to-staff
     *  swap (null otherwise, including for a Phase 6 absence-substitute, which has no partner). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "swap_partner_occurrence_id")
    private SessionOccurrence swapPartnerOccurrence;

    // ---- BR-55 special-class fields (populated only when occurrenceSource != REGULAR) ----

    @Enumerated(EnumType.STRING)
    @Column(name = "occurrence_source", nullable = false, length = 20)
    private OccurrenceSource occurrenceSource = OccurrenceSource.REGULAR;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id")
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_offering_id")
    private CourseOffering courseOffering;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cohort_section_id")
    private CohortSection cohortSection;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "period_id")
    private Period period;

    @Enumerated(EnumType.STRING)
    @Column(name = "session_type", length = 20)
    private ClassSessionType sessionType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classroom_id")
    private Classroom classroom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lab_id")
    private Lab lab;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinical_venue_id")
    private ClinicalVenue clinicalVenue;

    /** The faculty this special class is scheduled for -- distinct from {@link #effectiveFaculty}/
     *  {@link #facultyAbsence}, which are substitution-specific and semantically wrong to reuse
     *  for a request that was never a substitution in the first place. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_faculty_id")
    private Faculty requestedFaculty;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", length = 20)
    private SpecialClassApprovalStatus approvalStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by_faculty_id")
    private Faculty requestedByFaculty;

    @Column(name = "requested_at")
    private Instant requestedAt;

    @Column(name = "request_reason", length = 500)
    private String requestReason;

    /** DAY_REPEAT only -- the source weekday this occurrence's session was copied from. */
    @Enumerated(EnumType.STRING)
    @Column(name = "source_day_of_week")
    private DayOfWeek sourceDayOfWeek;

    /** Null for a single-subject SPECIAL_CLASS request; shared by every row a single DAY_REPEAT
     *  submission creates, so the whole batch can be approved/rejected/displayed atomically. */
    @Column(name = "request_batch_id")
    private UUID requestBatchId;

    @Column(name = "approved_by")
    private String approvedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public SessionOccurrence() {
    }

    public SessionOccurrence(ClassSchedule classSchedule, LocalDate occurrenceDate) {
        this.classSchedule = classSchedule;
        this.occurrenceDate = occurrenceDate;
    }

    /** BR-55: builds a PENDING ad-hoc row with no backing {@link ClassSchedule}. Leaves
     *  {@link #requestBatchId} null (single-subject request) -- callers building a DAY_REPEAT
     *  batch set it afterward via {@link #setRequestBatchId(UUID)}, shared across the batch. */
    public static SessionOccurrence forSpecialClass(OccurrenceSource source, LocalDate occurrenceDate,
            Subject subject, CourseOffering courseOffering, CohortSection cohortSection, Period period,
            ClassSessionType sessionType, Faculty requestedFaculty, Faculty requestedByFaculty,
            String requestReason) {
        SessionOccurrence occurrence = new SessionOccurrence();
        occurrence.occurrenceSource = source;
        occurrence.occurrenceDate = occurrenceDate;
        occurrence.subject = subject;
        occurrence.courseOffering = courseOffering;
        occurrence.cohortSection = cohortSection;
        occurrence.period = period;
        occurrence.sessionType = sessionType;
        occurrence.requestedFaculty = requestedFaculty;
        occurrence.requestedByFaculty = requestedByFaculty;
        occurrence.requestReason = requestReason;
        occurrence.requestedAt = Instant.now();
        occurrence.approvalStatus = SpecialClassApprovalStatus.PENDING;
        return occurrence;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ClassSchedule getClassSchedule() {
        return classSchedule;
    }

    public void setClassSchedule(ClassSchedule classSchedule) {
        this.classSchedule = classSchedule;
    }

    public LocalDate getOccurrenceDate() {
        return occurrenceDate;
    }

    public void setOccurrenceDate(LocalDate occurrenceDate) {
        this.occurrenceDate = occurrenceDate;
    }

    public Faculty getRecordedByFaculty() {
        return recordedByFaculty;
    }

    public void setRecordedByFaculty(Faculty recordedByFaculty) {
        this.recordedByFaculty = recordedByFaculty;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public List<SessionOccurrenceUnit> getUnitCoverages() {
        return unitCoverages;
    }

    public void setUnitCoverages(List<SessionOccurrenceUnit> unitCoverages) {
        this.unitCoverages = unitCoverages;
    }

    public Faculty getEffectiveFaculty() {
        return effectiveFaculty;
    }

    public void setEffectiveFaculty(Faculty effectiveFaculty) {
        this.effectiveFaculty = effectiveFaculty;
    }

    public FacultyAbsence getFacultyAbsence() {
        return facultyAbsence;
    }

    public void setFacultyAbsence(FacultyAbsence facultyAbsence) {
        this.facultyAbsence = facultyAbsence;
    }

    public OccurrenceStatus getOccurrenceStatus() {
        return occurrenceStatus;
    }

    public void setOccurrenceStatus(OccurrenceStatus occurrenceStatus) {
        this.occurrenceStatus = occurrenceStatus;
    }

    public SessionOccurrence getSwapPartnerOccurrence() {
        return swapPartnerOccurrence;
    }

    public void setSwapPartnerOccurrence(SessionOccurrence swapPartnerOccurrence) {
        this.swapPartnerOccurrence = swapPartnerOccurrence;
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

    public OccurrenceSource getOccurrenceSource() {
        return occurrenceSource;
    }

    public void setOccurrenceSource(OccurrenceSource occurrenceSource) {
        this.occurrenceSource = occurrenceSource;
    }

    public Subject getSubject() {
        return subject;
    }

    public void setSubject(Subject subject) {
        this.subject = subject;
    }

    public CourseOffering getCourseOffering() {
        return courseOffering;
    }

    public void setCourseOffering(CourseOffering courseOffering) {
        this.courseOffering = courseOffering;
    }

    public CohortSection getCohortSection() {
        return cohortSection;
    }

    public void setCohortSection(CohortSection cohortSection) {
        this.cohortSection = cohortSection;
    }

    public Period getPeriod() {
        return period;
    }

    public void setPeriod(Period period) {
        this.period = period;
    }

    public ClassSessionType getSessionType() {
        return sessionType;
    }

    public void setSessionType(ClassSessionType sessionType) {
        this.sessionType = sessionType;
    }

    public Classroom getClassroom() {
        return classroom;
    }

    public void setClassroom(Classroom classroom) {
        this.classroom = classroom;
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

    public Faculty getRequestedFaculty() {
        return requestedFaculty;
    }

    public void setRequestedFaculty(Faculty requestedFaculty) {
        this.requestedFaculty = requestedFaculty;
    }

    public SpecialClassApprovalStatus getApprovalStatus() {
        return approvalStatus;
    }

    public void setApprovalStatus(SpecialClassApprovalStatus approvalStatus) {
        this.approvalStatus = approvalStatus;
    }

    public Faculty getRequestedByFaculty() {
        return requestedByFaculty;
    }

    public void setRequestedByFaculty(Faculty requestedByFaculty) {
        this.requestedByFaculty = requestedByFaculty;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(Instant requestedAt) {
        this.requestedAt = requestedAt;
    }

    public String getRequestReason() {
        return requestReason;
    }

    public void setRequestReason(String requestReason) {
        this.requestReason = requestReason;
    }

    public DayOfWeek getSourceDayOfWeek() {
        return sourceDayOfWeek;
    }

    public void setSourceDayOfWeek(DayOfWeek sourceDayOfWeek) {
        this.sourceDayOfWeek = sourceDayOfWeek;
    }

    public UUID getRequestBatchId() {
        return requestBatchId;
    }

    public void setRequestBatchId(UUID requestBatchId) {
        this.requestBatchId = requestBatchId;
    }

    public String getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(String approvedBy) {
        this.approvedBy = approvedBy;
    }

    public Instant getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(Instant approvedAt) {
        this.approvedAt = approvedAt;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }
}
