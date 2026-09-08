package com.cms.inventory.procurement.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PurchaseRequisitionCreateRequest(

    @NotNull(message = "Location is required")
    Long locationId,

    LocalDate requisitionDate,

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    String notes
) {}
