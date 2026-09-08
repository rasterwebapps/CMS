package com.cms.inventory.issue.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StockIssueRequestCreateRequest(
    @NotNull Long requestingLocationId,
    @NotNull Long issuingLocationId,
    LocalDate requestDate,
    @Size(max = 500) String notes
) {}
