package com.cms.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import com.cms.model.enums.DayOfWeek;

public record FacultyAvailabilityResponse(
    Long id,
    Long facultyId,
    String facultyName,
    DayOfWeek dayOfWeek,
    LocalTime startTime,
    LocalTime endTime,
    String reason,
    LocalDate startDate,
    LocalDate endDate,
    Instant createdAt,
    Instant updatedAt
) {}
