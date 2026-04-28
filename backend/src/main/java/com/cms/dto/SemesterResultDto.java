package com.cms.dto;

import java.math.BigDecimal;

import com.cms.model.enums.ResultStatus;

public record SemesterResultDto(
    Long id,
    Long studentTermEnrollmentId,
    Long studentId,
    String studentName,
    Long termInstanceId,
    String termInstanceLabel,
    BigDecimal totalMaxMarks,
    BigDecimal totalMarksObtained,
    BigDecimal percentage,
    ResultStatus resultStatus,
    Boolean isLocked
) {}
