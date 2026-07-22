package com.cms.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RoomRequest(
    @NotBlank(message = "Room number is required")
    @Size(max = 50, message = "Room number must not exceed 50 characters")
    String roomNumber,

    @Min(value = 1, message = "Capacity must be at least 1")
    Integer capacity,

    @Size(max = 500, message = "Description must not exceed 500 characters")
    String description,

    Boolean isActive,

    /** Baked in from the path on nested creation (POST /zones/{zoneId}/rooms); used to
     *  re-parent an existing room to a different zone on update. */
    Long zoneId
) {}
