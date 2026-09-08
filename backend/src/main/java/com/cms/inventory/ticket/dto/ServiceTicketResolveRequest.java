package com.cms.inventory.ticket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ServiceTicketResolveRequest(
    @NotBlank @Size(max = 1000) String resolutionNotes
) {}
