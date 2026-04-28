package com.cms.dto;

import java.math.BigDecimal;

import com.cms.model.enums.MarkStatus;

public record StudentMarkDto(
    Long id,
    Long examEventId,
    String subjectName,
    Long courseRegistrationId,
    Long studentId,
    String studentName,
    MarkStatus markStatus,
    BigDecimal marksObtained,
    BigDecimal maxMarks,
    String remarks
) {}
