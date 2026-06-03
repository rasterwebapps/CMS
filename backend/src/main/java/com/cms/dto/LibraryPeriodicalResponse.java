package com.cms.dto;

import java.time.Instant;
import java.time.LocalDate;

import com.cms.model.enums.JournalType;
import com.cms.model.enums.SubscriptionStatus;

public record LibraryPeriodicalResponse(
    Long id,
    String journalName,
    JournalType journalType,
    String organization,
    String volumeNumber,
    String issueNumber,
    String monthRange,
    Integer year,
    int copiesCount,
    SubscriptionStatus subscriptionStatus,
    LocalDate receivedDate,
    String receivedBy,
    String remarks,
    Instant createdAt,
    Instant updatedAt
) {}
