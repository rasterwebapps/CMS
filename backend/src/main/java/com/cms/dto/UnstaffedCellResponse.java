package com.cms.dto;

import java.time.LocalTime;

import com.cms.model.enums.ClassSessionType;
import com.cms.model.enums.DayOfWeek;

/** One skeleton cell (R3 Phase 4) still waiting for faculty + room (R3 Phase 5). */
public record UnstaffedCellResponse(
    Long id,
    Long courseOfferingId,
    String subjectName,
    String subjectCode,
    Long subjectSpecialityId,
    String subjectSpecialityName,
    ClassSessionType sessionType,
    DayOfWeek dayOfWeek,
    Long periodId,
    String slotName,
    LocalTime startTime,
    LocalTime endTime,
    String batchName,
    Integer requiredStrength
) {}
