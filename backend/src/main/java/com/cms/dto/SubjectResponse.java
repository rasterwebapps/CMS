package com.cms.dto;

import java.time.Instant;
import java.util.List;

public record SubjectResponse(
    Long id,
    String name,
    String code,
    Integer credits,
    Integer theoryCredits,
    Integer labCredits,
    SpecialityResponse speciality,
    Integer termNumber,
    Boolean isActive,
    Integer labSessionBlockPeriods,
    Integer clinicalSessionBlockPeriods,
    Instant createdAt,
    Instant updatedAt,
    List<VenueOptionResponse> eligibleLabs,
    List<VenueOptionResponse> eligibleClinicalVenues,
    List<FacultyOptionResponse> eligibleFaculty
) {}
