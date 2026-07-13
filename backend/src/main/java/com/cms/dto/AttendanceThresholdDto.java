package com.cms.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.cms.model.enums.AttendanceType;

public record AttendanceThresholdDto(
    Long id,
    Long curriculumTermCourseId,
    AttendanceType attendanceType,
    BigDecimal minPercentage,
    Instant createdAt,
    Instant updatedAt
) {}
