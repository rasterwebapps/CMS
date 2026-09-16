package com.cms.inventory.procurement.dto;

import java.time.Instant;

public record QuotationRequestSupplierResponse(
    Long id,
    Long supplierId,
    String supplierName,
    Instant invitedAt
) {}
