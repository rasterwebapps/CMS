package com.cms.dto;

import java.time.Instant;

public record GuardianResponse(
    Long id,
    String firstName,
    String lastName,
    String email,
    String phone,
    String relationshipHint,
    Instant createdAt
) {}
