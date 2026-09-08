package com.cms.inventory.consignment.dto;

import java.time.Instant;
import java.time.LocalDate;

public record ConsignmentAgreementResponse(
    Long id,
    Long supplierId,
    String supplierName,
    Long locationId,
    String locationVirtualName,
    String agreementNumber,
    LocalDate startDate,
    LocalDate endDate,
    Integer billingCycleDays,
    String notes,
    boolean isActive,
    Instant createdAt,
    Instant updatedAt
) {}
