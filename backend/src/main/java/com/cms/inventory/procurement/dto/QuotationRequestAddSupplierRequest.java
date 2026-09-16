package com.cms.inventory.procurement.dto;

import jakarta.validation.constraints.NotNull;

public record QuotationRequestAddSupplierRequest(

    @NotNull(message = "Supplier is required")
    Long supplierId
) {}
