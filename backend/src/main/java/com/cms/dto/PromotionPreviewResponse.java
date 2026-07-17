package com.cms.dto;

import java.util.List;

public record PromotionPreviewResponse(
    Long cohortId,
    String cohortCode,
    Long fromTermInstanceId,
    String fromTermLabel,
    Long toTermInstanceId,
    String toTermLabel,
    Integer programTotalTerms,
    Integer maxDurationYears,
    List<StudentPromotionPreviewRow> students
) {}
