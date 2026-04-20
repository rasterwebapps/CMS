package com.cms.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

public record BulkRollNumberAssignmentRequest(
    @NotEmpty List<@Valid BulkRollNumberItem> assignments
) {}
