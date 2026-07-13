package com.cms.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CurriculumElectiveGroupRequest(
    @NotNull(message = "Curriculum version ID is required")
    Long curriculumVersionId,

    @NotNull(message = "Term number is required")
    @Min(value = 1, message = "Term number must be at least 1")
    Integer termNumber,

    @NotBlank(message = "Group name is required")
    String groupName,

    String groupCode
) {}
