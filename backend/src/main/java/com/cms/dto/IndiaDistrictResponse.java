package com.cms.dto;

import java.time.Instant;

public record IndiaDistrictResponse(
    Long id,
    Long stateId,
    String stateName,
    String name,
    Boolean isActive,
    Instant createdAt,
    Instant updatedAt
) {}

