package com.cms.inventory.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UomRequest(

    @NotBlank(message = "UOM code is required")
    @Size(max = 20, message = "Code must not exceed 20 characters")
    String code,

    @NotBlank(message = "UOM name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    String name,

    @Size(max = 500, message = "Description must not exceed 500 characters")
    String description,

    Boolean isActive
) {}
