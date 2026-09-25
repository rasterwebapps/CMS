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
    Integer theorySessionBlockPeriods,
    Instant createdAt,
    Instant updatedAt,
    List<VenueOptionResponse> eligibleLabs,
    List<VenueOptionResponse> eligibleClinicalVenues,
    List<FacultyOptionResponse> eligibleFaculty,
    /** True for the two subjects (SYSTEM-LIBRARY, SYSTEM-SPORTS) SubjectService itself treats as
     *  system-managed. The frontend edit form reads this rather than keeping its own copy of the
     *  two codes, so the backend stays the single source of truth. */
    boolean isSystemManaged
) {}
