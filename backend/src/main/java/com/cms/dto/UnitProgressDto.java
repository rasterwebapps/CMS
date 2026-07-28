package com.cms.dto;

import java.time.LocalDate;
import java.util.List;

import com.cms.model.enums.AttendanceType;

public record UnitProgressDto(
    Long unitId,
    Integer unitNumber,
    String title,
    AttendanceType componentType,
    Integer plannedHours,
    boolean covered,
    List<LocalDate> coveredDates
) {}
