package com.cms.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.cms.model.enums.EnquirySource;
import com.cms.model.enums.EnquiryStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EnquiryRequest(
    @NotBlank(message = "Name is required")
    String name,

    String email,

    String phone,

    Long programId,

    @NotNull(message = "Enquiry date is required")
    LocalDate enquiryDate,

    @NotNull(message = "Source is required")
    EnquirySource source,

    EnquiryStatus status,

    Long agentId,

    Long referralTypeId,

    String assignedTo,

    String remarks,

    BigDecimal feeDiscussedAmount,

    BigDecimal feeGuidelineTotal,

    BigDecimal referralAdditionalAmount,

    BigDecimal finalCalculatedFee,

    String yearWiseFees
) {}
