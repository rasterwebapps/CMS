package com.cms.inventory.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryRequest(

    @NotBlank(message = "Category name is required")
    @Size(max = 150, message = "Name must not exceed 150 characters")
    String name,

    /** Prefix half of an auto-generated Product code, e.g. "STA" -&gt; "STA-000001". 2-10 uppercase
     *  letters/digits, unique across all categories. */
    @NotBlank(message = "Short code is required")
    @Size(max = 10, message = "Short code must not exceed 10 characters")
    String shortCode,

    Long parentCategoryId,

    @Size(max = 500, message = "Description must not exceed 500 characters")
    String description,

    Boolean isActive
) {}
