package com.cms.dto;

import java.util.Set;

import com.cms.model.enums.WeekOfMonth;

import jakarta.validation.constraints.NotNull;

/** Empty {@code weeks} means this term hasn't opted in to Saturday scheduling at all. */
public record WorkingSaturdaysRequest(
    @NotNull(message = "Weeks is required (may be empty)")
    Set<WeekOfMonth> weeks
) {}
