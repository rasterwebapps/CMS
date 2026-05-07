package com.cms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CourseRequest(
    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name must not exceed 255 characters")
    String name,

    @NotBlank(message = "Code is required")
    @Size(max = 50, message = "Code must not exceed 50 characters")
    String code,

    @Size(max = 255, message = "Specialization must not exceed 255 characters")
    String specialization,

    @Size(max = 2, message = "Roll number code must be exactly 2 characters")
    String rollNumberCode,

    @NotNull(message = "Program ID is required")
    Long programId
) {}
