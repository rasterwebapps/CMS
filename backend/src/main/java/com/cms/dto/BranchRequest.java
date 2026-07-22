package com.cms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BranchRequest(
    @NotBlank(message = "Branch name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    String name,

    @NotBlank(message = "Branch code is required")
    @Size(max = 50, message = "Code must not exceed 50 characters")
    String code,

    @Size(max = 500, message = "Description must not exceed 500 characters")
    String description,

    Boolean isActive,

    /** Baked in from the path on nested creation (POST /organizations/{organizationId}/branches);
     *  used to re-parent an existing branch to a different organization on update. */
    Long organizationId
) {}
