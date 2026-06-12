package com.cms.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.cms.model.enums.FineStatus;
import com.cms.model.enums.LibraryMemberType;

public record LibraryFineDetailResponse(
    Long id,
    Long issueId,
    String accessionNumber,
    String bookTitle,
    LibraryMemberType memberType,
    String memberName,
    String memberCode,
    LocalDate issuedDate,
    LocalDate dueDate,
    LocalDate returnedDate,
    int overdueDays,
    BigDecimal finePerDay,
    BigDecimal totalFine,
    FineStatus status,
    String waivedBy,
    Instant collectedAt,
    String remarks,
    Instant createdAt
) {}
