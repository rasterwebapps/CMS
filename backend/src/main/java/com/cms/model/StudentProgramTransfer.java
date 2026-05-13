package com.cms.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "student_program_transfers")
public class StudentProgramTransfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "old_program_id", nullable = false)
    private Program oldProgram;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "new_program_id", nullable = false)
    private Program newProgram;

    @Column(name = "transferred_at", nullable = false)
    private Instant transferredAt;

    @Column(name = "transferred_by")
    private String transferredBy;

    @Column(name = "consent_confirmed", nullable = false)
    private boolean consentConfirmed;

    private String notes;

    public StudentProgramTransfer() {}

    public Long getId() { return id; }

    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }

    public Program getOldProgram() { return oldProgram; }
    public void setOldProgram(Program oldProgram) { this.oldProgram = oldProgram; }

    public Program getNewProgram() { return newProgram; }
    public void setNewProgram(Program newProgram) { this.newProgram = newProgram; }

    public Instant getTransferredAt() { return transferredAt; }
    public void setTransferredAt(Instant transferredAt) { this.transferredAt = transferredAt; }

    public String getTransferredBy() { return transferredBy; }
    public void setTransferredBy(String transferredBy) { this.transferredBy = transferredBy; }

    public boolean isConsentConfirmed() { return consentConfirmed; }
    public void setConsentConfirmed(boolean consentConfirmed) { this.consentConfirmed = consentConfirmed; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
