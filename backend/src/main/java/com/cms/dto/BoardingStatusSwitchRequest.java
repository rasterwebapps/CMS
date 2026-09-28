package com.cms.dto;

import com.cms.model.enums.StudentType;

import jakarta.validation.constraints.NotNull;

public record BoardingStatusSwitchRequest(
    @NotNull(message = "New student type is required")
    StudentType newStudentType,

    String remarks
) {}
