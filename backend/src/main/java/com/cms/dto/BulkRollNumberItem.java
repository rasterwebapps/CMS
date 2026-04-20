package com.cms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record BulkRollNumberItem(
    @NotNull Long studentId,
    @NotBlank String rollNumber
) {}
