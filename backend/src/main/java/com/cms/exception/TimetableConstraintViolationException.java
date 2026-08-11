package com.cms.exception;

import java.util.List;
import java.util.stream.Collectors;

import com.cms.dto.ConstraintViolation;

/** Thrown by timetable placement/staffing when one or more independent constraint checks fail,
 *  carrying every violation found rather than only the first — lets the caller fix everything in
 *  one pass instead of a fix-one-resubmit loop. {@code violations} is never empty when thrown. */
public class TimetableConstraintViolationException extends RuntimeException {

    private final List<ConstraintViolation> violations;

    public TimetableConstraintViolationException(List<ConstraintViolation> violations) {
        super(violations.stream().map(ConstraintViolation::message).collect(Collectors.joining("; ")));
        this.violations = violations;
    }

    public List<ConstraintViolation> getViolations() {
        return violations;
    }
}
