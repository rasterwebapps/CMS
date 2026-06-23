package com.cms.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.cms.model.enums.BankAccountType;

public record StaffReferrerResponse(
    Long id,
    String name,
    String phone,
    String email,
    String employeeCode,
    Long institutionId,
    String institutionName,
    BigDecimal commissionAmount,
    Boolean isActive,
    String panNumber,
    String aadhaarNumber,
    String bankAccountNumber,
    String bankIfscCode,
    String bankBranch,
    String bankName,
    String bankAccountHolder,
    BankAccountType bankAccountType,
    Instant createdAt,
    Instant updatedAt
) {}
