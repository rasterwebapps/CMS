package com.cms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ClassroomRequest(

    @NotBlank(message = "Classroom name is required")
    @Size(max = 255, message = "Name must not exceed 255 characters")
    String name,

    @Size(max = 255, message = "Building must not exceed 255 characters")
    String building,

    @Size(max = 255, message = "Room number must not exceed 255 characters")
    String roomNumber,

    @Positive(message = "Capacity must be positive")
    Integer capacity,

    Boolean isActive,

    /** Links this virtual venue to a physical Campus Setup Room — optional, admin-linked. */
    Long roomId
) {}
