package com.cms.dto;

import java.time.Instant;

public record LibraryResponse(
    Long id,
    String name,
    String code,
    String address,
    Boolean isActive,
    Instant createdAt,
    Instant updatedAt
) {}
