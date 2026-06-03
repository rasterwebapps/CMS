package com.cms.dto;

import java.time.LocalDate;

import com.cms.model.enums.JournalType;
import com.cms.model.enums.SubscriptionStatus;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LibraryPeriodicalRequest(

    @NotBlank(message = "Journal name is required")
    @Size(max = 300, message = "Journal name must not exceed 300 characters")
    String journalName,

    JournalType journalType,

    @Size(max = 200, message = "Organization must not exceed 200 characters")
    String organization,

    @Size(max = 20, message = "Volume number must not exceed 20 characters")
    String volumeNumber,

    @Size(max = 20, message = "Issue number must not exceed 20 characters")
    String issueNumber,

    @Size(max = 30, message = "Month range must not exceed 30 characters")
    String monthRange,

    Integer year,

    @Min(value = 1, message = "Copies count must be at least 1")
    Integer copiesCount,

    SubscriptionStatus subscriptionStatus,

    LocalDate receivedDate,

    @Size(max = 100, message = "Received by must not exceed 100 characters")
    String receivedBy,

    @Size(max = 500, message = "Remarks must not exceed 500 characters")
    String remarks
) {}
