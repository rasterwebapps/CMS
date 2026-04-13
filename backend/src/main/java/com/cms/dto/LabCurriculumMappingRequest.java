package com.cms.dto;

import com.cms.model.enums.MappingLevel;
import com.cms.model.enums.OutcomeType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LabCurriculumMappingRequest(
    @NotNull(message = "Experiment ID is required")
    Long experimentId,

    @NotNull(message = "Outcome type is required")
    OutcomeType outcomeType,

    @NotBlank(message = "Outcome code is required")
    @Size(max = 20, message = "Outcome code must not exceed 20 characters")
    String outcomeCode,

    @Size(max = 1000, message = "Outcome description must not exceed 1000 characters")
    String outcomeDescription,

    @NotNull(message = "Mapping level is required")
    MappingLevel mappingLevel,

    @Size(max = 500, message = "Justification must not exceed 500 characters")
    String justification
) {}
