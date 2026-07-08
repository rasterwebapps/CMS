package com.cms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LibraryShelfRequest(

    @NotNull(message = "Rack is required")
    Long rackId,

    @NotBlank(message = "Shelf name is required")
    @Size(max = 200, message = "Name must not exceed 200 characters")
    String name,

    @NotBlank(message = "Shelf code is required")
    @Size(max = 50, message = "Code must not exceed 50 characters")
    String code,

    @Size(max = 500, message = "Description must not exceed 500 characters")
    String description,

    Boolean isActive
) {}
