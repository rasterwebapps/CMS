package com.cms.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.cms.model.enums.AdmissionQuota;
import com.cms.model.enums.EnquiryStatus;
import com.cms.model.enums.Gender;
import com.cms.model.enums.StudentType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;

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

    String yearWiseFees,

    StudentType studentType,

    Long countryId,

    String state,

    String district,

    Long referredStudentId,

    Long referredFacultyId,

    Long referredStaffId,

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    LocalDate dateOfBirth,

    @NotNull(message = "Gender is required")
    Gender gender,

    AdmissionQuota admissionQuota,

    Long feeStateId
) {}
