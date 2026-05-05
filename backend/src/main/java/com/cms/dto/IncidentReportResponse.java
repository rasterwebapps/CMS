package com.cms.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import com.cms.model.enums.IncidentSeverity;
import com.cms.model.enums.IncidentStatus;
import com.cms.model.enums.IncidentType;

public record IncidentReportResponse(
    Long id,
    Long labId,
    String labName,
    String reportedBy,
    String reportedByEmail,
    LocalDate incidentDate,
    LocalTime incidentTime,
    String title,
    String description,
    IncidentSeverity severity,
    IncidentType incidentType,
    IncidentStatus status,
    String actionTaken,
    String investigatedBy,
    LocalDate resolvedDate,
    String preventiveMeasures,
    Instant createdAt,
    Instant updatedAt
) {}

