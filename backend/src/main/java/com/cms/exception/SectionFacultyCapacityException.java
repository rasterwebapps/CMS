package com.cms.exception;

import java.util.List;
import java.util.stream.Collectors;

import com.cms.dto.SectionFacultyCapacityFailure;

/** Thrown by {@code CourseOfferingSectionFacultyService#confirmSubstitutions} when one or more
 *  ticked Global Auto-Schedule substitution tips would put their substitute faculty over capacity
 *  -- collected across every ticked tip in one pass (not just the first) so the admin can see and
 *  fix every problem at once instead of a fix-one-resubmit loop. Distinct from the generic {@link
 *  TimetableConstraintViolationException} because the caller needs to attribute each failure back
 *  to the exact (courseOfferingId, facultyId) tip it came from, to offer a "Raise Cap" / "Reassign"
 *  action on that specific tip's own card -- the shared {@code ConstraintViolation(code, message)}
 *  record has no room for that without widening a contract every other violation site also flows
 *  through. {@code failures} is never empty when thrown; nothing is applied when this is thrown
 *  (see {@code confirmSubstitutions}'s validate-everything-then-apply structure). */
public class SectionFacultyCapacityException extends RuntimeException {

    private final List<SectionFacultyCapacityFailure> failures;

    public SectionFacultyCapacityException(List<SectionFacultyCapacityFailure> failures) {
        super(failures.stream().map(SectionFacultyCapacityFailure::message).collect(Collectors.joining("\n")));
        this.failures = failures;
    }

    public List<SectionFacultyCapacityFailure> getFailures() {
        return failures;
    }
}
