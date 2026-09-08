package com.cms.inventory.asset.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AssetStatusUpdateRequest(
    @NotBlank String status,
    @Size(max = 500) String notes
) {}
