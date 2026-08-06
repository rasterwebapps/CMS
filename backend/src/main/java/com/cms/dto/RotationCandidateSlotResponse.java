package com.cms.dto;

import com.cms.model.enums.ClassSessionType;
import com.cms.model.enums.DayOfWeek;

/** An already-placed, not-yet-rotating skeleton cell — a candidate to link into a new
 *  Rotation Group's slot list. */
public record RotationCandidateSlotResponse(
    Long classScheduleId,
    Long courseOfferingId,
    String subjectName,
    Long batchId,
    String batchName,
    ClassSessionType sessionType,
    DayOfWeek dayOfWeek,
    Long periodId,
    String periodName
) {}
