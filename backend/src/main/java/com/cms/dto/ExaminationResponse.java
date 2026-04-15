package com.cms.dto;

import java.time.Instant;
import java.time.LocalDate;

import com.cms.model.enums.ExamType;

public record ExaminationResponse(
    Long id,
    String name,
    Long subjectId,
    String subjectName,
    ExamType examType,
    LocalDate date,
    Integer duration,
    Integer maxMarks,
    Long semesterId,
    String semesterName,
    Instant createdAt,
    Instant updatedAt
) {}
