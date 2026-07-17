package com.cms.dto;

public record CohortTermOption(
    Long termInstanceId,
    String termLabel,
    long enrolledCount
) {}
