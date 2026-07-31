package com.cms.dto;

import jakarta.validation.constraints.NotNull;

public record ApplySubstituteRequest(
    @NotNull(message = "Substitute faculty ID is required")
    Long substituteFacultyId
) {}
