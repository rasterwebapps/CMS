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
    Long roomId,

    /** Marks this room (e.g. a large lecture/drawing hall) as eligible for concurrent,
     *  capacity-pooled sharing in the Special Class Scheduler — see {@code Classroom}. */
    Boolean allowsConcurrentSharing
) {}
