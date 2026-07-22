package com.cms.dto;

import com.cms.model.enums.GenderRestriction;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BlockRequest(
    @NotBlank(message = "Block name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    String name,

    @NotBlank(message = "Block code is required")
    @Size(max = 50, message = "Code must not exceed 50 characters")
    String code,

    @Size(max = 500, message = "Description must not exceed 500 characters")
    String description,

    /** Marks the whole block as hostel space; setting this (with genderRestriction) cascades
     *  the same values down to every floor and zone underneath. */
    Boolean isHostel,

    /** Null means unrestricted/mixed. */
    GenderRestriction genderRestriction,

    Boolean isActive,

    /** Baked in from the path on nested creation (POST /branches/{branchId}/blocks); used to
     *  re-parent an existing block to a different branch on update. */
    Long branchId
) {}
