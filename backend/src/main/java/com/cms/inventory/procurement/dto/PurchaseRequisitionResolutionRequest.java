package com.cms.inventory.procurement.dto;

import jakarta.validation.constraints.Size;

/** Body for both the approve and reject endpoints — notes are optional either way. */
public record PurchaseRequisitionResolutionRequest(

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    String notes
) {}
