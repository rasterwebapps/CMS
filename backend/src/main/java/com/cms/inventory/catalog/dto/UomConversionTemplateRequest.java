package com.cms.inventory.catalog.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UomConversionTemplateRequest(

    @NotBlank(message = "Template name is required")
    @Size(max = 150, message = "Name must not exceed 150 characters")
    String name,

    @Size(max = 500, message = "Description must not exceed 500 characters")
    String description,

    @NotNull(message = "Base unit of measure is required")
    Long baseUomId,

    Boolean isActive,

    @NotEmpty(message = "At least the base level is required")
    List<UomConversionTemplateLevelRequest> levels
) {}
