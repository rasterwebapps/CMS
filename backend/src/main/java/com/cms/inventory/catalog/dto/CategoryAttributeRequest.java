package com.cms.inventory.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CategoryAttributeRequest(

    @NotBlank(message = "Attribute name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    String name,

    @NotNull(message = "Data type is required")
    String dataType,

    @Size(max = 1000, message = "Enum options must not exceed 1000 characters")
    String enumOptions,

    Boolean isRequired,

    Integer displayOrder
) {}
