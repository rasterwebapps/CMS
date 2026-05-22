package com.cms.dto;

public record FeeStateResponse(
    Long id,
    String name,
    String code,
    boolean isDefault,
    boolean isFallback,
    int sortOrder
) {}
