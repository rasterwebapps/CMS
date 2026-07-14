package com.cms.dto;

import java.time.Instant;
import java.time.LocalDate;

import com.cms.model.enums.BookStatus;
import com.cms.model.enums.JournalType;
import com.cms.model.enums.SubscriptionStatus;

public record LibraryPeriodicalResponse(
    Long id,
    String accessionNumber,
    String barcode,
    String journalName,
    JournalType journalType,
    String organization,
    String volumeNumber,
    String issueNumber,
    String monthRange,
    Integer year,
    SubscriptionStatus subscriptionStatus,
    BookStatus status,
    LocalDate receivedDate,
    String receivedBy,
    String remarks,
    Instant createdAt,
    Instant updatedAt
) {}
