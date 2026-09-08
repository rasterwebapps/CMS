package com.cms.inventory.stock.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CycleCountCreateRequest(

    @NotNull(message = "Location is required")
    Long locationId,

    LocalDate countDate,

    /** FULL_LOCATION or AD_HOC — see {@code CycleCountScope}. */
    @NotBlank(message = "Scope is required")
    String scope,

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    String notes
) {}
