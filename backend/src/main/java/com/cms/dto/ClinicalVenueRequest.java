package com.cms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ClinicalVenueRequest(

    @NotBlank(message = "Clinical venue name is required")
    @Size(max = 255, message = "Name must not exceed 255 characters")
    String name,

    @Size(max = 255, message = "Hospital name must not exceed 255 characters")
    String hospitalName,

    @Size(max = 255, message = "Department must not exceed 255 characters")
    String department,

    @Positive(message = "Capacity must be greater than zero")
    Integer capacity,

    Boolean isActive,

    /** Links this virtual venue to a physical Campus Setup Room — for an off-site venue, that
     *  Room lives under a hospital Branch. Optional, admin-linked. */
    Long roomId
) {}
