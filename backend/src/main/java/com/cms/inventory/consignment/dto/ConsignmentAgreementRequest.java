package com.cms.inventory.consignment.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ConsignmentAgreementRequest(
    @NotNull Long supplierId,
    @NotNull Long locationId,
    @NotBlank @Size(max = 100) String agreementNumber,
    @NotNull LocalDate startDate,
    LocalDate endDate,
    @Positive Integer billingCycleDays,
    @Size(max = 500) String notes,
    Boolean isActive
) {}
