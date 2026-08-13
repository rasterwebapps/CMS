package com.cms.dto;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import com.cms.model.enums.ClassScheduleStatus;
import com.cms.model.enums.ClassSessionType;
import com.cms.model.enums.DayOfWeek;

/** One placed cell in the skeleton grid — deliberately leaner than {@link ClassScheduleResponse}
 *  since the skeleton stage has no faculty/room yet; {@code isStaffed} is false until Phase 5's
 *  staffing pass fills those in and the row can be published. {@code rotationGroupLabel} is
 *  non-null only for a cell that's part of a Rotation Group — {@code batchId}/{@code batchName}
 *  are null on those (there's no single fixed occupant) and {@code rotatingBatchNames} lists who
 *  alternates through it instead. */
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
    Long cohortSectionId,
    String cohortSectionLabel,
    boolean isStaffed,
    ClassScheduleStatus status,
    String rotationGroupLabel,
    List<String> rotatingBatchNames,
    Long courseOfferingId,
    String subjectName,
    String subjectCode,
    Long electiveGroupId,
    String electiveGroupName,

    /** OC-127 periodSpan: non-null only for a cell that's part of a multi-period session — every
     *  sibling row sharing this id is placed/staffed/removed together as one atomic unit. */
    UUID sessionGroupId
) {}
