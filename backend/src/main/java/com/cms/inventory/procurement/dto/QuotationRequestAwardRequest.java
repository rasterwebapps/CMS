package com.cms.inventory.procurement.dto;

import jakarta.validation.constraints.NotNull;

public record QuotationRequestAwardRequest(

    @NotNull(message = "A response to award is required")
    Long responseLineId
) {}
