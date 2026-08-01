package com.cms.dto;

import java.util.List;

public record CapacityPlanResponse(
    Long cohortId,
    String cohortLabel,
    Long termInstanceId,
    String termLabel,
    Integer semesterNumber,
    long cohortStrength,
    int workingDaysInTerm,
    double totalWorkingPeriodHours,
    double blockedHours,
    int curriculumHoursRequired,
    double bufferHours,
    int targetBatchSize,
    boolean theoryFits,
    String theoryShortfallMessage,
    List<VenueOptionResponse> fittingClassrooms,
    int labBatchesNeeded,
    List<VenueOptionResponse> fittingLabs,
    int clinicalBatchesNeeded,
    List<VenueOptionResponse> fittingClinicalVenues,
    List<VenueUtilizationResponse> classroomUtilization,
    List<VenueUtilizationResponse> labUtilization,
    List<VenueUtilizationResponse> clinicalVenueUtilization
) {}
