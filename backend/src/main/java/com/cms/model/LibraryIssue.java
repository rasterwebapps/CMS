package com.cms.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.cms.model.enums.IssueStatus;
import com.cms.model.enums.LibraryItemType;
import com.cms.model.enums.LibraryMemberType;

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

@Entity
@Table(name = "library_issues")
@EntityListeners(AuditingEntityListener.class)
public class LibraryIssue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id")
    private LibraryBook book;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "periodical_id")
    private LibraryPeriodical periodical;

    @Enumerated(EnumType.STRING)
    @Column(name = "member_type", nullable = false, length = 10)
    private LibraryMemberType memberType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "faculty_id")
    private Faculty faculty;

    @Column(name = "issued_date", nullable = false)
    private LocalDate issuedDate;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "returned_date")
    private LocalDate returnedDate;

    @Column(name = "renewal_count", nullable = false)
    private int renewalCount = 0;

    @Column(name = "last_renewed_date")
    private LocalDate lastRenewedDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IssueStatus status = IssueStatus.ISSUED;

    @Column(name = "issued_by", nullable = false, length = 100)
    private String issuedBy;

    @Column(name = "returned_to", length = 100)
    private String returnedTo;

    @Column(length = 500)
    private String remarks;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LibraryBook getBook() { return book; }
    public void setBook(LibraryBook book) { this.book = book; }

    public LibraryPeriodical getPeriodical() { return periodical; }
    public void setPeriodical(LibraryPeriodical periodical) { this.periodical = periodical; }

    // ── Derived item-agnostic accessors (book XOR periodical) ──────

    public LibraryItemType getItemType() {
        return book != null ? LibraryItemType.BOOK : LibraryItemType.JOURNAL;
    }

    public Long getItemId() {
        return book != null ? book.getId() : periodical.getId();
    }

    public String getItemAccessionNumber() {
        return book != null ? book.getAccessionNumber() : periodical.getAccessionNumber();
    }

    public String getItemTitle() {
        return book != null ? book.getTitle() : periodical.getJournalName();
    }

    /** Book authors, or a "Vol X • Issue Y • Month Year" summary for a journal. */
    public String getItemDetail() {
        if (book != null) return book.getAuthors();
        List<String> parts = new ArrayList<>();
        if (periodical.getVolumeNumber() != null) parts.add("Vol " + periodical.getVolumeNumber());
        if (periodical.getIssueNumber() != null) parts.add("Issue " + periodical.getIssueNumber());
        String monthYear = ((periodical.getMonthRange() != null ? periodical.getMonthRange() : "")
            + (periodical.getYear() != null ? " " + periodical.getYear() : "")).trim();
        if (!monthYear.isEmpty()) parts.add(monthYear);
        return String.join(" • ", parts);
    }

    /** Books only — journals have no shelf/call-number scheme. */
    public String getCallNumber() {
        return book != null ? book.getCallNumber() : null;
    }

    public String getShelfLocation() {
        return book != null ? book.getShelfLocation() : null;
    }

    public LibraryMemberType getMemberType() { return memberType; }
    public void setMemberType(LibraryMemberType memberType) { this.memberType = memberType; }

    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }

    public Faculty getFaculty() { return faculty; }
    public void setFaculty(Faculty faculty) { this.faculty = faculty; }

    public LocalDate getIssuedDate() { return issuedDate; }
    public void setIssuedDate(LocalDate issuedDate) { this.issuedDate = issuedDate; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public LocalDate getReturnedDate() { return returnedDate; }
    public void setReturnedDate(LocalDate returnedDate) { this.returnedDate = returnedDate; }

    public int getRenewalCount() { return renewalCount; }
    public void setRenewalCount(int renewalCount) { this.renewalCount = renewalCount; }

    public LocalDate getLastRenewedDate() { return lastRenewedDate; }
    public void setLastRenewedDate(LocalDate lastRenewedDate) { this.lastRenewedDate = lastRenewedDate; }

    public IssueStatus getStatus() { return status; }
    public void setStatus(IssueStatus status) { this.status = status; }

    public String getIssuedBy() { return issuedBy; }
    public void setIssuedBy(String issuedBy) { this.issuedBy = issuedBy; }

    public String getReturnedTo() { return returnedTo; }
    public void setReturnedTo(String returnedTo) { this.returnedTo = returnedTo; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
