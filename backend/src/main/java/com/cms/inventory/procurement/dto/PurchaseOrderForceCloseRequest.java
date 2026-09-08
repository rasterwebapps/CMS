package com.cms.inventory.procurement.dto;

import jakarta.validation.constraints.NotBlank;

public record PurchaseOrderForceCloseRequest(
    @NotBlank String reason
) {}
