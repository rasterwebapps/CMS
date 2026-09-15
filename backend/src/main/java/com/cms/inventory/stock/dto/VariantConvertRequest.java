package com.cms.inventory.stock.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Converts a stranded null-variant {@code StockBalance} row (predating its product's first
 * active variant) onto a chosen variant — see the 2026-09-11 "Parked: pre-existing null-variant
 * stock is stranded" decision-log entry and its 2026-09-15 follow-on.
 */
public record VariantConvertRequest(

    @NotNull(message = "Variant is required")
    Long variantId
) {}
