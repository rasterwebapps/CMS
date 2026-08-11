package com.cms.dto;

/** One independently-detected timetable placement/staffing constraint failure — several of
 *  these can be reported together by {@link com.cms.exception.TimetableConstraintViolationException}
 *  instead of surfacing only the first one found. */
public record ConstraintViolation(
    String code,
    String message
) {
}
