package com.cms.model;

import java.math.BigDecimal;
import java.time.Instant;

import com.cms.model.enums.StudentType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "student_boarding_status_switches")
public class StudentBoardingStatusSwitch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Enumerated(EnumType.STRING)
    @Column(name = "old_student_type", nullable = false, length = 20)
    private StudentType oldStudentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_student_type", nullable = false, length = 20)
    private StudentType newStudentType;

    @Column(name = "switched_at", nullable = false)
    private Instant switchedAt;

    @Column(name = "switched_by")
    private String switchedBy;

    private String remarks;

    @Column(name = "demands_adjusted", nullable = false)
    private int demandsAdjusted;

    @Column(name = "fee_delta", nullable = false, precision = 12, scale = 2)
    private BigDecimal feeDelta;

    public StudentBoardingStatusSwitch() {}

    public Long getId() { return id; }

    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }

    public StudentType getOldStudentType() { return oldStudentType; }
    public void setOldStudentType(StudentType oldStudentType) { this.oldStudentType = oldStudentType; }

    public StudentType getNewStudentType() { return newStudentType; }
    public void setNewStudentType(StudentType newStudentType) { this.newStudentType = newStudentType; }

    public Instant getSwitchedAt() { return switchedAt; }
    public void setSwitchedAt(Instant switchedAt) { this.switchedAt = switchedAt; }

    public String getSwitchedBy() { return switchedBy; }
    public void setSwitchedBy(String switchedBy) { this.switchedBy = switchedBy; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    public int getDemandsAdjusted() { return demandsAdjusted; }
    public void setDemandsAdjusted(int demandsAdjusted) { this.demandsAdjusted = demandsAdjusted; }

    public BigDecimal getFeeDelta() { return feeDelta; }
    public void setFeeDelta(BigDecimal feeDelta) { this.feeDelta = feeDelta; }
}
