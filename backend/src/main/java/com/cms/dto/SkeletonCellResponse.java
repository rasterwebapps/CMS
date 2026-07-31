package com.cms.dto;

import java.time.LocalTime;

import com.cms.model.enums.ClassScheduleStatus;
import com.cms.model.enums.ClassSessionType;
import com.cms.model.enums.DayOfWeek;

/** One placed cell in the skeleton grid — deliberately leaner than {@link ClassScheduleResponse}
 *  since the skeleton stage has no faculty/room yet; {@code isStaffed} is false until Phase 5's
 *  staffing pass fills those in and the row can be published. */
public record SkeletonCellResponse(
    Long id,
    ClassSessionType sessionType,
    DayOfWeek dayOfWeek,
    Long periodId,
    String slotName,
    LocalTime startTime,
    LocalTime endTime,
    Long batchId,
    String batchName,
    boolean isStaffed,
    ClassScheduleStatus status
) {}
