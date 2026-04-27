package com.cms.dto;

import java.math.BigDecimal;

public record CourseStatsDto(
    Long courseOfferingId,
    String subjectName,
    String subjectCode,
    Long termInstanceId,
    String termInstanceLabel,
    int totalStudents,
    int presentCount,
    int absentCount,
    int malpracticeCount,
    BigDecimal averageMarks,
    BigDecimal maxMarks
) {}
