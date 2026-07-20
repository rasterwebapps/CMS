package com.cms.dto;

import java.time.Instant;

public record ClassroomResponse(
    Long id,
    String name,
    String building,
    String roomNumber,
    Integer capacity,
    Boolean isActive,
    Instant createdAt,
    Instant updatedAt
) {}
