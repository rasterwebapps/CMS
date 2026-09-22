package com.cms.inventory.procurement.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record QuotationRequestResponse(
    Long id,
    String quotationNumber,
    Long locationId,
    String locationVirtualName,
    String status,
    LocalDate requestDate,
    String notes,
    String createdBy,
    Instant createdAt,
    String submittedBy,
    Instant submittedAt,
    Instant completedAt,
    Integer lineCount,
    Integer pendingCount,
    List<QuotationRequestSupplierResponse> suppliers,
    List<QuotationRequestLineResponse> lines
) {}
