package com.cms.inventory.ticket.dto;

import java.time.Instant;

public record ServiceTicketCategoryResponse(
    Long id,
    String name,
    String description,
    Boolean isActive,
    Instant createdAt,
    Instant updatedAt
) {}
