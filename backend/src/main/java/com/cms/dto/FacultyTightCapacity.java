package com.cms.dto;

import java.util.List;

/** One faculty whose total real term-hour demand is at or near (but not over) their effective
 *  term capacity — distinct from {@link FacultyOverCapacity}, which only fires once demand
 *  actually exceeds capacity. A faculty sitting at ~100% utilization has effectively zero slack:
 *  the aggregate sum "fits" on paper, but the actual day/period placement still has to compete
 *  with every other cohort/subject for the exact same slots, room, and day-uniqueness
 *  constraints — nothing here guarantees that real packing succeeds, only that the raw hours
 *  don't already rule it out. Surfaced so an admin sees the real risk before running, rather than
 *  discovering it only as an unplaced session afterward. */
public record FacultyTightCapacity(
    Long facultyId,
    String facultyName,
    double effectiveDailyCapacityHours,
    String dailyCapacityTier,
    int workingDaysInTerm,
    double termCapacityHours,
    double totalTermDemandHours,
    /** 0-100; always >= the tight-capacity threshold and < 100 + epsilon (at/over 100 would have
     *  already been reported as {@link FacultyOverCapacity} instead). */
    double utilizationPercent,
    List<OverageContributor> topContributors
) {}
