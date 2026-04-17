package com.cms.dto;

import java.time.Instant;

public record AgentResponse(
    Long id,
    String name,
    String phone,
    String email,
    String area,
    String locality,
    Integer allottedSeats,
    Boolean isActive,
    Instant createdAt,
    Instant updatedAt
) {}
