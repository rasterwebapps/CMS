package com.cms.inventory.procurement.dto;

import jakarta.validation.constraints.NotNull;

public record WantedListRejectRequest(
    @NotNull String reason,
    String notes
) {}
