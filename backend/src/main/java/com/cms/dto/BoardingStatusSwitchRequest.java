package com.cms.dto;

import java.util.List;

import com.cms.model.enums.StudentType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record BoardingStatusSwitchRequest(
    @NotNull(message = "New student type is required")
    StudentType newStudentType,

    String remarks,

    /** Sparse list -- only terms the admin actually edited. Unlisted terms keep using the
     *  calculated amount (or a prior override, if one was already set). */
    @Valid
    List<TermFeeOverrideInput> termFeeOverrides
) {}
