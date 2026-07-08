package com.cms.dto;

import java.time.Instant;

public record LibraryRackResponse(
    Long id,
    Long libraryId,
    String libraryName,
    String name,
    String code,
    String description,
    Boolean isActive,
    Instant createdAt,
    Instant updatedAt
) {}
