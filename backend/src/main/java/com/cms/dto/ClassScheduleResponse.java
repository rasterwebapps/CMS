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

    Long periodId,
    /** Session-type-neutral display fields, always resolved from the Period (THEORY, LAB, and
     *  CLINICAL rows all share the one Period master since V331 merged LabSlot into it). */
    String slotName,
    LocalTime startTime,
    LocalTime endTime,

    String batchName,
    Long batchId,

    Long classroomId,
    Long clinicalVenueId,
    /** Session-type-neutral room name, resolved from the Classroom, Lab, or Clinical Venue. */
    String roomName,

    Long courseOfferingId,
    /** The CourseOffering's own curriculum term/semester number (e.g. 1, 3, 5) — distinguishes
     *  which cohort/term a session belongs to on resource-comparison views (Resource Timetable)
     *  that pool sessions from every active cohort together for one faculty/room row. */
    Integer termNumber,

    DayOfWeek dayOfWeek,
    Long termInstanceId,
    String termInstanceLabel,
    Boolean isActive,
    Instant createdAt,
    Instant updatedAt
) {}
