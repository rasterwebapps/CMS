package com.cms.dto;

/**
 * One active {@link com.cms.model.ClinicalShiftGroup}'s real Clinical hours for the whole term —
 * {@code assignedHours} is already {@link com.cms.model.CourseOffering#getClinicalShiftDurationMinutes()}
 * converted to hours and multiplied by {@code weeksInTerm} (one shift occurrence/week per group).
 * Clinical Shift sessions bypass the period grid entirely (real clock times via
 * {@code SessionOccurrence.blockStartTime}/{@code blockEndTime}, no {@code ClassSchedule} row), so
 * they never appear in {@link SkeletonBuilderResponse#cells()} — this is how the frontend's
 * Theory/Lab/Clinical hours-assigned card learns about them instead of silently under-counting
 * Clinical for any cohort using Clinical Shift Groups.
 */
public record SkeletonClinicalShiftHours(
    Long courseOfferingId,
    Long cohortSectionId,
    double assignedHours
) {}
