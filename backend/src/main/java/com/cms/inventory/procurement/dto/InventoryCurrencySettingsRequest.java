package com.cms.inventory.procurement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InventoryCurrencySettingsRequest(

    @NotBlank(message = "Base currency code is required")
    @Size(min = 3, max = 3, message = "Currency code must be 3 characters")
    String baseCurrencyCode
) {}
