package com.cms.dto;

import java.time.Instant;
import java.time.LocalTime;

import com.cms.model.enums.ClassScheduleStatus;
import com.cms.model.enums.ClassSessionType;
import com.cms.model.enums.DayOfWeek;

public record ClassScheduleResponse(
    Long id,
    ClassSessionType sessionType,
    ClassScheduleStatus status,

    Long labId,
    String labName,

    Long subjectId,
    String subjectName,
    String subjectCode,

    Long facultyId,
    String facultyName,

    Long labSlotId,
    Long periodId,
    /** Session-type-neutral display fields, resolved from either the LabSlot or the Period. */
    String slotName,
    LocalTime startTime,
    LocalTime endTime,

    String batchName,
    Long batchId,

    Long classroomId,
    /** Session-type-neutral room name, resolved from either the Classroom or the Lab. */
    String roomName,

    Long courseOfferingId,

    DayOfWeek dayOfWeek,
    Long termInstanceId,
    String termInstanceLabel,
    Boolean isActive,
    Instant createdAt,
    Instant updatedAt
) {}
