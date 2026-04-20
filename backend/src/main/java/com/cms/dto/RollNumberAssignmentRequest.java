package com.cms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RollNumberAssignmentRequest(
    @NotBlank String rollNumber
) {}
