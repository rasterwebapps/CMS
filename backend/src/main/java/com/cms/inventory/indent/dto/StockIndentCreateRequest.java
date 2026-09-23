package com.cms.inventory.indent.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StockIndentCreateRequest(
    @NotNull Long requestingLocationId,
    @NotNull Long issuingLocationId,
    LocalDate requestDate,
    @Size(max = 500) String notes
) {}
