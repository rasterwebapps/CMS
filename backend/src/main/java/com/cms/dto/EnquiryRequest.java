package com.cms.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.cms.model.enums.EnquiryStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EnquiryRequest(
    @NotBlank(message = "Name is required")
    String name,

    String email,

    String phone,

    Long programId,

    Long courseId,

    @NotNull(message = "Enquiry date is required")
    LocalDate enquiryDate,

    @NotNull(message = "Referral type is required")
    Long referralTypeId,

    EnquiryStatus status,

    Long agentId,

    String remarks,

    BigDecimal feeDiscussedAmount,

    BigDecimal feeGuidelineTotal,

    BigDecimal referralAdditionalAmount,

    BigDecimal finalCalculatedFee,

    String yearWiseFees
) {}
