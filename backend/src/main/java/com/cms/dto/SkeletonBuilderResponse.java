package com.cms.dto;

import java.util.List;

public record SkeletonBuilderResponse(
    Long courseOfferingId,
    String subjectName,
    String subjectCode,
    String termInstanceLabel,
    List<SkeletonSubjectBudget> budgets,
    List<SkeletonCellResponse> cells,
    List<BatchDto> batches
) {}
