package com.cms.dto;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EscortRotationPoolRequest(
    @NotNull(message = "Batch ID is required")
    Long batchId,

    @NotNull(message = "Anchor date is required")
    LocalDate anchorOccurrenceDate,

    /** Ordered eligible-faculty pool -- position in this list fixes memberOrder (round-robin
     *  order), starting from whoever is on duty on the anchor date. Must have at least 2 faculty
     *  (a 1-faculty "rotation" is just a fixed assignment). */
    @NotEmpty(message = "At least 2 eligible faculty are required for a rotation")
    @Size(min = 2, message = "At least 2 eligible faculty are required for a rotation")
    List<Long> facultyIds
) {}
