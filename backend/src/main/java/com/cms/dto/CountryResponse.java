package com.cms.dto;

import java.time.Instant;

public record CountryResponse(
    Long id,
    String name,
    String isoCode,
    Boolean isActive,
    Instant createdAt,
    Instant updatedAt
) {}

