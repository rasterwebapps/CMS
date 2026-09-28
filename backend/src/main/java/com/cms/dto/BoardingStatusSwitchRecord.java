package com.cms.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.cms.model.enums.StudentType;

public record BoardingStatusSwitchRecord(
    Long id,
    Long studentId,
    String studentName,
    StudentType oldStudentType,
    StudentType newStudentType,
    Instant switchedAt,
    String switchedBy,
    String remarks,
    int demandsAdjusted,
    BigDecimal feeDelta
) {}
