package com.cms.dto;

import java.math.BigDecimal;

import com.cms.model.enums.BankAccountType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AgentRequest(
    @NotBlank(message = "Name is required")
    String name,

    String phone,

    String email,

    String area,

    String locality,

    Integer allottedSeats,

    BigDecimal commissionAmount,

    Boolean isActive,

    @Size(max = 20)
    String panNumber,

    @Size(max = 20)
    String aadhaarNumber,

    @Size(max = 40)
    String bankAccountNumber,

    @Size(max = 20)
    String bankIfscCode,

    @Size(max = 150)
    String bankBranch,

    @Size(max = 150)
    String bankName,

    @Size(max = 150)
    String bankAccountHolder,

    BankAccountType bankAccountType
) {}
