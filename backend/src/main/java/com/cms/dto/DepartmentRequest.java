package com.cms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DepartmentRequest(
    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name must not exceed 255 characters")
    String name,

    @NotBlank(message = "Code is required")
    @Size(max = 50, message = "Code must not exceed 50 characters")
    String code,

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    String description,

    @Size(max = 255, message = "HOD name must not exceed 255 characters")
    String hodName
) {}
