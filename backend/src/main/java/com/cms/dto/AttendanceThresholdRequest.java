package com.cms.dto;

import com.cms.model.enums.AttendanceType;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record AttendanceThresholdRequest(
    @NotNull(message = "Curriculum term course ID is required")
    Long curriculumTermCourseId,

    @NotNull(message = "Attendance type is required")
    AttendanceType attendanceType,

    @NotNull(message = "Minimum percentage is required")
    @DecimalMin(value = "0.0", message = "Minimum percentage cannot be negative")
    @DecimalMax(value = "100.0", message = "Minimum percentage cannot exceed 100")
    java.math.BigDecimal minPercentage
) {}
