package com.cms.inventory.ticket.dto;

import java.time.Instant;
import java.time.LocalDate;

public record ServiceTicketResponse(
    Long id,
    Long locationId,
    String locationVirtualName,
    Long categoryId,
    String categoryName,
    String requestedBy,
    String priority,
    String status,
    String description,
    String assignedTo,
    Instant assignedAt,
    String resolutionNotes,
    LocalDate resolutionDate,
    String resolvedBy,
    Integer feedbackRating,
    String closedBy,
    Instant closedAt,
    String cancelledBy,
    Instant cancelledAt,
    String cancellationReason,
    String createdBy,
    Instant createdAt,
    Instant updatedAt
) {}
