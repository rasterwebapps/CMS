package com.cms.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import com.cms.model.enums.IncidentSeverity;
import com.cms.model.enums.IncidentStatus;
import com.cms.model.enums.IncidentType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record IncidentReportRequest(
    @NotNull Long labId,
    @NotBlank String reportedBy,
    String reportedByEmail,
    @NotNull LocalDate incidentDate,
    LocalTime incidentTime,
    @NotBlank String title,
    String description,
    @NotNull IncidentSeverity severity,
    @NotNull IncidentType incidentType,
    @NotNull IncidentStatus status,
    String actionTaken,
    String investigatedBy,
    LocalDate resolvedDate,
    String preventiveMeasures
) {}

