package com.cms.inventory.procurement.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record VendorProductMappingRequest(

    @NotNull(message = "Supplier is required")
    Long supplierId,

    @NotNull(message = "Product is required")
    Long productId,

    Long rateContractId,

    @NotNull(message = "Unit price is required")
    @DecimalMin(value = "0", message = "Unit price cannot be negative")
    BigDecimal unitPrice,

    @Size(min = 3, max = 3, message = "Currency code must be 3 characters")
    String currencyCode,

    Long uomId,

    @DecimalMin(value = "0", message = "Minimum order quantity cannot be negative")
    BigDecimal minOrderQty,

    @Min(value = 0, message = "Lead time cannot be negative")
    Integer leadTimeDays,

    Boolean isPreferred,

    Boolean isActive
) {}
