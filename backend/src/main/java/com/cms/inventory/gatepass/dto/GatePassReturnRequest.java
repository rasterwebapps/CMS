package com.cms.inventory.gatepass.dto;

import jakarta.validation.constraints.Size;

public record GatePassReturnRequest(
    @Size(max = 500) String notes
) {}
