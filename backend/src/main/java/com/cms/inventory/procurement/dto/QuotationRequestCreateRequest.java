package com.cms.inventory.procurement.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record QuotationRequestCreateRequest(

    @NotNull(message = "Location is required")
    Long locationId,

    LocalDate requestDate,

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    String notes
) {}
