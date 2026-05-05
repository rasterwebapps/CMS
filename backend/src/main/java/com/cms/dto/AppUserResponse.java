package com.cms.dto;

import java.time.Instant;

public record AppUserResponse(
    Long id,
    String keycloakUsername,
    String email,
    String fullName,
    String roleName,
    String roleDisplayName,
    Integer hierarchyLevel,
    boolean isActive,
    String createdBy,
    Instant createdAt
) {}
