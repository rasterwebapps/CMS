package com.cms.dto;

import java.time.Instant;

import com.cms.model.enums.AdmissionCategory;

public record AdmissionCategoryHistoryRecord(
    Long id,
    Long studentId,
    String studentName,
    AdmissionCategory oldCategory,
    AdmissionCategory newCategory,
    String reason,
    String changedBy,
    Instant changedAt
) {
}
