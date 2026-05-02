package com.cms.dto;

public record ImportRowError(
    String sheet,
    int    rowNumber,
    String column,
    String message,
    String severity   // "ERROR" | "WARNING"
) {}
