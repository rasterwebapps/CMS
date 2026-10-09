package com.cms.model;

import java.math.BigDecimal;
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
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "student_term_fee_overrides",
    uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "semester_number"}))
public class StudentTermFeeOverride {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "semester_number", nullable = false)
    private Integer semesterNumber;

    @Column(name = "override_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal overrideAmount;

    @Column(name = "set_by")
    private String setBy;

    @Column(name = "set_at", nullable = false)
    private Instant setAt;

    public StudentTermFeeOverride() {}

    public Long getId() { return id; }

    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }

    public Integer getSemesterNumber() { return semesterNumber; }
    public void setSemesterNumber(Integer semesterNumber) { this.semesterNumber = semesterNumber; }

    public BigDecimal getOverrideAmount() { return overrideAmount; }
    public void setOverrideAmount(BigDecimal overrideAmount) { this.overrideAmount = overrideAmount; }

    public String getSetBy() { return setBy; }
    public void setSetBy(String setBy) { this.setBy = setBy; }

    public Instant getSetAt() { return setAt; }
    public void setSetAt(Instant setAt) { this.setAt = setAt; }
}
