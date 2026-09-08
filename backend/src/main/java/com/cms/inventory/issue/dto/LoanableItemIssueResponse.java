package com.cms.inventory.issue.dto;

import java.time.Instant;
import java.time.LocalDate;

public record LoanableItemIssueResponse(
    Long id,
    Long productId,
    String productCode,
    String productName,
    Long locationId,
    String locationVirtualName,
    String borrowerName,
    String borrowerContact,
    String status,
    LocalDate issueDate,
    LocalDate expectedReturnDate,
    LocalDate actualReturnDate,
    boolean overdue,
    String conditionOnIssue,
    String conditionOnReturn,
    String notes,
    String issuedBy,
    Instant issuedAt,
    String returnedBy,
    Instant returnedAt
) {}
