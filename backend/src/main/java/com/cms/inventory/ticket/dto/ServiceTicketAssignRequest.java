package com.cms.inventory.ticket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ServiceTicketAssignRequest(
    @NotBlank @Size(max = 200) String assignedTo
) {}
