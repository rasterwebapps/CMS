package com.cms.inventory.procurement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TaxJurisdictionSettingsRequest(

    @NotBlank(message = "Home state is required")
    @Size(max = 100, message = "Home state must not exceed 100 characters")
    String homeState
) {}
