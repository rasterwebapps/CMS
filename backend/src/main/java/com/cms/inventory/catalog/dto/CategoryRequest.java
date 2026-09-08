package com.cms.inventory.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryRequest(

    @NotBlank(message = "Category name is required")
    @Size(max = 150, message = "Name must not exceed 150 characters")
    String name,

    Long parentCategoryId,

    @Size(max = 500, message = "Description must not exceed 500 characters")
    String description,

    Boolean isActive
) {}
