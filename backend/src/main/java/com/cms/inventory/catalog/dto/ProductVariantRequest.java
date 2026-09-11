package com.cms.inventory.catalog.dto;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * {@code attributeValues} reuses {@link ProductAttributeValueRequest} — same {@code attributeId}
 * + {@code value} shape, just applied against the variant's own typed-EAV rows instead of the
 * parent product's.
 */
public record ProductVariantRequest(

    @NotBlank(message = "Variant code is required")
    @Size(max = 50, message = "Variant code must not exceed 50 characters")
    String variantCode,

    @NotBlank(message = "Variant name is required")
    @Size(max = 200, message = "Variant name must not exceed 200 characters")
    String variantName,

    @Size(max = 64, message = "Barcode must not exceed 64 characters")
    String barcode,

    /** NONE, BATCH, or SERIAL — null/blank defaults to NONE. */
    String trackingMode,

    BigDecimal standardCost,
    BigDecimal listPrice,

    Boolean isActive,

    List<ProductAttributeValueRequest> attributeValues
) {}
