package com.cms.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;

public record AppRoleRequest(
    @NotBlank(message = "Role name is required")
    String name,

    @NotBlank(message = "Display name is required")
    String displayName,

    String description,

    List<String> permissionCodes,

    /** Ordered dashboard widget keys for this role. May be null/empty. */
    List<String> dashboardWidgets
) {}