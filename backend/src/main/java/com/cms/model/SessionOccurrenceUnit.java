package com.cms.model;

import java.math.BigDecimal;

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

/**
 * How much of one {@link SyllabusUnit} was actually covered in one {@link SessionOccurrence} --
 * {@code hoursCovered} is a faculty-entered record of time actually spent (defaults to the
 * period's length in the UI, but is never assumed to equal it: a unit can finish early, or a
 * period can go partly unused, and the log should reflect what really happened, not a rigid
 * schedule). {@code markedComplete} is a deliberate faculty action, never inferred from
 * hoursCovered crossing the unit's plannedHours -- a unit finishing in fewer hours than planned
 * is still "complete" the moment the faculty says so.
 */
@Entity
@Table(name = "session_occurrence_units",
    uniqueConstraints = @UniqueConstraint(columnNames = {"session_occurrence_id", "syllabus_unit_id"}))
public class SessionOccurrenceUnit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_occurrence_id", nullable = false)
    private SessionOccurrence sessionOccurrence;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "syllabus_unit_id", nullable = false)
    private SyllabusUnit syllabusUnit;

    @Column(name = "hours_covered", precision = 5, scale = 2)
    private BigDecimal hoursCovered;

    @Column(name = "marked_complete", nullable = false)
    private Boolean markedComplete = false;

    public SessionOccurrenceUnit() {
    }

    public SessionOccurrenceUnit(SessionOccurrence sessionOccurrence, SyllabusUnit syllabusUnit,
                                  BigDecimal hoursCovered, Boolean markedComplete) {
        this.sessionOccurrence = sessionOccurrence;
        this.syllabusUnit = syllabusUnit;
        this.hoursCovered = hoursCovered;
        this.markedComplete = markedComplete != null ? markedComplete : false;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public SessionOccurrence getSessionOccurrence() {
        return sessionOccurrence;
    }

    public void setSessionOccurrence(SessionOccurrence sessionOccurrence) {
        this.sessionOccurrence = sessionOccurrence;
    }

    public SyllabusUnit getSyllabusUnit() {
        return syllabusUnit;
    }

    public void setSyllabusUnit(SyllabusUnit syllabusUnit) {
        this.syllabusUnit = syllabusUnit;
    }

    public BigDecimal getHoursCovered() {
        return hoursCovered;
    }

    public void setHoursCovered(BigDecimal hoursCovered) {
        this.hoursCovered = hoursCovered;
    }

    public Boolean getMarkedComplete() {
        return markedComplete;
    }

    public void setMarkedComplete(Boolean markedComplete) {
        this.markedComplete = markedComplete != null ? markedComplete : false;
    }
}
