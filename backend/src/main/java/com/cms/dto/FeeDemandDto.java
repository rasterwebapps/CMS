package com.cms.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.cms.model.enums.DemandStatus;

public record FeeDemandDto(
    Long id,
    Long enrollmentId,
    Long studentId,
    String studentName,
    String cohortCode,
    Long termInstanceId,
    String termInstanceLabel,
    Long academicYearId,
    String academicYearName,
    BigDecimal totalAmount,
    LocalDate dueDate,
    BigDecimal paidAmount,
    BigDecimal outstandingAmount,
    DemandStatus status
) {}
