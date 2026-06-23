package com.cms.dto;

import java.math.BigDecimal;

import com.cms.model.enums.BankAccountType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StaffReferrerRequest(
    @NotBlank(message = "Name is required")
    String name,

    String phone,

    String email,

    @NotBlank(message = "Employee code is required")
    @Size(max = 50, message = "Employee code must not exceed 50 characters")
    String employeeCode,

    @NotNull(message = "Institution is required")
    Long institutionId,

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
