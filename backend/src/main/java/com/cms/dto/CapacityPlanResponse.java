package com.cms.dto;

import java.util.List;

public record CapacityPlanResponse(
    Long cohortId,
    String cohortLabel,
    Long termInstanceId,
    String termLabel,
    Integer semesterNumber,
    long cohortStrength,
    long enrolledStrength,
    Integer sanctionedStrength,
    int workingDaysInTerm,
    double totalWorkingPeriodHours,
    double blockedHours,
    int curriculumHoursRequired,
    double bufferHours,
    boolean theoryFits,
    String theoryShortfallMessage,
    List<VenueOptionResponse> fittingClassrooms,
    List<VenueOptionResponse> classroomsForSectioning,
    List<VenueOptionResponse> fittingLabs,
    List<VenueOptionResponse> fittingClinicalVenues,
    List<VenueUtilizationResponse> classroomUtilization,
    List<VenueUtilizationResponse> labUtilization,
    List<VenueUtilizationResponse> clinicalVenueUtilization,
    List<SuggestedSectionResponse> suggestedSections,
    List<SuggestedBatchResponse> suggestedLabClinicalBatches,
    boolean labClinicalMappingSufficient,
    String labClinicalMappingIssuesMessage
) {}
