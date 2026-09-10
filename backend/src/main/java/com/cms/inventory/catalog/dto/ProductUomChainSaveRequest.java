package com.cms.inventory.catalog.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

/** Activates a chain configuration for a product — either a brand new version, or a reactivation
 * of a prior version whose levels exactly match what's submitted here (see {@code
 * ProductUomChainService.saveVersion}). */
public record ProductUomChainSaveRequest(
    @NotEmpty(message = "At least the base level is required") @Valid List<ProductUomLevelRequest> levels
) {}
