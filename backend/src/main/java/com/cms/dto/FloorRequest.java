package com.cms.dto;

import com.cms.model.enums.GenderRestriction;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record FloorRequest(
    @NotBlank(message = "Floor name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    String name,

    @NotNull(message = "Floor number is required")
    Integer floorNumber,

    /** Marks this floor as hostel space; setting this (with genderRestriction) cascades the
     *  same values down to every zone underneath. */
    Boolean isHostel,

    /** Null means unrestricted/mixed. */
    GenderRestriction genderRestriction,

    /** Explicit skyline (false, above ground) vs earthline (true, basement) marker for the Campus
     *  Setup diagram — independent of {@code floorNumber}'s ordering role. */
    Boolean isBasement,

    Boolean isActive,

    /** Baked in from the path on nested creation (POST /blocks/{blockId}/floors); used to
     *  re-parent an existing floor to a different block on update. */
    Long blockId
) {}
