package com.cms.dto;

import java.time.Instant;

public record LibraryShelfResponse(
    Long id,
    Long rackId,
    String rackName,
    Long libraryId,
    String libraryName,
    String name,
    String code,
    String description,
    Boolean isActive,
    Instant createdAt,
    Instant updatedAt
) {}
