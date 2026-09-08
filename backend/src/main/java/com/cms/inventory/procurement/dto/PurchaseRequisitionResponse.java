package com.cms.inventory.procurement.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record PurchaseRequisitionResponse(
    Long id,
    Long locationId,
    String locationVirtualName,
    String status,
    LocalDate requisitionDate,
    String notes,
    String createdBy,
    Instant createdAt,
    String submittedBy,
    Instant submittedAt,
    Instant completedAt,
    Integer lineCount,
    Integer pendingCount,
    List<PurchaseRequisitionItemResponse> lines
) {}
