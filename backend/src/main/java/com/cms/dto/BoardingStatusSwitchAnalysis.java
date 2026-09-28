package com.cms.dto;

import java.math.BigDecimal;

import com.cms.model.enums.StudentType;

public record BoardingStatusSwitchAnalysis(
    Long studentId,
    String studentName,
    StudentType currentStudentType,
    StudentType targetStudentType,
    boolean blocked,
    String blockReason,
    int demandsAffected,
    BigDecimal estimatedFeeDelta
) {}
