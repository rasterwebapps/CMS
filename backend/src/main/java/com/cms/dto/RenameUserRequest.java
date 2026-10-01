package com.cms.dto;

import jakarta.validation.constraints.NotBlank;

public record RenameUserRequest(
    @NotBlank(message = "Full name is required")
    String fullName
) {}
