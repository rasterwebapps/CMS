package com.cms.dto;

import com.cms.model.enums.ElectiveSelectionMode;

import jakarta.validation.constraints.NotNull;

public record UpdateElectiveSelectionModeRequest(
    @NotNull(message = "Selection mode is required")
    ElectiveSelectionMode selectionMode
) {}
