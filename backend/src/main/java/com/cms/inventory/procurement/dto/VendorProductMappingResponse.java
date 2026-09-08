package com.cms.inventory.procurement.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record VendorProductMappingResponse(
    Long id,
    Long supplierId,
    String supplierName,
    Long productId,
    String productCode,
    String productName,
    Long rateContractId,
    BigDecimal unitPrice,
    String currencyCode,
    Long uomId,
    String uomCode,
    BigDecimal minOrderQty,
    Integer leadTimeDays,
    Boolean isPreferred,
    Boolean isActive,

    /** unitPrice, unless an active, in-window RateContract line for this product overrides it. */
    BigDecimal effectivePrice,
    /** "CONTRACT" when effectivePrice came from a RateContractLine, "STANDARD" otherwise. */
    String priceSource,

    Instant createdAt,
    Instant updatedAt
) {}
