package com.cms.dto;

import com.cms.model.enums.GenderRestriction;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ZoneRequest(
    @NotBlank(message = "Zone name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    String name,

    /** Marks this zone as hostel space. Leaf level of the cascade — no further children to
     *  propagate to. */
    Boolean isHostel,

    /** Null means unrestricted/mixed. */
    GenderRestriction genderRestriction,

    /** Optional per-zone warden. */
    Long wardenId,

    Boolean isActive,

    /** Baked in from the path on nested creation (POST /floors/{floorId}/zones); used to
     *  re-parent an existing zone to a different floor on update. */
    Long floorId
) {}
