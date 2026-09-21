package com.cms.dto;

import java.time.Instant;
import java.time.LocalDate;

import com.cms.model.enums.TermInstanceStatus;
import com.cms.model.enums.TermType;

public record TermInstanceDto(
    Long id,
    Long academicYearId,
    String academicYearName,
    TermType termType,
    LocalDate startDate,
    LocalDate endDate,
    TermInstanceStatus status,
    Instant createdAt,
    Instant updatedAt,
    /** How many real working Saturdays this term has, per its configured pattern (see
     *  WorkingSaturdaysFlyoutComponent) -- 0 means the term hasn't opted in to Saturday scheduling
     *  at all (Mon-Fri only). Drives the published timetable's Generic week grid's Saturday
     *  column visibility. */
    long workingSaturdayCount
) {}
