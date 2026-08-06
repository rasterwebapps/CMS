package com.cms.dto;

import com.cms.model.enums.RoomPurposeCategoryCode;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RoomPurposeCategoryRequest(

    @NotBlank(message = "Category name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    String name,

    /** Picked from a fixed list, not typed — ignored on update (code is immutable once set), see
     *  {@link com.cms.service.RoomPurposeCategoryService#update}. */
    @NotNull(message = "Category code is required")
    RoomPurposeCategoryCode code,

    Boolean isResidential,

    @Size(max = 500, message = "Description must not exceed 500 characters")
    String description,

    Boolean isActive
) {}
