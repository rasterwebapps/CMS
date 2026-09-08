package com.cms.inventory.ticket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ServiceTicketCreateRequest(
    @NotNull Long locationId,
    @NotNull Long categoryId,
    @NotBlank @Size(max = 200) String requestedBy,
    String priority,
    @NotBlank @Size(max = 1000) String description
) {}
