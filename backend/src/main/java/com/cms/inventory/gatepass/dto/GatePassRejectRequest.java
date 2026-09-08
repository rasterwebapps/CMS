package com.cms.inventory.gatepass.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GatePassRejectRequest(
    @NotBlank @Size(max = 500) String reason
) {}
